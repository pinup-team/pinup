package kr.co.pinup.stores.scheduler;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static kr.co.pinup.stores.model.enums.StoreStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StoreStatusSchedulerTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreCategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @DisplayName("오늘 날짜가 시작 날짜와 같거나 이후라면 스토어 상태를 RESOLVED로 업데이트한다.")
    @Test
    void updateStoreStatusResolved() {
        // Arrange
        final StoreStatusScheduler scheduler = new StoreStatusScheduler(
                storeRepository,
                () -> LocalDate.of(2025, 10, 7)
        );

        final StoreCategory category = createCategory();
        categoryRepository.save(category);

        final Location location = createLocation();
        locationRepository.save(location);

        final Store store = getStore(
                PENDING,
                LocalDate.of(2025, 10, 7),
                LocalDate.of(2025, 10, 20),
                category,
                location
        );
        storeRepository.save(store);

        // Act
        scheduler.updateStoreStatuses();

        // Assert
        final Store result = storeRepository.findById(store.getId()).get();

        assertThat(result).isNotNull();
        assertThat(result.getStoreStatus()).isEqualTo(RESOLVED);
    }

    @DisplayName("오늘 날짜가 종료 날짜보다 이후라면 스토어 상태를 DISMISSED로 업데이트한다.")
    @Test
    void updateStoreStatusDismissed() {
        // Arrange
        final StoreStatusScheduler scheduler = new StoreStatusScheduler(
                storeRepository,
                () -> LocalDate.of(2025, 10, 21)
        );

        final StoreCategory category = createCategory();
        categoryRepository.save(category);

        final Location location = createLocation();
        locationRepository.save(location);

        final Store store = getStore(
                RESOLVED,
                LocalDate.of(2025, 10, 7),
                LocalDate.of(2025, 10, 20),
                category,
                location
        );
        storeRepository.save(store);

        // Act
        scheduler.updateStoreStatuses();

        // Assert
        final Store result = storeRepository.findById(store.getId()).get();

        assertThat(result).isNotNull();
        assertThat(result.getStoreStatus()).isEqualTo(DISMISSED);
    }

    @DisplayName("오늘 날짜가 시작 날짜보다 이전이면 스토어 상태를 업데이트하지 않는다.")
    @Test
    void notUpdateStoreStatusWithPending() {
        // Arrange
        final StoreStatusScheduler scheduler = new StoreStatusScheduler(
                storeRepository,
                () -> LocalDate.of(2025, 10, 6)
        );

        final StoreCategory category = createCategory();
        categoryRepository.save(category);

        final Location location = createLocation();
        locationRepository.save(location);

        final Store store = getStore(
                PENDING,
                LocalDate.of(2025, 10, 7),
                LocalDate.of(2025, 10, 20),
                category,
                location
        );
        storeRepository.save(store);

        // Act
        scheduler.updateStoreStatuses();

        // Assert
        final Store result = storeRepository.findById(store.getId()).get();

        assertThat(result).isNotNull();
        assertThat(result.getStoreStatus()).isEqualTo(PENDING);
    }

    @DisplayName("오늘 날짜가 시작 & 종료 날짜 사이면 스토어 상태를 업데이트하지 않는다.")
    @Test
    void notUpdateStoreStatusWithResolved() {
        // Arrange
        final StoreStatusScheduler scheduler = new StoreStatusScheduler(
                storeRepository,
                () -> LocalDate.of(2025, 10, 10)
        );

        final StoreCategory category = createCategory();
        categoryRepository.save(category);

        final Location location = createLocation();
        locationRepository.save(location);

        final Store store = getStore(
                RESOLVED,
                LocalDate.of(2025, 10, 7),
                LocalDate.of(2025, 10, 20),
                category,
                location
        );
        storeRepository.save(store);

        // Act
        scheduler.updateStoreStatuses();

        // Assert
        final Store result = storeRepository.findById(store.getId()).get();

        assertThat(result).isNotNull();
        assertThat(result.getStoreStatus()).isEqualTo(RESOLVED);
    }

    private StoreCategory createCategory() {
        return StoreCategory.builder()
                .name("뷰티")
                .build();
    }

    private Location createLocation() {
        return Location.builder()
                .name("서울 송파구 올림픽로 300")
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .longitude(127.104302)
                .latitude(37.513713)
                .build();
    }

    private Store getStore(
            final StoreStatus storeStatus,
            final LocalDate startDate,
            final LocalDate endDate,
            final StoreCategory category,
            final Location location
    ) {
        return Store.builder()
                .name("store 1")
                .description("store description 1")
                .storeStatus(storeStatus)
                .startDate(startDate)
                .endDate(endDate)
                .snsUrl("https://instgram.com/test")
                .category(category)
                .location(location)
                .build();
    }
}