package kr.co.pinup.verification.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.verification.model.enums.VerifyPurpose;
import lombok.Builder;

@Builder
public record VerificationConfirm(
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @NotBlank(message = "인증코드는 빈 값일 수 없습니다.")
        String code,

        @NotNull(message = "인증목적은 빈 값일 수 없습니다.")
        VerifyPurpose purpose
) {
}