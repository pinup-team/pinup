package kr.co.pinup.stores.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.stores.model.enums.StoreStatus;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreUpdateRequest {

    //TODO SpringValidation 이용한 업데이트로 변경

    @NotBlank(message = "스토어 이름은 필수 입력값입니다.")
    private String name;

    @NotBlank(message = "스토어 설명은 필수 입력값입니다.")
    private String description;

    @NotBlank(message = "이미지 Url은 필수 입력값입니다.")
    private String imageUrl;

    @NotNull(message = "카테고리는 필수 입력값입니다.")
    private Long categoryId;

    @NotNull(message = "위치는 필수 입력값입니다.")
    private Long locationId;

    @NotNull(message = "시작 날짜는 필수 입력값입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료 날짜는 필수 입력값입니다.")
    private LocalDate endDate;

    @NotNull(message = "스토어 상태는 필수 입력값입니다.")
    private StoreStatus storeStatus;
}
