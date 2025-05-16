package kr.co.pinup.config;

import kr.co.pinup.util.SecretsFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class KakaoWebClientConfigTest {

    private SecretsFetcher secretsFetcher;
    private KakaoWebClientConfig kakaoWebClientConfig;

    @BeforeEach
    void setUp() {
        secretsFetcher = Mockito.mock(SecretsFetcher.class);
        when(secretsFetcher.getSecretField("kakao.api.key.rest")).thenReturn("f4bf7b7aa63cec62e8d39812abc50612");
        when(secretsFetcher.getSecretField("kakao.api.key.js")).thenReturn("e6c84f4e658018f352467363162e6f92");

        kakaoWebClientConfig = new KakaoWebClientConfig(secretsFetcher);
    }

    @Test
    void testPrintKey() {
        kakaoWebClientConfig.printKey();
        verify(secretsFetcher, times(1)).getSecretField("kakao.api.key.rest");
        verify(secretsFetcher, times(1)).getSecretField("kakao.api.key.js");
    }

    @Test
    void testKakaoWebClient() {
        WebClient kakaoWebClient = kakaoWebClientConfig.kakaoWebClient();

        Mono<String> response = kakaoWebClient.get()
                .uri("/v2/local/search/address.json?query=서울특별시")
                .retrieve()
                .bodyToMono(String.class);

        String result = response.block();
        assertThat(result).isNotNull();
        System.out.println("📦 Response: " + result);
    }
}
