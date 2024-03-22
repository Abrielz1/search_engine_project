package searchengine.dto.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponseDTO {

    private boolean result;

    private Integer count;

    private List<PageDataDTO> data;

    private String error;
}
