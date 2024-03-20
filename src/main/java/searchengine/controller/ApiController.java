package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.StatisticsResponse;
import searchengine.service.StatisticsService;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;

    @GetMapping("/statistics")
    @ResponseStatus
    public StatisticsResponse statistics() {

        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    @ResponseStatus(HttpStatus.OK)
    public Boolean startIndexing() {

        return false;
    }

    @GetMapping("/stopIndexing")
    @ResponseStatus(HttpStatus.OK)
    public Boolean stopIndexing() {

        return false;
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<String> searchWord(@RequestParam String query,
                           @Positive @RequestParam(defaultValue = "0") Integer from,
                           @PositiveOrZero @RequestParam(defaultValue = "10") Integer size) {

        PageRequest page = PageRequest.of(from / size, size);

        return new ArrayList<>();
    }

    @PostMapping("/indexPage")
    @ResponseStatus(HttpStatus.CREATED)
    public Boolean indexPage(@RequestBody String url) {

        return false;
    }
}
