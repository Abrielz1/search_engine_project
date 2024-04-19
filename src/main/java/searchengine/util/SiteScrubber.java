package searchengine.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
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
public class SiteScrubber {

    private static boolean scanningIsStopped = false;

    private SiteAccessController siteAccessController;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final Set<String> setSitesList;

    private URL page;

    private Site site; //?

    private URL startUrl; //?

    private final int maxDepth = 10;

    int depth = 0;

    private Set<URL> markedPages = new HashSet<>();

    private ConcurrentLinkedQueue<URL> queueToWrite = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<URL> queueToScan = new ConcurrentLinkedQueue<>();

    public SiteScrubber(SiteRepository siteRepository, PageRepository pageRepository, Set<String> setSitesList) {
        this.siteAccessController = new SiteAccessController(site, pageRepository);
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.setSitesList = setSitesList;
    }

    public void scan(List<String> urls) {


        List<URL> list = new ArrayList<>();

        for (String url : urls) {
            try {
                startUrl = new URL(url);
                list.add(startUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

        }

            ForkJoinPool pool  = new ForkJoinPool();;
            ArrayList<Thread> threads = new ArrayList<>();

        for (URL site : list) {
            threads.add(new Thread(() -> {
                pool.invoke(new Scrubber(site, list));

            }));
        }
        threads.forEach(Thread::start);

//            try {
//                startUrl = new URL(url);
//                Scrubber scrubber = new Scrubber(startUrl, list);
//                forkJoinPool.invoke(scrubber);
//
//            } catch (MalformedURLException e) {
//                throw new RuntimeException(e);
//            } finally {
//                forkJoinPool.shutdown();
//            }

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
                extractLinks(queueToScan.poll());
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

            pageRepository.save(page);
        }
    }
}
