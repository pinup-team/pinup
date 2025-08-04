package kr.co.pinup.stores;

import kr.co.pinup.locations.Location;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static kr.co.pinup.stores.model.enums.StoreStatus.PENDING;
import static kr.co.pinup.stores.model.enums.StoreStatus.RESOLVED;
import static org.assertj.core.api.Assertions.assertThat;

class StoreTest {

    @DisplayName("팝업스토어 상태를 업데이트한다")
    @Test
    void updateStatus() {
        // Arrange
        final StoreStatus newStatus = RESOLVED;
        final Store store = createStore();

        // Act
        store.updateStatus(newStatus);

        // Assert
        assertThat(store.getStoreStatus()).isEqualTo(newStatus);
    }

    @DisplayName("팝업스토어 수정시 필드를 전체 수정한다")
    @Test
    void update() {
        // Arrange
        final StoreUpdateRequest request = createStoreUpdateRequest();

        final Store store = createStore();

        // Act
        store.update(request, store.getCategory(), store.getLocation());

        // Assert
        assertThat(store).extracting(
                Store::getName,
                Store::getDescription,
                Store::getStartDate,
                Store::getEndDate
        ).containsOnly(
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );
    }

    @DisplayName("팝업스토어 삭제시 isDeleted가 true로 변경되어야 한다.")
    @Test
    void changeIsDeletedValueTrue() {
        // Arrange
        final Store store = createStore();

        // Act
        store.deleteStore(true);

        // Assert
        assertThat(store.isDeleted()).isTrue();
    }

    private Store createStore() {
        return Store.builder()
                .name("store")
                .description("description")
                .storeStatus(PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .category(createCategory())
                .location(createLocation())
                .build();
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

    private StoreUpdateRequest createStoreUpdateRequest() {
        return StoreUpdateRequest.builder()
                .name("Store")
                .description("Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(createOperatingHourRequest()))
                .deletedImageIds(List.of())
                .build();
    }

    private StoreOperatingHourRequest createOperatingHourRequest() {
        return StoreOperatingHourRequest.builder()
                .days("월~금")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(20, 0))
                .build();
    }
}