package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageScrubber {

    private final EntityManipulator manipulator;

    public void siteScrubber(String url) {

        try {

            Document document = Jsoup.connect(url).get();
            String tempUrl = url;
            tempUrl = url.endsWith("/") ? manipulator.removeLastDash(url) : tempUrl;

            Optional<Site> siteFromDb = manipulator.findSiteByUrl(tempUrl);
            siteFromDb.ifPresent(site -> manipulator.checkSiteAndSavePageToDb(
                    document,
                    site,
                    url.replace(site.getUrl(), "")));

            if (siteFromDb.isEmpty()) {
                Site siteToCreate = new Site();

                String name = siteFromDb.get().getName() != null ?
                        siteFromDb.get().getName() : "";

                siteToCreate.setLastError(null);
                siteToCreate.setStatusTime(LocalDateTime.now());
                siteToCreate.setUrl(tempUrl);
                siteToCreate.setName(name);

                manipulator.createPage(document, siteToCreate, tempUrl);
            }

        } catch (IOException ex) {
            String message = "Страницу по адресу: %s проиндексировать не удалось".formatted(url);
            manipulator.setFailedStateSite(message);
        }
    }
}
