package searchengine.service;

import searchengine.dto.indexing.IndexingPagingResponseDTO;
import searchengine.dto.indexing.IndexingStaringResponseDTO;
import searchengine.dto.indexing.IndexingStoppingResponseDTO;

public interface IndexingService {

    IndexingStaringResponseDTO getStartResponse();

    IndexingStoppingResponseDTO stopIndexingResponse();

    IndexingPagingResponseDTO startIndexPage(String url);

    boolean isIndexing();
}
