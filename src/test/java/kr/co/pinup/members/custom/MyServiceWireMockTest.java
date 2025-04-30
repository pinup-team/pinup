package kr.co.pinup.members.custom;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

@WireMockTest(httpPort = 8888) // WireMock 서버가 http://localhost:8080 에서 실행됨
public class MyServiceWireMockTest {
    @Test
    void testStubResponse() {
        // stub 설정: GET /api/test 요청 시 JSON 응답 반환
        stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"message\": \"Hello, WireMock!\" }")));

        // 이곳에서 WebClient, RestTemplate 등으로 http://localhost:8080/api/test 호출
        // 예를 들어, WebClient를 이용하면 반환된 JSON을 파싱해서 검증
        // MyService 호출 시, 내부에서 http://localhost:8080/api/test를 호출하도록 설정되어 있어야 함.

        // 검증 예시: WireMock에서 해당 요청이 발생했는지 확인
        verify(getRequestedFor(urlEqualTo("/api/test")));
    }
}
