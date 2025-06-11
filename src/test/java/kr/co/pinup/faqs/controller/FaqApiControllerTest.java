package kr.co.pinup.faqs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
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

import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
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

@WebMvcTest(controllers = FaqApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
@ExtendWith(RestDocumentationExtension.class)
@Import({RestDocsSupport.class, LoggerConfig.class})
class FaqApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    @MockitoBean
    private FaqService faqService;

    @BeforeEach
    void setUp(final WebApplicationContext context,
               final RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @DisplayName("FAQ 전체를 조회한다.")
    @Test
    void findAll() throws Exception {
        // Arrange
        final LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 0, 0);
        final LocalDateTime time2 = LocalDateTime.of(2025, 1, 2, 0, 0);

        FaqResponse response1 = createFaqResponse(1L, "question 1", "answer 1", time1);
        FaqResponse response2 = createFaqResponse(2L, "question 2", "answer 2", time2);
        List<FaqResponse> responses = List.of(response2, response1);

        given(faqService.findAll()).willReturn(responses);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].question").exists())
                .andExpect(jsonPath("$[0].answer").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());

        result.andDo(restDocs.document(
                responseFields(
                        fieldWithPath("[].id").type(NUMBER).description("FAQ ID"),
                        fieldWithPath("[].question").type(STRING).description("FAQ 질문"),
                        fieldWithPath("[].answer").type(STRING).description("FAQ 답변"),
                        fieldWithPath("[].category").type(STRING).description("FAQ 카테고리"),
                        fieldWithPath("[].member.id").type(NUMBER).description("작성자 ID"),
                        fieldWithPath("[].member.name").type(STRING).description("작성자 이름"),
                        fieldWithPath("[].member.email").type(STRING).description("작성자 이메일"),
                        fieldWithPath("[].member.nickname").type(STRING).description("작성자 닉네임"),
                        fieldWithPath("[].member.providerType").type(STRING).description("OAuth 제공자"),
                        fieldWithPath("[].member.role").type(STRING).description("작성자 권한"),
                        fieldWithPath("[].member.deleted").type(BOOLEAN).description("작성자 탈퇴 여부"),
                        fieldWithPath("[].createdAt").type(STRING).description("FAQ 작성일"),
                        fieldWithPath("[].updatedAt").type(STRING).optional().description("FAQ 수정일")
                )
        ));

        then(faqService).should(times(1))
                .findAll();
    }

    @DisplayName("ID로 1개의 FAQ를 조회한다.")
    @Test
    void findById() throws Exception {
        // Arrange
        long faqId = 1L;
        final LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0);

        FaqResponse response = createFaqResponse(1L, "question", "answer", time);

        given(faqService.find(faqId)).willReturn(response);

        // Act & Assert
        final ResultActions result = mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("faqId").description("FAQ ID")
                ),
                responseFields(
                        fieldWithPath("id").type(NUMBER).description("FAQ ID"),
                        fieldWithPath("question").type(STRING).description("FAQ 질문"),
                        fieldWithPath("answer").type(STRING).description("FAQ 답변"),
                        fieldWithPath("category").type(STRING).description("FAQ 카테고리"),
                        fieldWithPath("member.id").type(NUMBER).description("작성자 ID"),
                        fieldWithPath("member.name").type(STRING).description("작성자 이름"),
                        fieldWithPath("member.email").type(STRING).description("작성자 이메일"),
                        fieldWithPath("member.nickname").type(STRING).description("작성자 닉네임"),
                        fieldWithPath("member.providerType").type(STRING).description("OAuth 제공자"),
                        fieldWithPath("member.role").type(STRING).description("작성자 권한"),
                        fieldWithPath("member.deleted").type(BOOLEAN).description("작성자 탈퇴 여부"),
                        fieldWithPath("createdAt").type(STRING).description("FAQ 작성일"),
                        fieldWithPath("updatedAt").type(STRING).optional().description("FAQ 수정일")
                )
        ));

        then(faqService).should(times(1))
                .find(faqId);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 조회시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void findByWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        given(faqService.find(faqId)).willThrow(new FaqNotFound());

        // Act & Assert
        mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(faqService).should(times(1))
                .find(faqId);
    }

    @DisplayName("FAQ를 정상적으로 저장한다.")
    @Test
    void save() throws Exception {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        FaqCreateRequest request = createFaqCreateRequest("question", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(faqService)
                .save(memberInfo, request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                )
        ));

        then(faqService).should(times(1))
                .save(any(MemberInfo.class), eq(request));
    }

    @DisplayName("FAQ 저장시 질문 내용은 필수값이다.")
    @Test
    void invalidQuestionToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(null)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.question").description("질문은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ 저장시 질문 내용의 길이는 1~100자 이내이다.")
    @Test
    void invalidQuestionLengthToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("A".repeat(101), "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.question").description("질문은 1~100자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("FAQ 저장시 답변 내용은 필수값이다.")
    @Test
    void invalidAnswerToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(null)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.answer").description("답변은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ 저장시 답변 내용은 1~500자 이내이다.")
    @Test
    void invalidAnswerLengthToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "A".repeat(501), USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.answer").description("답변은 1~500자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("FAQ 저장시 카테고리는 필수값이다.")
    @Test
    void invalidCategoryToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "answer", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.category").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(null)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.category").description("카테고리는 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ를 정상적으로 수정한다.")
    @Test
    void update() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("update question", "update answer", USE);
        String body = objectMapper.writeValueAsString(request);

        willDoNothing().given(faqService)
                .update(faqId, request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("faqId").description("FAQ ID")
                ),
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                )
        ));

        then(faqService).should(times(1))
                .update(faqId, request);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 수정시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void updatingWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        willThrow(new FaqNotFound()).given(faqService)
                .update(faqId, request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(faqService).should(times(1))
                .update(faqId, request);
    }

    @DisplayName("FAQ 수정시 질문 내용은 필수값이다.")
    @Test
    void invalidQuestionToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(null)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.question").description("질문은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ 수정시 질문 내용의 길이는 1~100자 이내이다.")
    @Test
    void invalidQuestionLengthToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("A".repeat(101), "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.question").description("질문은 1~100자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("FAQ 수정시 답변 내용은 필수값이다.")
    @Test
    void invalidAnswerToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(null)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.answer").description("답변은 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ 수정시 답변 내용의 길이는 1~500자 이내이다.")
    @Test
    void invalidAnswerLengthToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "A".repeat(501), USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(STRING)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.answer").description("답변은 1~500자 이내여야 합니다.")
                )
        ));
    }

    @DisplayName("FAQ 수정시 카테고리는 필수값이다.")
    @Test
    void invalidCategoryToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "answer", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        final ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.category").exists());

        result.andDo(restDocs.document(
                requestFields(
                        fieldWithPath("question").type(STRING)
                                .description("FAQ 질문")
                                .attributes(key("constraints").value(getConstraintDescription("question"))),
                        fieldWithPath("answer").type(STRING)
                                .description("FAQ 답변")
                                .attributes(key("constraints").value(getConstraintDescription("answer"))),
                        fieldWithPath("category").type(null)
                                .description("FAQ 카테고리")
                                .attributes(key("constraints").value(getConstraintDescription("category")))
                ),
                responseFields(
                        fieldWithPath("status").description("상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("validation.category").description("카테고리는 필수 입력 필드입니다.")
                )
        ));
    }

    @DisplayName("FAQ를 정상적으로 삭제한다.")
    @Test
    void remove() throws Exception {
        // Arrange
        long faqId = 1L;

        willDoNothing().given(faqService)
                .remove(faqId);

        // Act & Assert
        final ResultActions result = mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNoContent());

        result.andDo(restDocs.document(
                pathParameters(
                        parameterWithName("faqId").description("FAQ ID")
                )
        ));

        then(faqService).should(times(1))
                .remove(faqId);
    }

    @DisplayName("존재하지 않는 ID로 FAQ 삭제시 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void deletingWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        willThrow(new FaqNotFound()).given(faqService)
                .remove(faqId);

        // Act & Assert
        mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(faqService).should(times(1))
                .remove(faqId);
    }

    private FaqResponse createFaqResponse(Long id, String question, String answer, LocalDateTime dateTime) {
        return FaqResponse.builder()
                .id(id)
                .question(question)
                .answer(answer)
                .category(USE)
                .createdAt(dateTime)
                .member(createMemberResponse())
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
                .nickname("nickname")
                .provider(NAVER)
                .role(ROLE_ADMIN)
                .build();
    }

    private FaqCreateRequest createFaqCreateRequest(String question, String answer, FaqCategory category) {
        return FaqCreateRequest.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .build();
    }

    private FaqUpdateRequest createFaqUpdateRequest(String question, String answer, FaqCategory category) {
        return FaqUpdateRequest.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .build();
    }

    private List<String> getConstraintDescription(String fieldName) {
        final ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(FaqCreateRequest.class);
        return constraintDescriptions.descriptionsForProperty(fieldName);
    }
}