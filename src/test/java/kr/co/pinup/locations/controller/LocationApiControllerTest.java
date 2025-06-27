package kr.co.pinup.locations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import kr.co.pinup.locations.service.LocationService;
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
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LocationApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
@ExtendWith(RestDocumentationExtension.class)
@Import({RestDocsSupport.class, LoggerConfig.class})
class LocationApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    @MockitoBean
    private LocationService locationService;

    @BeforeEach
    void setUp(
            final WebApplicationContext context,
            final RestDocumentationContextProvider provider
    ) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @DisplayName("위치를 저장한다.")
    @Test
    void createLocation() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                "서울",
                "송파구",
                "서울 송파구 올림픽로 240"
        );
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final LocationResponse response = getLocationResponse(addressDocument);

        final String body = objectMapper.writeValueAsString(request);

        given(locationService.createLocation(request)).willReturn(response);

        // Act Assert
        final ResultActions result = mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").exists())
                    .andExpect(jsonPath("$.zonecode").exists())
                    .andExpect(jsonPath("$.sido").exists())
                    .andExpect(jsonPath("$.sigungu").exists())
                    .andExpect(jsonPath("$.address").exists())
                    .andExpect(jsonPath("$.addressDetail").exists())
                    .andExpect(jsonPath("$.latitude").exists())
                    .andExpect(jsonPath("$.longitude").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("zonecode")
                                .type(STRING)
                                .description("우편번호")
                                .attributes(key("constraints").value(getConstraintDescription("zonecode"))),
                        fieldWithPath("sido")
                                .type(STRING)
                                .description("도/특별시/광역시")
                                .attributes(key("constraints").value(getConstraintDescription("sido"))),
                        fieldWithPath("sigungu")
                                .type(STRING)
                                .description("시/군/구")
                                .attributes(key("constraints").value(getConstraintDescription("sigungu"))),
                        fieldWithPath("address")
                                .type(STRING)
                                .description("주소")
                                .attributes(key("constraints").value(getConstraintDescription("address"))),
                        fieldWithPath("addressDetail")
                                .type(STRING)
                                .description("상세 주소")
                                .optional()
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("아이디"),
                        fieldWithPath("name").type(STRING).description("이름"),
                        fieldWithPath("zonecode").type(STRING).description("우편번호"),
                        fieldWithPath("sido").type(STRING).description("도/특별시/광역시"),
                        fieldWithPath("sigungu").type(STRING).description("시/군/구"),
                        fieldWithPath("address").type(STRING).description("주소"),
                        fieldWithPath("addressDetail").type(STRING).description("상세 주소"),
                        fieldWithPath("longitude").type(NUMBER).description("x 좌표"),
                        fieldWithPath("latitude").type(NUMBER).description("y 좌표")
                )
        ));
    }

    @DisplayName("위치 저장시 우편번호는 필수값이다.")
    @Test
    void invalidZonecodeToSave() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest(
                null,
                "서울",
                "송파구",
                "서울 송파구 올림픽로 240"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(post("/api/locations")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.zonecode").exists());
    }

    @DisplayName("위치 저장시 도/특별시/광역시는 필수값이다.")
    @Test
    void invalidSidoToSave() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                null,
                "송파구",
                "서울 송파구 올림픽로 240"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.sido").exists());
    }

    @DisplayName("위치 저장시 시/군/구는 필수값이다.")
    @Test
    void invalidSigunguToSave() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                "서울",
                null,
                "서울 송파구 올림픽로 240"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.sigungu").exists());
    }

    @DisplayName("위치 저장시 주소는 필수값이다.")
    @Test
    void invalidAddressToSave() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                "서울",
                "송파구",
                null
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.address").exists());
    }

    @DisplayName("ID에 일치하는 위치를 수정한다.")
    @Test
    void updateLocation() throws Exception {
        // Arrange
        final long locationId = 1L;
        final UpdateLocationRequest request = getUpdateLocationRequest(
                "05551",
                "서울",
                "송파구",
                "서울 송파구 올림픽로 300"
        );
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final LocationResponse response = getLocationResponse(addressDocument);

        final String body = objectMapper.writeValueAsString(request);

        given(locationService.updateLocation(locationId, request)).willReturn(response);

        // Act Assert
        final ResultActions result = mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.zonecode").exists())
                .andExpect(jsonPath("$.sido").exists())
                .andExpect(jsonPath("$.sigungu").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.addressDetail").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("locationId").description("위치 ID")
                ),
                requestFields(
                        fieldWithPath("zonecode")
                                .type(STRING)
                                .description("우편번호")
                                .attributes(key("constraints").value(getConstraintDescription("zonecode"))),
                        fieldWithPath("sido")
                                .type(STRING)
                                .description("도/특별시/광역시")
                                .attributes(key("constraints").value(getConstraintDescription("sido"))),
                        fieldWithPath("sigungu")
                                .type(STRING)
                                .description("시/군/구")
                                .attributes(key("constraints").value(getConstraintDescription("sigungu"))),
                        fieldWithPath("address")
                                .type(STRING)
                                .description("주소")
                                .attributes(key("constraints").value(getConstraintDescription("address"))),
                        fieldWithPath("addressDetail")
                                .type(STRING)
                                .description("상세 주소")
                                .optional()
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("아이디"),
                        fieldWithPath("name").type(STRING).description("이름"),
                        fieldWithPath("zonecode").type(STRING).description("우편번호"),
                        fieldWithPath("sido").type(STRING).description("도/특별시/광역시"),
                        fieldWithPath("sigungu").type(STRING).description("시/군/구"),
                        fieldWithPath("address").type(STRING).description("주소"),
                        fieldWithPath("addressDetail").type(STRING).description("상세 주소"),
                        fieldWithPath("longitude").type(NUMBER).description("x 좌표"),
                        fieldWithPath("latitude").type(NUMBER).description("y 좌표")
                )
        ));
    }

    @DisplayName("위치 수정시 우편번호는 필수값이다.")
    @Test
    void invalidZonecodeToUpdate() throws Exception {
        // Arrange
        final long locationId = 1L;
        final UpdateLocationRequest request = getUpdateLocationRequest(
                null,
                "서울",
                "송파구",
                "서울 송파구 올림픽로 300"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.zonecode").exists());
    }

    @DisplayName("위치 수정시 도/특별시/광역시는 필수값이다.")
    @Test
    void invalidSidoToUpdate() throws Exception {
        // Arrange
        final long locationId = 1L;
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                null,
                "송파구",
                "서울 송파구 올림픽로 240"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.sido").exists());
    }

    @DisplayName("위치 수정시 시/군/구는 필수값이다.")
    @Test
    void invalidSigunguToUpdate() throws Exception {
        // Arrange
        final long locationId = 1L;
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                "서울",
                null,
                "서울 송파구 올림픽로 240"
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.sigungu").exists());
    }

    @DisplayName("위치 수정시 주소는 필수값이다.")
    @Test
    void invalidAddressToUpdate() throws Exception {
        // Arrange
        final long locationId = 1L;
        final CreateLocationRequest request = getCreateLocationRequest(
                "05554",
                "서울",
                "송파구",
                null
        );
        final String body = objectMapper.writeValueAsString(request);

        // Act Assert
        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.address").exists());
    }

    private CreateLocationRequest getCreateLocationRequest(
            final String zonecode,
            final String sido,
            final String sigungu,
            final String address
    ) {
        return CreateLocationRequest.builder()
                .zonecode(zonecode)
                .sido(sido)
                .sigungu(sigungu)
                .address(address)
                .addressDetail("롯데백화점 잠실점 10F 웨이브 행사장 (LG전자 콜라보 행사)")
                .build();
    }

    private UpdateLocationRequest getUpdateLocationRequest(
            final String zonecode,
            final String sido,
            final String sigungu,
            final String address
    ) {
        return UpdateLocationRequest.builder()
                .zonecode(zonecode)
                .sido(sido)
                .sigungu(sigungu)
                .address(address)
                .addressDetail("잠실 롯데월드몰 1층 아트리움")
                .build();
    }

    private KakaoAddressDocument getAddressDocument(String addressName) {
        return KakaoAddressDocument.builder()
                .addressName(addressName)
                .longitude(127.098142)
                .latitude(37.51131)
                .build();
    }

    private LocationResponse getLocationResponse(final KakaoAddressDocument addressDocument) {
        return LocationResponse.builder()
                .id(1L)
                .name(addressDocument.addressName())
                .zonecode("05554")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 240")
                .addressDetail("롯데백화점 잠실점 10F 웨이브 행사장 (LG전자 콜라보 행사)")
                .latitude(addressDocument.latitude())
                .longitude(addressDocument.longitude())
                .build();
    }

    private List<String> getConstraintDescription(String fieldName) {
        final ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(CreateLocationRequest.class);
        return constraintDescriptions.descriptionsForProperty(fieldName);
    }
}