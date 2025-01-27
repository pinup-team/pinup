package kr.co.pinup.notices;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.repository.NoticeRepository;
import kr.co.pinup.notices.service.NoticeService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class NoticeServiceTest {

    static final String NOTICE_NOT_FOUND_MESSAGE = "공지사항이 존재하지 않습니다.";

    @Autowired
    NoticeService noticeService;

    @Autowired
    NoticeRepository noticeRepository;

    @Autowired
    MemberRepository memberRepository;

    Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("두려운고양이")
                .providerType(OAuthProvider.NAVER)
                .providerId("hdiJZoHQ-XDUkGvVCDLr1_NnTNZGcJjyxSAEUFjEi6A")
                .role(MemberRole.ROLE_ADMIN)
                .build();
        memberRepository.save(member);

        noticeRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    static Stream<Arguments> noticeProvider() {
        return Stream.of(arguments("공지사항 제목", "공지사항 내용"));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 작성")
    void save(String title, String content) {
        // given
        MemberInfo memberInfo = MemberInfo.builder()
                .nickname("두려운고양이")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_ADMIN)
                .build();

        NoticeCreateRequest noticeCreate = NoticeCreateRequest.builder()
                .title(title)
                .content(content)
                .build();

        // when
        noticeService.save(memberInfo, noticeCreate);

        // then
        assertThat(noticeRepository.count()).isEqualTo(1L);
    }

    @Test
    @Transactional
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
                .member(member)
                .build();
        Notice request2 = Notice.builder()
                .title(title2)
                .content(content2)
                .member(member)
                .build();
        noticeRepository.saveAll(List.of(request1, request2));

        // when
        List<NoticeResponse> notices = noticeService.findAll();

        // then
        assertThat(noticeRepository.count()).isEqualTo(2L);
        assertThat(notices.get(0).title()).isEqualTo(title2);
        assertThat(notices.get(0).content()).isEqualTo(content2);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 단일 조회")
    void find(String title, String content) {
        // given
        Notice request = Notice.builder()
                .title(title)
                .content(content)
                .member(member)
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

    @Transactional
    @ParameterizedTest
    @MethodSource("noticeProvider")
    @DisplayName("공지사항 제목 수정")
    void update(String title, String content) {
        // given
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .member(member)
                .build();
        noticeRepository.save(notice);

        String updateTitle = "공지사항 제목 수정";
        NoticeUpdateRequest noticeUpdate = NoticeUpdateRequest.builder()
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
        NoticeUpdateRequest noticeUpdate = NoticeUpdateRequest.builder()
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
                .member(member)
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