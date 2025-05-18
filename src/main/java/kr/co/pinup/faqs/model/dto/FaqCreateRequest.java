package kr.co.pinup.faqs.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import lombok.Builder;

@Builder
public record FaqCreateRequest(
        @NotBlank(message = "질문 내용을 입력하세요.")
        @Size(min = 1, max = 100, message = "질문 내용을 1~100자 이내로 입력하세요.")
        String question,

        @NotBlank(message = "답변 내용을 입력하세요.")
        @Size(min = 1, max = 500, message = "답변 내용을 1~500자 이내로 입력하세요.")
        String answer,

        @NotNull(message = "카테고리는 필수입니다.")
        FaqCategory category) {
}
