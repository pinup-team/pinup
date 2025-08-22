package kr.co.pinup.verification.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record VerificationRequest(
        @Schema(description = "이메일")
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @Schema(description = "인증코드")
        @NotBlank(message = "인증코드는 빈 값일 수 없습니다.")
        String code
) {
}