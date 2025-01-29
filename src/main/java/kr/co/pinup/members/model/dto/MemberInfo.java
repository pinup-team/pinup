package kr.co.pinup.members.model.dto;

import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Builder;

@Builder
public record MemberInfo(
        String nickname,
        OAuthProvider provider,
        MemberRole role
) {
}