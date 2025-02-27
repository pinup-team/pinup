package kr.co.pinup.faqs;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.members.Member;
import lombok.*;

@Entity
@Table(name = "faqs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Faq extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String question;

    @Column(nullable = false, length = 500)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FaqCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void update(FaqUpdateRequest faqUpdate) {
        question = faqUpdate.question();
        answer = faqUpdate.answer();
        category = faqUpdate.category();
    }
}
