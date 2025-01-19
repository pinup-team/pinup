package kr.co.pinup.users.oauth.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kr.co.pinup.users.oauth.OAuthProvider;
import kr.co.pinup.users.oauth.OAuthResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverResponse implements OAuthResponse {
    @JsonProperty("response")
    public Response response;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private String id;
        private String name;
        private String email;

        public Response(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    @Override
    public String getId() {
        return response.getId();
    }

    @Override
    public String getName() {
        return response.getName();
    }

    @Override
    public String getEmail() {
        return response.getEmail();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.NAVER;
    }
}
