package kr.co.pinup.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import kr.co.pinup.common.BaseEntity;
import kr.co.pinup.notice.request.NoticeUpdate;
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

    public void update(NoticeUpdate update) {
        title = update.title();
        content = update.content();
    }

}
