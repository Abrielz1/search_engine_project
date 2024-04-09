package searchengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private boolean indexingInProcess = false;

    private final SitesList sites;

    private List<Site> sitesContainer;

    private final IndexRepository indexRepository;



    @Override
    public IndexingStaringResponseDTO getStartResponse() {

        String siteFish = "https://wh40k.lexicanum.com/wiki/Main_Page";

        IndexingStaringResponseDTO response = new IndexingStaringResponseDTO();

        System.out.println("Sites: " + "\n");

        System.out.println(sites.getSites());

        return null;
    }
}
