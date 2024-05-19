package searchengine.service.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.config.JsoupSettings;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingPagingResponseDTO;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.dto.indexing.IndexingStoppingResponseDTO;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.util.EntityManipulator;
import searchengine.service.util.PageScrubber;
import searchengine.service.util.SiteScrubber;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import static searchengine.model.enums.SiteStatus.FAILED;
import static searchengine.model.enums.SiteStatus.INDEXED;

@Slf4j
@Getter
@Setter
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private boolean indexingInProcess = false;

    private final JsoupSettings settings;

    private ForkJoinPool pool;

    private final SitesList sitesList;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    private final LemmaRepository lemmaRepository;

    private final PageScrubber pageScrubber;

    private final EntityManipulator manipulator;

    @Override
    public IndexingStaringResponseDTO getStartResponse() {

        IndexingStaringResponseDTO response = new IndexingStaringResponseDTO();

        if (!isIndexing()) {
            response.setResult(true);
            this.beginIndexingSites();
        } else {
            response.setError("Индексация уже запущена");
            response.setResult(false);
        }

        return response;
    }

    @Override
    public IndexingStoppingResponseDTO stopIndexingResponse() {

        IndexingStoppingResponseDTO response = new IndexingStoppingResponseDTO();
        this.stopIndexingSites();

        if (SiteScrubber.isStopped) {
            response.setResult(true);
            return response;
        } else if (!isIndexing()) {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }

        return response;
    }

    @Override
    public IndexingPagingResponseDTO startIndexPage(String url) {
        IndexingPagingResponseDTO response = new IndexingPagingResponseDTO();

        if (!StringUtils.hasText(url)) {
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }

        try {
            if (manipulator.urlChecker(url)) {
                response.setResult(true);
              new  Thread(() -> pageScrubber.siteScrubber(url)).start();

            } else {
                response.setResult(false);
                response.setError("Индексация не запущена");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            response.setError("Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        }

        return response;
    }

    private void beginIndexingSites() {
        SiteScrubber.isStopped = false;
        manipulator.clearDB();
        manipulator.siteSaver();
        pool = new ForkJoinPool();
        List<Thread> threads = new ArrayList<>();
        List<Site> siteList = siteRepository.findAll();

        for (Site site : siteList) {
            threads.add(new Thread(() -> {
                pool.invoke(new SiteScrubber(site,
                        "",
                        settings,
                        siteRepository,
                        pageRepository,
                        manipulator));

                this.setSitesIndexedStatus(site);
            }));
        }
        threads.forEach(Thread::start);
    }

    private void setSitesIndexedStatus(Site site) {
        if (SiteScrubber.isStopped) {
            return;
        }

        Optional<Site> siteFromDB = siteRepository.findFirstByUrl(site.getUrl());
        if (siteFromDB.isPresent() && !siteFromDB.get().getStatus().equals(FAILED)) {
            siteFromDB.get().setStatus(INDEXED);
            siteFromDB.get().setStatusTime(LocalDateTime.now());
            siteFromDB.get().setLastError(null);
            siteRepository.saveAndFlush(siteFromDB.get());
        }
    }

    @Override
    public boolean isIndexing() {
        if (pool == null) {
            return false;
        }

        return !pool.isQuiescent();
    }

    private void stopIndexingSites() {
        if (!isIndexing()) {
            return;
        }

        SiteScrubber.isStopped = true;
        pool.shutdown();
        manipulator.setFailedState();
        log.info("Индексация остановлена!");
    }
}

