package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    Optional<Lemma> findFirstByLemma(String lemma);

    @Query(value = """
                   SELECT *
                   FROM lemma
                   WHERE 'lemma' IN :lemmaSet
                   ORDER BY 'frequency'
                   """, nativeQuery = true)
    List<Lemma> findByLemma(Set<String> lemmasSet);
}
