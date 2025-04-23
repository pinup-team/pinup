package kr.co.pinup.config;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.util.SecretsManagerUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoWebClientConfig {

    //TODO RestTemplate 관련 내용 같이 블로그에 정리하기
    //TODO clientConfig 나중에 없애기

    private final SecretsManagerUtil secretsManagerUtil;

    public KakaoWebClientConfig(SecretsManagerUtil secretsManagerUtil) {
        this.secretsManagerUtil = secretsManagerUtil;
    }

    @PostConstruct
    public void printKey() {
        String restKey = secretsManagerUtil.getSecretField("kakao-map", "kakao.api.key.rest");
        String jsKey = secretsManagerUtil.getSecretField("kakao-map", "kakao.api.key.js");
        System.out.println("🔑 kakaoRestKey = " + restKey);
        System.out.println("🔑 kakaoJsKey   = " + jsKey);
    }

    @Bean
    public WebClient kakaoWebClient() {
        String KakaoRestKey = secretsManagerUtil.getSecretField("kakao-map", "kakao.api.key.rest");
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + KakaoRestKey)
                .build();
    }
}
