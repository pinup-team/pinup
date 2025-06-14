package kr.co.pinup.api.kakao;

import kr.co.pinup.api.kakao.exception.KakaoApiException;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class KakaoApiClientTest {

    private KakaoApiClient kakaoApiClient;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        kakaoApiClient = new KakaoApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DisplayName("Kakao API로 주소 정보를 가져온다.")
    @Test
    void searchAddress() {
        // Arrange
        final String address = "서울 송파구 올림픽로 300";
        final double longitude = 127.104302;
        final double latitude = 37.513713;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody(String.format("""
                        {
                            "documents": [
                                {
                                    "address_name": "%s",
                                    "x": "%f",
                                    "y": "%f"
                                }
                            ]
                        }
                        """, address, longitude, latitude))
                .addHeader("Content-Type", APPLICATION_JSON_VALUE)
        );

        // Act
        final KakaoAddressDocument kakaoAddressDocument = kakaoApiClient.searchAddress(address);

        // Assert
        assertThat(kakaoAddressDocument.addressName()).isEqualTo(address);
        assertThat(kakaoAddressDocument.longitude()).isEqualTo(longitude);
        assertThat(kakaoAddressDocument.latitude()).isEqualTo(latitude);
    }

    @DisplayName("Kakao API 요청 시 400 오류가 발생하면 KakaoApiException 예외가 발생한다.")
    @Test
    void searchAddress_400Error() {
        // Arrange
        final String address = "잘못된주소";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("""
                    {
                        "error": "Bad Request"
                    }
                    """)
                .addHeader("Content-Type", APPLICATION_JSON_VALUE));

        // Act & Assert
        assertThatThrownBy(() -> {
            kakaoApiClient.searchAddress(address);
        })
                .isInstanceOf(KakaoApiException.class)
                .hasMessageStartingWith("Kakao API 클라이언트 오류");
    }

    @DisplayName("Kakao API 요청 시 500 오류가 발생하면 KakaoApiException 예외가 발생한다.")
    @Test
    void searchAddress_500Error() {
        // Arrange
        final String address = "정상주소";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("""
                        {
                            "error": "Internal Server Error"
                        }
                        """)
                .addHeader("Content-Type", APPLICATION_JSON_VALUE));

        // Act & Assert
        assertThatThrownBy(() -> kakaoApiClient.searchAddress(address))
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Kakao API 서버 오류");
    }
}