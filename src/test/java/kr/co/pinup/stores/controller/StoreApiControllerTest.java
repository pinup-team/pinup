package kr.co.pinup.stores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.ExceptionHandlerConfig;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.storecategories.model.dto.StoreCategoryResponse;
import kr.co.pinup.storeimages.model.dto.StoreImageResponse;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourResponse;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.dto.StoreRequest;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreThumbnailResponse;
import kr.co.pinup.stores.model.dto.StoreUpdateRequest;
import kr.co.pinup.stores.service.StoreService;
import kr.co.pinup.support.RestDocsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import wiremock.com.jayway.jsonpath.JsonPath;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static kr.co.pinup.stores.model.enums.StoreStatus.RESOLVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StoreApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
@ExtendWith(RestDocumentationExtension.class)
@Import({RestDocsSupport.class, LoggerConfig.class, ExceptionHandlerConfig.class})
public class StoreApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    @MockitoBean
    private StoreService storeService;

    @BeforeEach
    void setUp(final WebApplicationContext context,
               final RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @DisplayName("팝업스토어 전체를 조회한다")
    @Test
    void getStores() throws Exception {
        // Arrange
        final StoreResponse storeResponse1 = getStoreResponse();
        final StoreResponse storeResponse2 = getStoreResponse();

        final List<StoreResponse> response = List.of(storeResponse1, storeResponse2);

        given(storeService.getStores()).willReturn(response);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists())
                .andExpect(jsonPath("$[0].websiteUrl").exists())
                .andExpect(jsonPath("$[0].snsUrl").exists())
                .andExpect(jsonPath("$[0].viewCount").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].location").exists())
                .andExpect(jsonPath("$[0].operatingHours").exists())
                .andExpect(jsonPath("$[0].storeImages").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());

        then(storeService).should(times(1))
                .getStores();

        result.andDo(restDocs.document(
                responseFields(
                        fieldWithPath("[].id").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("[].name").type(STRING).description("팝업스토어명"),
                        fieldWithPath("[].description").type(STRING).description("팝업스토어 설명"),
                        fieldWithPath("[].status").type(STRING).description("팝업스토어 상태"),
                        fieldWithPath("[].startDate").type(STRING).description("팝업스토어 시작날짜"),
                        fieldWithPath("[].endDate").type(STRING).description("팝업스토어 종료날짜"),
                        fieldWithPath("[].websiteUrl").type(STRING).optional().description("팝업스토어 참고 홈페이지 주소"),
                        fieldWithPath("[].snsUrl").type(STRING).optional().description("팝업스토어 참고 SNS 주소"),
                        fieldWithPath("[].viewCount").type(NUMBER).description("팝업스토어 조회수"),
                        fieldWithPath("[].category.id").type(NUMBER).description("팝업스토어 카테고리 아이디"),
                        fieldWithPath("[].category.name").type(STRING).description("팝업스토어 카테고리명"),
                        fieldWithPath("[].category.createdAt").type(STRING).description("팝업스토어 카테고리 등록날짜"),
                        fieldWithPath("[].category.updatedAt").type(STRING).optional().description("팝업스토어 카테고리 수정날짜"),
                        fieldWithPath("[].location.id").type(NUMBER).description("위치 아이디"),
                        fieldWithPath("[].location.name").type(STRING).description("위치명"),
                        fieldWithPath("[].location.zonecode").type(STRING).description("위치 우편번호"),
                        fieldWithPath("[].location.sido").type(STRING).description("위치 시/도"),
                        fieldWithPath("[].location.sigungu").type(STRING).description("위치 시/군/구"),
                        fieldWithPath("[].location.latitude").type(NUMBER).description("위치 위도"),
                        fieldWithPath("[].location.longitude").type(NUMBER).description("위치 경도"),
                        fieldWithPath("[].location.address").type(STRING).description("위치 주소"),
                        fieldWithPath("[].location.addressDetail").type(STRING).description("위치 상세주소"),
                        fieldWithPath("[].operatingHours[].days").type(STRING).description("팝업스토어 운영 날짜"),
                        fieldWithPath("[].operatingHours[].startTime").type(STRING).description("팝업스토어 운영 오픈시간"),
                        fieldWithPath("[].operatingHours[].endTime").type(STRING).description("팝업스토어 운영 마감시간"),
                        fieldWithPath("[].storeImages[].id").type(NUMBER).description("팝업스토어 이미지 아이디"),
                        fieldWithPath("[].storeImages[].storeId").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("[].storeImages[].imageUrl").type(STRING).description("팝업스토어 이미지 URL"),
                        fieldWithPath("[].storeImages[].isThumbnail").type(BOOLEAN).description("팝업스토어 썸네일 이미지 여부"),
                        fieldWithPath("[].createdAt").type(STRING).description("팝업스토어 등록날짜"),
                        fieldWithPath("[].updatedAt").type(STRING).optional().description("팝업스토어 수정날짜")
                )
        ));
    }

    @DisplayName("limit 갯수만큼 팝업스토어 요약 정보를 반환한다")
    @Test
    void getStoreThumbnails() throws Exception {
        // Arrange
        final int limit = 2;

        final StoreThumbnailResponse response1 = getStoreThumbnailResponse();
        final StoreThumbnailResponse response2 = getStoreThumbnailResponse();

        final List<StoreThumbnailResponse> responses = List.of(response1, response2);

        given(storeService.getStoresThumbnailWithLimit(limit)).willReturn(responses);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/stores/summary")
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(limit))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].endDate").exists())
                .andExpect(jsonPath("$[0].categoryName").exists())
                .andExpect(jsonPath("$[0].sigungu").exists())
                .andExpect(jsonPath("$[0].thumbnailImage").exists());

        then(storeService).should(times(1))
                .getStoresThumbnailWithLimit(limit);

        result.andDo(restDocs.document(
                responseFields(
                        fieldWithPath("[].id").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("[].name").type(STRING).description("팝업스토어명"),
                        fieldWithPath("[].status").type(STRING).description("팝업스토어 상태"),
                        fieldWithPath("[].startDate").type(STRING).description("팝업스토어 시작날짜"),
                        fieldWithPath("[].endDate").type(STRING).description("팝업스토어 종료날짜"),
                        fieldWithPath("[].categoryName").type(STRING).description("팝업스토어 카테고리명"),
                        fieldWithPath("[].sigungu").type(STRING).description("팝업스토어 위치 시/군/구"),
                        fieldWithPath("[].thumbnailImage").type(STRING).description("팝업스토어 썸네일 이미지 URL")
                )
        ));
    }

    @DisplayName("팝업스토어 ID로 조회")
    @Test
    void getStoreById() throws Exception {
        // Arrange
        final long id = 1L;
        final StoreResponse storeResponse = getStoreResponse();

        given(storeService.getStoreById(id)).willReturn(storeResponse);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/stores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.websiteUrl").exists())
                .andExpect(jsonPath("$.snsUrl").exists())
                .andExpect(jsonPath("$.viewCount").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.location").exists())
                .andExpect(jsonPath("$.operatingHours").exists())
                .andExpect(jsonPath("$.storeImages").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        then(storeService).should(times(1))
                .getStoreById(id);

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("id").description("팝업스토어 아이디")
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("name").type(STRING).description("팝업스토어명"),
                        fieldWithPath("description").type(STRING).description("팝업스토어 설명"),
                        fieldWithPath("status").type(STRING).description("팝업스토어 상태"),
                        fieldWithPath("startDate").type(STRING).description("팝업스토어 시작날짜"),
                        fieldWithPath("endDate").type(STRING).description("팝업스토어 종료날짜"),
                        fieldWithPath("websiteUrl").type(STRING).optional().description("팝업스토어 참고 홈페이지 주소"),
                        fieldWithPath("snsUrl").type(STRING).optional().description("팝업스토어 참고 SNS 주소"),
                        fieldWithPath("viewCount").type(NUMBER).description("팝업스토어 조회수"),
                        fieldWithPath("category.id").type(NUMBER).description("팝업스토어 카테고리 아이디"),
                        fieldWithPath("category.name").type(STRING).description("팝업스토어 카테고리명"),
                        fieldWithPath("category.createdAt").type(STRING).description("팝업스토어 카테고리 등록날짜"),
                        fieldWithPath("category.updatedAt").type(STRING).optional().description("팝업스토어 카테고리 수정날짜"),
                        fieldWithPath("location.id").type(NUMBER).description("위치 아이디"),
                        fieldWithPath("location.name").type(STRING).description("위치명"),
                        fieldWithPath("location.zonecode").type(STRING).description("위치 우편번호"),
                        fieldWithPath("location.sido").type(STRING).description("위치 시/도"),
                        fieldWithPath("location.sigungu").type(STRING).description("위치 시/군/구"),
                        fieldWithPath("location.latitude").type(NUMBER).description("위치 위도"),
                        fieldWithPath("location.longitude").type(NUMBER).description("위치 경도"),
                        fieldWithPath("location.address").type(STRING).description("위치 주소"),
                        fieldWithPath("location.addressDetail").type(STRING).description("위치 상세주소"),
                        fieldWithPath("operatingHours[].days").type(STRING).description("팝업스토어 운영 날짜"),
                        fieldWithPath("operatingHours[].startTime").type(STRING).description("팝업스토어 운영 오픈시간"),
                        fieldWithPath("operatingHours[].endTime").type(STRING).description("팝업스토어 운영 마감시간"),
                        fieldWithPath("storeImages[].id").type(NUMBER).description("팝업스토어 이미지 아이디"),
                        fieldWithPath("storeImages[].storeId").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("storeImages[].imageUrl").type(STRING).description("팝업스토어 이미지 URL"),
                        fieldWithPath("storeImages[].isThumbnail").type(BOOLEAN).description("팝업스토어 썸네일 이미지 여부"),
                        fieldWithPath("createdAt").type(STRING).description("팝업스토어 등록날짜"),
                        fieldWithPath("updatedAt").type(STRING).optional().description("팝업스토어 수정날짜")
                )
        ));
    }

    @DisplayName("존재하지 않는 ID로 팝업스토어 조회시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void getStoreByIdWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        final long id = Long.MAX_VALUE;

        given(storeService.getStoreById(id)).willThrow(new StoreNotFoundException());

        // Act & Assert
        mockMvc.perform(get("/api/stores/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(storeService).should(times(1))
                .getStoreById(id);
    }

    @DisplayName("팝업스토어를 정상적으로 저장한다")
    @Test
    void createStore() throws Exception {
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

        final StoreResponse storeResponse = getStoreResponse();

        given(storeService.createStore(any(StoreRequest.class), any(List.class)))
                .willReturn(storeResponse);

        // Act & Assert
        final ResultActions result = mockMvc.perform(multipart("/api/stores")
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.websiteUrl").exists())
                .andExpect(jsonPath("$.snsUrl").exists())
                .andExpect(jsonPath("$.viewCount").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.location").exists())
                .andExpect(jsonPath("$.operatingHours").exists())
                .andExpect(jsonPath("$.storeImages").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        then(storeService).should(times(1))
                .createStore(any(StoreRequest.class), any(List.class));

        result.andDo(restDocs.document(
                requestParts(
                        partWithName("images").description("이미지들"),
                        partWithName("storeRequest").description("팝업스토어 등록 정보")
                ),
                requestPartFields("storeRequest",
                        fieldWithPath("name")
                                .description("팝업스토어명")
                                .attributes(key("constraints").value(getConstraintDescription("name"))),
                        fieldWithPath("description")
                                .description("팝업스토어 상세설명")
                                .attributes(key("constraints").value(getConstraintDescription("description"))),
                        fieldWithPath("startDate")
                                .description("팝업스토어 시작날짜")
                                .attributes(key("constraints").value(getConstraintDescription("startDate"))),
                        fieldWithPath("endDate")
                                .description("팝업스토어 종료날짜")
                                .attributes(key("constraints").value(getConstraintDescription("endDate"))),
                        fieldWithPath("websiteUrl")
                                .description("팝업스토어 참고 홈페이지 주소")
                                .optional(),
                        fieldWithPath("snsUrl")
                                .description("팝업스토어 참고 SNS 주소")
                                .optional(),
                        fieldWithPath("thumbnailIndex")
                                .description("썸네일 선택 인덱스")
                                .attributes(key("constraints").value(getConstraintDescription("thumbnailIndex"))),
                        fieldWithPath("categoryId")
                                .description("팝업스토어 카테고리 아이디")
                                .attributes(key("constraints").value(getConstraintDescription("categoryId"))),
                        fieldWithPath("locationId")
                                .description("위치 아이디")
                                .attributes(key("constraints").value(getConstraintDescription("locationId"))),
                        fieldWithPath("operatingHours[0].days")
                                .description("팝업스토어 운영 요일"),
                        fieldWithPath("operatingHours[0].startTime")
                                .description("팝업스토어 운영 오픈 시간"),
                        fieldWithPath("operatingHours[0].endTime")
                                .description("팝업스토어 운영 마감 시간")
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("name").type(STRING).description("팝업스토어명"),
                        fieldWithPath("description").type(STRING).description("팝업스토어 설명"),
                        fieldWithPath("status").type(STRING).description("팝업스토어 상태"),
                        fieldWithPath("startDate").type(STRING).description("팝업스토어 시작날짜"),
                        fieldWithPath("endDate").type(STRING).description("팝업스토어 종료날짜"),
                        fieldWithPath("websiteUrl").type(STRING).optional().description("팝업스토어 참고 홈페이지 주소"),
                        fieldWithPath("snsUrl").type(STRING).optional().description("팝업스토어 참고 SNS 주소"),
                        fieldWithPath("viewCount").type(NUMBER).description("팝업스토어 조회수"),
                        fieldWithPath("category.id").type(NUMBER).description("팝업스토어 카테고리 아이디"),
                        fieldWithPath("category.name").type(STRING).description("팝업스토어 카테고리명"),
                        fieldWithPath("category.createdAt").type(STRING).description("팝업스토어 카테고리 등록날짜"),
                        fieldWithPath("category.updatedAt").type(STRING).optional().description("팝업스토어 카테고리 수정날짜"),
                        fieldWithPath("location.id").type(NUMBER).description("위치 아이디"),
                        fieldWithPath("location.name").type(STRING).description("위치명"),
                        fieldWithPath("location.zonecode").type(STRING).description("위치 우편번호"),
                        fieldWithPath("location.sido").type(STRING).description("위치 시/도"),
                        fieldWithPath("location.sigungu").type(STRING).description("위치 시/군/구"),
                        fieldWithPath("location.latitude").type(NUMBER).description("위치 위도"),
                        fieldWithPath("location.longitude").type(NUMBER).description("위치 경도"),
                        fieldWithPath("location.address").type(STRING).description("위치 주소"),
                        fieldWithPath("location.addressDetail").type(STRING).description("위치 상세주소"),
                        fieldWithPath("operatingHours[].days").type(STRING).description("팝업스토어 운영 날짜"),
                        fieldWithPath("operatingHours[].startTime").type(STRING).description("팝업스토어 운영 오픈시간"),
                        fieldWithPath("operatingHours[].endTime").type(STRING).description("팝업스토어 운영 마감시간"),
                        fieldWithPath("storeImages[].id").type(NUMBER).description("팝업스토어 이미지 아이디"),
                        fieldWithPath("storeImages[].storeId").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("storeImages[].imageUrl").type(STRING).description("팝업스토어 이미지 URL"),
                        fieldWithPath("storeImages[].isThumbnail").type(BOOLEAN).description("팝업스토어 썸네일 이미지 여부"),
                        fieldWithPath("createdAt").type(STRING).description("팝업스토어 등록날짜"),
                        fieldWithPath("updatedAt").type(STRING).optional().description("팝업스토어 수정날짜")
                )
        ));
    }

    @DisplayName("팝업스토어 저장시 name은 필수값이다")
    @Test
    void invalidNameToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.name").exists());
    }

    @DisplayName("팝업스토어 저장시 description은 필수값이다")
    @Test
    void invalidDescriptionToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.description").exists());
    }

    @DisplayName("팝업스토어 저장시 startDate은 필수값이다")
    @Test
    void invalidStartDateToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.startDate").exists());
    }

    @DisplayName("팝업스토어 저장시 endDate은 필수값이다")
    @Test
    void invalidEndDateToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.endDate").exists());
    }

    @DisplayName("팝업스토어 저장시 thumbnailIndex은 정수값 이어야한다")
    @Test
    void invalidThumbnailIndexToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(-1L)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.thumbnailIndex").exists());
    }

    @DisplayName("팝업스토어 저장시 categoryId은 필수값이다")
    @Test
    void invalidCategoryIdToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.categoryId").exists());
    }

    @DisplayName("팝업스토어 저장시 locationId은 필수값이다")
    @Test
    void invalidLocationIdToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.locationId").exists());
    }

    @DisplayName("팝업스토어 저장시 operatingHours의 days은 필수값이다")
    @Test
    void invalidOperatingHoursOfDaysToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.days").exists());
    }

    @DisplayName("팝업스토어 저장시 operatingHours의 startTime은 필수값이다")
    @Test
    void invalidOperatingHoursOfStartTimeToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
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
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.startTime").exists());
    }

    @DisplayName("팝업스토어 저장시 operatingHours의 endTime은 필수값이다")
    @Test
    void invalidOperatingHoursOfEndTimeToSave() throws Exception {
        // Arrange
        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
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
                        .build()))
                .build();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.endTime").exists());
    }

    @DisplayName("팝업스토어를 정상적으로 수정한다")
    @Test
    void updateStore() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreUpdateRequest storeRequest = getStoreUpdateRequest();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        final StoreResponse storeResponse = getStoreResponse();

        given(storeService.updateStore(anyLong(), any(StoreUpdateRequest.class), any(List.class)))
                .willReturn(storeResponse);

        // Act & Assert
        final ResultActions result = mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startDate").exists())
                .andExpect(jsonPath("$.endDate").exists())
                .andExpect(jsonPath("$.websiteUrl").exists())
                .andExpect(jsonPath("$.snsUrl").exists())
                .andExpect(jsonPath("$.viewCount").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.location").exists())
                .andExpect(jsonPath("$.operatingHours").exists())
                .andExpect(jsonPath("$.storeImages").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        then(storeService).should(times(1))
                .updateStore(anyLong(), any(StoreUpdateRequest.class), any(List.class));

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("id").description("팝업스토어 ID")
                ),
                requestParts(
                        partWithName("images").description("이미지들"),
                        partWithName("request").description("팝업스토어 수정 정보")
                ),
                requestPartFields("request",
                        fieldWithPath("name")
                                .description("팝업스토어명")
                                .attributes(key("constraints").value(getConstraintDescription("name"))),
                        fieldWithPath("description")
                                .description("팝업스토어 상세설명")
                                .attributes(key("constraints").value(getConstraintDescription("description"))),
                        fieldWithPath("startDate")
                                .description("팝업스토어 시작날짜")
                                .attributes(key("constraints").value(getConstraintDescription("startDate"))),
                        fieldWithPath("endDate")
                                .description("팝업스토어 종료날짜")
                                .attributes(key("constraints").value(getConstraintDescription("endDate"))),
                        fieldWithPath("websiteUrl")
                                .description("팝업스토어 참고 홈페이지 주소")
                                .optional(),
                        fieldWithPath("snsUrl")
                                .description("팝업스토어 참고 SNS 주소")
                                .optional(),
                        fieldWithPath("thumbnailId")
                                .description("팝업스토어 기존 아이디")
                                .optional(),
                        fieldWithPath("thumbnailIndex")
                                .description("썸네일 선택 인덱스")
                                .attributes(key("constraints").value(getConstraintDescription("thumbnailIndex"))),
                        fieldWithPath("categoryId")
                                .description("팝업스토어 카테고리 아이디")
                                .attributes(key("constraints").value(getConstraintDescription("categoryId"))),
                        fieldWithPath("locationId")
                                .description("위치 아이디")
                                .attributes(key("constraints").value(getConstraintDescription("locationId"))),
                        fieldWithPath("operatingHours[0].days")
                                .description("팝업스토어 운영 요일"),
                        fieldWithPath("operatingHours[0].startTime")
                                .description("팝업스토어 운영 오픈 시간"),
                        fieldWithPath("operatingHours[0].endTime")
                                .description("팝업스토어 운영 마감 시간"),
                        fieldWithPath("deletedImageIds")
                                .description("팝업스토어 기존 이미지 삭제")
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("name").type(STRING).description("팝업스토어명"),
                        fieldWithPath("description").type(STRING).description("팝업스토어 설명"),
                        fieldWithPath("status").type(STRING).description("팝업스토어 상태"),
                        fieldWithPath("startDate").type(STRING).description("팝업스토어 시작날짜"),
                        fieldWithPath("endDate").type(STRING).description("팝업스토어 종료날짜"),
                        fieldWithPath("websiteUrl").type(STRING).optional().description("팝업스토어 참고 홈페이지 주소"),
                        fieldWithPath("snsUrl").type(STRING).optional().description("팝업스토어 참고 SNS 주소"),
                        fieldWithPath("viewCount").type(NUMBER).description("팝업스토어 조회수"),
                        fieldWithPath("category.id").type(NUMBER).description("팝업스토어 카테고리 아이디"),
                        fieldWithPath("category.name").type(STRING).description("팝업스토어 카테고리명"),
                        fieldWithPath("category.createdAt").type(STRING).description("팝업스토어 카테고리 등록날짜"),
                        fieldWithPath("category.updatedAt").type(STRING).optional().description("팝업스토어 카테고리 수정날짜"),
                        fieldWithPath("location.id").type(NUMBER).description("위치 아이디"),
                        fieldWithPath("location.name").type(STRING).description("위치명"),
                        fieldWithPath("location.zonecode").type(STRING).description("위치 우편번호"),
                        fieldWithPath("location.sido").type(STRING).description("위치 시/도"),
                        fieldWithPath("location.sigungu").type(STRING).description("위치 시/군/구"),
                        fieldWithPath("location.latitude").type(NUMBER).description("위치 위도"),
                        fieldWithPath("location.longitude").type(NUMBER).description("위치 경도"),
                        fieldWithPath("location.address").type(STRING).description("위치 주소"),
                        fieldWithPath("location.addressDetail").type(STRING).description("위치 상세주소"),
                        fieldWithPath("operatingHours[].days").type(STRING).description("팝업스토어 운영 날짜"),
                        fieldWithPath("operatingHours[].startTime").type(STRING).description("팝업스토어 운영 오픈시간"),
                        fieldWithPath("operatingHours[].endTime").type(STRING).description("팝업스토어 운영 마감시간"),
                        fieldWithPath("storeImages[].id").type(NUMBER).description("팝업스토어 이미지 아이디"),
                        fieldWithPath("storeImages[].storeId").type(NUMBER).description("팝업스토어 아이디"),
                        fieldWithPath("storeImages[].imageUrl").type(STRING).description("팝업스토어 이미지 URL"),
                        fieldWithPath("storeImages[].isThumbnail").type(BOOLEAN).description("팝업스토어 썸네일 이미지 여부"),
                        fieldWithPath("createdAt").type(STRING).description("팝업스토어 등록날짜"),
                        fieldWithPath("updatedAt").type(STRING).optional().description("팝업스토어 수정날짜")
                )
        ));
    }

    @DisplayName("팝업스토어 수정시 name은 필수값이다")
    @Test
    void invalidNameToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreUpdateRequest storeRequest = StoreUpdateRequest.builder()
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .deletedImageIds(null)
                .build();

        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.name").exists());
    }

    @DisplayName("팝업스토어 수정시 description은 필수값이다")
    @Test
    void invalidDescriptionToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreUpdateRequest storeRequest = StoreUpdateRequest.builder()
                .name("name")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .deletedImageIds(null)
                .build();

        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.description").exists());
    }

    @DisplayName("팝업스토어 수정시 startDate은 필수값이다")
    @Test
    void invalidStartDateToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreUpdateRequest storeRequest = StoreUpdateRequest.builder()
                .name("name")
                .description("description")
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .deletedImageIds(null)
                .build();

        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.startDate").exists());
    }

    @DisplayName("팝업스토어 수정시 endDate은 필수값이다")
    @Test
    void invalidEndDateToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreUpdateRequest storeRequest = StoreUpdateRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .websiteUrl("")
                .snsUrl("")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .deletedImageIds(null)
                .build();

        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.endDate").exists());
    }

    @DisplayName("팝업스토어 수정시 categoryId은 필수값이다")
    @Test
    void invalidCategoryIdToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.categoryId").exists());
    }

    @DisplayName("팝업스토어 수정시 locationId은 필수값이다")
    @Test
    void invalidLocationIdToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.locationId").exists());
    }

    @DisplayName("팝업스토어 수정시 operatingHours의 days은 필수값이다")
    @Test
    void invalidOperatingHoursOfDaysToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(result -> {
                    Map<String, Object> validation = JsonPath.read(result.getResponse()
                            .getContentAsString(), "$.validation");
                    boolean containsStartTime = validation.keySet().stream()
                            .anyMatch(key -> key.contains("days"));
                    assertThat(containsStartTime).isTrue();
                });
    }

    @DisplayName("팝업스토어 수정시 operatingHours의 startTime은 필수값이다")
    @Test
    void invalidOperatingHoursOfStartTimeToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
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
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .build();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(result -> {
                    Map<String, Object> validation = JsonPath.read(result.getResponse()
                            .getContentAsString(), "$.validation");
                    boolean containsStartTime = validation.keySet().stream()
                            .anyMatch(key -> key.contains("startTime"));
                    assertThat(containsStartTime).isTrue();
                });
    }

    @DisplayName("팝업스토어 수정시 operatingHours의 endTime은 필수값이다")
    @Test
    void invalidOperatingHoursOfEndTimeToUpdate() throws Exception {
        // Arrange
        final long id = 1L;

        final MockMultipartFile images = new MockMultipartFile(
                "images", "image.jpeg", IMAGE_JPEG_VALUE, "data".getBytes());

        final StoreRequest storeRequest = StoreRequest.builder()
                .name("name")
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
                        .build()))
                .build();
        final MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                objectMapper.writeValueAsString(storeRequest)
                        .getBytes(UTF_8));

        // Act & Assert
        mockMvc.perform(multipart("/api/stores/{id}", id)
                        .file(images)
                        .file(request)
                        .contentType(MULTIPART_FORM_DATA)
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(result -> {
                    Map<String, Object> validation = JsonPath.read(result.getResponse()
                            .getContentAsString(), "$.validation");
                    boolean containsStartTime = validation.keySet().stream()
                            .anyMatch(key -> key.contains("endTime"));
                    assertThat(containsStartTime).isTrue();
                });
    }

    @DisplayName("팝업스토어를 정상적으로 삭제한다")
    @Test
    void deleteStore() throws Exception {
        // Arrange
        final long id = 1L;

        willDoNothing().given(storeService)
                .deleteStore(id);

        // Act & Assert
        final ResultActions result = mockMvc.perform(delete("/api/stores/{id}", id))
                .andExpect(status().isNoContent());

        then(storeService).should(times(1))
                .deleteStore(id);

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("id").description("팝업스토어 ID")
                )
        ));
    }

    @DisplayName("존재하지 않는 ID로 팝업스토어 삭제시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void deleteStoreWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        final long id = Long.MAX_VALUE;

        willThrow(new StoreNotFoundException()).given(storeService)
                        .deleteStore(id);

        // Act & Assert
        mockMvc.perform(delete("/api/stores/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(storeService).should(times(1))
                .deleteStore(id);
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

    private StoreUpdateRequest getStoreUpdateRequest() {
        return StoreUpdateRequest.builder()
                .name("store")
                .description("description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .websiteUrl("")
                .snsUrl("")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(StoreOperatingHourRequest.builder()
                        .days("월~금")
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plusHours(10))
                        .build()))
                .deletedImageIds(List.of(1L))
                .build();
    }

    private StoreResponse getStoreResponse() {
        return new StoreResponse(
                1L,
                "store",
                "description",
                RESOLVED,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "",
                "https://instgram.com/test",
                0,
                getStoreCategoryResponse(),
                getLocationResponse(),
                List.of(getStoreOperatingHourResponse()),
                List.of(getStoreImageResponse()),
                LocalDateTime.now(),
                null
        );
    }

    private StoreCategoryResponse getStoreCategoryResponse() {
        return new StoreCategoryResponse(1L, "뷰티", LocalDateTime.now(), null);
    }

    private LocationResponse getLocationResponse() {
        return LocationResponse.builder()
                .id(1L)
                .name("서울 송파구 올림픽로 300")
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .longitude(127.104302)
                .latitude(37.513713)
                .addressDetail("")
                .build();
    }

    private StoreOperatingHourResponse getStoreOperatingHourResponse() {
        return new StoreOperatingHourResponse(
                "월~금",
                LocalTime.now(),
                LocalTime.now().plusHours(10)
        );
    }

    private StoreImageResponse getStoreImageResponse() {
        return StoreImageResponse.builder()
                .id(1L)
                .storeId(1L)
                .imageUrl("http://127.0.0.1:4566/pinup/store/image1.png")
                .isThumbnail(true)
                .build();
    }

    private StoreThumbnailResponse getStoreThumbnailResponse() {
        return new StoreThumbnailResponse(
                1L,
                "store",
                RESOLVED,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "뷰티",
                "송파구",
                "http://127.0.0.1:4566/pinup/store/image1.png"
        );
    }

    private List<String> getConstraintDescription(String fieldName) {
        final ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(NoticeCreateRequest.class);
        return constraintDescriptions.descriptionsForProperty(fieldName);
    }
}
