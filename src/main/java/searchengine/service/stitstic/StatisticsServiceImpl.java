package searchengine.service.stitstic;

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
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static searchengine.model.enums.SiteStatus.INDEXED;

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
    public StatisticsResponse getStatistics() {

        return responseManipulator(this.totalStatisticsManipulator(),
                this.detailedStatisticsItemManipulator(sites, this.totalStatisticsManipulator()));
    }

    private TotalStatistics totalStatisticsManipulator() {
        TotalStatistics total = new TotalStatistics();

        total.setSites(siteRepository.findAll().size());
        total.setPages(pageRepository.findAll().size());
        total.setLemmas(lemmaRepository.findAll().size());
        total.setIndexing(indexingService.isIndexing());
        return total;
    }

    private List<DetailedStatisticsItem> detailedStatisticsItemManipulator(SitesList sites, TotalStatistics total) {
        List<DetailedStatisticsItem> statisticsItems = new ArrayList<>();

        for (SiteConfig site : sites.getSites()) {

            Optional<Site> siteInDB = siteRepository.findFirstByUrl(manipulator.removeLastDash(site.getUrl()));

            if (siteInDB.isEmpty()) {
                this.siteSaver(site);
            }

            siteInDB.ifPresent(siteToList -> statisticsItems.add(
                    this.createStatisticsData(siteToList, total)));
        }

        return statisticsItems;
    }

    private StatisticsResponse responseManipulator(TotalStatistics total,
                                                   List<DetailedStatisticsItem> statisticsItems) {
        StatisticsResponse response = new StatisticsResponse();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(statisticsItems);

        response.setResult(true);
        response.setStatistics(data);

        return response;
    }

    private DetailedStatisticsItem createStatisticsData(Site site, TotalStatistics total) {
        DetailedStatisticsItem data = new DetailedStatisticsItem();
        data.setUrl(site.getUrl());
        data.setName(site.getName());
        data.setStatus(site.getStatus().toString());
        data.setStatusTime(site.getStatusTime().get(ChronoField.MILLI_OF_SECOND));
        data.setError(site.getLastError());
        data.setPages(total.getPages());
        data.setLemmas(total.getLemmas());
        return data;
    }

    private void siteSaver(SiteConfig site) {
        Site siteToSave = new Site();
        siteToSave.setUrl(manipulator.removeLastDash(site.getUrl()));
        siteToSave.setName(site.getName());
        siteToSave.setStatus(INDEXED);
        siteToSave.setLastError(null);
        siteToSave.setStatusTime(LocalDateTime.now());

        siteRepository.saveAndFlush(siteToSave);
    }
}
