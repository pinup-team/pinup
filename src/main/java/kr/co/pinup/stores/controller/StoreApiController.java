package kr.co.pinup.stores.controller;


import jakarta.validation.Valid;
import kr.co.pinup.annotation.ValidImageFile;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreThumbnailResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreApiController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getStores() {
        return ResponseEntity.ok(storeService.getStores());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<StoreThumbnailResponse>> getStoreThumbnails(
            @RequestParam(defaultValue = "5") int limit) {
        log.debug("getStoreThumbnails limit={}", limit);

        return ResponseEntity.ok(storeService.getStoresThumbnailWithLimit(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @Valid @RequestPart("storeRequest") StoreRequest request,
            @ValidImageFile @RequestParam(value = "images") List<MultipartFile> images) {
        log.debug("createStore StoreRequest={}, images size={}", request, images.size());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.createStore(request, images));
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long id,
            @Valid @RequestPart("request") StoreUpdateRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        log.debug("updateStore id={}, request={}", id, request);

        final List<MultipartFile> imageFiles = images != null ? images : List.of();
        log.debug("updateStore imageFiles size={}", imageFiles.size());
        StoreResponse updatedStore = storeService.updateStore(id, request, imageFiles);

        return ResponseEntity.ok(updatedStore);
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}