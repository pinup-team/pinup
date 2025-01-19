package kr.co.pinup.faq.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.faq.FaqService;
import kr.co.pinup.faq.exception.FaqNotFound;
import kr.co.pinup.faq.request.FaqCreate;
import kr.co.pinup.faq.response.FaqResponse;
import kr.co.pinup.notice.request.FaqUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(FaqApiController.class)
class FaqApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FaqService faqService;

    @Test
    @DisplayName("FAQ 저장")
    void save() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .category("USE")
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
    @DisplayName("FAQ 저장시 category는 필수 값이다")
    void invalidCategoryToSave() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .question("이거 어떻게 해야 하나요?")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.category").exists())
                .andExpect(jsonPath("$.validation.category").value("카테고리는 필수입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 저장시 question은 필수 값이다")
    void invalidQuestionToSave() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .category("USE")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question").value("질문 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 저장시 question 길이는 1~100까지 이다")
    void invalidQuestionLengthToSave() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .category("USE")
                .question("A".repeat(101))
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question")
                        .value("질문 내용을 1~100자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 저장시 answer 값은 필수다")
    void invalidAnswerToSave() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer").value("답변 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 저장시 answer 길이는 1~200까지 이다")
    void invalidAnswerLengthToSave() throws Exception {
        // given
        FaqCreate request = FaqCreate.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .answer("A".repeat(201))
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer")
                        .value("답변 내용을 1~200자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 전체 조회")
    void findAll() throws Exception {
        // given
        List<FaqResponse> response = IntStream.range(0, 5)
                .mapToObj(i -> FaqResponse.builder()
                        .category("이용")
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
                .andExpect(jsonPath("$[0].category").value("이용"))
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
                .category("이용")
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
        mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("FAQ가 존재하지 않습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 수정")
    void update() throws Exception {
        // given
        long faqId = 1L;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
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
    @DisplayName("FAQ 수정시 question은 필수 값이다")
    void invalidQuestionToUpdate() throws Exception {
        // given
        long faqId = 1L;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question").value("질문 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 수정시 question 길이는 1~100까지 이다")
    void invalidQuestionLengthToUpdate() throws Exception {
        // given
        long faqId = 1L;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
                .question("A".repeat(101))
                .answer("이렇게 저렇게 하시면 됩니다.")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.question").exists())
                .andExpect(jsonPath("$.validation.question")
                        .value("질문 내용을 1~100자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 수정시 answer 값은 필수다")
    void invalidAnswerToUpdate() throws Exception {
        // given
        long faqId = 1L;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer").value("답변 내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("FAQ 수정시 answer 길이는 1~200까지 이다")
    void invalidAnswerLengthToUpdate() throws Exception {
        // given
        long faqId = 1L;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
                .question("이거 어떻게 해야 하나요?")
                .answer("A".repeat(201))
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.answer").exists())
                .andExpect(jsonPath("$.validation.answer")
                        .value("답변 내용을 1~200자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 수정시 에러")
    void updateWithNonExistId() throws Exception {
        // given
        long faqId = Long.MAX_VALUE;

        FaqUpdate request = FaqUpdate.builder()
                .category("USE")
                .question("질문")
                .answer("답변")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // when
        doThrow(new FaqNotFound()).when(faqService).update(faqId, request);

        // expected
        mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("FAQ가 존재하지 않습니다."))
                .andDo(print());
    }

    @Test
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
    @DisplayName("존재하지 않는 ID로 삭제시 에러")
    void removeWithNonExistId() throws Exception {
        // given
        long faqId = Long.MAX_VALUE;

        // when
        doThrow(new FaqNotFound()).when(faqService).remove(faqId);

        // expected
        mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("FAQ가 존재하지 않습니다."))
                .andDo(print());
    }
}