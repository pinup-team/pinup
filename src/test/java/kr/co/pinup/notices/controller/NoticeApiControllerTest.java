package kr.co.pinup.notices.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.service.NoticeService;
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

import java.time.LocalDateTime;
import java.util.List;

import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NoticeApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsSupport.class)
class NoticeApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    @MockitoBean
    private NoticeService noticeService;

    @BeforeEach
    void setUp(final WebApplicationContext context,
               final RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @DisplayName("공지사항 전체를 조회한다.")
    @Test
    void findAll() throws Exception {
        // Arrange
        final LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        final LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 1, 0, 0);

        NoticeResponse response1 = createNoticeResponse(1L, "title 1", "content 1", time1);
        NoticeResponse response2 = createNoticeResponse(2L, "title 2", "content 2", time2);
        List<NoticeResponse> responses = List.of(response2, response1);

        given(noticeService.findAll()).willReturn(responses);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());

        result.andDo(restDocs.document(
                responseFields(
                        fieldWithPath("[].id").type(NUMBER).description("공지사항 아이디"),
                        fieldWithPath("[].title").type(STRING).description("공지사항 제목"),
                        fieldWithPath("[].content").type(STRING).description("공지사항 내용"),
                        fieldWithPath("[].member.id").type(NUMBER).description("작성자 아이디"),
                        fieldWithPath("[].member.name").type(STRING).description("작성자 이름"),
                        fieldWithPath("[].member.email").type(STRING).description("작성자 이메일"),
                        fieldWithPath("[].member.nickname").type(STRING).description("작성자 닉네임"),
                        fieldWithPath("[].member.providerType").type(STRING).description("OAuth 제공자"),
                        fieldWithPath("[].member.role").type(STRING).description("작성자 권한"),
                        fieldWithPath("[].member.deleted").type(BOOLEAN).description("작성자 탈퇴 여부"),
                        fieldWithPath("[].createdAt").type(STRING).description("공지사항 작성일"),
                        fieldWithPath("[].updatedAt").type(STRING).optional().description("공지사항 수정일")
                )
        ));

        then(noticeService).should(times(1))
                .findAll();
    }

    @DisplayName("ID로 1개의 공지사항을 조회한다.")
    @Test
    void findById() throws Exception {
        // Arrange
        long noticeId = 1L;
        final LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        NoticeResponse response = createNoticeResponse(noticeId, "title", "content", time);

        given(noticeService.find(noticeId)).willReturn(response);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/notices/{noticeId}", noticeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("공지사항 아이디"),
                        fieldWithPath("title").type(STRING).description("공지사항 제목"),
                        fieldWithPath("content").type(STRING).description("공지사항 내용"),
                        fieldWithPath("member.id").type(NUMBER).description("작성자 아이디"),
                        fieldWithPath("member.name").type(STRING).description("작성자 이름"),
                        fieldWithPath("member.email").type(STRING).description("작성자 이메일"),
                        fieldWithPath("member.nickname").type(STRING).description("작성자 닉네임"),
                        fieldWithPath("member.providerType").type(STRING).description("OAuth 제공자"),
                        fieldWithPath("member.role").type(STRING).description("작성자 권한"),
                        fieldWithPath("member.deleted").type(BOOLEAN).description("작성자 탈퇴 여부"),
                        fieldWithPath("createdAt").type(STRING).description("공지사항 작성일"),
                        fieldWithPath("updatedAt").type(STRING).optional().description("공지사항 수정일")
                )
        ));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 조회시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void findByNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        given(noticeService.find(noticeId)).willThrow(new NoticeNotFound());

        // Act & Assert
        mockMvc.perform(get("/api/notices/{noticeId}", noticeId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("공지사항을 정상적으로 저장한다.")
    @Test
    void save() throws Exception {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        NoticeCreateRequest request = createRequest("title 1", "content 1");
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(noticeService)
                .save(memberInfo, request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                )
        ));

        then(noticeService).should(times(1))
                .save(any(MemberInfo.class), eq(request));
    }

    @DisplayName("공지사항 저장시 제목은 필수 값이다.")
    @Test
    void invalidTitleToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest(null, "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("title")
                                .type(null)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.title").description("제목은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("공지사항 저장시 제목의 길이는 1~100자 이내이다.")
    @Test
    void invalidTitleLengthToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("A".repeat(101), "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.title").description("제목은 1~100자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("공지사항 저장시 내용은 필수 값이다.")
    @Test
    void invalidContentToSave() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("title 1", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.content").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(null)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.content").description("내용은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("공지사항을 정상적으로 수정한다.")
    @Test
    void update() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(noticeService)
                .update(noticeId, request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                ),
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                )
        ));

        then(noticeService).should(times(1))
                .update(noticeId, request);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 수정시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void updatingNoticeNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        willThrow(new NoticeNotFound()).given(noticeService)
                .update(noticeId, request);

        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .update(noticeId, request);
    }

    @DisplayName("공지사항 수정시 제목은 필수 값이다.")
    @Test
    void invalidTitleToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest(null, "update content");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                ),
                requestFields(
                        fieldWithPath("title")
                                .type(null)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.title").description("제목은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("공지사항 수정시 제목의 길이는 1~100자 이내이다.")
    @Test
    void invalidTitleLengthToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("A".repeat(101), "update content");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.title").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                ),
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(STRING)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.title").description("제목은 1~100자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("공지사항 수정시 내용은 필수 값이다.")
    @Test
    void invalidContentToUpdate() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("title update", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.content").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                ),
                requestFields(
                        fieldWithPath("title")
                                .type(STRING)
                                .description("공지사항 제목")
                                .attributes(key("constraints").value(getConstraintDescription("title"))),
                        fieldWithPath("content")
                                .type(null)
                                .description("공지사항 내용")
                                .attributes(key("constraints").value(getConstraintDescription("content")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.content").description("내용은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("공지사항을 정상적으로 삭제한다.")
    @Test
    void remove() throws Exception {
        // Arrange
        long noticeId = 1L;

        willDoNothing().given(noticeService)
                .remove(noticeId);

        // Act & Assert
        final ResultActions result = mockMvc.perform(delete("/api/notices/{noticeId}", noticeId))
                .andExpect(status().isNoContent());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("noticeId").description("공지사항 아이디")
                )
        ));

        then(noticeService).should(times(1))
                .remove(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 삭제시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void deletingNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        willThrow(new NoticeNotFound()).given(noticeService)
                .remove(noticeId);

        // Act & Assert
        mockMvc.perform(delete("/api/notices/{noticeId}", noticeId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .remove(noticeId);
    }

    private NoticeResponse createNoticeResponse(Long id, String title, String content,
                                                LocalDateTime dateTime) {
        return NoticeResponse.builder()
                .id(id)
                .title(title)
                .content(content)
                .member(createMemberResponse())
                .createdAt(dateTime)
                .build();
    }

    private MemberResponse createMemberResponse() {
        return MemberResponse.builder()
                .id(1L)
                .name("핀업")
                .email("pinup0106@gmail.com")
                .nickname("핀업")
                .providerType(NAVER)
                .role(ROLE_ADMIN)
                .isDeleted(false)
                .build();
    }

    private MemberInfo createMemberInfo() {
        return MemberInfo.builder()
                .nickname("핀업")
                .provider(NAVER)
                .role(ROLE_ADMIN)
                .build();
    }

    private NoticeCreateRequest createRequest(String title, String content) {
        return NoticeCreateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }

    private NoticeUpdateRequest createUpdateRequest(String title, String content) {
        return NoticeUpdateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }

    private List<String> getConstraintDescription(String fieldName) {
        final ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(NoticeCreateRequest.class);
        return constraintDescriptions.descriptionsForProperty(fieldName);
    }
}
