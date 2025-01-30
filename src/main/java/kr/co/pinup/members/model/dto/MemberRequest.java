package kr.co.pinup.members.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Builder;

@Builder
public record MemberRequest(
        @Schema(description = "이름")
        @Size(min = 2, message = "2자 이상 올바른 이름을 입력해주세요.")
        @NotBlank(message = "이름은 빈 값일 수 없습니다.")
        String name,

        @Schema(description = "이메일")
        @Email(message = "이메일 형식에 맞춰 입력해주세요.")
        @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
        String email,

        @Schema(description = "닉네임")
        @NotBlank(message = "닉네임은 빈 값일 수 없습니다.")
        String nickname,

        @Schema(description = "OAuth제공자")
        @NotBlank(message = "OAuth제공자는 빈 값일 수 없습니다.")
        OAuthProvider providerType,

        @Schema(description = "권한")
        @NotBlank(message = "권한은 빈 값일 수 없습니다.")
        MemberRole role
) {
}