package searchengine.service.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.exception.exceptions.ObjectNotFoundException;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.LemmaFinder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static searchengine.model.enums.SiteStatus.FAILED;
import static searchengine.model.enums.SiteStatus.INDEXED;

@Slf4j
@Getter
@Setter
@Service
@RequiredArgsConstructor
public class EntityManipulator {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SitesList sitesList;

    private final LemmaFinder lemmaFinder;

    public void setFailedStateSite(String url, String message) {

        Site siteFromDb = this.siteChecker(url);
        siteFromDb.setStatus(FAILED);
        siteFromDb.setLastError(message);
        siteFromDb.setStatusTime(LocalDateTime.now());

        siteRepository.saveAndFlush(siteFromDb);
    }

    public Site siteChecker(String url) {
        return siteRepository.findFirstByUrl(url).orElseThrow(() -> {

            log.error("По ссылке: %s сайта нет!".formatted(url));
            return new ObjectNotFoundException("По ссылке: %s сайта нет!".formatted(url));
        });
    }

    public void checkSiteAndSavePageToDb(Document document, Site site, String path) {
        Site siteFromDb = this.siteChecker(site.getUrl());

        if (siteFromDb != null) {
            siteFromDb.setLastError(null);
            siteFromDb.setStatusTime(LocalDateTime.now());
            siteRepository.saveAndFlush(siteFromDb);
        }

        Page page = checkPage(document, site, path);
        if (page.getCode() < 220) {
            this.savePageInBd(page);
            this.proceedLemmasAndIndexes(page);
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

    public String urlVerification(String url, Site site) {
        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }

    private synchronized void savePageInBd(Page page) {
        pageRepository.saveAndFlush(page);
    }

    private String siteHtmlTagsCleaner(String html) {
        return Jsoup.parse(html).text();
    }

    public boolean urlChecker(String url) throws IOException {

        String regex = "https?://[\\w\\W]+";

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

    public void setFailedState() {
        List<Site> sitesList = siteRepository.findAll();
        sitesList.forEach(site -> {
            site.setStatus(FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Индексация остановлена пользователем");

            log.info("Список сайтов не прошедших индексацию сохранён!");
            siteRepository.saveAllAndFlush(sitesList);
        });
    }

    public void clearDB() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
    }

    public void siteSaver() {
        List<Site> sites = new ArrayList<>();
        for (SiteConfig site : sitesList.getSites()) {
            Site siteToSave = new Site();
            siteToSave.setUrl(this.removeLastDash(site.getUrl()));
            siteToSave.setName(site.getName());
            siteToSave.setStatus(INDEXED);
            siteToSave.setLastError(null);
            siteToSave.setStatusTime(LocalDateTime.now());
            sites.add(siteToSave);
        }
        siteRepository.saveAllAndFlush(sites);
    }

    public String removeLastDash(String url) {
        return url.trim().endsWith("/") ?
                url.substring(0, url.length() - 1) : url;
    }


    public String urlChecker(String url, Site  site) {
        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }

    public void proceedLemmasAndIndexes(Page page) {

    String pageText = Jsoup.clean(page.getContent(), Safelist.relaxed())
            .replaceAll("[Ёё]", "е").trim();

        Map<String, Integer> lemmasWithRanks = lemmaFinder.collectLemmas(pageText);
        this.lemmasAndRanksManipulatorAndSaver(lemmasWithRanks, page);
    }

    private synchronized void lemmasAndRanksManipulatorAndSaver(Map<String, Integer> lemmasWithRanks,
                                                                Page page) {
        Set<Lemma> lemmas = ConcurrentHashMap.newKeySet();
        Set<Index> indices = ConcurrentHashMap.newKeySet();

        lemmasWithRanks.forEach((lemma, rank) -> {
            if (SiteScrubber.isStopped) {
                return;
            }

            Lemma newLemma = this.createLemma(lemma, page);
            lemmas.add(newLemma);
            indices.add(this.createindex(newLemma, page, rank));
        });

        lemmaRepository.saveAllAndFlush(lemmas);
        indexRepository.saveAllAndFlush(indices);
    }

    private Lemma createLemma(String lemma, Page page) {
        Lemma newLemma;
        Optional<Lemma> optLemma = lemmaRepository.findFirstByLemma(lemma);
        if (optLemma.isPresent()) {
            newLemma = optLemma.get();
            newLemma.setFrequency(optLemma.get().getFrequency() + 1);
        } else {
            newLemma = new Lemma();
            newLemma.setLemma(lemma);
            newLemma.setSite(page.getSite());
            newLemma.setFrequency(1);
        }
        return newLemma;
    }

    private Index createindex(Lemma lemma, Page page, float rank) {
        Index newIndex = new Index();
        newIndex.setLemma(lemma);
        newIndex.setPage(page);
        newIndex.setRank(rank);

        return newIndex;
    }
}
