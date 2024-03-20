package searchengine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.StatisticsResponse;
import searchengine.service.StatisticsService;

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
}
