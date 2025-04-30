package kr.co.pinup.notices;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NoticeTest {

    @DisplayName("Notice 생성시 작성자가 존재하지 않으면 예외가 발생한다.")
    @Test
    void createNoticeWithoutMemberThrowsException() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> Notice.builder()
                .title("title 1")
                .content("content 1")
                .isDeleted(false)
                .build())
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("작성자는 필수입니다.");
    }

    @DisplayName("공지사항 수정시 title, content가 변경되어야 한다.")
    @Test
    void changeTitleAndContent() {
        // Arrange
        Notice notice = createNotice("title 1", "content 1");
        NoticeUpdateRequest request = createUpdateRequest();
        
        // Act
        notice.update(request);
        
        // Assert
        assertThat(notice).extracting(Notice::getTitle, Notice::getContent)
            .containsOnly(request.title(), request.content());
    }

    @DisplayName("공지사항 삭제시 isDeleted가 true로 변경되어야 한다.")
    @Test
    void changeIsDeletedValueTrue() {
        // Arrange
        Notice notice = createNotice("title 1", "content 2");
        
        // Act
        notice.changeDeleted(true);
        
        // Assert
        assertThat(notice.isDeleted()).isTrue();
    }

    private Notice createNotice(String title, String content) {
        return Notice.builder()
                .title(title)
                .content(content)
                .isDeleted(false)
                .member(mock(Member.class))
                .build();
    }

    private NoticeUpdateRequest createUpdateRequest() {
        return NoticeUpdateRequest.builder()
                .title("update title")
                .content("update content")
                .build();
    }
}
