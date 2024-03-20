package searchengine.dto;

import lombok.Data;

@Data
public class PageData {

    private String site;

    private String siteName;

    private String uri;

    private String title;

    private String snippet;

    private Double relevance;
}
