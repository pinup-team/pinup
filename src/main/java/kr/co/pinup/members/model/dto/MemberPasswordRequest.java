package kr.co.pinup.members.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Builder;

@Builder
public record MemberPasswordRequest(
        @Schema(description = "이메일")
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @Schema(description = "비밀번호")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        @Pattern(regexp = ".*[!@#$%^&*()].*", message = "특수문자가 포함되어야 합니다.")
        @NotBlank(message = "비밀번호는 빈 값일 수 없습니다.")
        String password,

        @Schema(description = "OAuth제공자")
        @NotNull(message = "OAuth제공자는 빈 값일 수 없습니다.")
        OAuthProvider providerType
) {
}
