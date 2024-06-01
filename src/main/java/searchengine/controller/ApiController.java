package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

@Slf4j
@Validated
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

        log.info("\nStatistics  was send via controller at time: "
                + LocalDateTime.now() + "\n");

        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingStaringResponseDTO startIndexing() {

        log.info("\nIndex  was started via controller at time: "
                + LocalDateTime.now() + "\n");

        return indexingService.getStartResponse();
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public IndexingStoppingResponseDTO stopIndexing() {

        log.info("\nIndex  was stopped via controller at time: "
                + LocalDateTime.now() + "\n");

        return indexingService.stopIndexingResponse();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.CREATED)
    public IndexingPagingResponseDTO indexPage(@RequestParam String url) {

        return indexingService.startIndexPage(url);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponseDTO search(@NotBlank @RequestParam String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam(defaultValue = "0") Integer from,
                                    @RequestParam (defaultValue = "20") Integer size) {

        log.info("\nQuery with: %s was sent via controller at time: ".formatted(query)
                + LocalDateTime.now() + "\n");

        return searchingService.search(query.replaceAll("[Ёё]]", "е"),
                                       site,
                                       from,
                                       size);
    }
}
