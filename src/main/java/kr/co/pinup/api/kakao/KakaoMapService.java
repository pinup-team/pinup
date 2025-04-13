package kr.co.pinup.api.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class KakaoMapService {

    private final WebClient kakaoWebClient;

    public KakaoMapService(@Qualifier("kakaoWebClinet") WebClient kakaoWebClient) {
        this.kakaoWebClient = kakaoWebClient;
    }

    public Map<String, String> searchLatLng(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "/v2/local/search/address.json?query=" + encodedAddress;

            String response = kakaoWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class) // JSON 문자로 응답하기 때문에 문자열 그대로 비동기 수신
                    .block(); // subscribe() 대신 비동기 응답을 동기적으로 리턴, 나중에 변경 필요할수도

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(response);

            JsonNode document = json.get("document").get(0);

            if (document == null || document.isMissingNode()) {
                return Map.of("error", "no_result");
            }

            return Map.of(
                    "lat", document.path("y").asText(),
                    "lng", document.path("x").asText()
            );
        } catch (Exception e) {
            // TODO kakaoApi 전용 exception 만들기
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

}
