package kr.co.pinup;

import kr.co.pinup.api.kakao.KakaoApiKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KakaoApiKeyProperties.class)
public class PinupApplication {

    public static void main(String[] args) {
        SpringApplication.run(PinupApplication.class, args);
    }

}
