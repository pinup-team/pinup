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
import kr.co.pinup.store_operatingHour.model.dto.OperatingHourRequest;
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

import java.time.LocalDate;
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

        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        if (request.getCategoryId() != null) {
            StoreCategory category = storeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(StoreCategoryNotFoundException::new);
            store.setCategory(category);
        }

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(LocationNotFoundException::new);
            store.setLocation(location);
        }

        store.setName(request.getName());
        store.setDescription(request.getDescription());
        store.setContactNumber(request.getContactNumber());
        store.setWebsiteUrl(request.getWebsiteUrl());
        store.setSnsUrl(request.getSnsUrl());
        store.setStartDate(request.getStartDate());
        store.setEndDate(request.getEndDate());

        if (request.getOperatingHours() != null) {
            store.getOperatingHours().clear();
            for (OperatingHourRequest hourRequest : request.getOperatingHours()) {
                OperatingHour operatingHour = OperatingHour.builder()
                        .day(hourRequest.day())
                        .startTime(hourRequest.startTime())
                        .endTime(hourRequest.endTime())
                        .store(store)
                        .build();
                store.getOperatingHours().add(operatingHour);
            }
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            try {
                storeImageService.deleteStoreImage(store.getId());

                StoreImageRequest imageRequest = StoreImageRequest.builder()
                        .images(imageFiles)
                        .build();
                List<StoreImage> storeImages = storeImageService.uploadStoreImages(store, imageRequest);

                int thumbnailIndex = request.getThumbnailImage() != null ? request.getThumbnailImage() : 0;
                if (!storeImages.isEmpty() && thumbnailIndex >= 0 && thumbnailIndex < storeImages.size()) {
                    store.setImageUrl(storeImages.get(thumbnailIndex).getImageUrl());
                }
            } catch (StoreImageDeleteFailedException e) {
                log.error("이미지 삭제 실패: {}", e.getMessage());
                throw e;
            }
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

        try {
            StoreCategory category = storeCategoryRepository.findById(request.categoryId())
                    .orElseThrow(StoreNotFoundException::new);
            log.info("카테고리 불러오기 완료");

            Location location = locationRepository.findById(request.locationId())
                    .orElseThrow(LocationNotFoundException::new);
            log.info("위치 불러오기 완료");

            Status storeStatus = request.startDate().isAfter(LocalDate.now()) ? Status.PENDING : Status.RESOLVED;
            log.info("상태 설정 완료: {}", storeStatus);

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
            log.info("스토어 빌드 완료");

            if (request.operatingHours() != null && !request.operatingHours().isEmpty()) {
                List<OperatingHour> operatingHours = request.operatingHours().stream()
                        .map(hourRequest -> {
                            return OperatingHour.builder()
                                    .day(hourRequest.day())
                                    .startTime(hourRequest.startTime())
                                    .endTime(hourRequest.endTime())
                                    .store(store)
                                    .build();
                        })
                        .toList();
                store.getOperatingHours().addAll(operatingHours);
            }

            storeRepository.save(store);
            log.info("스토어 저장 완료 - ID: {}", store.getId());

            if (imageFiles != null && imageFiles.length > 0) {
                StoreImageRequest storeImageRequest = StoreImageRequest.builder()
                        .images(Arrays.asList(imageFiles))
                        .build();
                log.info("스토어 이미지 빌드 완료");

                List<StoreImage> storeImages = storeImageService.uploadStoreImages(store, storeImageRequest);
                log.info("이미지 업로드 완료 - 개수: {}", storeImages.size());
                store.setStoreImages(storeImages);

                int thumbnailIndex = request.thumbnailImage() != null ? request.thumbnailImage() : 0;
                if (!storeImages.isEmpty() && thumbnailIndex >= 0 && thumbnailIndex < storeImages.size()) {
                    store.setImageUrl(storeImages.get(thumbnailIndex).getImageUrl());
                    log.info("썸네일 설정 완료: {}", store.getImageUrl());
                }
                storeRepository.save(store);
            }

            return StoreResponse.from(store);
        } catch (Exception e) {
            log.error("❌ 팝업스토어 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }

        //TODO 예외처리 분리하기
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
