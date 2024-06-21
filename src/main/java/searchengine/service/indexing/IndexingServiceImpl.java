package searchengine.service.indexing;

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
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.util.EntityManipulator;
import searchengine.service.util.SiteScrubber;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import static searchengine.model.enums.SiteStatus.FAILED;
import static searchengine.model.enums.SiteStatus.INDEXED;
import static searchengine.model.enums.SiteStatus.INDEXING;

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

    private final EntityManipulator manipulator;

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

        log.info("Scanner is started now: " + LocalDateTime.now());
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

        log.info("Scanner is stopped now: " + LocalDateTime.now());
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
            if (manipulator.urlChecker(url)) {
                this.checkSite(url);
                response.setResult(true);
                new Thread(() -> this.siteScrubber(url)).start();
                log.info("Indexing of page: %s".formatted(url));
                return response;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        response.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");

        log.info("Indexing of page: %s failed".formatted(url));
        return response;
    }

    private void checkSite(String url) {

       String path = url.substring(0, url.indexOf(".ru") + 3);
       Site site = siteRepository.getSiteByUrl(path).orElse(null);

       if (site == null) {
          Site siteToSave = new Site();
           siteToSave.setStatus(INDEXING);
           siteToSave.setUrl(url);
           siteToSave.setName(path);
           siteToSave.setStatusTime(LocalDateTime.now());

           log.info("site was added to database");
           siteRepository.saveAndFlush(siteToSave);

          List<SiteConfig> list = sitesList.getSites();
           SiteConfig siteConfig = new SiteConfig();
           siteConfig.setName(path);
           siteConfig.setUrl(url);
          list.add(siteConfig);
          sitesList.setSites(list);

          log.info("site was added to list sites to index");
       }
    }

    public void siteScrubber(String url) {

        try {
            Document document = Jsoup.connect(url).get();
            String tempUrl = url;
            tempUrl = url.endsWith("/") ? manipulator.removeLastDash(url) : tempUrl;

            Site siteFromDb = manipulator.findSiteByUrl(tempUrl);

            if (siteFromDb != null) {
                manipulator.checkSiteAndSavePageToDb(document, siteFromDb,
                        url.replace(siteFromDb.getUrl(), ""));

                log.info("сайт по url {} просканирован", url);
            }
        } catch (IOException ex) {
            String message = "Страницу по адресу: %s проиндексировать не удалось".formatted(url);
            this.setFailedStateSite(message);
        }
    }

    private void beginIndexingSites() {

        SiteScrubber.isStopped = false;
        manipulator.clearDB();
        manipulator.siteSaver();
        pool = new ForkJoinPool();
        List<Thread> threads = new ArrayList<>();
        List<Site> siteList = siteRepository.findAll();

        for (Site site : siteList) {
            threads.add(new Thread(() -> {
                pool.invoke(new SiteScrubber(
                        site,
                        "",
                        settings,
                        siteRepository,
                        pageRepository,
                        manipulator));

                this.setSitesIndexedStatus(site);
                log.info("Job is done! Sites is indexed at time: " + LocalDateTime.now());
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

    @Override
    public boolean isIndexing() {

        if (pool == null) {
            return false;
        }

        return !pool.isQuiescent();
    }

    private void stopIndexingSites() {

        if (!isIndexing()) {
            log.info("indexing is not launched");
            return;
        }

        SiteScrubber.isStopped = true;
        pool.shutdown();
        this.setFailedStateSite("Индексация остановлена пользователем");
        log.info("Индексация остановлена!");
    }

    public void setFailedStateSite(String message) {

        List<Site> sites = siteRepository.findAll();

        try {
            if (pool.awaitTermination(3_000,
                    TimeUnit.MILLISECONDS)) {
                sites.forEach(site -> {
                    site.setStatus(FAILED);
                    site.setLastError(message);
                    siteRepository.saveAllAndFlush(sites);
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
            sites.forEach(site -> {
                site.setStatus(FAILED);
                site.setLastError(message);
                siteRepository.saveAllAndFlush(sites);
            });

        log.info("site index state failed try to relaunch indexing");
        SiteScrubber.isStopped = false;
    }
}

