package kr.co.pinup.faqs;

import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.faqs.model.dto.FaqResponse;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static kr.co.pinup.faqs.model.enums.FaqCategory.MEMBER;
import static kr.co.pinup.faqs.model.enums.FaqCategory.USE;
import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static kr.co.pinup.oauth.OAuthProvider.NAVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("unchecked")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaqIntegrationTest {

    private static final String VIEW_PATH = "views/faqs";
    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";
    private static final String FAQ_ERROR_MESSAGE = "FAQ가 존재하지 않습니다.";

    @Autowired
    private MockMvc mockMvc;

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
                .nickname("nickname")
                .providerType(NAVER)
                .providerId("test")
                .role(ROLE_ADMIN)
                .build();
        memberRepository.save(member);
    }

    @DisplayName("FAQ 리스트 페이지를 반환한다.")
    @Test
    void returnFaqListView() throws Exception {
        // Arrange
        Map<FaqCategory, String> responseCategory = getFaqCategoryToMap();
        Faq faq1 = createFaq("question 1", "answer 1", USE);
        Faq faq2 = createFaq("question 2", "answer 2", MEMBER);

        faqRepository.saveAll(List.of(faq1, faq2));

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/faqs"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("faqs"))
                .andExpect(model().attribute("category", responseCategory));

        List<FaqResponse> response = getModelAttribute(result, "faqs", List.class);

        assertThat(response).hasSize(2)
                .extracting(FaqResponse::question, FaqResponse::answer, FaqResponse::category)
                .containsExactly(
                        tuple(faq2.getQuestion(), faq2.getAnswer(), faq2.getCategory()),
                        tuple(faq1.getQuestion(), faq1.getAnswer(), faq1.getCategory()));
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("FAQ 등록 패이지 반환은 관리자 권한일 때만 가능하다.")
    @Test
    void returnFaqCreateViewWithAdminRole() throws Exception {
        // Arrange
        Map<FaqCategory, String> responseCategory = getFaqCategoryToMap();

        // Act & Assert
        mockMvc.perform(get("/faqs/new"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attribute("category", responseCategory));
    }

    @WithMockMember
    @DisplayName("사용자 권한일 때 FAQ 등록 페이지 요청은 403 예외를 발생한다.")
    @Test
    void requestCreateViewUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/faqs/new"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("FAQ 수정 페이지 반환은 관리자일 때만 가능하다.")
    @Test
    void returnFaqUpdateViewWithAdminRole() throws Exception {
        // Arrange
        Map<FaqCategory, String> responseCategory = getFaqCategoryToMap();
        Faq faq = createFaq("question", "answer", USE);

        faqRepository.save(faq);

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/faqs/{faqId}/update", faq.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("faq"))
                .andExpect(model().attribute("category", responseCategory));

        FaqResponse response = getModelAttribute(result, "faq", FaqResponse.class);

        assertThat(response).extracting(FaqResponse::question, FaqResponse::answer, FaqResponse::category)
                .containsExactly(faq.getQuestion(), faq.getAnswer(), faq.getCategory());
    }

    @WithMockMember
    @DisplayName("사용자 권한일 때 FAQ 수정 페이지 요청은 403 예외를 발생한다.")
    @Test
    void requestUpdateViewUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long faqId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/faqs/{faqId}/update", faqId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("존재하지 않는 ID로 FAQ 수정 페이지 요청은 404 예외를 발생한다.")
    @Test
    void returnUpdateViewWithNonExistIdAndReturnErrorView() throws Exception {
        // Arrange
        long faqId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/faqs/{faqId}/update", faqId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

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

    private <T> T getModelAttribute(ResultActions resultActions, String attributeName, Class<T> clazz) {
        return clazz.cast(resultActions.andReturn()
                .getModelAndView()
                .getModel()
                .getOrDefault(attributeName, null));
    }

    private Map<FaqCategory, String> getFaqCategoryToMap() {
        return Arrays.stream(FaqCategory.values())
                .collect(Collectors.toMap(Function.identity(), FaqCategory::getName));
    }
}
