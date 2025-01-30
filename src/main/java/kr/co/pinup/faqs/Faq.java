package kr.co.pinup.faqs;

import jakarta.persistence.*;
import kr.co.pinup.BaseEntity;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.members.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "faqs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String question;

    @Column(nullable = false, length = 200)
    private String answer;

    @Convert(converter = FaqCategoryConverter.class)
    @Column(nullable = false, length = 10)
    private FaqCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public Faq(String question, String answer, FaqCategory category,
               Member member, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.member = member;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(FaqUpdateRequest faqUpdate) {
        question = faqUpdate.question();
        answer = faqUpdate.answer();
        category = FaqCategory.valueOf(faqUpdate.category().toUpperCase());
    }
}
