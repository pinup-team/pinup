package kr.co.pinup.faqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static kr.co.pinup.faqs.model.enums.FaqCategory.MEMBER;
import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaqApiIntegrationTest {

    private static final String FAQ_ERROR_MESSAGE = "FAQ가 존재하지 않습니다.";
    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@gmail.com")
                .name("name")
                .nickname("네이버TestMember")
                .providerType(NAVER)
                .providerId("test").role(ROLE_ADMIN)
                .build();
        memberRepository.save(member);
    }

    @DisplayName("rest api로 FAQ 전체 조회시 데이터를 반환한다.")
    @Test
    void findAllRestApi() throws Exception {
        // Arrange
        Faq faq1 = createFaq("question 1", "answer 1", USE);
        Faq faq2 = createFaq("question 2", "answer 2", MEMBER);

        faqRepository.saveAll(List.of(faq1, faq2));

        // Act & Assert
        mockMvc.perform(get("/api/faqs"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].question").exists())
                .andExpect(jsonPath("$[0].answer").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].question").value(faq2.getQuestion()))
                .andExpect(jsonPath("$[0].answer").value(faq2.getAnswer()))
                .andExpect(jsonPath("$[0].category").value(faq2.getCategory().name()));
    }

    @DisplayName("rest api로 FAQ 1개 조회시 데이터를 반환한다.")
    @Test
    void findRestApi() throws Exception {
        // Arrange
        Faq faq = createFaq("question", "answer", USE);

        faqRepository.save(faq);

        // Act & Assert
        mockMvc.perform(get("/api/faqs/{faqId}", faq.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.member").exists())
                .andExpect(jsonPath("$.question").value(faq.getQuestion()))
                .andExpect(jsonPath("$.answer").value(faq.getAnswer()))
                .andExpect(jsonPath("$.category").value(faq.getCategory().name()));
    }

    @DisplayName("rest api로 존재하지 않는 ID로 FAQ 1개 조회시 404 예외를 발생한다.")
    @Test
    void findWithNonExistIdAndNotFoundErrorViewRestApi() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), FAQ_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 FAQ 저장시 201을 응답한다.")
    @Test
    void saveFaqReturnIsCreated() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 FAQ 저장시 403을 응답한다.")
    @Test
    void saveUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        FaqCreateRequest request = createFaqCreateRequest("question", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(post("/api/faqs")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 관리자 권한일 때 FAQ 수정시 204을 응답한다.")
    @Test
    void updateFaqReturnNoContent() throws Exception {
        // Arrange
        Faq faq = createFaq("question", "answer", USE);

        faqRepository.save(faq);

        FaqUpdateRequest request = createFaqUpdateRequest("update question", "update answer", MEMBER);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/faqs/{faqId}", faq.getId())
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 FAQ 수정시 403을 응답한다.")
    @Test
    void updateUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long faqId = 1L;

        FaqUpdateRequest request = createFaqUpdateRequest("update question", "update answer", MEMBER);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 존재하지 않는 ID로 FAQ 수정시 404 예외를 발생한다.")
    @Test
    void updateWithNonExistIdAndNotFoundErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        FaqUpdateRequest request = createFaqUpdateRequest("question", "answer", USE);
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(put("/api/faqs/{faqId}", faqId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), FAQ_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 관리자 권한일 때 FAQ 삭제시 204을 응답한다.")
    @Test
    void deleteFaqReturnNoContent() throws Exception {
        // Arrange
        Faq faq = createFaq("question", "answer", USE);

        faqRepository.save(faq);

        // Act & Assert
        mockMvc.perform(delete("/api/faqs/{faqId}", faq.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 FAQ 삭제시 403을 응답한다.")
    @Test
    void deleteUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long faqId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 존재하지 않는 ID로 FAQ 삭제시 404 예외를 발생한다.")
    @Test
    void deleteWithNonExistIdAndNotFoundErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        // Act & Assert
        ResultActions result = mockMvc.perform(delete("/api/faqs/{faqId}", faqId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), FAQ_ERROR_MESSAGE);
    }

    private Faq createFaq(String question, String answer, FaqCategory category) {
        return Faq.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .member(member)
                .build();
    }

    private ErrorResponse getErrorResponse(ResultActions result) {
        return (ErrorResponse) Objects.requireNonNull(result.andReturn()
                        .getModelAndView())
                .getModel()
                .get("error");
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
