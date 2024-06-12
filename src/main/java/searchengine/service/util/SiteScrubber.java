package searchengine.service.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupSettings;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;
import static searchengine.model.enums.SiteStatus.FAILED;

@Slf4j
public class SiteScrubber extends RecursiveAction {

    public volatile static boolean isStopped = true;

    private final Site site;

    private final String path;

    private final JsoupSettings settings;

    private final EntityManipulator manipulator;

    private final SiteController siteController;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    public SiteScrubber(Site site,
                        String path,
                        JsoupSettings settings,
                        SiteRepository siteRepository,
                        PageRepository pageRepository,
                        EntityManipulator manipulator) {

        this.site = site;
        this.path = path;
        this.settings = settings;
        this.siteController = new SiteController(siteRepository, settings);
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.manipulator = manipulator;
    }

    @Override
    protected void compute() {

        Set<SiteScrubber> threadPool = ConcurrentHashMap.newKeySet();

        try {

            if (pageRepository.existsByPathAndSite(path, site)
                    || isStopped) {
                return;
            }

            Document document = this.documentGetter();
            this.saverSiteAndPageByPath(document, site, path);
            Set<String> setUrlsToScan = this.getUrls(document);

            for (String urlToScan : setUrlsToScan) {
                threadPool.add(this.createSiteScrubberThread(urlToScan));
                Thread.sleep(2000);
            }

            ForkJoinTask.invokeAll(threadPool);

        } catch (Throwable e) {
            this.setErrorAndFailedStateToSite(e);
        }
    }

    private void saverSiteAndPageByPath(Document document,
                       Site site,
                       String path) {

        manipulator.checkSiteAndSavePageToDb(document,
                                             site,
                                             path);
    }

    private Document documentGetter() {

        Document document;
        try {
            document = siteController.accessSite(site.getUrl().concat(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return document;
    }

    private Set<String> getUrls(Document document) {

        return document.select("a[href]")
                .stream()
                .map(url -> url.absUrl("href"))
                .filter(this::isPathCorrect)
                .collect(Collectors.toSet());
    }

    private boolean isPathCorrect(String url) {

        if (!url.startsWith(site.getUrl())) {
            return false;
        }

        return !url.matches("[\\w\\W]+(\\.pdf|\\.PDF|\\.doc|\\.DOC" +
                "|\\.png|\\.PNG|\\.jpe?g|\\.JPE?G|\\.JPG|\\.GIF|\\.gif" +
                "|\\.php[\\W\\w]|#[\\w\\W]*|\\?[\\w\\W]+)$");
    }

    private SiteScrubber createSiteScrubberThread(String url) {

        return new SiteScrubber(site,
                manipulator.urlChecker(
                        url,
                        site),
                settings,
                siteRepository,
                pageRepository,
                manipulator);
    }

    private void setErrorAndFailedStateToSite(Throwable e) {

        Optional<Site> siteFromDb = siteRepository
                .findFirstByUrl(site.getUrl());

        if (siteFromDb.isPresent()) {
            siteFromDb.get().setStatus(FAILED);
            siteFromDb.get()
                    .setLastError(String.format("Произошла ошибка: %s при обработке страницы: %s текст ошибки: %s",
                        site.getUrl(),
                        path,
                        e.toString()
                    ));

            siteRepository.saveAndFlush(siteFromDb.get());
        }
    }
}

