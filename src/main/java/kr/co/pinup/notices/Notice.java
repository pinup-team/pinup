package kr.co.pinup.notices;

import jakarta.persistence.*;

import kr.co.pinup.BaseEntity;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.members.Member;
import lombok.*;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member member;

    public void update(NoticeUpdateRequest update) {
        title = update.title();
        content = update.content();
    }

}
