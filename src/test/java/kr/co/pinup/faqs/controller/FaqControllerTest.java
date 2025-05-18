package kr.co.pinup.faqs.controller;

import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.service.FaqService;
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
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FaqController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class FaqControllerTest {

    private static final String VIEW_PATH = "views/faqs";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FaqService faqService;

    @DisplayName("FAQ list 페이지를 반환한다.")
    @Test
    void returnListView() throws Exception {
        // Arrange
        final LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 0, 0);
        final LocalDateTime time2 = LocalDateTime.of(2025, 1, 2, 0, 0);

        FaqResponse response1 = createFaqResponse("question 1", "answer 1", time1);
        FaqResponse response2 = createFaqResponse("question 2", "answer 2", time2);
        List<FaqResponse> responses = List.of(response2, response1);

        given(faqService.findAll()).willReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/faqs"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("faqs"))
                .andExpect(model().attribute("category", aMapWithSize(3)))
                .andExpect(model().attribute("faqs", hasSize(2)));

        then(faqService).should(times(1))
                .findAll();
    }

    @DisplayName("FAQ create 페이지를 반환한다.")
    @Test
    void returnCreateView() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/faqs/new"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attribute("category", aMapWithSize(3)));
    }

    @DisplayName("FAQ update 페이지를 반환한다.")
    @Test
    void returnUpdateView() throws Exception {
        // Arrange
        long faqId = 1L;
        final LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0);

        FaqResponse response = createFaqResponse("question 1", "answer 1", time);

        given(faqService.find(faqId)).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/faqs/{faqId}/update", faqId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("faq"))
                .andExpect(model().attribute("category", aMapWithSize(3)));

        then(faqService).should(times(1))
                .find(faqId);
    }

    @DisplayName("존재하지 않는 ID로 update 페이지를 요청시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void requestUpdateViewWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long faqId = Long.MAX_VALUE;

        given(faqService.find(faqId)).willThrow(new FaqNotFound());

        // Act & Assert
        mockMvc.perform(get("/faqs/{faqId}/update", faqId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(faqService).should(times(1))
                .find(faqId);
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
}