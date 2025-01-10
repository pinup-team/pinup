package kr.co.pinup.faq.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import kr.co.pinup.common.BaseEntity;
import kr.co.pinup.faq.FaqCategory;
import kr.co.pinup.faq.FaqCategoryConverter;
import kr.co.pinup.notice.request.FaqUpdate;
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

    @Column(nullable = false, length = 200)
    private String answer;

    @Convert(converter = FaqCategoryConverter.class)
    @Column(nullable = false, length = 10)
    private FaqCategory category;

    public void update(FaqUpdate faqUpdate) {
        question = faqUpdate.question();
        answer = faqUpdate.answer();
        category = FaqCategory.valueOf(faqUpdate.category().toUpperCase());
    }
}
