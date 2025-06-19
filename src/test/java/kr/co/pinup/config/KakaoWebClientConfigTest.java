package kr.co.pinup.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@TestConfiguration
public class KakaoWebClientConfigTest {

    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080/mock-kakao")
                .defaultHeader(AUTHORIZATION, "KakaoAK test-key")
                .build();
    }
}
