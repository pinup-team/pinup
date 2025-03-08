package kr.co.pinup.locations.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LocationService  {

    private final LocationRepository locationRepository;

    public LocationResponse createLocation(CreateLocationRequest locationRequest) {
        Location location = Location.builder()
                .name(locationRequest.name())
                .zoneCode(locationRequest.zoneCode())
                .state(locationRequest.state())
                .district(locationRequest.district())
                .latitude(locationRequest.latitude())
                .longitude(locationRequest.longitude())
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


