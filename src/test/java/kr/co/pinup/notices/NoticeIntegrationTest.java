package kr.co.pinup.notices;

import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.model.dto.NoticeResponse;
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
class NoticeIntegrationTest {

    private static final String VIEW_PATH = "views/notices";
    private static final String NOTICE_ERROR_MESSAGE = "공지사항이 존재하지 않습니다.";
    private static final String FORBIDDEN_ERROR_MESSAGE = "접근 권한이 없습니다.";

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
                .nickname("nickname")
                .providerType(NAVER)
                .providerId("test")
                .role(ROLE_ADMIN)
                .build();
        memberRepository.save(member);
    }

    @DisplayName("공지사항 리스트 페이지를 반환한다.")
    @Test
    void returnNoticeListView() throws Exception {
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
        ResultActions result = mockMvc.perform(get("/notices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("notices"));

        List<NoticeResponse> response = getModelAttribute(result, "notices", List.class);

        assertThat(response).hasSize(2)
                .extracting(NoticeResponse::title, NoticeResponse::content)
                .containsExactly(
                        tuple(notice2.getTitle(), notice2.getContent()),
                        tuple(notice1.getTitle(), notice1.getContent()));
    }

    @DisplayName("공지사항 상세 페이지를 반환한다.")
    @Test
    void returnNoticeDetailView() throws Exception {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/{noticeId}", notice.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/detail"))
                .andExpect(model().attributeExists("notice"));

        NoticeResponse response = getModelAttribute(result, "notice", NoticeResponse.class);

        assertThat(response).extracting(NoticeResponse::title, NoticeResponse::content)
                .containsExactly(notice.getTitle(), notice.getContent());
    }

    @DisplayName("존재하지 않는 ID로 공지사항 상세 페이지를 요청하면 NOT_FOUND 예외가 발생한다.")
    @Test
    void returnDetailViewWithNonExistIdAndReturnErrorView() throws Exception {
        // Arrange
        long noticeId = Long.MAX_VALUE;
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/{noticeId}", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), NOTICE_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("공지사항 등록 패이지 반환은 관리자 권한일 때만 가능하다.")
    @Test
    void returnNoticeCreateViewWithAdminRole() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/notices/new"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"));
    }

    @WithMockMember
    @DisplayName("사용자 권한일 때 공지사항 등록 페이지 요청은 403 예외를 발생한다.")
    @Test
    void requestCreateViewUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/new"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("공지사항 수정 페이지 반환은 관리자일 때만 가능하다.")
    @Test
    void returnNoticeUpdateViewWithAdminRole() throws Exception {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");

        noticeRepository.save(notice);

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/{noticeId}/update", notice.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("notice"));

        NoticeResponse response = getModelAttribute(result, "notice", NoticeResponse.class);

        assertThat(response).extracting(NoticeResponse::title, NoticeResponse::content)
                .containsExactly(notice.getTitle(), notice.getContent());
    }

    @WithMockMember
    @DisplayName("사용자 권한일 때 공지사항 수정 페이지 요청은 403 예외를 발생한다.")
    @Test
    void requestUpdateViewUnAuthorizedRoleToErrorView() throws Exception {
        // Arrange
        long noticeId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/{noticeId}/update", noticeId))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(FORBIDDEN.value(), FORBIDDEN_ERROR_MESSAGE);
    }

    @WithMockMember(role = ROLE_ADMIN)
    @DisplayName("존재하지 않는 ID로 공지사항 수정 페이지 요청은 404 예외를 발생한다.")
    @Test
    void returnUpdateViewWithNonExistIdAndReturnErrorView() throws Exception {
        // Arrange
        long noticeId = 1L;

        // Act & Assert
        ResultActions result = mockMvc.perform(get("/notices/{noticeId}/update", noticeId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        ErrorResponse response = getModelAttribute(result, "error", ErrorResponse.class);

        assertThat(response).extracting(ErrorResponse::status, ErrorResponse::message)
                .containsExactly(NOT_FOUND.value(), NOTICE_ERROR_MESSAGE);
    }

    private Notice createNotice(String title, String content) {
        return Notice.builder()
                .title(title)
                .content(content)
                .member(member)
                .build();
    }

    private <T> T getModelAttribute(ResultActions result, String attributeName, Class<T> clazz) {
        return clazz.cast(Objects.requireNonNull(result.andReturn()
                        .getModelAndView())
                .getModel()
                .getOrDefault(attributeName, null));
    }
}
