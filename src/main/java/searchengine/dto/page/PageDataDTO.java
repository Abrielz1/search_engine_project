package searchengine.dto.page;

import lombok.Data;

@Data
public class PageDataDTO implements Comparable<PageDataDTO> {

    private String site;

    private String siteName;

    private String uri;

    private String title;

    private String snippet;

    private float relevance;

    @Override
    public int compareTo(PageDataDTO o) {
        return Float.compare(getRelevance(),
                o.getRelevance());
    }
}
