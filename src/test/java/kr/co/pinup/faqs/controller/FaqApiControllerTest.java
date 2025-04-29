package kr.co.pinup.faqs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FaqApiController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class FaqApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FaqService faqService;

    @DisplayName("FAQ 전체를 조회한다.")
    @Test
    void findAll() throws Exception {
        // Arrange
        FaqResponse response1 = createFaqResponse("question 1", "answer 1",
                LocalDateTime.of(2025, 1, 1, 0, 0));
        FaqResponse response2 = createFaqResponse("question 2", "answer 2",
                LocalDateTime.of(2025, 1, 2, 0, 0));
        List<FaqResponse> responses = List.of(response2, response1);

        given(faqService.findAll()).willReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/faqs"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].question").exists())
                .andExpect(jsonPath("$[0].answer").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());

        then(faqService).should(times(1))
                .findAll();
    }

    @DisplayName("ID로 1개의 FAQ를 조회한다.")
    @Test
    void findById() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqResponse response = createFaqResponse("question", "answer",
                LocalDateTime.of(2025, 1, 1, 0, 0));

        given(faqService.find(faqId)).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.createdAt").exists());

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
                .andDo(print())
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
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated());

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
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());
    }

    @DisplayName("FAQ 저장시 질문 내용의 길이는 1~100자 이내이다.")
    @Test
    void invalidQuestionLengthToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("A".repeat(101), "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());
    }

    @DisplayName("FAQ 저장시 답변 내용은 필수값이다.")
    @Test
    void invalidAnswerToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());
    }

    @DisplayName("FAQ 저장시 답변 내용은 1~500자 이내이다.")
    @Test
    void invalidAnswerLengthToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "A".repeat(501), USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());
    }

    @DisplayName("FAQ 저장시 카테고리는 필수값이다.")
    @Test
    void invalidCategoryToSave() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "answer", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.category").exists());
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
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());

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
                .andDo(print())
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
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());
    }

    @DisplayName("FAQ 수정시 질문 내용의 길이는 1~100자 이내이다.")
    @Test
    void invalidQuestionLengthToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("A".repeat(101), "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.question").exists());
    }

    @DisplayName("FAQ 수정시 답변 내용은 필수값이다.")
    @Test
    void invalidAnswerToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());
    }

    @DisplayName("FAQ 수정시 답변 내용의 길이는 1~500자 이내이다.")
    @Test
    void invalidAnswerLengthToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "A".repeat(501), USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.answer").exists());
    }

    @DisplayName("FAQ 수정시 카테고리는 필수값이다.")
    @Test
    void invalidCategoryToUpdate() throws Exception {
        // Arrange
        long faqId = 1L;
        FaqUpdateRequest request = createFaqUpdateRequest("question", "answer", null);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.validation.category").exists());
    }

    @DisplayName("FAQ를 정상적으로 삭제한다.")
    @Test
    void remove() throws Exception {
        // Arrange
        long faqId = 1L;

        willDoNothing().given(faqService)
                .remove(faqId);

        // Act & Assert
        mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isNoContent());

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

    private FaqResponse createFaqResponse(String question, String answer, LocalDateTime dateTime) {
        return FaqResponse.builder()
                .question(question)
                .answer(answer)
                .category(USE)
                .createdAt(dateTime)
                .member(mock(MemberResponse.class))
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
}