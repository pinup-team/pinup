package kr.co.pinup.notices.model.dto;

import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.notices.Notice;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NoticeResponse(Long id, String title, String content, MemberResponse member, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {

    public NoticeResponse(Notice notice) {
        this(notice.getId(), notice.getTitle(), notice.getContent(),
                new MemberResponse(notice.getMember()), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
