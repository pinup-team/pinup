package kr.co.pinup.locations.controller;

import jakarta.validation.Valid;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@RequestBody @Valid CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createLocation(request));
    }
}