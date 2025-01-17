package kr.co.pinup.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

public interface OAuthResponse {
    String getId();
    String getName();
    String getEmail();
    OAuthProvider getOAuthProvider();
}
