package kr.co.pinup.api.kakao;

import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoApiService {

    private final KakaoApiClient kakaoApiClient;

    public KakaoAddressDocument searchAddress(String address) {
        return kakaoApiClient.searchAddress(address);
    }
}
