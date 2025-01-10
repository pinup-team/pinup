package kr.co.pinup.notice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record NoticeUpdate(
        @NotBlank(message = "제목을 입력하세요.")
        @Size(min = 1, max = 100, message = "제목을 1~100자 이내로 입력하세요.")
        String title,

        @NotBlank(message = "내용을 입력하세요.")
        @Size(min = 1, max = 200, message = "내용을 1~200자 이내로 입력하세요.")
        String content) {
}
