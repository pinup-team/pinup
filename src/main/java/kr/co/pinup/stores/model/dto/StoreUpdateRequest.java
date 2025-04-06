package kr.co.pinup.stores.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.co.pinup.stores.model.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreUpdateRequest {

    //TODO SpringValidation 이용한 업데이트로 변경

//    @NotBlank(message = "스토어 이름은 필수 입력값입니다.")
    private String name;

//    @NotBlank(message = "스토어 설명은 필수 입력값입니다.")
    private String description;

//    @NotNull(message = "카테고리는 필수 입력값입니다.")
    private Long categoryId;

//    @NotNull(message = "위치는 필수 입력값입니다.")
    private Long locationId;

//    @NotNull(message = "시작 날짜는 필수 입력값입니다.")
    private LocalDate startDate;

//    @NotNull(message = "종료 날짜는 필수 입력값입니다.")
    private LocalDate endDate;

//    @NotNull(message = "스토어 상태는 필수 입력값입니다.")
    private Status status;

    private String contactNumber;

    private String websiteUrl;

    private String snsUrl;

    private String imageUrl;
}
