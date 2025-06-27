package kr.co.pinup.locations;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.api.kakao.KakaoApiService;
import kr.co.pinup.api.kakao.model.dto.KakaoAddressDocument;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.locations.model.dto.CreateLocationRequest;
import kr.co.pinup.locations.model.dto.UpdateLocationRequest;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Objects;
import java.util.Optional;

import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LocationApiIntegrationTest {

    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationRepository locationRepository;

    @MockitoBean
    private KakaoApiService kakaoApiService;

    @MockitoBean
    private MemberService memberService;

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("POST /api/locations 요청 시 201 Created와 응답 정보를 반환한다.")
    @Test
    void createLocation() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final Location location = getLocation(addressDocument);

        given(kakaoApiService.searchAddress(anyString())).willReturn(addressDocument);
        given(locationRepository.save(any(Location.class))).willReturn(location);

        // Act Assert
        mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.zonecode").exists())
                .andExpect(jsonPath("$.sido").exists())
                .andExpect(jsonPath("$.sigungu").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.addressDetail").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists());

        then(kakaoApiService).should(times(1))
                .searchAddress(anyString());
        then(locationRepository).should(times(1))
                .save(any(Location.class));
    }

    @WithMockMember
    @DisplayName("POST /api/locations 요청 시 사용자 권한이면 403을 응답한다.")
    @Test
    void shouldReturnForbiddenWhenRoleUserOnCreateLocation() throws Exception {
        // Arrange
        final CreateLocationRequest request = getCreateLocationRequest();

        // Act & Assert
        ResultActions result = mockMvc.perform(post("/api/locations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("PUT /api/locations 요청 시 200 OK와 응답 정보를 반환한다.")
    @Test
    void updateLocation() throws Exception {
        // Arrange
        final long locationId = 1L;
        final UpdateLocationRequest request = getUpdateLocationRequest();
        final KakaoAddressDocument addressDocument = getAddressDocument(request.address());
        final Location location = getLocation(addressDocument);

        given(kakaoApiService.searchAddress(anyString())).willReturn(addressDocument);
        given(locationRepository.findById(anyLong())).willReturn(Optional.ofNullable(location));

        // Act Assert
        mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.zonecode").exists())
                .andExpect(jsonPath("$.sido").exists())
                .andExpect(jsonPath("$.sigungu").exists())
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.addressDetail").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists());

        then(kakaoApiService).should(times(1))
                .searchAddress(anyString());
        then(locationRepository).should(times(1))
                .findById(locationId);
    }

    @WithMockMember
    @DisplayName("PUT /api/locations 요청 시 사용자 권한이면 403을 응답한다.")
    @Test
    void shouldReturnForbiddenWhenRoleUserOnUpdateLocation() throws Exception {
        // Arrange
        final long locationId = 1L;
        final UpdateLocationRequest request = getUpdateLocationRequest();

        // Act & Assert
        ResultActions result = mockMvc.perform(put("/api/locations/{locationId}", locationId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    private CreateLocationRequest getCreateLocationRequest() {
        return CreateLocationRequest.builder()
                .zonecode("05554")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 240")
                .addressDetail("롯데백화점 잠실점 10F 웨이브 행사장 (LG전자 콜라보 행사)")
                .build();
    }

    private UpdateLocationRequest getUpdateLocationRequest() {
        return UpdateLocationRequest.builder()
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
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

    private Location getLocation(final KakaoAddressDocument addressDocument) {
        return Location.builder()
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

    private ErrorResponse getErrorResponse(ResultActions result) {
        return (ErrorResponse) Objects.requireNonNull(result.andReturn()
                        .getModelAndView())
                .getModel()
                .get("error");
    }
}
