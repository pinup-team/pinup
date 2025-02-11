package kr.co.pinup.stores.controller;


import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreSummaryResponse;
import kr.co.pinup.stores.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;

    public StoreApiController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StoreResponse> updateStore(@PathVariable Long id, @Valid @RequestBody StoreUpdateRequest request) {
        log.info("팝업스토어 수정 요청 - ID: {}", id);
        return ResponseEntity.ok(storeService.updateStore(id, request));
    }

    @GetMapping
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
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody StoreRequest request) {
        log.info("팝업스토어 생성 요청 - 이름: {}", request.name());
        return ResponseEntity.ok(storeService.createStore(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }
}