package kr.co.pinup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretsManagerConfig {

    @Value("${kakao.api.key.rest}")
    private String kakaoRestKey;

    @Value("${kakao.api.key.js")
    private String kakaoJsKey;
}
