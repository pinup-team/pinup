package kr.co.pinup.api.kakao;

import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoApiService {

    private final AppLogger logger;
    private final KakaoApiClient kakaoApiClient;

    public KakaoAddressDocument searchAddress(String address) {
        logger.info(new InfoLog("카카오 맵 검색 주소=" + address));

        return kakaoApiClient.searchAddress(address);
    }
}
