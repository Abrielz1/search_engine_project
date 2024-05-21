package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    Optional<Page> findFirstByPathAndSite(String path, Site site);

    boolean existsByPathAndSite(String path, Site site);

    @Query(value = """
                   SELECT *
                   FROM page AS p
                   JOIN site s ON p.site_id = s.id
                   JOIN lemma l ON s.id = l.site_id
                   WHERE l.id IN :listNonFrequentLemmas 
                   AND p.site_id LIKE :site
                   """, nativeQuery = true)
    Set<Page> findFirstByLemmasAndSite(List<Lemma> listNonFrequentLemmas,
                                       String site);

    @Query(value = """
                    SELECT *
                   FROM page AS p
                   JOIN site s ON p.site_id = s.id
                   JOIN lemma l ON s.id = l.site_id
                   WHERE l.id IN :listNonFrequentLemmas
                   AND p.site_id IN :sites
                   AND p.id IN :pagesToProceed
                   """, nativeQuery = true)
    Set<Page> findByOneLemmaAndSitesAndPages(Lemma lemma,
                                             List<Site> sites,
                                             Set<Page> pagesToProceed);
}
