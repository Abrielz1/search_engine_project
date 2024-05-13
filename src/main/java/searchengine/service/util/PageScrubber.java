package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageScrubber {

    private final EntityManipulator manipulator;

    public void siteScrubber(String url) {

        try {
            Document document = Jsoup.connect(url).get();
            String tempUrl = url;
            url = url.endsWith("/") ? tempUrl = manipulator.removeLastDash(url) : tempUrl;
            Site siteFromDb = manipulator.siteChecker(tempUrl);

            if (siteFromDb != null) {
                manipulator.checkSiteAndSavePageToDb(
                        document,
                        siteFromDb,
                        url.replace(siteFromDb.getUrl(), ""));
            }

        } catch (IOException ex) {
            String message = "Страницу " + url + " проиндексировать не удалось";
            manipulator.setFailedStateSite(url, message);
        }
    }
}
