package kr.co.pinup.locations.service;

import kr.co.pinup.api.kakao.KakaoApiService;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService  {

    private final KakaoApiService kakaoApiService;
    private final LocationRepository locationRepository;

    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        final KakaoAddressDocument addressDocument = kakaoApiService.searchAddress(request.address());
        log.debug("createLocation addressDocument={}", addressDocument);

        Location location = locationBuilder(request, addressDocument);
        Location savedLocation = locationRepository.save(location);

        return LocationResponse.from(savedLocation);
    }

    public Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(LocationNotFoundException::new);
    }

    public LocationResponse getLocationId(Long id) {
        Location location = getLocation(id);

        return LocationResponse.from(location);
    }

    @Transactional
    public LocationResponse updateLocation(final long locationId, final UpdateLocationRequest request) {
        final KakaoAddressDocument addressDocument = kakaoApiService.searchAddress(request.address());
        log.debug("updateLocation addressDocument={}", addressDocument);

        final Location location = locationRepository.findById(locationId)
                .orElseThrow(LocationNotFoundException::new);

        location.update(request, addressDocument);

        return LocationResponse.from(location);
    }

    private Location locationBuilder(
            final CreateLocationRequest request,
            final KakaoAddressDocument addressDocument
    ) {
        return Location.builder()
                .name(addressDocument.addressName())
                .zonecode(request.zonecode())
                .sido(request.sido())
                .sigungu(request.sigungu())
                .latitude(addressDocument.latitude())
                .longitude(addressDocument.longitude())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .build();
    }
}


