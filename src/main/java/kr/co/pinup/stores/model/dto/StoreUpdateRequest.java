package kr.co.pinup.stores.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.pinup.stores.model.enums.Status;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreUpdateRequest {
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private Long locationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;
}
