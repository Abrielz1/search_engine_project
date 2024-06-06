package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupSettings;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import static searchengine.model.enums.SiteStatus.FAILED;

@Slf4j
@RequiredArgsConstructor
public class SiteController {

    private final Semaphore semaphore = new Semaphore(1, true);

    private final SiteRepository siteRepository;

    private final JsoupSettings settings;

    public Document accessSite(String url) throws IOException {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (url == null) {
            return null;
        }

        Document pageContent = Jsoup.connect(url)
                .userAgent(settings.getUserAgent())
                .referrer(settings.getReferrer())
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .followRedirects(false)
                .timeout(10_000)
                .get();

        try {
        Thread.sleep(1000);
        } catch (Exception exception) {
            exception.printStackTrace();
            this.setFailedStatus(url, exception);
        }
        finally {
            semaphore.release();
        }

        return pageContent;
    }

    private void setFailedStatus(String url,
                                 Exception exception) {

        Optional<Site> siteFromBd = siteRepository.findFirstByUrl(url);
        if (siteFromBd.isPresent()) {
            siteFromBd.get().setStatusTime(LocalDateTime.now());
            siteFromBd.get().setLastError(String.format("При индексации сайта: %s возникла ошибка: %s", url, exception.toString()));
            siteFromBd.get().setStatus(FAILED);

            siteRepository.saveAndFlush(siteFromBd.get());
            log.info("сайт %s со статусом FAILED сохранён".formatted(url));
        }
    }
}
