package searchengine.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.config.JsoupSettings;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingPagingResponseDTO;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.dto.indexing.IndexingStoppingResponseDTO;
import searchengine.exception.exceptions.ObjectNotFoundException;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.util.SiteScrubber;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import static searchengine.model.enums.SiteStatus.FAILED;
import static searchengine.model.enums.SiteStatus.INDEXED;

@Slf4j
@Getter
@Setter
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private boolean indexingInProcess = false;

    private final JsoupSettings settings;

    private ForkJoinPool pool;

    private final SitesList sitesList;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    private final LemmaRepository lemmaRepository;

    @Override
    public IndexingStaringResponseDTO getStartResponse() {

        IndexingStaringResponseDTO response = new IndexingStaringResponseDTO();

        if (!isIndexing()) {
            response.setResult(true);
            this.beginIndexingSites();
        } else {
            response.setError("Индексация уже запущена");
            response.setResult(false);
        }

        return response;
    }

    @Override
    public IndexingStoppingResponseDTO stopIndexingResponse() {

        IndexingStoppingResponseDTO response = new IndexingStoppingResponseDTO();
        this.stopIndexingSites();

        if (SiteScrubber.isStopped) {
            response.setResult(true);
            return response;
        } else if (!isIndexing()) {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }

        return response;
    }

    @Override
    public IndexingPagingResponseDTO startIndexPage(String url) {
        IndexingPagingResponseDTO response = new IndexingPagingResponseDTO();

        if (!StringUtils.hasText(url)) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }

        try {
            if (this.urlChecker(url)) {
                response.setResult(true);
                new Thread(() -> this.siteScrubber(url)).start();
            } else {
                response.setResult(false);
                response.setError("Индексация не запущена");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        response.setError("""
                            Данная страница находится за пределами сайтов,
                            указанных в конфигурационном файле
                            """);

        return response;
    }

    private void beginIndexingSites() {
        SiteScrubber.isStopped = false;
        this.clearDB();
        this.siteSaver();
        pool = new ForkJoinPool();
        List<Thread> threads = new ArrayList<>();
        List<Site> siteList = siteRepository.findAll();

        for (Site site : siteList) {
            threads.add(new Thread(() -> {
                pool.invoke(new SiteScrubber(site,
                        "",
                        settings,
                        siteRepository,
                        pageRepository));

                this.setSitesIndexedStatus(site);
            }));
        }
        threads.forEach(Thread::start);
    }

    private void setSitesIndexedStatus(Site site) {
        if (SiteScrubber.isStopped) {
            return;
        }

        Optional<Site> siteFromDB = siteRepository.findFirstByUrl(site.getUrl());
        if (siteFromDB.isPresent() && !siteFromDB.get().getStatus().equals(FAILED)) {
            siteFromDB.get().setStatus(INDEXED);
            siteFromDB.get().setStatusTime(LocalDateTime.now());
            siteFromDB.get().setLastError(null);
            siteRepository.saveAndFlush(siteFromDB.get());
        }
    }

    public boolean isIndexing() {
        if (pool == null) {
            return false;
        }

        return !pool.isQuiescent();
    }

    private void clearDB() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    private void siteSaver() {
        List<Site> sites = new ArrayList<>();
        for (SiteConfig site : sitesList.getSites()) {
            Site siteToSave = new Site();
            siteToSave.setUrl(site.getUrl());
            siteToSave.setName(site.getName());
            siteToSave.setStatus(INDEXED);
            siteToSave.setLastError(null);
            siteToSave.setStatusTime(LocalDateTime.now());
            sites.add(siteToSave);
        }
        siteRepository.saveAllAndFlush(sites);
    }

    private void stopIndexingSites() {
        if (!isIndexing()) {
            return;
        }

        SiteScrubber.isStopped = true;
        pool.shutdown();
        this.setFailedState();
        log.info("Индексация остановлена!");
    }

    private void setFailedState() {
        List<Site> sitesList = siteRepository.findAll();
        sitesList.forEach(site -> {
            site.setStatus(FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Индексация остановлена пользователем");

            log.info("Список сайтов не прошедших индексацию сохранён!");
            siteRepository.saveAllAndFlush(sitesList);
        });
    }

    private boolean urlChecker(String url) throws IOException {

        String regex = "https://[\\w\\W]+";

        if (!url.matches(regex))
            return false;

        if (Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get()
                .connection()
                .response()
                .statusCode() == 404) {
            return false;
        }

        for (SiteConfig site : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }

        return false;
    }

    private void siteScrubber(String url) {

        try {
            Document document = Jsoup.connect(url).get();
            Site siteFromDb = this.siteChecker(url);

            if (siteFromDb != null) {
                this.checkSiteAndSavePageToDb(
                        document,
                        siteFromDb,
                        url.replace(siteFromDb.getUrl(), ""));
            }

        } catch (IOException ex) {
            String message = "Страницу " + url + " проиндексировать не удалось";
            this.setFailedStateSite(url, message);
        }
    }

    private Site siteChecker(String url) {
        return siteRepository.findFirstByUrl(url).orElseThrow(() -> {

            log.error("По ссылке: %s сайта нет!".formatted(url));
            return new ObjectNotFoundException("По ссылке: %s сайта нет!".formatted(url));
        });
    }

    private void setFailedStateSite(String url, String message) {
        Site siteFromDb = this.siteChecker(url);
        siteFromDb.setStatus(FAILED);
        siteFromDb.setLastError(message);
        siteFromDb.setStatusTime(LocalDateTime.now());

        siteRepository.saveAndFlush(siteFromDb);
    }

    private void checkSiteAndSavePageToDb(Document document, Site site, String path) {
        Site siteFromDb = this.siteChecker(site.getUrl());

        if (siteFromDb != null) {
            siteFromDb.setLastError(null);
            siteFromDb.setStatusTime(LocalDateTime.now());
            siteRepository.saveAndFlush(siteFromDb);
        }

        Page page = checkPage(document, site, path);
        if (page.getCode() < 220) {
            this.savePageInBd(page);
        }
    }

    private synchronized Page checkPage(Document document, Site site, String path) {
        log.error("стрвницы нет!");
        return pageRepository.findFirstByPathAndSite(this.urlVerification(path, site), site).orElseGet(()
                -> this.createPage(document, site, path));
    }

    private Page createPage(Document document, Site site, String path) {
        Page newPage = new Page();
        newPage.setCode(document.connection().response().statusCode());
        newPage.setPath(this.urlVerification(path, site));
        newPage.setSite(site);
        newPage.setContent(this.siteHtmlTagsCleaner(document.html()));

        return newPage;
    }

    private String urlVerification(String url, Site site) {
        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }

    private synchronized void savePageInBd(Page page) {
        pageRepository.saveAndFlush(page);
    }

    private String siteHtmlTagsCleaner(String html) {
        return Jsoup.parse(html).text();
    }
}

