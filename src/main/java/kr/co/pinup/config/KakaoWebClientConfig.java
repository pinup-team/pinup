package kr.co.pinup.config;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.util.SecretsFetcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class KakaoWebClientConfig {

    private final SecretsFetcher secretsFetcher;

    public KakaoWebClientConfig(SecretsFetcher secretsFetcher) {
        this.secretsFetcher = secretsFetcher;
    }

    @PostConstruct
    public void printKeys() {
        try {
            String restKey = secretsFetcher.getSecretField("kakao.api.key.rest");
            String jsKey = secretsFetcher.getSecretField("kakao.api.key.js");
            log.info("🔑 Kakao REST Key: {}", restKey);
            log.info("🔑 Kakao JS Key: {}", jsKey);
        } catch (Exception e) {
            log.error("🔑 시크릿 로드 실패: {}", e.getMessage());
        }
    }

    @Bean
    public WebClient kakaoWebClient() {
        try {
            String kakaoRestKey = secretsFetcher.getSecretField("kakao.api.key.rest");
            return WebClient.builder()
                    .baseUrl("https://dapi.kakao.com")
                    .defaultHeader("Authorization", "KakaoAK " + kakaoRestKey)
                    .build();
        } catch (Exception e) {
            log.error("🔑 WebClient 생성 중 시크릿 로드 실패: {}", e.getMessage());
            throw new RuntimeException("WebClient 생성 실패", e);
        }
    }
}
