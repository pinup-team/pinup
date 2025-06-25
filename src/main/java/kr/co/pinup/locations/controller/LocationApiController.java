package kr.co.pinup.locations.controller;

import jakarta.validation.Valid;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import kr.co.pinup.locations.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@RequestBody @Valid CreateLocationRequest request) {
        log.debug("createLocation CreateLocationRequest={}", request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createLocation(request));
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PutMapping("/{locationId}")
    public ResponseEntity<LocationResponse> updateLocation(
            @PathVariable Long locationId,
            @RequestBody @Valid UpdateLocationRequest request
    ) {
        log.debug("updateLocation locationId={}, UpdateLocationRequest={}", locationId, request);

        return ResponseEntity.ok(locationService.updateLocation(locationId, request));
    }
}