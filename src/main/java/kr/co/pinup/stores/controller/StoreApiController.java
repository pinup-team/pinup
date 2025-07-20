package kr.co.pinup.stores.controller;


import jakarta.validation.Valid;
import kr.co.pinup.annotation.ValidImageFile;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreApiController {

    private final StoreService storeService;

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long id,
            @RequestPart("request") @Valid StoreUpdateRequest request,
            @RequestPart(value = "imageFiles", required = false) MultipartFile[] imageFiles
    ) {
        List<MultipartFile> imageFileList = (imageFiles != null) ? Arrays.asList(imageFiles) : List.of();
        StoreResponse updatedStore = storeService.updateStore(id, request, imageFileList);
        return ResponseEntity.ok(updatedStore);
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        log.info("모든 팝업스토어 목록 요청됨");
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable Long id) {
        log.info("팝업스토어 조회 요청 - ID: {}", id);
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @Valid @ModelAttribute StoreRequest request,
            @ValidImageFile @RequestParam(value = "images") List<MultipartFile> images
    ) {
        log.debug("createStore StoreRequest={}, images size={}", request, images.size());

        return ResponseEntity.ok(storeService.createStore(request, images));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}