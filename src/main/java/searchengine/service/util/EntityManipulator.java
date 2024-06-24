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
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.indexing.LemmaFinder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import static searchengine.model.enums.SiteStatus.INDEXING;

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

    private ForkJoinPool pool;

    public void checkSiteAndSavePageToDb(Document document,
                                         Site site,
                                         String path) {

        Optional<Site> siteFromDb = this.siteChecker(site.getUrl());

        if (siteFromDb.isPresent()) {
            siteFromDb.get().setLastError(null);
            siteFromDb.get().setStatusTime(LocalDateTime.now());

            siteRepository.saveAndFlush(siteFromDb.get());
        }

        Page page;
        synchronized (this) {
            page = this.checkPage(
                    document,
                    site,
                    path);

                this.savePageInBd(page);
        }

        if (page.getCode() < 400) {
            this.proceedLemmasAndIndexes(page);
        }
    }

    private Optional<Site> siteChecker(String url) {

        return siteRepository.findFirstByUrl(url);
    }

    private Page checkPage(Document document,
                           Site site,
                           String path) {

        return pageRepository.findFirstByPathAndSite(this.urlVerification(path,
                                                                          site),
                                                                          site)
                .orElseGet(() -> this.createPage(document, site, path));
    }

    private Page createPage(Document document,
                            Site site,
                            String path) {

        Page newPage = new Page();
        newPage.setCode(document.connection().response().statusCode());
        newPage.setPath(this.urlVerification(path, site));
        newPage.setSite(site);
        newPage.setContent((document.html()));

        return newPage;
    }

    public String urlVerification(String url,
                                  Site site) {

        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }

    private void savePageInBd(Page page) {

        pageRepository.saveAndFlush(page);
    }

    public boolean urlChecker(String url) throws IOException {

        if (!url.matches("https?://[\\w\\W]+")) {

            return false;
        }

        if (Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get()
                .connection()
                .response()
                .statusCode() == 404) {

            return false;
        }

        for (SiteConfig site
                : sitesList.getSites()) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }

        return false;
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
            siteToSave.setStatus(INDEXING);
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

    public String urlChecker(String url,
                             Site site) {

        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }

    public void proceedLemmasAndIndexes(Page page) {

        String pageText = Jsoup.clean(page.getContent(), Safelist.relaxed())
                .replaceAll("[Ёё]", "е")
                .trim();

        Map<String, Integer> lemmasWithRanks = lemmaFinder.collectLemmas(pageText);
        this.lemmasAndRanksManipulatorAndSaver(lemmasWithRanks,
                                               page);
    }

    public Site findSiteByUrl(String url) {

        return siteRepository.findAll()
                .stream()
                .filter(s -> s.getUrl().equals(url))
                .findFirst()
                .orElse(null);
    }

    private synchronized void lemmasAndRanksManipulatorAndSaver(Map<String,
                                                                Integer> lemmasWithRanks,
                                                                Page page) {

        Set<Lemma> lemmas = ConcurrentHashMap.newKeySet();
        Set<Index> indices = ConcurrentHashMap.newKeySet();

        lemmasWithRanks.forEach((lemma, rank) -> {
            if (SiteScrubber.isStopped) {
                return;
            }

            Lemma newLemma = this.createLemma(lemma,
                                              page);
            lemmas.add(newLemma);
            indices.add(this.createindex(newLemma,
                                         page,
                                         rank));
        });

        lemmaRepository.saveAllAndFlush(lemmas);
        indexRepository.saveAllAndFlush(indices);
    }

    private Lemma createLemma(String lemma,
                              Page page) {

        Lemma newLemma = new Lemma();
        Optional<Lemma> fromDbLemma = lemmaRepository.findFirstByLemma(lemma);

        if (fromDbLemma.isPresent()) {
            newLemma = fromDbLemma.get();
            newLemma.setFrequency(fromDbLemma.get().getFrequency() + 1);
        } else {
            newLemma.setLemma(lemma);
            newLemma.setSite(page.getSite());
            newLemma.setFrequency(1);
        }

        return newLemma;
    }

    private Index createindex(Lemma lemma,
                              Page page,
                              float rank) {

        Index newIndex = new Index();
        newIndex.setLemma(lemma);
        newIndex.setPage(page);
        newIndex.setRank(rank);

        return newIndex;
    }
}
