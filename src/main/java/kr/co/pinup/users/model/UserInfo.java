package kr.co.pinup.users.model;

import kr.co.pinup.users.model.enums.UserRole;
import kr.co.pinup.users.oauth.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String nickname;
    private OAuthProvider provider;
    private UserRole role;
}
