package kr.co.pinup.users.oauth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.co.pinup.users.oauth.OAuthProvider;
import kr.co.pinup.users.oauth.OAuthResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleResponse implements OAuthResponse {
    private String sub;
    private String name;
    private String email;

    @Override
    public String getId() {
        return sub;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.GOOGLE;
    }
}
