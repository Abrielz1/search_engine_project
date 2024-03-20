package searchengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexingPagingResponse {

    private String url;

    private String name;

    private String status;

    private Long statusTime;

    private String error;

    private Integer pages;

    private Integer lemmas;
}
