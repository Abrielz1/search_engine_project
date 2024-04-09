package searchengine.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class SiteIndexer {

    private SiteAccessController siteAccessController;

    private URL startUrl;

    private static final int allAvailableCores = Runtime.getRuntime().availableProcessors();

    private static boolean isIndexingProcessingStopped = false;

    private long processStart = System.currentTimeMillis();

    private short startIndexDepth;

    private short endIndexDepth = 10;

    private long processStop;

    private SiteRepository siteRepository;

    private PageRepository pageRepository;

    private Site site;

    ExecutorService executor;

    private void stopIndexing() {
        isIndexingProcessingStopped = true;
    }

    public SiteIndexer(SiteRepository siteRepository, PageRepository pageRepository, Site site, URL startUrl) throws MalformedURLException {
        siteAccessController = new SiteAccessController(site, pageRepository);
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.site = site;
        startUrl =  new URL(site.getUrl());
    }
}
