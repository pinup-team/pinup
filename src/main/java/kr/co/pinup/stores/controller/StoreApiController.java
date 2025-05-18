package kr.co.pinup.stores.controller;


import jakarta.validation.Valid;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreSummaryResponse;
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
            @RequestPart(value = "imageFiles", required = false) MultipartFile[] imageFiles) {

        List<MultipartFile> imageFileList = (imageFiles != null) ? Arrays.asList(imageFiles) : List.of();

        StoreResponse updatedStore = storeService.updateStore(id, request, imageFileList);

        return ResponseEntity.ok(updatedStore);
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.getStoreById(id));
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @ModelAttribute StoreRequest request,
                                                     @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles) {
        return ResponseEntity.ok(storeService.createStore(request, imageFiles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}