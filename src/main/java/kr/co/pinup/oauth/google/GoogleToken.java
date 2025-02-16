package kr.co.pinup.oauth.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.co.pinup.oauth.OAuthToken;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleToken implements OAuthToken {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("scope")
    private String scope;

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String getRefreshToken() {
        return refreshToken;
    }
}
