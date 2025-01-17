package kr.co.pinup.oauth.google;

import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@NoArgsConstructor
public class GoogleLoginParams implements OAuthLoginParams {
    private String code;
    private String state;

    public void setCode(String code) {
        this.code = code;
    }

    public void setState(String state) {
        this.state = state;
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
}
