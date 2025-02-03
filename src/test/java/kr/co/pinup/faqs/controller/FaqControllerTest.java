package kr.co.pinup.faqs.controller;

import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfigTest.class)
@ExtendWith(SpringExtension.class)
@WebMvcTest(FaqController.class)
class FaqControllerTest {

    static final String VIEW_PATH = "views/faqs";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FaqService faqService;

    @MockitoBean
    MemberService memberService;

    @Test
    @WithMockMember(nickname = "두려운 고양이", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 리스트 페이지 이동")
    void listPage() throws Exception {
        // given
        List<FaqResponse> mockFaqs = IntStream.range(0, 5)
                .mapToObj(i -> FaqResponse.builder()
                        .category("이용")
                        .question("자주 묻는 질문 " + (5 - i))
                        .answer("자주 묻는 질문 답변 " + (5 - i))
                        .build())
                .toList();

        // when
        when(faqService.findAll()).thenReturn(mockFaqs);

        // expected
        mockMvc.perform(get("/faqs"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("faqs"))
                .andExpect(model().attribute("faqs", is(mockFaqs)))
                .andDo(print());
    }

    @Test
    @WithMockMember(nickname = "두려운 고양이", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 생성 페이지 이동")
    void newPage() throws Exception {
        // given

        // expected
        mockMvc.perform(get("/faqs/new"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attribute("category", is(not(empty()))))
                .andExpect(model().attribute("category", hasEntry("USE", "이용")))
                .andExpect(model().attribute("category", hasEntry("MEMBER", "회원")))
                .andExpect(model().attribute("category", hasEntry("COMPANY", "기업")))
                .andDo(print());
    }

    @Test
    @WithMockMember(nickname = "두려운 고양이", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    @DisplayName("FAQ 수정 페이지 이동")
    void updatePage() throws Exception {
        // given
        long faqId = 1L;
        FaqResponse mockFaq = FaqResponse.builder()
                .category("이용")
                .question("질문")
                .answer("답변")
                .build();

        // when
        when(faqService.find(faqId)).thenReturn(mockFaq);

        // expected
        mockMvc.perform(get("/faqs/{faqId}/update", faqId))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("faq"))
                .andExpect(model().attribute("category", is(not(empty()))))
                .andExpect(model().attribute("category", hasEntry("USE", "이용")))
                .andExpect(model().attribute("category", hasEntry("MEMBER", "회원")))
                .andExpect(model().attribute("category", hasEntry("COMPANY", "기업")))
                .andExpect(model().attribute("faq", is(mockFaq)))
                .andDo(print());
    }
}