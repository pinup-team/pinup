package kr.co.pinup.locations.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.api.kakao.KakaoMapService;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LocationService  {

    private final LocationRepository locationRepository;
    private final KakaoMapService kakaoMapService;

    public LocationResponse createLocation(CreateLocationRequest locationRequest) {

        Map<String, String> geo = kakaoMapService.searchLatLng(locationRequest.address());

        log.info("카카오맵 좌표 응답: {}", geo);

        if (!geo.containsKey("lat") || !geo.containsKey("lng")) {
            throw new IllegalArgumentException("카카오 지도 API로부터 위도/경도 정보를 가져오지 못했습니다.");
        }

        Double latitude = Double.valueOf(geo.get("lat"));
        Double longitude = Double.valueOf(geo.get("lng"));

        Location location = Location.builder()
                .name(locationRequest.name())
                .zoneCode(locationRequest.zoneCode())
                .state(locationRequest.state())
                .district(locationRequest.district())
                .latitude(latitude)
                .longitude(longitude)
                .address(locationRequest.address())
                .addressDetail(locationRequest.addressDetail())
                .build();

        Location savedLocation = locationRepository.save(location);

        return LocationResponse.from(savedLocation);

    }

    public LocationResponse getLocationId(Long id) {

        Location location = locationRepository.findById(id)
                    .orElseThrow(LocationNotFoundException::new);

            return LocationResponse.from(location);

    }

}


