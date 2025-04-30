package kr.co.pinup.notices.service;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.Notice;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.repository.NoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static kr.co.pinup.members.model.enums.MemberRole.ROLE_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    private static final String NOTICE_ERROR_MESSAGE = "공지사항이 존재하지 않습니다.";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeService noticeService;

    @DisplayName("공지사항 전체를 조회한다.")
    @Test
    void findAll() {
        // Arrange
        Notice notice1 = createNotice("title 1", "content 1");
        Notice notice2 = createNotice("title 2", "content 2");
        ReflectionTestUtils.setField(notice1, "createdAt", LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        ReflectionTestUtils.setField(notice2, "createdAt", LocalDateTime.of(2025, 1, 1, 1, 0, 0));

        List<Notice> notices = List.of(notice2, notice1);

        given(noticeRepository.findAllByIsDeletedFalseOrderByCreatedAtDescIdDesc()).willReturn(notices);

        // Act
        List<NoticeResponse> result = noticeService.findAll();

        // Assert
        NoticeResponse response = result.get(0);

        assertThat(result).hasSize(2);
        assertThat(response.title()).isEqualTo(notice2.getTitle());
        assertThat(response.content()).isEqualTo(notice2.getContent());

        then(noticeRepository).should(times(1))
                .findAllByIsDeletedFalseOrderByCreatedAtDescIdDesc();
    }

    @DisplayName("공지사항 ID로 1개의 공지사항을 조회한다.")
    @Test
    void findNotice() {
        // Arrange
        long noticeId = 1L;
        Optional<Notice> response = Optional.ofNullable(
                createNotice("title 1", "content 1"));

        given(noticeRepository.findByIdAndIsDeletedFalse(noticeId)).willReturn(response);

        // Act
        NoticeResponse result = noticeService.find(noticeId);

        // Assert
        Notice notice = response.get();

        assertThat(result).isNotNull()
            .extracting(NoticeResponse::title, NoticeResponse::content)
            .containsOnly(notice.getTitle(), notice.getContent());
        
        then(noticeRepository).should(times(1))
                .findByIdAndIsDeletedFalse(noticeId);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 조회시 예외가 발생한다.")
    @Test
    void findWithNonExistIdThrowException() {
        // Arrange
        long noticeId = Long.MAX_VALUE;

        given(noticeRepository.findByIdAndIsDeletedFalse(noticeId)).willThrow(new NoticeNotFound());
        
        // Act & Assert
        assertThatThrownBy(() -> noticeService.find(noticeId))
            .isInstanceOf(NoticeNotFound.class)
            .hasMessage(NOTICE_ERROR_MESSAGE);
        
        then(noticeRepository).should(times(1))
                .findByIdAndIsDeletedFalse(noticeId);
    }

    @DisplayName("공지사항을 정상적으로 저장한다.")
    @Test
    void saveNotice() {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        NoticeCreateRequest request = createRequest();
        Member member = Member.builder()
            .nickname("nickname")
            .build();

        given(memberRepository.findByNickname(memberInfo.nickname())).willReturn(Optional.of(member));
        
        // Act
        noticeService.save(memberInfo, request);
        
        // Assert
        then(noticeRepository).should(times(1))
                .save(any(Notice.class));
    }

    @DisplayName("공지사항 저장시에 멤버 정보가 없으면 예외가 발생한다.")
    @Test
    void savingWithoutMemberInfoThrowsException() {
        // Arrange
        MemberInfo memberInfo = createMemberInfo();
        NoticeCreateRequest request = createRequest();
        
        given(memberRepository.findByNickname(memberInfo.nickname()))
                .willThrow(new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));
        
        // Act & Assert
        assertThatThrownBy(() -> noticeService.save(memberInfo, request))
            .isInstanceOf(MemberNotFoundException.class)
            .hasMessage(memberInfo.nickname() + "님을 찾을 수 없습니다.");
    }

    @DisplayName("공지사항을 정상적으로 수정한다.")
    @Test
    void updateNotice() {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest();

        Notice noticeMock = mock(Notice.class);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.ofNullable(noticeMock));
        
        // Act
        noticeService.update(noticeId, request);
        
        // Assert
        then(noticeMock).should(times(1))
                .update(request);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 수정시 예외를 발생한다.")
    @Test
    void updatingWithNonExistIdThrowsException() {
        // Arrange
        long noticeId = 1L;
        NoticeUpdateRequest request = createUpdateRequest();

        given(noticeRepository.findById(noticeId)).willThrow(new NoticeNotFound());
        
        // Act & Assert
        assertThatThrownBy(() -> noticeService.update(noticeId, request))
            .isInstanceOf(NoticeNotFound.class)
            .hasMessage(NOTICE_ERROR_MESSAGE);

        then(noticeRepository).should(times(1))
                .findById(noticeId);
    }

    @DisplayName("공지사항을 삭제시 isDeleted를 true로 변경한다.")
    @Test
    void deletingNoticeChangeIsDeleted() {
        // Arrange
        long noticeId = 1L;
        Notice noticeMock = mock(Notice.class);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(noticeMock));
        
        // Act
        noticeService.remove(noticeId);
        
        // Assert
        then(noticeMock).should(times(1))
                .changeDeleted(true);
    }

    @DisplayName("존재하지 않는 ID로 공지사항 삭제시 예외를 발생한다.")
    @Test
    void deleteNoticeWithNonExistId() {
        // Arrange
        long noticeId = 1L;

        given(noticeRepository.findById(noticeId)).willThrow(new NoticeNotFound());
        
        // Act & Assert
        assertThatThrownBy(() -> noticeService.remove(noticeId))
            .isInstanceOf(NoticeNotFound.class)
            .hasMessage(NOTICE_ERROR_MESSAGE);
    }

    private Notice createNotice(String title, String content) {
        return Notice.builder()
            .title(title)
            .content(content)
            .isDeleted(false)
            .member(mock(Member.class))
            .build();
    }

    private MemberInfo createMemberInfo() {
        return MemberInfo.builder()
            .nickname("nickname")
            .role(ROLE_ADMIN)
            .build();
    }

    private NoticeCreateRequest createRequest() {
        return NoticeCreateRequest.builder()
            .title("title 1")
            .content("content 1")
            .build();
    }

    private NoticeUpdateRequest createUpdateRequest() {
        return NoticeUpdateRequest.builder()
            .title("title 1")
            .content("update content")
            .build();
    }
}
