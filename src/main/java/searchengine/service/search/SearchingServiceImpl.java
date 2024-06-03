package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import searchengine.config.SitesList;
import searchengine.dto.page.PageDataDTO;
import searchengine.dto.page.SearchResponseDTO;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.service.indexing.LemmaFinder;
import searchengine.service.util.SnippetManipulator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {

    private final IndexRepository indexRepository;

    private final SitesList sitesList;

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaFinder lemmaFinder;

    private final SnippetManipulator snippetManipulator;

    private Float maxRelevance;

    @Override
    @Transactional
    public SearchResponseDTO search(String query,
                                    String site,
                                    Integer from,
                                    Integer size) {

        SearchResponseDTO newSearchResponseDTO = new SearchResponseDTO();

        Set<String> lemmasSet = lemmaFinder.getLemmaSet(query);
        List<Lemma> lemmaList = this.findListOfLemmasInDbAversSorted(lemmasSet);

        if (site != null && !this.siteChecker(site)) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Указанная страница не найдена");
            return newSearchResponseDTO;
        }

        if (lemmasSet.isEmpty()) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Задан пустой поисковый запрос");
        }

        if (lemmaList.size() == 0) {
            newSearchResponseDTO.setResult(false);
            newSearchResponseDTO.setError("Страниц, удовлетворяющих запрос, нет");
        }

        return this.searchResponse(lemmaList,
                                   site,
                                   from,
                                   size);
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

    private SearchResponseDTO searchResponse(List<Lemma> sortedLemmas,
                                             String site,
                                             Integer from,
                                             Integer size) {
    SearchResponseDTO response = new SearchResponseDTO();
    Set<Page> setPagesInDb = this.checkPageInDb(sortedLemmas,
                                                site);

        if (CollectionUtils.isEmpty(setPagesInDb)) {
            response.setError("Слова в запросе встречаются слишком часто, попробуйте уточнить запрос подробнее");
            return response;
        }

    List<PageDataDTO> responseDataDtoList = this.createResponseDataDtoList(sortedLemmas,
                                                                           setPagesInDb);

        response.setCount(responseDataDtoList.size());
        response.setResult(true);
        response.setError(null);
        response.setData(this.getListOfData(responseDataDtoList,
                                            sortedLemmas.size(),
                                            from,
                                            size));



        return response;
    }

    private Set<Page> checkPageInDb(List<Lemma> lemmaList,
                                    String site) {
    List<Site> sites = findSitesListInDb(site);
    List<Lemma> listNonFrequentLemmas = this.findNonFrequentLemmas(lemmaList);
    Set<Page> resultOfProceedPages = pageRepository.getByLemmasAndSite(listNonFrequentLemmas,
                                                                       sites);


        for (Lemma lemma : listNonFrequentLemmas) {
            Set<Page> foundPagesSet = pageRepository.findByOneLemmaAndSitesAndPages(lemma,
                                                                                    sites,
                                                                                    resultOfProceedPages);
            resultOfProceedPages.clear();
            resultOfProceedPages.addAll(foundPagesSet);
        }

    return resultOfProceedPages;
    }

    private List<PageDataDTO> createResponseDataDtoList(List<Lemma> lemmaList,
                                                        Set<Page> setPagesInDb) {
    List<PageDataDTO> resultList = new ArrayList<>();

        for (Page page: setPagesInDb) {
            String pageContent = page.getContent();
            PageDataDTO newData = this.collectData(page,
                                                   pageContent,
                                                   lemmaList);
            resultList.add(newData);
        }

        resultList.sort(Collections.reverseOrder());

        return resultList;
    }

    private List<PageDataDTO> getListOfData(List<PageDataDTO> responceDataDtoList,
                                            Integer lemmaListSize,
                                            Integer from,
                                            Integer size) {

        return responceDataDtoList.subList(from,
                Math.min(from + size,
                        lemmaListSize));
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

    private List<Lemma> findNonFrequentLemmas(List<Lemma> lemmaList) {
        return lemmaList.stream()
                .filter(frequency->
                        frequency.getFrequency() < 250)
                .collect(Collectors.toList());
    }

    private PageDataDTO collectData(Page page,
                                    String content,
                                    List<Lemma> sortedLemmas) {
        PageDataDTO pageDataDTO = new PageDataDTO();

        pageDataDTO.setSite(page.getSite().getUrl());
        pageDataDTO.setUri(page.getPath());
        pageDataDTO.setSiteName(page.getSite().getName());
        pageDataDTO.setTitle(this.findTitle(content));
        pageDataDTO.setRelevance(this.getRelevance(page));
        pageDataDTO.setSnippet(snippetManipulator.createSnippet(this.pageProceed(content),
                                                                sortedLemmas));

        return pageDataDTO;
    }

    private Float getRelevance(Page page) {
       if (maxRelevance == null) {
           maxRelevance = indexRepository.getMaxIndex();
       }

        return indexRepository.getReference(page,
                                            maxRelevance);
    }

    private String findTitle(String content) {
        if (content.contains("<title>") && content.contains("</title>")) {

            return content.substring(content.indexOf("<title>") + "<title>".length(),
                                     content.indexOf("</title>"));
        } else {

            return null;
        }
    }

    private String pageProceed(String content) {
        return Jsoup.clean(content, Safelist.relaxed())
                .replaceAll("&nbsp;", " ")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("https?://[\\w\\W]\\S+", "")
                .replaceAll("\\s*\\n+\\s*", " · ");
    }
}

