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

    //TODO RestTemplate 관련 내용 같이 블로그에 정리하기
    //TODO clientConfig 나중에 없애기

    private final SecretsFetcher secretsFetcher;

    public KakaoWebClientConfig(SecretsFetcher secretsFetcher) {
        this.secretsFetcher = secretsFetcher;
    }

    @PostConstruct
    public void printKey() {
        String restKey = secretsFetcher.getSecretField("kakao.api.key.rest");
        String jsKey = secretsFetcher.getSecretField("kakao.api.key.js");
        log.info("🔑 kakaoRestKey = {}", restKey);
        log.info("🔑 kakaoJsKey   = {}", jsKey);
    }

    @Bean
    public WebClient kakaoWebClient() {
        String KakaoRestKey = secretsFetcher.getSecretField("kakao.api.key.rest");
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + KakaoRestKey)
                .build();
    }
}
