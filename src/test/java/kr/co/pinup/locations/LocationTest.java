package kr.co.pinup.locations;

import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationTest {

    @DisplayName("위치 수정시 필드 전체 업데이트한다.")
    @Test
    void update() {
        // Arrange
        final UpdateLocationRequest request = getUpdateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final Location location = createLocation(addressDocument);

        // Act
        location.update(request, addressDocument);

        // Assert
        assertThat(location).extracting(
                Location::getZonecode,
                Location::getAddress,
                Location::getSido,
                Location::getSigungu,
                Location::getLatitude,
                Location::getLongitude
        ).containsOnly(
                request.zonecode(),
                request.address(),
                request.sido(),
                request.sigungu(),
                addressDocument.latitude(),
                addressDocument.longitude()
        );
    }

    private UpdateLocationRequest getUpdateLocationRequest() {
        return UpdateLocationRequest.builder()
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .addressDetail("잠실 롯데월드몰 1층 아트리움")
                .build();
    }

    private KakaoAddressDocument getAddressDocument(String addressName) {
        return KakaoAddressDocument.builder()
                .addressName(addressName)
                .longitude(127.098142)
                .latitude(37.51131)
                .build();
    }

    private Location createLocation(final KakaoAddressDocument addressDocument) {
        return Location.builder()
                .name(addressDocument.addressName())
                .zonecode("05554")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 240")
                .addressDetail("롯데백화점 잠실점 10F 웨이브 행사장 (LG전자 콜라보 행사)")
                .latitude(addressDocument.latitude())
                .longitude(addressDocument.longitude())
                .build();
    }
}