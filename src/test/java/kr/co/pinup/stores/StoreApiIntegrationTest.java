package kr.co.pinup.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeimages.repository.StoreImageRepository;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.storeoperatinghour.repository.StoreOperatingHourRepository;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.stores.model.enums.StoreStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StoreApiIntegrationTest {

    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreCategoryRepository categoryRepository;

    @Autowired
    private StoreImageRepository imageRepository;

    @Autowired
    private StoreOperatingHourRepository operatingHourRepository;

    @Autowired
    private LocationRepository locationRepository;

    @BeforeEach
    void setUp() {
        imageRepository.deleteAllInBatch();
        operatingHourRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        locationRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("POST /api/stores 요청 시 201 Created와 응답 정보를 반환한다.")
    @Test
    void createStore() throws Exception {
        // Arrange
        final StoreCategory category = categoryRepository.save(createCategory());
        final Location location = locationRepository.save(createLocation());

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = getStoreRequest(category.getId(), location.getId());
        final MockMultipartFile request = new MockMultipartFile(
                "storeRequest",
                "storeRequest.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores")
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @WithMockMember
    @DisplayName("POST /api/stores 요청 시 사용자 권한이면 403을 응답한다.")
    @Test
    void shouldReturnForbiddenWhenRoleUserOnCreateStore() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = getStoreRequest();
        final MockMultipartFile request = new MockMultipartFile(
                "storeRequest",
                "storeRequest.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        final ResultActions result = mockMvc.perform(multipart("/api/stores")
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @DisplayName("GET /api/stores 요청시 팝업스토어 전체 데이터를 응답한다")
    @Test
    void getStores() throws Exception {
        // Arrange
        final StoreCategory category = categoryRepository.save(createCategory());
        final Location location = locationRepository.save(createLocation());

        final Store store1 = getStore("store1", "description1", PENDING, category, location);
        final Store store2 = getStore("store2", "description2", PENDING, category, location);
        storeRepository.saveAll(List.of(store1, store2));

        // Act & Assert
        mockMvc.perform(get("/api/stores"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists())
                .andExpect(jsonPath("$[0].snsUrl").exists())
                .andExpect(jsonPath("$[0].viewCount").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].location").exists())
                .andExpect(jsonPath("$[0].operatingHours").exists())
                .andExpect(jsonPath("$[0].storeImages").exists());
    }

    @DisplayName("GET /api/stores/summary?limit= 요청시 팝업스토어 요약 썸네일 데이터를 응답한다")
    @Test
    void getStoreThumbnails() throws Exception {
        // Arrange
        final StoreCategory category = categoryRepository.save(createCategory());
        final Location location = locationRepository.save(createLocation());

        final Store store1 = getStoreWithThumbnail("store1", "description1", PENDING, category, location);
        final Store store2 = getStoreWithThumbnail("store2", "description2", RESOLVED, category, location);
        final Store store3 = getStoreWithThumbnail("store2", "description2", DISMISSED, category, location);
        storeRepository.saveAll(List.of(store1, store2, store3));

        // Act & Assert
        mockMvc.perform(get("/api/stores/summary")
                        .param("limit", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists())
                .andExpect(jsonPath("$[0].categoryName").exists())
                .andExpect(jsonPath("$[0].sigungu").exists())
                .andExpect(jsonPath("$[0].thumbnailImage").exists());
    }

    @DisplayName("GET /api/stores/{id} 요청시 해당 ID 팝업스토어 데이터를 응답한다")
    @Test
    void getStoreById() throws Exception {
        // Arrange
        final StoreCategory category = categoryRepository.save(createCategory());
        final Location location = locationRepository.save(createLocation());

        final Store store = getStore("store", "description", RESOLVED, category, location);
        storeRepository.save(store);

        // Act & Assert
        mockMvc.perform(get("/api/stores/{id}", store.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.snsUrl").exists())
                .andExpect(jsonPath("$.viewCount").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.location").exists())
                .andExpect(jsonPath("$.operatingHours").exists())
                .andExpect(jsonPath("$.storeImages").exists());
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
            final String store,
            final String description,
            final StoreStatus storeStatus,
            final StoreCategory category,
            final Location location
    ) {
        return Store.builder()
                .name(store)
                .description(description)
                .storeStatus(storeStatus)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .category(category)
                .location(location)
                .build();
    }

    private Store getStoreWithThumbnail(
            final String store,
            final String description,
            final StoreStatus storeStatus,
            final StoreCategory category,
            final Location location
    ) {
        final Store storeEntity = getStore(store, description, storeStatus, category, location);

        final StoreImage storeImage = StoreImage.builder()
                .imageUrl("http://127.0.0.1:4566/pinup/store/image1.png")
                .isThumbnail(true)
                .build();

        storeEntity.addImages(List.of(storeImage));

        return storeEntity;
    }

    private StoreRequest getStoreRequest() {
        return StoreRequest.builder()
                .name("store")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
    }

    private StoreRequest getStoreRequest(final long categoryId, final long locationId) {
        return StoreRequest.builder()
                .name("store")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(categoryId)
                .locationId(locationId)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
    }

    private ErrorResponse getErrorResponse(ResultActions result) {
        return (ErrorResponse) Objects.requireNonNull(result.andReturn()
                        .getModelAndView())
                .getModel()
                .get("error");
    }
}
