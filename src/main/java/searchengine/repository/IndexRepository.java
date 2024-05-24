package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Page;

@Repository
public interface IndexRepository extends JpaRepository<Index, Long> {

    @Query(value = """
                   SELECT MAX(sum_rank)
                    FROM (SELECT SUM(rank) AS sum_rank
                     FROM index GROUP BY page_id) AS value
                   """, nativeQuery = true)
    Float getMaxIndex();

    @Query(value = """
                   SELECT SUM(rank) / :maxRelevance
                   FROM index
                   WHERE page_id = :page
                   """, nativeQuery = true)
    Float getReference(@Param("page") Page page,
                       @Param("maxRelevance") Float maxRelevance);
}
