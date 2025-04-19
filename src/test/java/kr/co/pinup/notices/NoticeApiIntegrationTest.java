package kr.co.pinup.notices;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.repository.NoticeRepository;
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
class NoticeApiIntegrationTest {

    private static final String NOTICE_ERROR_MESSAGE = "공지사항이 존재하지 않습니다.";
    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

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

    @DisplayName("rest api로 공지사항 전체 조회시 데이터를 반환한다.")
    @Test
    void findAllRestApi() throws Exception {
        // Arrange
        Notice notice1 = createNotice("title 1", "content 1");
        Notice notice2 = createNotice("title 2", "content 2");
        Notice notice3 = Notice.builder()
                .title("title 3")
                .content("content 3")
                .member(member)
                .isDeleted(true)
                .build();

        noticeRepository.saveAll(List.of(notice1, notice2, notice3));

        // Act & Assert
        mockMvc.perform(get("/api/notices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].content").exists())
                .andExpect(jsonPath("$[0].member").exists())
                .andExpect(jsonPath("$[0].title").value(notice2.getTitle()))
                .andExpect(jsonPath("$[0].content").value(notice2.getContent()));
    }

    @DisplayName("rest api로 공지사항 1개 조회시 데이터를 반환한다.")
    @Test
    void findRestApi() throws Exception {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        // Act & Assert
        mockMvc.perform(get("/api/notices/{noticeId}", notice.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.title").value("title 1"))
                .andExpect(jsonPath("$.content").value("content 1"));
    }

    @DisplayName("rest api로 존재하지 않는 ID로 공지사항 1개 조회시 404 예외를 발생한다.")
    @Test
    void findWithNonExistIdAndNotFoundErrorViewRestApi() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), NOTICE_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 공지사항 저장시 201을 응답한다.")
    @Test
    void saveNoticeReturnIsCreated() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("title 1", "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/notices")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 공지사항 저장시 403을 응답한다.")
    @Test
    void saveUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        NoticeCreateRequest request = createRequest("title 1", "content 1");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(post("/api/notices")
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
    @DisplayName("rest api로 관리자 권한일 때 공지사항 수정시 204을 응답한다.")
    @Test
    void updateNoticeReturnNoContent() throws Exception {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(put("/api/notices/{noticeId}", notice.getId())
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 공지사항 수정시 403을 응답한다.")
    @Test
    void updateUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
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
    @DisplayName("rest api로 존재하지 않는 ID로 공지사항 수정시 404 예외를 발생한다.")
    @Test
    void updateWithNonExistIdAndNotFoundErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;
        NoticeUpdateRequest request = createUpdateRequest("title 1", "update content");
        String body = objectMapper.writeValueAsString(request);

        // Act & Assert
        ResultActions result = mockMvc.perform(put("/api/notices/{noticeId}", noticeId)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), NOTICE_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 관리자 권한일 때 공지사항 삭제시 204을 응답한다.")
    @Test
    void deleteNoticeReturnNoContent() throws Exception {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        // Act & Assert
        mockMvc.perform(delete("/api/notices/{noticeId}", notice.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @WithMockMember
    @DisplayName("rest api로 사용자 권한으로 공지사항 삭제시 403을 응답한다.")
    @Test
    void deleteUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long noticeId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(delete("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("rest api로 존재하지 않는 ID로 공지사항 삭제시 404 예외를 발생한다.")
    @Test
    void deleteWithNonExistIdAndNotFoundErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        // Act & Assert
        ResultActions result = mockMvc.perform(delete("/api/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getErrorResponse(result);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), NOTICE_ERROR_MESSAGE);
    }

    private ErrorResponse getErrorResponse(ResultActions result) {
        return (ErrorResponse) Objects.requireNonNull(result.andReturn()
                        .getModelAndView())
                .getModel()
                .get("error");
    }

    private Notice createNotice(String title, String content) {
        return Notice.builder()
                .title(title)
                .content(content)
                .member(member)
                .build();
    }

    private NoticeCreateRequest createRequest(String title, String content) {
        return NoticeCreateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }

    private NoticeUpdateRequest createUpdateRequest(String title, String content) {
        return NoticeUpdateRequest.builder()
                .title(title)
                .content(content)
                .build();
    }
}
