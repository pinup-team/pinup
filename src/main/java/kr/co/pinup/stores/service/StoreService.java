package kr.co.pinup.stores.service;

import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.service.LocationService;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.service.StoreCategoryService;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeimages.service.StoreImageService;
import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.storeoperatinghour.service.StoreOperatingHourService;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.dto.*;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private static final String S3_PATH_PREFIX = "store";

    private final StoreRepository storeRepository;
    private final StoreCategoryService categoryService;
    private final LocationService locationService;
    private final StoreImageService storeImageService;
    private final StoreOperatingHourService operatingHourService;
    private final S3Service s3Service;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Transactional(readOnly = true)
    public List<StoreResponse> getStores() {
        return storeRepository.findAllByIsDeletedFalse().stream()
                .map(StoreResponse::from)
                .toList();
    }

    public List<StoreThumbnailResponse> findAll(final StoreStatus selectedStatus, final String sigungu) {
        if (selectedStatus != null && !sigungu.equals("all")) {
            return getStoresByStatusAndLocationBySigungu(selectedStatus, sigungu);
        } else if (selectedStatus != null) {
            return getStoresByStatus(selectedStatus);
        } else if (!sigungu.equals("all")) {
            return getStoresByLocationBySigungu(sigungu);
        }

        return getStoresSortedByStatusPriority();
    }

    @Transactional(readOnly = true)
    public List<StoreThumbnailResponse> getStoresThumbnailWithLimit(final int limit) {
        return storeRepository.findAllByIsDeletedFalse().stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .limit(limit)
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreThumbnailResponse> getStoresSortedByStatusPriority() {
        return storeRepository.findAllByIsDeletedFalse().stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreThumbnailResponse> getStoresByStatus(StoreStatus status) {
        return storeRepository.findAllByStoreStatusAndIsDeletedFalse(status).stream()
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public List<StoreThumbnailResponse> getStoresByStatusAndLocationBySigungu(
            final StoreStatus selectedStatus, final String sigungu) {
        return storeRepository.findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, selectedStatus)
                .stream()
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreThumbnailResponse> getStoresByLocationBySigungu(final String sigungu) {
        return storeRepository.findAllByLocation_SigunguAndIsDeletedFalse(sigungu).stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    public StoreCreateResponse createStore(StoreRequest request, List<MultipartFile> images, LocalDate today) {
        final StoreStatus storeStatus = calculateStatus(today, request.startDate(), request.endDate());
        log.debug("storeStatus: {}", storeStatus);

        final List<CompletableFuture<String>> s3UploadFutures = asyncS3UploadFiles(images);

        final StoreCategory category = categoryService.findCategoryById(request.categoryId());
        final Location location = locationService.getLocation(request.locationId());

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .storeStatus(storeStatus)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .websiteUrl(request.websiteUrl())
                .snsUrl(request.snsUrl())
                .category(category)
                .location(location)
                .build();

        final List<String> uploadUrls = s3UploadFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        createStoreTransactional(request, uploadUrls, store);

        return StoreCreateResponse.from(store);
    }

    @Transactional
    public void createStoreTransactional(
            final StoreRequest request,
            final List<String> uploadUrls,
            final Store store
    ) {
        final List<StoreOperatingHour> operatingHours =
                operatingHourService.createOperatingHours(store, request.operatingHours());
        log.debug("createStore operatingHours={}", operatingHours);
        store.addOperatingHours(operatingHours);

        final List<StoreImage> storeImages =
                storeImageService.createUploadImages(store, uploadUrls, request.thumbnailIndex());
        log.debug("createStore storeImages={}", storeImages);
        store.addImages(storeImages);

        storeRepository.save(store);
    }

    @Transactional
    public StoreResponse updateStore(Long id, StoreUpdateRequest request, List<MultipartFile> images) {
        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        final StoreCategory category = categoryService.findCategoryById(request.categoryId());
        final Location location = locationService.getLocation(request.locationId());

        store.update(request, category, location);

        store.operatingHoursClear();

        final List<StoreOperatingHour> operatingHours =
                operatingHourService.createOperatingHours(store, request.operatingHours());
        log.debug("createStore operatingHours: {}", operatingHours);
        store.addOperatingHours(operatingHours);

        storeImageService.removeStoreImage(id, request.deletedImageIds());

        List<StoreImage> storeImages;
        if (images.isEmpty()) {
            log.debug("updateStore not add images");
            storeImages = storeImageService.updateThumbnailImage(id, request.thumbnailId());
        } else {
            log.debug("updateStore not add images or add images");
            storeImages = storeImageService.updateUploadImages(
                    store, images, request.thumbnailId(), request.thumbnailIndex()
            );
        }
        log.debug("createStore storeImages={}", storeImages);
        store.addImages(storeImages);

        return StoreResponse.from(store);
    }

    @Transactional
    public void deleteStore(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);
        
        store.deleteStore(true);
    }

    private int getStoreStatusOrder(final StoreStatus status) {
        return switch (status) {
            case RESOLVED -> 0;
            case PENDING -> 1;
            case DISMISSED -> 2;
        };
    }

    private StoreStatus calculateStatus(final LocalDate now, final LocalDate startDate, final LocalDate endDate) {
        if (startDate.isAfter(now)) {
            return StoreStatus.PENDING;
        } else if (endDate.isBefore(now)) {
            return StoreStatus.DISMISSED;
        }

        return StoreStatus.RESOLVED;
    }

    private List<CompletableFuture<String>> asyncS3UploadFiles(List<MultipartFile> images) {
        return images.stream()
                .map(image -> CompletableFuture.supplyAsync(
                        () -> s3Service.uploadFile(image, S3_PATH_PREFIX),
                        taskExecutor
                ))
                .collect(Collectors.toList());
    }
}
