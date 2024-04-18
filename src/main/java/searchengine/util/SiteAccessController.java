package searchengine.util;

import lombok.extern.slf4j.Slf4j;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

@Slf4j

public class SiteAccessController {

    private final PageRepository pageRepository;

    private final Semaphore semaphore;

    private final Site site;

    public SiteAccessController(Site site, PageRepository pageRepository) {
        this.semaphore = new Semaphore(1, true);
        this.pageRepository = pageRepository;
        this.site = site;
    }


    public String accessSite(URL url) throws InterruptedException, IOException {

        if (url == null) {
            return null;
        }

        semaphore.acquire();
        String content;

        try {

        log.info("thread is going to start work at" + System.currentTimeMillis() + " ms");

        content = new Scanner(url.openStream(), StandardCharsets.UTF_8)
                .useDelimiter("\\A").next();

        log.info("thread is going to sleep at" + System.currentTimeMillis() + " ms");

        Thread.sleep(500);

        log.info("thread is going to wake up at" + System.currentTimeMillis() + " ms");
        return content;

        } finally {
            this.unlock();
        }
    }

    private void unlock() {
        log.info("semaphore was released at " + System.currentTimeMillis() + " ms");
        semaphore.release();
    }
}
