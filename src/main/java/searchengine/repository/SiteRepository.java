package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    @Query(value = """
UPDATE search_engine.public.site
       SET status_time = :time
       WHERE id = :id
""", nativeQuery = true)
    void updateSiteByIndexStatusAndIndexingTimeById(@Param("id") Long id,
                                                    @Param("time") LocalDateTime time);

    Optional<Site> findFirstByUrl(String url);
}
