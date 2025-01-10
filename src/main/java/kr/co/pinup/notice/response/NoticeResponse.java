package kr.co.pinup.notice.response;

import kr.co.pinup.notice.domain.Notice;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoticeResponse(Long id, String title, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public NoticeResponse(Notice notice) {
        this(notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
