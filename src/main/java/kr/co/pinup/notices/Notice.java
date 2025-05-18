package kr.co.pinup.notices;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member member;

    @Builder
    private Notice(String title, String content, boolean isDeleted, Member member) {
        if (member == null) {
            throw new MemberNotFoundException("작성자는 필수입니다.");
        }
        this.title = title;
        this.content = content;
        this.isDeleted = isDeleted;
        this.member = member;
    }

    public void update(NoticeUpdateRequest update) {
        title = update.title();
        content = update.content();
    }

    public void changeDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
