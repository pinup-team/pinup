package kr.co.pinup.stores.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.store_images.service.StoreImageService;
import kr.co.pinup.store_operatingHour.model.dto.OperatingHourRequest;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreSummaryResponse;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServiceUnitTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StoreImageService storeImageService;

    @InjectMocks
    private StoreService storeService;

    private Store sampleStore;

    @BeforeEach
    void setUp() {
        StoreCategory category = mock(StoreCategory.class);
        lenient().when(category.getName()).thenReturn("뷰티");

        Location location = mock(Location.class);
        lenient().when(location.getDistrict()).thenReturn("강서구");


        sampleStore = mock(Store.class);
        lenient().when(sampleStore.getId()).thenReturn(1L);
        lenient().when(sampleStore.getName()).thenReturn("배민 계란프라이 데이");
        lenient().when(sampleStore.getDescription()).thenReturn("장보기도, 도전도, 선물도 한-계란 없는 날! 🥚+🥚");
        lenient().when(sampleStore.getCategory()).thenReturn(category);
        lenient().when(sampleStore.getLocation()).thenReturn(location);
        lenient().when(sampleStore.getStatus()).thenReturn(Status.PENDING);
        lenient().when(sampleStore.getStartDate()).thenReturn(LocalDate.of(2025, 6, 9));
        lenient().when(sampleStore.getEndDate()).thenReturn(LocalDate.of(2025, 6, 11));

    }

    @Test
    @DisplayName("특정 스토어 조회 - 성공")
    void getStoreByIdSuccess() {
        // given
        when(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore));

        // when
        StoreResponse response = storeService.getStoreById(1L);

        // then
        assertNotNull(response);
        assertEquals(sampleStore.getName(), response.name());
        assertEquals(sampleStore.getDescription(), response.description());
        verify(storeRepository, times(1)).findById(1L);
        // verify:  Mock 객체 메서드 호출 검증용
    }

    @Test
    @DisplayName("모든 스토어 조회 - 성공")
    void getAllStoresSummariesSuccess() {
        // given
        when(storeRepository.findAll()).thenReturn(List.of(sampleStore));

        // when
        List<StoreSummaryResponse> summaries = storeService.getStoreSummaries();

        // tehn
        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        assertEquals(sampleStore.getId(), summaries.get(0).id());
        assertEquals(sampleStore.getName(), summaries.get(0).name());
        assertEquals(sampleStore.getLocation().getDistrict(), summaries.get(0).district());
        assertEquals(sampleStore.getCategory().getName(), summaries.get(0).categoryName());
        assertEquals(sampleStore.getStartDate(), summaries.get(0).startDate());
        assertEquals(sampleStore.getEndDate(), summaries.get(0).endDate());
        assertEquals(sampleStore.getImageUrl(), summaries.get(0).imageUrl());
        assertEquals(sampleStore.getStatus(), summaries.get(0).status());

        verify(storeRepository, times(1)).findAll();

    }

    @Test
    @DisplayName("스토어 생성 - 성공")
    void createStoreSuccess() {
        // given
        StoreRequest request = new StoreRequest(
                "배민 계란프라이 데이",
                "장보기도, 도전도, 선물도 한-계란 없는 날! \uD83E\uDD5A+\uD83E\uDD5A",
                1L,
                1L,
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 6, 11),
                0,
                "010-0000-0000",
                "https://example.com",
                "https://www.instagram.com/baemin_official/",
                List.of(new OperatingHourRequest("월~금", LocalTime.of(10, 30), LocalTime.of(20, 0))));

        StoreCategory category = mock(StoreCategory.class);
        when(category.getId()).thenReturn(1L);

        Location location = mock(Location.class);
        when(location.getId()).thenReturn(1L);

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .category(category)
                .location(location)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(Status.PENDING)
                .contactNumber(request.contactNumber())
                .websiteUrl(request.websiteUrl())
                .snsUrl(request.snsUrl())
                .build();

//        when(storeCategoryRepository.findById(category.getId())).thenReturn(category);
        when(storeCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(storeRepository.save(any(Store.class))).thenReturn(store);

        // when
        StoreResponse response = storeService.createStore(request, new MultipartFile[0]);

        //t hen
        assertNotNull(response);
        assertEquals(request.name(), response.name());
        assertEquals(request.description(), response.description());
        verify(storeRepository, times(1)).save(any(Store.class));
        verify(storeImageService, never()).uploadStoreImages(any(), any());
    }



/*    @Test
    @DisplayName("스토어 생성 - 필수 필드 누락")
    void createStoreWithoutRequiredFileds() {
        // given
        StoreRequest request = new StoreRequest(
                "배민 계란프라이 데이",
                null,
                1L,
                1L,
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 6, 11),
                0,
                "010-0000-0000",
                "https://example.com",
                "https://www.instagram.com/baemin_official/",
                List.of(new OperatingHourRequest("월~금", LocalTime.of(10, 30), LocalTime.of(20, 0))));

        StoreCategory category = mock(StoreCategory.class);
        when(category.getId()).thenReturn(1L);
        when(storeCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Location location = mock(Location.class);
        when(location.getId()).thenReturn(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

    }*/


}


//    @Mock
//    private Sto




