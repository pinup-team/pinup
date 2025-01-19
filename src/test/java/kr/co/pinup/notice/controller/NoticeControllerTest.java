package kr.co.pinup.notice.controller;

import kr.co.pinup.notice.NoticeService;
import kr.co.pinup.notice.response.NoticeResponse;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NoticeService noticeService;

    String viewName = "views/notice";

    @Test
    @DisplayName("공지사항 리스트 페이지 이동")
    void listPage() throws Exception {
        // given
        List<NoticeResponse> mockNotices = IntStream.range(0, 10)
                .mapToObj(i ->  NoticeResponse.builder()
                        .title("공지사항 제목 " + (10 - i))
                        .content("공지사항 내용 " + (10 - i))
                        .build())
                .toList();

        // when
        when(noticeService.findAll()).thenReturn(mockNotices);

        // expected
        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName + "/list"))
                .andExpect(model().attributeExists("notices"))
                .andExpect(model().attribute("notices", is(mockNotices)))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 생성 페이지 이동")
    void newPage() throws Exception {
        // expected
        mockMvc.perform(get("/notices/new"))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName + "/create"))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 상세 페이지 이동")
    void detailPage() throws Exception {
        // given
        long noticeId = 1L;
        NoticeResponse mockNotice = NoticeResponse.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .build();

        // when
        when(noticeService.find(noticeId)).thenReturn(mockNotice);

        // expected
        mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName + "/detail"))
                .andExpect(model().attributeExists("notice"))
                .andExpect(model().attribute("notice", is(mockNotice)))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 수정 페이지 이동")
    void updatePage() throws Exception {
        // given
        long noticeId = 1L;
        NoticeResponse mockNotice = NoticeResponse.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .build();

        // when
        when(noticeService.find(noticeId)).thenReturn(mockNotice);

        // expected
        mockMvc.perform(get("/notices/{noticeId}/update", noticeId))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName + "/update"))
                .andExpect(model().attributeExists("notice"))
                .andExpect(model().attribute("notice", is(mockNotice)))
                .andDo(print());
    }
}