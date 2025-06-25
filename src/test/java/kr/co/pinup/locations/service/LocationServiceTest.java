package kr.co.pinup.locations.service;

import kr.co.pinup.api.kakao.KakaoApiService;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class LocationServiceTest {

    private static final String LOCATION_ERROR_MESSAGE = "해당 로케이션이 존재하지 않습니다.";

    @Mock
    private KakaoApiService kakaoApiService;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @DisplayName("위치를 저장한다.")
    @Test
    void createLocation() {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final Location savedLocation = getLocation(addressDocument);

        given(kakaoApiService.searchAddress(request.address())).willReturn(addressDocument);
        given(locationRepository.save(any(Location.class))).willReturn(savedLocation);

        // Act
        final LocationResponse result = locationService.createLocation(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.address()).isEqualTo(request.address());
        assertThat(result.sido()).isEqualTo(request.sido());
        assertThat(result.sigungu()).isEqualTo(request.sigungu());
        assertThat(result.longitude()).isEqualTo(addressDocument.longitude());
        assertThat(result.latitude()).isEqualTo(addressDocument.latitude());

        then(kakaoApiService).should(times(1))
                .searchAddress(request.address());
        then(locationRepository).should(times(1))
                .save(any(Location.class));
    }

    @DisplayName("ID로 위치를 조회한다.")
    @Test
    void getLocationId() {
        // Arrange
        final String addressName = "서울 송파구 올림픽로 240";
        final KakaoAddressDocument addressDocument = getAddressDocument(addressName);
        final Location location = getLocation(addressDocument);

        given(locationRepository.findById(location.getId())).willReturn(Optional.of(location));

        // Act
        final LocationResponse result = locationService.getLocationId(location.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.address()).isEqualTo(location.getAddress());
        assertThat(result.sido()).isEqualTo(location.getSido());
        assertThat(result.sigungu()).isEqualTo(location.getSigungu());
        assertThat(result.longitude()).isEqualTo(addressDocument.longitude());
        assertThat(result.latitude()).isEqualTo(addressDocument.latitude());

        then(locationRepository).should(times(1))
                .findById(location.getId());
    }

    @DisplayName("존재하지 않는 ID로 위치 조회시 예외가 발생한다.")
    @Test
    void getLocationIdWithNonExistId() {
        // Arrange
        final long locationId = Long.MAX_VALUE;

        // Act Assert
        assertThatThrownBy(() -> locationService.getLocationId(locationId))
                .isInstanceOf(LocationNotFoundException.class)
                .hasMessage(LOCATION_ERROR_MESSAGE);

        then(locationRepository).should(times(1))
                .findById(locationId);
    }

    @DisplayName("ID에 일치하는 위치를 수정한다.")
    @Test
    void updateLocation() {
        // Arrange
        final long locationId = 1L;
        final UpdateLocationRequest request = getUpdateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final Location mockLocation = mock(Location.class);

        given(kakaoApiService.searchAddress(request.address())).willReturn(addressDocument);
        given(locationRepository.findById(locationId)).willReturn(Optional.ofNullable(mockLocation));

        // Act
        final LocationResponse result = locationService.updateLocation(locationId, request);

        // Assert
        assertThat(result).isNotNull();

        then(mockLocation).should(times(1))
                .update(request, addressDocument);
    }

    @DisplayName("존재하지 않는 ID로 위치 수정시 예외가 발생한다.")
    @Test
    void updateLocationWithNonExistId() {
        // Arrange
        final long locationId = Long.MAX_VALUE;
        final UpdateLocationRequest request = getUpdateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());

        given(kakaoApiService.searchAddress(request.address())).willReturn(addressDocument);

        // Act Assert
        assertThatThrownBy(() -> locationService.updateLocation(locationId, request))
                .isInstanceOf(LocationNotFoundException.class)
                .hasMessage(LOCATION_ERROR_MESSAGE);

        then(locationRepository).should(times(1))
                .findById(locationId);
    }

    private CreateLocationRequest getCreateLocationRequest() {
        return CreateLocationRequest.builder()
                .zonecode("05554")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 240")
                .addressDetail("롯데백화점 잠실점 10F 웨이브 행사장 (LG전자 콜라보 행사)")
                .build();
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

    private Location getLocation(final KakaoAddressDocument addressDocument) {
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
