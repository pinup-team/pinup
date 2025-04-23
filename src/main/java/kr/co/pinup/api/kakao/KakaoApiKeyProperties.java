package kr.co.pinup.api.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.api.key")
@Getter
@Setter
public class KakaoApiKeyProperties {
    private String js;
    private String rest;
}