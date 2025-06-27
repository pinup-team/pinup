package kr.co.pinup.api.kakao;

import kr.co.pinup.api.kakao.exception.KakaoApiException;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.custom.logging.AppLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class KakaoApiServiceTest {

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private AppLogger logger;

    @InjectMocks
    private KakaoApiService kakaoApiService;

    @DisplayName("주소로 주소 정보를 조회한다.")
    @Test
    void searchAddress() {
        // Arrange
        final String address = "서울 송파구 올림픽로 300";
        final double longitude = 127.104302;
        final double latitude = 37.513713;

        final KakaoAddressDocument kakaoAddressDocument = KakaoAddressDocument.builder()
                .addressName(address)
                .longitude(longitude)
                .latitude(latitude)
                .build();

        given(kakaoApiClient.searchAddress(address)).willReturn(kakaoAddressDocument);

        // Act
        final KakaoAddressDocument result = kakaoApiService.searchAddress(address);

        // Assert
        assertThat(result.addressName()).isEqualTo(address);
        assertThat(result.longitude()).isEqualTo(longitude);
        assertThat(result.latitude()).isEqualTo(latitude);

        then(kakaoApiClient).should(times(1))
                .searchAddress(address);
    }

    @DisplayName("잘못된 주소로 주소 정보를 조회하면 KakaoApiException 예외가 발생한다.")
    @Test
    void invalidSearchAddress() {
        // Arrange
        final String address = "잘못된 주소";

        given(kakaoApiClient.searchAddress(address)).willThrow(KakaoApiException.class);

        // Act Assert
        assertThatThrownBy(() -> kakaoApiService.searchAddress(address))
                .isInstanceOf(KakaoApiException.class);

        then(kakaoApiClient).should(times(1))
                .searchAddress(address);
    }
}