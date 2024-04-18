package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanTask {

    private SiteRepository siteRepository;

    private PageRepository pageRepository;

    public void scan() {

        new ForkJoinPool().invoke(new ScanSite(""));

    }

    public List<String> scrub() {

        String rootUrlPart = "";

        List<String> result = new ArrayList<>();


        Document site;
        try {
            site = Jsoup.connect(rootUrlPart).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements element = site.select("a[href]");

        for (Element e: element) {

            String urlToGet = e.attr("href");

            if (urlToGet.contains("#") || urlToGet.endsWith("pdf") || urlToGet.endsWith("jpg")) {
                continue;
            }

            if (urlToGet.endsWith("/")) {
                urlToGet = urlToGet.substring(0, urlToGet.length() - 1);
            }

            if (urlToGet.startsWith("h") || !urlToGet.startsWith("/")) {
                continue;
            }

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(rootUrlPart).append(urlToGet);

            result.add(stringBuffer.toString());
        }

        return result;
    }


    @RequiredArgsConstructor
    public class ScanSite extends RecursiveTask<Void>{

       // private final ArrayList<Site> sitesList;

        private String url;


        public ScanSite(String url) {
            this.url = url;
        }

        List<String> strings = new ArrayList<>();

        /*
                   List<String> sitesUrlsList = sites.getSites()
                    .stream()
                    .map(Site::getUrl)
                    .toList();

            List<ScanSite> scanTasks = new ArrayList<>();

             for(String s: sitesUrlsList) {

                 ScanSite task = new ScanSite(s);
                 scanTasks.add(task);
                 strings.add(s);
                 task.fork();
             }




       */

        @Override
        protected Void compute() {


            // todo: грохнуть в бд сайты и страницы например deleteAllSitesAndAllPages(sitesUrlsList);
            // todo: создать создавалку сайтов по урлу createSites(sitesUrlsList) те создаем и пихаем в бд

            return null;
        }


    }
}
