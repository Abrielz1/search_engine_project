package searchengine.dto.page;

import lombok.Data;

@Data
public class PageDataDTO {

    private String site;

    private String siteName;

    private String uri;

    private String title;

    private String snippet;

    private Float relevance;
}
