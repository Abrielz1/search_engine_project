package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingPagingResponseDTO;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.dto.indexing.IndexingStoppingResponseDTO;
import searchengine.dto.page.SearchResponseDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.service.indexing.IndexingService;
import searchengine.service.search.SearchingService;
import searchengine.service.stitstic.StatisticsService;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final SearchingService searchingService;

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResponse statistics() {

        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingStaringResponseDTO startIndexing() {

        return indexingService.getStartResponse();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingStoppingResponseDTO stopIndexing() {

        return indexingService.stopIndexingResponse();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.CREATED)
    public IndexingPagingResponseDTO indexPage(@RequestParam String url) {

        return indexingService.startIndexPage(url);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponseDTO search(
                                   @RequestParam String query,
                                   @RequestParam(required = false) String site,
                                   @Positive @RequestParam(defaultValue = "0") Integer from,
                                   @PositiveOrZero @RequestParam(defaultValue = "20") Integer size) {

        return searchingService.search(query.replaceAll("[Ёё]]", "е"), site, from, size);
    }
}
