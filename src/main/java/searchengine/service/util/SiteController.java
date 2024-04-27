package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupSettings;
import java.io.IOException;
import java.util.concurrent.Semaphore;

@RequiredArgsConstructor
public class SiteController {

    private final Semaphore semaphore = new Semaphore(1, true);

    private final JsoupSettings settings;

    public Document accessSite(String url) throws IOException {

        if (url == null) {
            return null;
        }

        Document pageContent = Jsoup.connect(url)
                .userAgent(settings.getUserAgent())
                .referrer(settings.getReferrer())
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(false)
                .timeout(10_000).get();
        try {
        Thread.sleep(500);
        } catch (InterruptedException e ) {
            e.printStackTrace();
        }
        finally {
            semaphore.release();
        }
        return pageContent;
    }
}
