package searchengine.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.model.Site;
import searchengine.model.enums.SiteStatus;
import searchengine.repository.IndexRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.util.ScanTask;
import searchengine.util.SiteAccessController;
import searchengine.util.SiteScrubber;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private boolean indexingInProcess = false;

    private final SiteScrubber scanTask;

    private ArrayList<Site> sitesM;

    private final SitesList sites;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    @Override
    public IndexingStaringResponseDTO getStartResponse() {

        IndexingStaringResponseDTO responseDTO = new IndexingStaringResponseDTO();
        responseDTO.setResult(indexingInProcess);
        responseDTO.setError(indexingInProcess ? "Индексация уже запущена": null);

        if (!indexingInProcess) {
            indexingInProcess = true;
            this.startIndexing();
        }

        return responseDTO;
    }

    public void startIndexing() {

        sitesM = new ArrayList<>();

        List<String> siteUrls = sites.getSites().stream()
                .map(searchengine.config.Site::getUrl)
                .toList();

        scanTask.scan(siteUrls);
    }
}

