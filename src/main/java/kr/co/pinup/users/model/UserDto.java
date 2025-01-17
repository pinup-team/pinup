package kr.co.pinup.users.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.users.model.enums.UserRole;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String nickname;
    private OAuthProvider providerType;
    private UserRole role;

    @JsonCreator
    public UserDto(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("providerType") String providerType,
            @JsonProperty("role") String role
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.nickname = nickname;
        this.providerType = OAuthProvider.valueOf(providerType);
        this.role = UserRole.valueOf(role);
    }
}
