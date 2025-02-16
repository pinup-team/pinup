package kr.co.pinup.oauth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
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
