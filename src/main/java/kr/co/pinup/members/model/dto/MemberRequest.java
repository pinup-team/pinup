package kr.co.pinup.members.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import kr.co.pinup.custom.memberRequest.PasswordRequiredForLocal;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Builder;

@Builder
@PasswordRequiredForLocal       // 자체로그인과 oauth에서 혼용해 사용하기 위해 커스텀 어노테이션 적용
public record MemberRequest(
        @Schema(description = "이름")
        @Size(min = 2, message = "2자 이상 올바른 이름을 입력해주세요.")
        @NotBlank(message = "이름은 빈 값일 수 없습니다.")
        String name,

        @Schema(description = "이메일")
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @Schema(description = "비밀번호")
        @Size(min=8, max=20, message="비밀번호는 8~20자여야 합니다.")
        @Pattern(regexp=".*[!@#$%^&*()].*", message="특수문자가 포함되어야 합니다.")
        String password,

        @Schema(description = "닉네임")
        @NotBlank(message = "닉네임은 빈 값일 수 없습니다.")
        String nickname,

        @Schema(description = "OAuth제공자")
        @NotNull(message = "OAuth제공자는 빈 값일 수 없습니다.")
        OAuthProvider providerType
) {
}