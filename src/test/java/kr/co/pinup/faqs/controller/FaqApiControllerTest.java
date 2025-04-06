package kr.co.pinup.faqs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfigTest.class)
@WebMvcTest(FaqApiController.class)
class FaqApiControllerTest {

    static final String VIEWS_ERROR = "error";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    FaqService faqService;

    @MockitoBean
    MemberService memberService;

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장")
    void save() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장시 category는 필수 값이다")
    void invalidCategoryToSave() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.category").exists())
                .andExpect(jsonPath("$.validation.category").value("카테고리는 필수입니다."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장시 question은 필수 값이다")
    void invalidQuestionToSave() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(USE)
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question").value("질문 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장시 question 길이는 1~100까지 이다")
    void invalidQuestionLengthToSave() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(USE)
                .question("A".repeat(101))
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question")
                        .value("질문 내용을 1~100자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장시 answer 값은 필수다")
    void invalidAnswerToSave() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(USE)
                .question("이거 어떻게 해야 하나요?")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer").value("답변 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 저장시 answer 길이는 1~500까지 이다")
    void invalidAnswerLengthToSave() throws Exception {
        // given
        FaqCreateRequest request = FaqCreateRequest.builder()
                .category(USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("A".repeat(501))
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer")
                        .value("답변 내용을 1~500자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 전체 조회")
    void findAll() throws Exception {
        // given
        List<FaqResponse> response = IntStream.range(0, 5)
                .mapToObj(i -> FaqResponse.builder()
                        .category(USE)
                        .question("자주 묻는 질문 " + (5 - i))
                        .answer("자주 묻는 질문 답변 " + (5 - i))
                        .build())
                .toList();

        // when
        given(faqService.findAll()).willReturn(response);

        // expected
        mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].question").exists())
                .andExpect(jsonPath("$[0].answer").exists())
                .andExpect(jsonPath("$[0].category").value(USE.toString()))
                .andExpect(jsonPath("$[0].question").value("자주 묻는 질문 5"))
                .andExpect(jsonPath("$[0].answer").value("자주 묻는 질문 답변 5"))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 단일 조회")
    void find() throws Exception {
        // given
        long faqId = 1L;
        FaqResponse response = FaqResponse.builder()
                .category(USE)
                .question("질문")
                .answer("답변")
                .build();

        // when
        given(faqService.find(faqId)).willReturn(response);

        // expected
        mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.question").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.question").value(response.question()))
                .andExpect(jsonPath("$.answer").value(response.answer()))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회시 에러")
    void findWithNonExistId() throws Exception {
        // given
        long faqId = Long.MAX_VALUE;

        // when
        when(faqService.find(faqId)).thenThrow(new FaqNotFound());

        // expected
        MvcResult result = mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNotFound())
                .andExpect(view().name(VIEWS_ERROR))
                .andExpect(model().attributeExists("error"))
                .andDo(print())
                .andReturn();

        ErrorResponse response = (ErrorResponse) result.getModelAndView().getModel().get("error");
        assertThat(response.status()).isEqualTo(NOT_FOUND.value());
        assertThat(response.message()).isEqualTo("FAQ가 존재하지 않습니다.");
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정")
    void update() throws Exception {
        // given
        long faqId = 1L;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .question("질문")
                .answer("답변")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정시 question은 필수 값이다")
    void invalidQuestionToUpdate() throws Exception {
        // given
        long faqId = 1L;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question").value("질문 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정시 question 길이는 1~100까지 이다")
    void invalidQuestionLengthToUpdate() throws Exception {
        // given
        long faqId = 1L;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .question("A".repeat(101))
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question")
                        .value("질문 내용을 1~100자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정시 answer 값은 필수다")
    void invalidAnswerToUpdate() throws Exception {
        // given
        long faqId = 1L;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .question("이거 어떻게 해야 하나요?")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer").value("답변 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정시 answer 길이는 1~500까지 이다")
    void invalidAnswerLengthToUpdate() throws Exception {
        // given
        long faqId = 1L;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .question("이거 어떻게 해야 하나요?")
                .answer("A".repeat(501))
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer")
                        .value("답변 내용을 1~500자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("존재하지 않는 ID로 수정시 에러")
    void updateWithNonExistId() throws Exception {
        // given
        long faqId = Long.MAX_VALUE;
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .category(USE)
                .question("질문")
                .answer("답변")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // when
        doThrow(new FaqNotFound()).when(faqService).update(faqId, request);

        // expected
        MvcResult result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(view().name(VIEWS_ERROR))
                .andExpect(model().attributeExists("error"))
                .andDo(print())
                .andReturn();

        ErrorResponse response = (ErrorResponse) Objects.requireNonNull(result.getModelAndView())
                .getModel()
                .get("error");
        assertThat(response.status()).isEqualTo(NOT_FOUND.value());
        assertThat(response.message()).isEqualTo("FAQ가 존재하지 않습니다.");
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 삭제")
    void remove() throws Exception {
        // given
        long faqId = 1L;

        // when
        doNothing().when(faqService).remove(faqId);

        // expected
        mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("존재하지 않는 ID로 삭제시 에러")
    void removeWithNonExistId() throws Exception {
        // given
        long faqId = Long.MAX_VALUE;

        // when
        doThrow(new FaqNotFound()).when(faqService).remove(faqId);

        // expected
        MvcResult result = mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNotFound())
                .andExpect(view().name(VIEWS_ERROR))
                .andExpect(model().attributeExists("error"))
                .andDo(print())
                .andReturn();

        ErrorResponse response = (ErrorResponse) Objects.requireNonNull(result.getModelAndView())
                .getModel()
                .get("error");
        assertThat(response.status()).isEqualTo(NOT_FOUND.value());
        assertThat(response.message()).isEqualTo("FAQ가 존재하지 않습니다.");
    }
}