package kr.co.pinup.oauth.google;

import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginParams implements OAuthLoginParams {
    private String code;
    private String state;
    private String error;

    public void setCode(String code) {
        this.code = code;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public OAuthProvider oAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public MultiValueMap<String, String> makeParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("state", state);
        return params;
    }

    @Override
    public MultiValueMap<String, String> catchErrors() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("error", error);
        params.add("state", state);
        return params;
    }
}
