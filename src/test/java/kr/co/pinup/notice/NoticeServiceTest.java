package kr.co.pinup.notice;

import kr.co.pinup.notice.domain.Notice;
import kr.co.pinup.notice.exception.NoticeNotFound;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.*;

@SpringBootTest
class NoticeServiceTest {

    private static final String NOTICE_NOT_FOUND_MESSAGE = "공지사항이 존재하지 않습니다.";

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
    }

    static Stream<Arguments> noticeProvider() {
        return Stream.of(arguments("공지사항 제목", "공지사항 내용"));
    }

    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 작성")
    void save(String title, String content) {
        // given
        NoticeCreate noticeCreate = NoticeCreate.builder()
                .title(title)
                .content(content)
                .build();

        // when
        noticeService.save(noticeCreate);

        // then
        assertThat(noticeRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("공지사항 전체 조회")
    void findAll() {
        // given
        String title1 = "공지사항 제목1";
        String content1 = "공지사항 내용1";
        String title2 = "공지사항 제목2";
        String content2 = "공지사항 내용2";

        Notice request1 = Notice.builder()
                .title(title1)
                .content(content1)
                .build();

        Notice request2 = Notice.builder()
                .title(title2)
                .content(content2)
                .build();
        noticeRepository.save(request1);
        noticeRepository.save(request2);

        // when
        List<NoticeResponse> notices = noticeService.findAll();

        // then
        assertThat(noticeRepository.count()).isEqualTo(2L);
        assertThat(notices.get(0).title()).isEqualTo(title1);
        assertThat(notices.get(0).content()).isEqualTo(content1);
    }

    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 단일 조회")
    void find(String title, String content) {
        // given
        Notice request = Notice.builder()
                .title(title)
                .content(content)
                .build();
        noticeRepository.save(request);

        // when
        NoticeResponse notice = noticeService.find(request.getId());

        // then
        assertThat(notice).isNotNull();
        assertThat(noticeRepository.count()).isEqualTo(1L);
        assertThat(notice.title()).isEqualTo(title);
        assertThat(notice.content()).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(longs = 1)
    @DisplayName("존재하지 않는 공지사항 조회")
    void findError(Long noticeId) {
        // given

        // expected
        assertThatThrownBy(() -> noticeService.find(noticeId))
                .isInstanceOf(NoticeNotFound.class)
                .hasMessage(NOTICE_NOT_FOUND_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 제목 수정")
    void update(String title, String content) {
        // given
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .build();
        noticeRepository.save(notice);

        String updateTitle = "공지사항 제목 수정";
        NoticeUpdate noticeUpdate = NoticeUpdate.builder()
                .title(updateTitle)
                .content(content)
                .build();

        // when
        noticeService.update(notice.getId(), noticeUpdate);

        // then
        Notice result = noticeRepository.findById(notice.getId())
                .orElseThrow(() -> new RuntimeException("공지사항이 존재하지 않습니다. id= " + notice.getId()));
        assertThat(result.getId()).isEqualTo(notice.getId());
        assertThat(result.getTitle()).isEqualTo(updateTitle);
        assertThat(result.getContent()).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(longs = 1)
    @DisplayName("존재하지 않는 공지사항 수정")
    void updateError(Long noticeId) {
        // given
        NoticeUpdate noticeUpdate = NoticeUpdate.builder()
                .title("공지사항 제목")
                .content("공지사항 내용")
                .build();

        // expected
        assertThatThrownBy(() -> noticeService.update(noticeId, noticeUpdate))
                .isInstanceOf(NoticeNotFound.class)
                .hasMessage(NOTICE_NOT_FOUND_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 삭제")
    void remove(String title, String content) {
        // given
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .build();
        noticeRepository.save(notice);

        // when
        noticeService.remove(notice.getId());

        // then
        assertThat(noticeRepository.count()).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(longs = 1)
    @DisplayName("존재하지 않는 공지사항 삭제")
    void removeError(Long noticeId) {
        // given

        // expected
        assertThatThrownBy(() -> noticeService.remove(noticeId))
                .isInstanceOf(NoticeNotFound.class)
                .hasMessage(NOTICE_NOT_FOUND_MESSAGE);
    }
}