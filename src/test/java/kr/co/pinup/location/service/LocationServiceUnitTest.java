package kr.co.pinup.location.service;

import kr.co.pinup.api.kakao.KakaoMapService;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.locations.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class})
public class LocationServiceUnitTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private KakaoMapService kakaoMapService;

    @InjectMocks
    private LocationService locationService;

    private CreateLocationRequest sampleRequest;
    private Location sampleLocation;

    @BeforeEach
    void setUp() {
        sampleRequest = CreateLocationRequest.builder()
                .name("배민 계란프라이 팝업")
                .zoneCode("12345")
                .state("서울특별시")
                .district("강남구")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("3층")
                .build();

        sampleLocation = mock(Location.class);
        when(sampleLocation.getId()).thenReturn(1L);
        when(sampleLocation.getName()).thenReturn("배민 계란프라이 팝업");
        when(sampleLocation.getZoneCode()).thenReturn("12345");
        when(sampleLocation.getState()).thenReturn("서울특별시");
        when(sampleLocation.getDistrict()).thenReturn("강남구");
        when(sampleLocation.getLatitude()).thenReturn(37.5665);
        when(sampleLocation.getLongitude()).thenReturn(126.9780);
        when(sampleLocation.getAddress()).thenReturn("서울특별시 강남구 테헤란로 123");
        when(sampleLocation.getAddressDetail()).thenReturn("3층");
    }

    @Test
    @DisplayName("위치 생성 - 성공")
    void createLocationSuccess() {
        // given
        Map<String, String> mockGeo = Map.of("lat", "37.5665", "lng", "126.9780");
        when(kakaoMapService.searchLatLng(anyString())).thenReturn(mockGeo);
        when(locationRepository.save(any(Location.class))).thenReturn(sampleLocation);

        // when
        LocationResponse response = locationService.createLocation(sampleRequest);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("배민 계란프라이 팝업", response.name());
        assertEquals(37.5665, response.latitude());
        assertEquals(126.9780, response.longitude());
        verify(locationRepository, times(1)).save(any(Location.class));
        verify(kakaoMapService, times(1)).searchLatLng(anyString());
    }

    @Test
    @DisplayName("위치 조회 - 성공")
    void getLocationByIdSuccess() {
        // given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(sampleLocation));

        // when
        LocationResponse response = locationService.getLocationId(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("배민 계란프라이 팝업", response.name());
        verify(locationRepository, times(1)).findById(1L);
    }

}
