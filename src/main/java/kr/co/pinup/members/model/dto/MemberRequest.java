package kr.co.pinup.members.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberRequest {
    @Schema(description = "이름")
    @Size(min = 2, message = "2자 이상 올바른 이름을 입력해주세요.")
    @NotBlank(message = "이름은 빈 값일 수 없습니다.")
    private String name;
    @Schema(description = "이메일")
    @Email(message = "이메일 형식에 맞춰 입력해주세요.")
    @NotBlank(message = "이메일은 빈 값일 수 없습니다.")
    private String email;
    @Schema(description = "닉네임")
    @NotBlank(message = "닉네임은 빈 값일 수 없습니다.")
    private String nickname;
    @Schema(description = "OAuth제공자")
    @NotBlank(message = "OAuth제공자는 빈 값일 수 없습니다.")
    private OAuthProvider providerType;
    @Schema(description = "권한")
    @NotBlank(message = "권한은 빈 값일 수 없습니다.")
    private MemberRole role;

    public MemberRequest(Member member) {
        name = member.getName();
        email = member.getEmail();
        nickname = member.getNickname();
        providerType = member.getProviderType();
        role = member.getRole();
    }
}
