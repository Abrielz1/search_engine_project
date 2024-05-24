package searchengine.service.search;

import searchengine.dto.page.SearchResponseDTO;

public interface SearchingService {
    SearchResponseDTO search(String query,
                             String site,
                             Integer from,
                             Integer size);
}
