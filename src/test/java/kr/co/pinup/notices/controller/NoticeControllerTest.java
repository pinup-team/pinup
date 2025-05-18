package kr.co.pinup.notices.controller;

import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.service.NoticeService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NoticeController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class NoticeControllerTest {

    private static final String VIEW_PATH = "views/notices";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @DisplayName("공지사항 list 페이지 뷰를 반환한다.")
    @Test
    void returnListView() throws Exception {
        // Arrange
        final LocalDateTime time1 = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        final LocalDateTime time2 = LocalDateTime.of(2025, 1, 1, 1, 0, 0);

        NoticeResponse response1 = createNoticeResponse("title 1", "content 1", time1);
        NoticeResponse response2 = createNoticeResponse("title 2", "content 2", time2);
        List<NoticeResponse> responses = List.of(response2, response1);

        given(noticeService.findAll()).willReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/notices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("notices"))
                .andExpect(model().attribute("notices", hasSize(2)));

        then(noticeService).should(times(1))
                .findAll();
    }

    @DisplayName("공지사항 create 페이지 뷰를 반환한다.")
    @Test
    void returnCreateView() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/notices/new"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"));
    }

    @DisplayName("공지사항 detail 페이지 뷰를 반환한다.")
    @Test
    void returnDetailView() throws Exception {
        // Arrange
        long noticeId = 1L;
        final LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        NoticeResponse response = createNoticeResponse("title 1", "content 1", time);

        given(noticeService.find(noticeId)).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/detail"))
                .andExpect(model().attributeExists("notice"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 detail 페이지를 요청시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void requestDetailViewWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        given(noticeService.find(noticeId)).willThrow(new NoticeNotFound());

        // Act & Assert
        mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("공지사항 update 페이지 뷰를 반환한다.")
    @Test
    void returnUpdateView() throws Exception {
        // Arrange
        long noticeId = 1L;
        final LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        NoticeResponse response = createNoticeResponse("title 1", "content 1", time);

        given(noticeService.find(noticeId)).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/notices/{noticeId}/update", noticeId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("notice"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 update 페이지를 요청시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void requestUpdateViewWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        given(noticeService.find(noticeId)).willThrow(new NoticeNotFound());

        // Act & Assert
        mockMvc.perform(get("/notices/{noticeId}/update", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(noticeService).should(times(1))
                .find(noticeId);
    }

    private NoticeResponse createNoticeResponse(String title, String content, LocalDateTime dateTime) {
        return NoticeResponse.builder()
                .title(title)
                .content(content)
                .member(mock(MemberResponse.class))
                .createdAt(dateTime)
                .build();
    }
}
