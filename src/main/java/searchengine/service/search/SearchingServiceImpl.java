package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.page.PageDataDTO;
import searchengine.dto.page.SearchResponseDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.indexing.LemmaFinder;
import searchengine.service.util.EntityManipulator;
import searchengine.service.util.SnippetManipulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final SnippetManipulator snippetManipulator;

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
    List<Site> sites = findSitesListInDb(site);
    List<Lemma> listNonFrequentLemmas = this.pickNonFrequentLemmas(lemmaList);
    Set<Page> resultOfProceedPages = pageRepository.findFirstByLemmasAndSite(listNonFrequentLemmas, site);


        for (Lemma lemma : listNonFrequentLemmas) {
            Set<Page> foundPagesSet = pageRepository.findByOneLemmaAndSitesAndPages(lemma,
                                                                                    sites,
                                                                                    resultOfProceedPages);
            resultOfProceedPages.clear();
            resultOfProceedPages.addAll(foundPagesSet);
        }

    return resultOfProceedPages;
    }

    private List<PageDataDTO> createResponceDataDtoList(List<Lemma> lemmaList,
                                                        Set<Page> setPagesInDb) {
    List<PageDataDTO> resultList = new ArrayList<>();

        return null;
    }


    private List<PageDataDTO> getListOfData(List<PageDataDTO> responceDataDtoList,
                                            Integer from,
                                            Integer size) {
        int endIndex = from + size;
        return responceDataDtoList.subList(from, Math.max(endIndex, responceDataDtoList.size()));
    }

    private List<Site> findSitesListInDb(String site) {
        List<Site> sites = new ArrayList<>();
        Optional<Site> siteInBd = siteRepository.findFirstByUrl(site);
        if (site != null && siteInBd.isPresent()) {
            sites.add(siteInBd.get());
        } else {
            sites.addAll(siteRepository.findAll());
        }

        return sites;
    }

    private List<Lemma> pickNonFrequentLemmas(List<Lemma> lemmaList) {
        return lemmaList.stream()
                .filter(frequency->
                        frequency.getFrequency() < 250).
                collect(Collectors.toList());
    }

    private PageDataDTO collectData(Page page,
                                    String content,
                                    List<Lemma> sortedLemmas) {
        PageDataDTO pageDataDTO = new PageDataDTO();

        String pageText = Jsoup.clean(content, Safelist.relaxed())
                .replaceAll("&nbsp;", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("https?://[\\w\\W]\\S+", "")
                .replaceAll("\\s*\\n+\\s*", " · ");

        pageDataDTO.setSite(page.getSite().getUrl());
        pageDataDTO.setUri(page.getPath());
        pageDataDTO.setSiteName(page.getSite().getName());
        pageDataDTO.setTitle(this.findTitle(content));
        pageDataDTO.setRelevance(this.getRelevance(page));
        pageDataDTO.setSnippet(snippetManipulator.createSnippet(pageText, sortedLemmas));

        return pageDataDTO;

    }

    private Float getRelevance(Page page) {
        return null;
    }

    private String findTitle(String content) {
        return null;
    }
}

