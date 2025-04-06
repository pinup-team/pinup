package kr.co.pinup.oauth.naver;

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
public class NaverLoginParams implements OAuthLoginParams {
    private String code;
    private String state;
    private String error;
    private String errorDescription;

    public void setCode(String code) {
        this.code = code;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public OAuthProvider oAuthProvider() {
        return OAuthProvider.NAVER;
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
        params.add("errorDescription", errorDescription);
        return params;
    }
}