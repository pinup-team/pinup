package kr.co.pinup.notices;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.members.Member;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import lombok.*;

@Entity
@Table(name = "notices")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    public void update(NoticeUpdateRequest update) {
        title = update.title();
        content = update.content();
    }

    public void changeDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
