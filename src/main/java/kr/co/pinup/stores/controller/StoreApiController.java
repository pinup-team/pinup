package kr.co.pinup.stores.controller;


import jakarta.validation.Valid;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreSummaryResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreApiController {

    private final StoreService storeService;

    @PatchMapping("/{id}")
    public ResponseEntity<StoreResponse> updateStore(@PathVariable Long id,
                                                     @Valid @RequestPart StoreUpdateRequest request,
                                                     @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {

        log.info("팝업스토어 수정 요청 - ID: {}", id);
        return ResponseEntity.ok(storeService.updateStore(id, request, imageFiles));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<StoreSummaryResponse>> getStores() {
        log.info("홈페이지 팝업스토어 목록 요청됨");
        return ResponseEntity.ok(storeService.getStoreSummaries());
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
    public ResponseEntity<StoreResponse> createStore(@Valid @ModelAttribute StoreRequest request,
                                                     @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles) {
        log.info("팝업스토어 생성 요청 - 이름: {}", request.name());
        return ResponseEntity.ok(storeService.createStore(request, imageFiles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}