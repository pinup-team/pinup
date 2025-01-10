package kr.co.pinup.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.notice.exception.NoticeNotFound;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeService noticeService;

    static Stream<Arguments> noticeProvider() {
        return Stream.of(arguments("공지사항 제목", "공지사항 내용"));
    }

    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 저장")
    void save(String title, String content) throws Exception {
        // given
        NoticeCreate request = NoticeCreate.builder()
                .title(title)
                .content(content)
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/notices")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 저장 시 title 값은 필수이다")
    void invalidTitleToSave() throws Exception {
        // given
        NoticeCreate request = NoticeCreate.builder()
                .content("공지사항 내용")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/notices")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.title").exists())
                .andExpect(jsonPath("$.validation.title").value("제목을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 저장 시 title 길이는 1~100까지 이다")
    void invalidTitleLengthToSave() throws Exception {
        // given
        NoticeCreate request = NoticeCreate.builder()
                .title("A".repeat(101))
                .content("공지사항 내용")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/notices")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.title").exists())
                .andExpect(jsonPath("$.validation.title").value("제목을 1~100자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 저장 시 content 값은 필수이다")
    void invalidContentToSave() throws Exception {
        // given
        NoticeCreate request = NoticeCreate.builder()
                .title("공지사항 제목")
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/notices")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.content").exists())
                .andExpect(jsonPath("$.validation.content").value("내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 저장 시 content 길이는 1~200까지 이다")
    void invalidContentLengthToSave() throws Exception {
        // given
        NoticeCreate request = NoticeCreate.builder()
                .title("공지사항 제목")
                .content("A".repeat(201))
                .build();

        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/notices")
                .contentType(APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.content").exists())
                .andExpect(jsonPath("$.validation.content").value("내용을 1~200자 이내로 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 전체 조회")
    void findAll() throws Exception {
        // given
        List<NoticeResponse> notices = IntStream.range(0, 10)
                .mapToObj(i ->  NoticeResponse.builder()
                        .title("공지사항 제목 " + (10 - i))
                        .content("공지사항 내용 " + (10 - i))
                        .build())
                .toList();

        given(noticeService.findAll()).willReturn(notices);

        // expected
        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].title").value("공지사항 제목 10"))
                .andExpect(jsonPath("$[0].content").value("공지사항 내용 10"))
                .andDo(print());
    }

    @ParameterizedTest
    @ValueSource(longs = 1)
    @DisplayName("공지사항 단일 조회")
    void find(long noticeId) throws Exception {
        // given
        NoticeResponse notice = NoticeResponse.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .build();

        given(noticeService.find(noticeId)).willReturn(notice);

        // expected
        mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.title").value(notice.title()))
                .andExpect(jsonPath("$.content").value(notice.content()))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 조회")
    void findError() throws Exception {
        // given
        long noticeId = 99999L;

        // when
        when(noticeService.find(noticeId)).thenThrow(new NoticeNotFound());

        // expected
        mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("공지사항이 존재하지 않습니다."))
                .andDo(print());
    }
    
    @Test
    @DisplayName("공지사항 수정")
    void update() throws Exception {
        // given
        long noticeId = 1L;
        NoticeUpdate request = NoticeUpdate.builder()
                .title("공지사항 제목 수정")
                .content("공지사항 내용")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 수정은 title 값이 필수다")
    void invalidTitleToUpdate() throws Exception {
        // given
        long noticeId = 1L;
        NoticeUpdate request = NoticeUpdate.builder()
                .content("공지사항 내용")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation").exists())
                .andExpect(jsonPath("$.validation.title").exists())
                .andExpect(jsonPath("$.validation.title").value("제목을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 수정은 content 값이 필수다")
    void invalidContentToUpdate() throws Exception {
        // given
        long noticeId = 1L;
        NoticeUpdate request = NoticeUpdate.builder()
                .title("공지사항 제목")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(put("/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation").exists())
                .andExpect(jsonPath("$.validation.content").exists())
                .andExpect(jsonPath("$.validation.content").value("내용을 입력하세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 수정")
    void updateError() throws Exception {
        // given
        long noticeId = 99999L;
        NoticeUpdate request = NoticeUpdate.builder()
                .title("공지사항 수정")
                .content("공지사항 내용")
                .build();
        String body = objectMapper.writeValueAsString(request);

        // when
        doThrow(new NoticeNotFound()).when(noticeService).update(noticeId, request);

        // expected
        mockMvc.perform(put("/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("공지사항이 존재하지 않습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("공지사항 삭제")
    void deleteTest() throws Exception {
        // given
        long noticeId = 1L;

        // when
        doNothing().when(noticeService).delete(noticeId);

        // expected
        mockMvc.perform(delete("/notices/{noticeId}", noticeId))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 삭제")
    void deleteError() throws Exception {
        // given
        long noticeId = 99999L;

        // when
        doThrow(new NoticeNotFound()).when(noticeService).delete(noticeId);

        // expected
        mockMvc.perform(MockMvcRequestBuilders.delete("/notices/{noticeId}", noticeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("공지사항이 존재하지 않습니다."))
                .andDo(print());
    }

}