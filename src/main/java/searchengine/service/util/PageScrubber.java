package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageScrubber {
    private final SiteRepository siteRepository;

    private final EntityManipulator manipulator;

    public void siteScrubber(String url) {

        try {

            Document document = Jsoup.connect(url).get();
            String tempUrl = url;
            tempUrl = url.endsWith("/") ? manipulator.removeLastDash(url) : tempUrl;

            Site siteFromDb = manipulator.findSiteByUrl(tempUrl);
            if (siteFromDb != null) {
                manipulator.checkSiteAndSavePageToDb(document, siteFromDb,
                        url.replace(siteFromDb.getUrl(), ""));

                log.info("сайт по url {} просканирован", url);
            }

        } catch (IOException ex) {
            String message = "Страницу по адресу: %s проиндексировать не удалось".formatted(url);
            manipulator.setFailedStateSite(message);
        }
    }

    private Site findSiteByPageURL(String url) {
        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (url.startsWith(site.getUrl())) {
                return site;
            }
        }
        return null;
    }
}
