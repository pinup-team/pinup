package kr.co.pinup.locations.model.dto;

import kr.co.pinup.locations.Location;

public record LocationResponse(
        Long id,
        String name,
        String zoneCode,
        String state,
        String district,
        Double latitude,
        Double longitude,
        String address,
        String addressDetail
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getZoneCode(),
                location.getState(),
                location.getDistrict(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAddress(),
                location.getAddressDetail()
        );
    }
}
