package kr.co.pinup.members.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MemberResponse {
    private Long id;
    private String name;
    private String email;
    private String nickname;
    private OAuthProvider providerType;
    private MemberRole role;
    private boolean isDeleted;

    @JsonCreator
    public MemberResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("providerType") String providerType,
            @JsonProperty("role") String role,
            @JsonProperty("isDeleted") boolean isDeleted
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.nickname = nickname;
        this.providerType = OAuthProvider.valueOf(providerType);
        this.role = MemberRole.valueOf(role);
        this.isDeleted = isDeleted;
    }

    public MemberResponse(Member member) {
        id = member.getId();
        name = member.getName();
        email = member.getEmail();
        nickname = member.getNickname();
        providerType = member.getProviderType();
        role = member.getRole();
        isDeleted = member.isDeleted();
    }

    public static MemberResponse fromMember(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .providerType(member.getProviderType())
                .role(member.getRole())
                .isDeleted(member.isDeleted())
                .build();
    }
}
