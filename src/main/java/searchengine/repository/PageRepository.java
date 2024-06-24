package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Integer countPageBySite(Site site);

    @Query(value = """
            SELECT *
            FROM page AS p
            JOIN index i ON i.page_id = p.id
            JOIN lemma l ON i.lemma_id = l.id
            WHERE l.id IN :listNonFrequentLemmas
            AND p.site_id IN :sites
            """, nativeQuery = true)
    Set<Page> getByLemmasAndSite(@Param("listNonFrequentLemmas") List<Lemma> listNonFrequentLemmas,
                                 @Param("sites") List<Site> sites);


    @Query(value = """
            SELECT *
            FROM page AS p
            JOIN index i ON i.page_id = p. id
            JOIN lemma l ON i.lemma_id = l.id
                WHERE l.id = :lemma
                 AND p.site_id IN :sites
                 AND p.id IN :pagesToProceed
                    """, nativeQuery = true)
    Set<Page> findByOneLemmaAndSitesAndPages(@Param("lemma") Lemma lemma,
                                             @Param("sites") List<Site> sites,
                                             @Param("pagesToProceed") Set<Page> pagesToProceed);
}
