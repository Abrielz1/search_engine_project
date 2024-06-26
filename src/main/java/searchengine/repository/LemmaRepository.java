package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Optional<Lemma> findFirstByLemma(String lemma);

    Integer countLemmaBySite(Site site);

    @Query(value = """
                   SELECT *
                   FROM lemma
                   WHERE lemma IN :lemmasSet
                   ORDER BY frequency ASC
                   """, nativeQuery = true)
    Vector<Lemma> findByLemma(@Param("lemmasSet") Set<String> lemmasSet);
}
