package searchengine.service.search;

import org.springframework.data.domain.PageRequest;
import searchengine.dto.page.SearchResponseDTO;

public interface SearchingService {
    SearchResponseDTO search(String query,
                             String site,
                             Integer from,
                             Integer size);
}
