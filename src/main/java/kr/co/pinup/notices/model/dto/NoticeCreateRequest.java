package kr.co.pinup.notices.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record NoticeCreateRequest(
        @NotBlank(message = "제목을 입력하세요.")
        @Size(min = 1, max = 100, message = "제목을 1~100자 이내로 입력하세요.")
        String title,

        @NotBlank(message = "내용을 입력하세요.")
        String content) {
}
