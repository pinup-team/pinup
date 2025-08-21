package kr.co.pinup.config;

import kr.co.pinup.api.aws.AwsSecretsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class KakaoWebClientConfig {

    private static final String API_BASE_URL = "https://dapi.kakao.com";
    private static final String KAKAO_API_REST_KEY = "kakao.api.key.rest";

    private final AwsSecretsProvider secretsProvider;

    @Bean
    public WebClient kakaoWebClient() {
        String restKey = secretsProvider.getSecretValue(KAKAO_API_REST_KEY);

        return WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restKey)
                .build();
    }
}
