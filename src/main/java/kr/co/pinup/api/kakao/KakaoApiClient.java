package kr.co.pinup.api.kakao;

import kr.co.pinup.api.kakao.exception.KakaoApiException;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private static final String API_SEARCH_ADDRESS = "/v2/local/search/address.json";

    private final WebClient kakaoWebClient;

    public KakaoAddressDocument searchAddress(String address) {
        return Objects.requireNonNull(kakaoWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(API_SEARCH_ADDRESS)
                                .queryParam("query", address)
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                                clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                    log.warn("Kakao API 클라이언트 오류 status={}, address={}, body={}",
                                            clientResponse.statusCode(), address, errorBody);
                                    return Mono.error(new KakaoApiException("Kakao API 클라이언트 오류 발생: " + errorBody));
                                })
                        )
                        .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                                clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                    log.error("Kakao API 서버 오류 status={}, address={}, body={}",
                                            clientResponse.statusCode(), address, errorBody);
                                    return Mono.error(new RuntimeException("Kakao API 서버 오류 발생: " + errorBody));
                                })
                        )
                        .bodyToMono(KakaoAddressResponse.class)
                        .block())
                .toDocument();
    }
}
