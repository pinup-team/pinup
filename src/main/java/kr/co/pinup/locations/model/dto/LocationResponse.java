package kr.co.pinup.locations.model.dto;

import kr.co.pinup.locations.Location;
import lombok.Builder;

@Builder
public record LocationResponse(
        Long id,
        String name,
        String zonecode,
        String sido,
        String sigungu,
        Double latitude,
        Double longitude,
        String address,
        String addressDetail
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getZonecode(),
                location.getSido(),
                location.getSigungu(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAddress(),
                location.getAddressDetail()
        );
    }
}
