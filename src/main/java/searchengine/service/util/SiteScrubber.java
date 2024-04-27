package searchengine.service.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.JsoupSettings;
import searchengine.exception.exceptions.ObjectNotFoundException;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@Slf4j
public class SiteScrubber extends RecursiveAction {

    public volatile static boolean isStopped = true;

    private final Site site;

    private final String path;

    private final JsoupSettings settings;

    public SiteScrubber(Site site, String path, JsoupSettings settings, SiteRepository siteRepository, PageRepository pageRepository) {
        this.site = site;
        this.path = path;
        this.settings = settings;
        this.siteController = new SiteController(settings);
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    SiteController siteController;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    @Override
    protected void compute() {

        if (pageRepository.existsByPathAndSite(path, site) || isStopped) {
            return;
        }

        Document document = this.documentGetter();
        Set<SiteScrubber> threadPool = ConcurrentHashMap.newKeySet();
        Set<String> setUrlsToScan = this.getUrls(document);
        for (String url : setUrlsToScan) {
            threadPool.add(this.createSiteScrubberThread(url));
        }
        ForkJoinTask.invokeAll(threadPool);
    }

    private Document documentGetter() {
        Document document;
        try {
            document = siteController.accessSite(site.getUrl().concat(path));
            this.siteAndPageSaverToDb(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    private void siteAndPageSaverToDb(Document document) {

        Site siteInDb = siteChecker(site.getUrl());
        siteInDb.setLastError(null);
        siteInDb.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteInDb);

        Page page = checkerPageInDb(document);

        if (page.getCode() < 220) {
            pageRepository.saveAndFlush(page);
        }
    }

    private Site siteChecker(String url) {
        return siteRepository.findFirstByUrl(url).orElseThrow(() ->
                new ObjectNotFoundException("Такого сайта нет!"));
    }

    private Page createPage(Document document, Site site, String path) {
        Page newPage = new Page();
        newPage.setCode(document.connection().response().statusCode());
        newPage.setPath(this.urlChecker(path));
        newPage.setSite(site);
        newPage.setContent(document.html());

        return newPage;
    }

    private synchronized Page checkerPageInDb(Document document) {

          return pageRepository.findFirstByPathAndSite(this.urlChecker(path), site).orElseGet(()
                    -> createPage(document, site, path));
    }

    private Set<String> getUrls(Document document) {
        Elements urlElement = document.select("a[href]");

        return urlElement.stream()
                .map(url -> url.absUrl("href"))
                .filter(this::isPathCorrect)
                .collect(Collectors.toSet());
    }

    private boolean isPathCorrect(String url) {
        if (!url.startsWith(site.getUrl())) {
            return false;
        }

        return !url.matches("[\\w\\W]+(\\.pdf|\\.PDF|\\.doc|\\.DOC" +
                "|\\.png|\\.PNG|\\.jpe?g|\\.JPE?G|\\.JPG|\\.Gif|\\.gif" +
                "|\\.php[\\W\\w]|#[\\w\\W]*|\\?[\\w\\W]+)$");
    }

    private SiteScrubber createSiteScrubberThread(String url) {

        String path = this.urlChecker(url);
        return new SiteScrubber(site,
                path,
                settings,
                siteRepository,
                pageRepository);
    }

    private String urlChecker(String url) {
        return url.equals(site.getUrl()) ? "/"
                : url.replace(site.getUrl(), "");
    }
}
