package kr.co.pinup.storeoperatinghour.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StoreOperatingHourServiceTest {

    private StoreOperatingHourService storeOperatingHourService;

    @BeforeEach
    void setUp() {
        storeOperatingHourService = new StoreOperatingHourService();
    }

    @DisplayName("팝업스토어 운영 시간들을 생성해서 반환한다.")
    @Test
    void createOperatingHours() {
        // Arrange
        final String day = "월~금";
        final LocalTime startTime = LocalTime.of(10, 30);
        final LocalTime endTime = LocalTime.of(20, 0);

        final StoreCategory storeCategory = getStoreCategory();
        final Location location = getLocation();
        Store store = getStore(storeCategory, location);

        final StoreOperatingHourRequest operatingHour = getOperatingHourRequest(day, startTime, endTime);
        List<StoreOperatingHourRequest> operatingHourRequest = List.of(operatingHour);

        // Act
        final List<StoreOperatingHour> result =
                storeOperatingHourService.createOperatingHours(store, operatingHourRequest);

        // Assert
        assertThat(result).hasSize(1)
                .extracting("days", "startTime", "endTime")
                .contains(
                        tuple(day, startTime, endTime)
                );
    }

    private StoreCategory getStoreCategory() {
        return StoreCategory.builder()
                .name("패션")
                .build();
    }

    private Location getLocation() {
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

    private Store getStore(final StoreCategory storeCategory, final Location location) {
        return Store.builder()
                .name("팝업스토어")
                .description("팝업스토어가 올해도 여러분을 찾아갑니다!")
                .category(storeCategory)
                .location(location)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 7))
                .storeStatus(StoreStatus.PENDING)
                .build();
    }

    private StoreOperatingHourRequest getOperatingHourRequest(
            final String days,
            final LocalTime startTime,
            final LocalTime endTime
    ) {
        return StoreOperatingHourRequest.builder()
                .days(days)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}