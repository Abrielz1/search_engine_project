package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.page.PageDataDTO;
import searchengine.dto.page.SearchResponseDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.indexing.LemmaFinder;
import searchengine.service.util.EntityManipulator;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {

    private final SitesList sitesList;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final EntityManipulator manipulator;

    private final LemmaFinder lemmaFinder;

    @Override
    public SearchResponseDTO search(String query,
                                    String site,
                                    PageRequest page,
                                    Integer from,
                                    Integer size) {

        SearchResponseDTO newSearchResponseDTO = new SearchResponseDTO();

        Set<String> lemmasSet = lemmaFinder.getLemmaSet(query);

        if (site != null && !this.siteChecker(site)) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Указанная страница не найдена");
            return newSearchResponseDTO;
        }

        if (lemmasSet.isEmpty()) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Задан пустой поисковый запрос");
        }

        List<Lemma> lemmaList = this.findListOfLemmasInDbAversSorted(lemmasSet);
        if (lemmaList.size() == 0) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Страниц, удовлетворяющих запрос, нет");
        }
        return this.searchResponse(lemmaList, site, page, from, size);
    }

    private boolean siteChecker(String site) {
        return sitesList.getSites().stream().anyMatch(s-> (
                s.getUrl().endsWith("/") ?
                        s.getUrl().substring(0, s.getUrl().length() - 1) : s.getUrl())
                .equals(site));
    }

    private List<Lemma> findListOfLemmasInDbAversSorted(Set<String> lemmasSet) {
        return lemmaFinder.getSortedLemmasSetFromDbAversSorted(lemmasSet);
    }

    private SearchResponseDTO searchResponse(List<Lemma> lemmaList,
                                             String site,
                                             PageRequest page,
                                             Integer from,
                                             Integer size) {
    SearchResponseDTO response = new SearchResponseDTO();
    Set<Page> setPagesInDb = this.checkPageInDb(lemmaList, site);
    List<PageDataDTO> responceDataDtoList = this.createResponceDataDtoList(lemmaList, setPagesInDb); // todo

        response.setResult(true);
        response.setError(null);
        response.setCount(responceDataDtoList.size());
        response.setData(this.getListOfData(responceDataDtoList, from, size));

        return response;
    }

    private Set<Page> checkPageInDb(List<Lemma> lemmaList,
                                    String site) {


    }

    private List<PageDataDTO> createResponceDataDtoList(List<Lemma> lemmaList,
                                                        Set<Page> setPagesInDb) {


    }


    private List<PageDataDTO> getListOfData(List<PageDataDTO> responceDataDtoList,
                                            Integer from,
                                            Integer size) {
        int endIndex = from + size;
        return responceDataDtoList.subList(from, Math.max(endIndex, responceDataDtoList.size()));
    }
}

