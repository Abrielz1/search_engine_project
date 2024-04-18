package searchengine.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class SiteScrubber {

    private static boolean scanningIsStopped = false;

    private SiteAccessController siteAccessController;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private URL page;

    private final Set<String> setSitesList;

    private Site site; //?

    private URL startUrl; //?

    private final int maxDepth = 10;

    int depth = 0;

    private Set<URL> markedPages = new HashSet<>();

    private ConcurrentLinkedQueue<URL> queueToWrite = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<URL> queueToScan = new ConcurrentLinkedQueue<>();

    public void scan(List<String> urls) {

        for (String url : urls) {

        List<URL> list = new ArrayList<>();
            ForkJoinPool forkJoinPool  = new ForkJoinPool();;
            try {
                startUrl = new URL(url);
                Scrubber scrubber = new Scrubber(startUrl, list);
                forkJoinPool.invoke(scrubber);

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } finally {
                forkJoinPool.shutdown();
            }
        }
    }

    @RequiredArgsConstructor
    private class Scrubber extends RecursiveTask<Void> {

        private URL page;

        List<URL> site;

        public Scrubber(URL page, List<URL> site) {
            this.page = page;
            this.site = site;
        }

        @Override
        protected Void compute() {

            List<Scrubber> tasks = new ArrayList<>();

            scanningIsStopped = false;
            queueToScan.offer(startUrl);

            while (!queueToScan.isEmpty() || maxDepth <= depth || scanningIsStopped) {

                Scrubber task = new Scrubber(queueToScan.poll(), site);
                tasks.add(task);
                task.fork();

                for (Scrubber t : tasks) {
                    t.join();
                }

                while (!queueToScan.isEmpty()) {


                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // todo: if(scanningIsStopped) написать реализацию неудачи обхода и заталкивание этого в бд
                }

                if (!task.isCompletedNormally()) {
                    // todo: if(scanningIsStopped) написать реализацию неудачи обхода и заталкивание этого в бд//
                }

                queueToScan.addAll(queueToWrite);
                queueToWrite.clear();

                depth++;
            }

            // todo: обновить сайт в бд и поставить Site.SiteStatus.INDEXED
            return null;
        }
    }

    private void extractLinks(URL pageUrl) {

        if (scanningIsStopped) {
            return;
        }

        List<URL> result = new ArrayList<>();
        String content;
        try {
           content = siteAccessController.accessSite(pageUrl);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (content == null) {
            return;
        }

        Page page = new Page();
        page.setSite(site);
        page.setPath(pageUrl.getPath());
        page.setCode(200);
        page.setContent(content);

        Document document = Jsoup.parse(content);
        System.gc();
        Elements elements = document.select("a[href]");

        for (Element e: elements) {
            String link = e.attr("href");

            if (link.contains("#") || link.endsWith("pdf") || link.endsWith("jpg") || link.endsWith("png")) {
                continue;
            }

            if (link.endsWith("/")) {
                link = link.substring(0, link.length() - 1);
            }

            URL absoluteURL;
            try {
                absoluteURL = new URL(link);
            } catch (MalformedURLException ex) {
                continue;
            }

            if (!absoluteURL.equals(startUrl.getHost())) {
                continue;
            }

            synchronized (markedPages) {
                if (markedPages.contains(absoluteURL)) {
                    continue;
                }
                markedPages.add(absoluteURL);
            }

            queueToWrite.add(absoluteURL);
        }
    }
}
