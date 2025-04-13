package kr.co.pinup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoWebClientConfig {

    //TODO RestTemplate 관련 내용 같이 블로그에 정리하기

    @Value("${kakao.api.key.rest}")
    private String kakaoRestKey;

    @Bean
    public WebClient kakaoWebClinet() {
        System.out.println("kakaoRestKey: " + kakaoRestKey);
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + kakaoRestKey)
                .build();
    }
}
