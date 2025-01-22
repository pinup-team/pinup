package kr.co.pinup.members.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class OauthConfig {
    private Map<String, Registration> registration;
    private Map<String, Provider> provider;

    @Data
    public static class Registration {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String clientName;
        private String authorizationGrantType;
        private List<String> scope; // scope는 리스트 형태
    }

    @Data
    public static class Provider {
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;
    }
}
