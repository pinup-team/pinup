package kr.co.pinup.members.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Builder;

@Builder
public record MemberLoginRequest(
        @Schema(description = "이메일")
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @Schema(description = "비밀번호")
        @NotBlank(message = "비밀번호는 빈 값일 수 없습니다.")
        String password,

        @Schema(description = "OAuth제공자")
        @NotNull(message = "OAuth제공자는 빈 값일 수 없습니다.")
        OAuthProvider providerType
) {
}