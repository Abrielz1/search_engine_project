package searchengine.repository;

import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findFirstByUrl(String url);
}
