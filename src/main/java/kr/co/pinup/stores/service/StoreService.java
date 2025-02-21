package kr.co.pinup.stores.service;

import jakarta.validation.Valid;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.exception.StoreCategoryNotFoundException;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreSummaryResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final StoreCategoryRepository storeCategoryRepository;

    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request) {
        log.info("팝업스토어 수정 요청 - ID: {}", id);
        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        StoreCategory category = null;
        if (request.getCategoryId() != null) {
            category = storeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(StoreCategoryNotFoundException::new);
        }

        Location location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(LocationNotFoundException::new);
        }

        store.updateStore(request, category, location);

        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public List<StoreSummaryResponse> getStoreSummaries() {
        log.info("홈페이지 목록 조회 요청됨");
        return storeRepository.findAll().stream()
                .map(StoreSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        log.info("모든 팝업스토어 조회 요청");
        return storeRepository.findAll().stream()
                .map(StoreResponse::from)
                .toList();
    }


    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        log.info("특정 팝업스토어 조회 요청 - ID: {}", id);

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        log.info("팝업스토어 생성 요청 - 이름: {}", request.name());

        StoreCategory category = storeCategoryRepository.findById(request.categoryId())
                .orElseThrow(StoreNotFoundException::new);

        log.info("StoreCategory - {}", category.getId());

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(LocationNotFoundException::new);

        log.info("StoreCategory - {}", location.getId());

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .category(category)
                .location(location)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(Status.RESOLVED)
                .imageUrl(request.imageUrl())
                .build();

        storeRepository.save(store);
        log.info("팝업스토어 생성 완료 - ID: {}", store.getId());

        return StoreResponse.from(store);
    }

    @Transactional
    public void deleteStore(Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        storeRepository.delete(store);
        log.info("팝업스토어 삭제 완료 - ID: {}", id);
    }

}
