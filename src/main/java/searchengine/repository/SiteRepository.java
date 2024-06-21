package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findFirstByUrl(String url);

    @Query(value = """
                   SELECT *
                   FROM site
                   WHERE site.url like :path
                   """, nativeQuery = true)
    Optional<Site> getSiteByUrl(@Param("path") String path);
}
