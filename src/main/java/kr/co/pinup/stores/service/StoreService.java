package kr.co.pinup.stores.service;

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
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreThumbnailResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreCategoryService categoryService;
    private final LocationService locationService;
    private final StoreImageService storeImageService;
    private final StoreOperatingHourService operatingHourService;

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

    public List<StoreThumbnailResponse> getStoresThumbnailWithLimit(final int limit) {
        return storeRepository.findAllByIsDeletedFalse().stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .limit(limit)
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    public List<StoreThumbnailResponse> getStoresSortedByStatusPriority() {
        return storeRepository.findAllByIsDeletedFalse().stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    public List<StoreThumbnailResponse> getStoresByStatus(StoreStatus status) {
        return storeRepository.findAllByStoreStatusAndIsDeletedFalse(status).stream()
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    public StoreResponse getStoreById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(StoreNotFoundException::new);

        return StoreResponse.from(store);
    }

    public List<StoreThumbnailResponse> getStoresByStatusAndLocationBySigungu(
            final StoreStatus selectedStatus, final String sigungu) {
        return storeRepository.findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, selectedStatus)
                .stream()
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    public List<StoreThumbnailResponse> getStoresByLocationBySigungu(final String sigungu) {
        return storeRepository.findAllByLocation_SigunguAndIsDeletedFalse(sigungu).stream()
                .sorted(Comparator.comparingInt(store -> getStoreStatusOrder(store.getStoreStatus())))
                .map(StoreThumbnailResponse::from)
                .toList();
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request, List<MultipartFile> images) {
        final StoreCategory category = categoryService.findCategoryById(request.categoryId());
        final Location location = locationService.getLocation(request.locationId());

        final LocalDate now = LocalDate.now();
        final LocalDate startDate = request.startDate();
        StoreStatus storeStatus;
        if (request.startDate().isAfter(now)) {
            storeStatus = StoreStatus.PENDING;
        } else if (request.endDate().isBefore(now)) {
            storeStatus = StoreStatus.DISMISSED;
        } else {
            storeStatus = StoreStatus.RESOLVED;
        }
        log.debug("createStore storeStatus={}", storeStatus);

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
        storeRepository.save(store);

        final List<StoreOperatingHour> operatingHours =
                operatingHourService.createOperatingHours(store, request.operatingHours());
        log.debug("createStore operatingHours={}", operatingHours);
        store.addOperatingHours(operatingHours);

        final List<StoreImage> storeImages =
                storeImageService.createUploadImages(store, images, request.thumbnailIndex());
        log.debug("createStore storeImages={}", storeImages);
        store.addImages(storeImages);

        return StoreResponse.from(store);
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

}
