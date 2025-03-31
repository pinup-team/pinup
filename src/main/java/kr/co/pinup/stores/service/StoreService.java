package kr.co.pinup.stores.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.exception.LocationNotFoundException;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.exception.StoreCategoryNotFoundException;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.store_images.StoreImage;
import kr.co.pinup.store_images.exception.StoreImageDeleteFailedException;
import kr.co.pinup.store_images.model.dto.StoreImageRequest;
import kr.co.pinup.store_images.service.StoreImageService;
import kr.co.pinup.store_operatingHour.OperatingHour;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final StoreImageService storeImageService;

    private final String PATH_PREFIX = "store";

    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request, List<MultipartFile> imageFiles) {
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

        if (imageFiles != null && !imageFiles.isEmpty()) {
            log.info("기존 이미지 삭제 및 새로운 이미지 업로드 시작 - Store ID: {}", store.getId());
            try {
                storeImageService.deleteStoreImage(store.getId());
                log.info("기존 이미지 삭제 완료");
            } catch (StoreImageDeleteFailedException e) {
                log.error("스토어 이미지 삭제 실패: {}", e.getMessage());
            }


            StoreImageRequest storeImageRequest = StoreImageRequest.builder()
                    .images(imageFiles)
                    .build();

            storeImageService.uploadStoreImages(store, storeImageRequest);
            log.info("이미지 업데이트 완료 - Store ID: {}", store.getId());

        }

        return StoreResponse.from(store);
    }

    public List<StoreSummaryResponse> getStoreSummaries() {
        log.info("홈페이지 목록 조회 요청됨");
        return storeRepository.findAll().stream()
                .map(StoreSummaryResponse::from)
                .toList();
    }

    public List<StoreResponse> getAllStores() {
        log.info("모든 팝업스토어 조회 요청");
        return storeRepository.findAll().stream()
                .map(StoreResponse::from)
                .toList();
    }

    public StoreResponse getStoreById(Long id) {
        log.info("특정 팝업스토어 조회 요청 - ID: {}", id);

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);


        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request, MultipartFile[] imageFiles) {
        log.info("팝업스토어 생성 요청 - 이름: {}", request.name());

        StoreCategory category = storeCategoryRepository.findById(request.categoryId())
                .orElseThrow(StoreNotFoundException::new);

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(LocationNotFoundException::new);

        Status storeStatus = request.startDate().isAfter(java.time.LocalDate.now()) ? Status.PENDING : Status.RESOLVED;

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .category(category)
                .location(location)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(storeStatus)
                .contactNumber(request.contactNumber())
                .websiteUrl(request.websiteUrl())
                .snsUrl(request.snsUrl())
                .build();

        if (request.operatingHours() != null) {
            request.operatingHours().forEach(hour -> {
                OperatingHour operatingHour = OperatingHour.builder()
                        .day(hour.day())
                        .startTime(hour.startTime())
                        .endTime(hour.endTime())
                        .store(store)
                        .build();
                store.getOperatingHours().add(operatingHour);
            });
        }

        storeRepository.save(store);

        StoreImageRequest storeImageRequest = StoreImageRequest.builder()
                .images(Arrays.asList(imageFiles))
                .build();

        List<StoreImage> storeImages = storeImageService.uploadStoreImages(store, storeImageRequest);

        if (!storeImages.isEmpty()) {
            int thumbnailIndex = request.thumbnailImage() != null ? request.thumbnailImage() : 0;
            if (thumbnailIndex >= 0 && thumbnailIndex < storeImages.size()) {
                store.setImageUrl(storeImages.get(thumbnailIndex).getImageUrl());
                storeRepository.save(store);
            }
        }

        log.info("팝업스토어 생성 완료 - ID: {}", store.getId());

        return StoreResponse.from(store);
    }

/*    @Transactional
    public void deleteStore(Long id) {
        log.info("팝업스토어 삭제 요청 - ID: {}", id);

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        try {
            storeImageService.deleteStoreImage(id);
            log.info("스토어 이미지 삭제 완료");
        } catch (StoreImageDeleteFailedException e) {
            log.error("스토어 이미지 삭제 실패: {}", e.getMessage());
        }

        storeRepository.delete(store);
        log.info("팝업스토어 삭제 완료 - ID: {}", id);
    }*/

    @Transactional
    public void deleteStore(Long id) {

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        try {
            store.deleteStore();
        } catch (RuntimeException e) {
            log.error("스토어 isDeleted 상태 변환 중 에러 발생", e);
        }
    }

    public List<StoreSummaryResponse> getStoreSummariesByStatus(Status status) {
        return storeRepository.findByStatusAndDeletedFalse(status).stream()
                .map(StoreSummaryResponse::from)
                .toList();
    }
}
