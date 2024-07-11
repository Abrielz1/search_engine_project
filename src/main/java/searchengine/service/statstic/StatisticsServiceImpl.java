package searchengine.service.statstic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.indexing.IndexingService;
import searchengine.service.util.EntityManipulator;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static searchengine.model.enums.SiteStatus.INDEXING;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final EntityManipulator manipulator;

    private final SitesList sites;

    private final IndexingService indexingService;

    private final SiteRepository siteRepository;

    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    @Override
    @Transactional
    public StatisticsResponse getStatistics() {

        return responseManipulator(this.totalStatisticsManipulator(),
                                   this.detailedStatisticsItemManipulator(sites,
                                                                          this.totalStatisticsManipulator()));
    }

    private StatisticsResponse responseManipulator(TotalStatistics total,
                                                   List<DetailedStatisticsItem> statisticsItems) {

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(statisticsItems);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);

        return response;
    }

    private TotalStatistics totalStatisticsManipulator() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setPages(pageRepository.findAll().size());
        total.setLemmas(lemmaRepository.findAll().size());
        total.setIndexing(indexingService.isIndexing());

        return total;
    }

    private List<DetailedStatisticsItem> detailedStatisticsItemManipulator(SitesList sites,
                                                                           TotalStatistics total) {
        List<DetailedStatisticsItem> statisticsItems = new ArrayList<>();

        for (SiteConfig site : sites.getSites()) {

            Optional<Site> siteInDB = siteRepository.findFirstByUrl(
                    manipulator.removeLastDash(site.getUrl()));

            if (siteInDB.isEmpty()) {
                this.siteSaver(site);
            }

            siteInDB.ifPresent(siteToList -> statisticsItems.add(
                    this.createStatisticsData(siteToList, total)));
        }

        return statisticsItems;
    }

    private DetailedStatisticsItem createStatisticsData(Site site,
                                                        TotalStatistics total) {
        DetailedStatisticsItem data = new DetailedStatisticsItem();
        data.setUrl(site.getUrl());
        data.setName(site.getName());
        data.setStatus(site.getStatus().toString());
        data.setStatusTime(Timestamp.valueOf(site.getStatusTime()).getTime());
        data.setError(site.getLastError());
        data.setPages(total.getPages() + pageRepository.countPageBySite(site));
        data.setLemmas(total.getLemmas() + lemmaRepository.countLemmaBySite(site));

        return data;
    }

    private void siteSaver(SiteConfig site) {
        Site siteToSave = new Site();
        siteToSave.setUrl(manipulator.removeLastDash(site.getUrl()));
        siteToSave.setName(site.getName());
        siteToSave.setStatus(siteToSave.getStatus() == null ? INDEXING : siteToSave.getStatus());
        siteToSave.setLastError(null);
        siteToSave.setStatusTime(LocalDateTime.now());

        siteRepository.saveAndFlush(siteToSave);
    }
}
