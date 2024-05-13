package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
import searchengine.service.IndexingService;
import searchengine.service.StatisticsService;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

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

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<SearchResponseDTO> searchWord(
                                   @RequestParam String query,
                                   @RequestParam(required = false) String site,
                                   @Positive @RequestParam(defaultValue = "0") Integer from,
                                   @PositiveOrZero @RequestParam(defaultValue = "20") Integer size) {

        PageRequest page = PageRequest.of(from / size, size);

        return new ArrayList<>();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.CREATED)
    public IndexingPagingResponseDTO indexPage(@RequestParam String url) {

        return null;
    }
}
