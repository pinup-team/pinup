package kr.co.pinup.faqs;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import lombok.*;

@Entity
@Table(name = "faqs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
    private Faq(String question, String answer, FaqCategory category, Member member) {
        if (member == null) {
            throw new MemberNotFoundException("작성자는 필수입니다.");
        }
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.member = member;
    }

    public void update(FaqUpdateRequest faqUpdate) {
        question = faqUpdate.question();
        answer = faqUpdate.answer();
        category = faqUpdate.category();
    }
}
