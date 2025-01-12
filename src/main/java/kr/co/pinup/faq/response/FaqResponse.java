package kr.co.pinup.faq.response;

import kr.co.pinup.faq.domain.Faq;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FaqResponse(Long id, String question, String answer, String category,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {

    public FaqResponse(Faq faq) {
        this(faq.getId(), faq.getQuestion(), faq.getAnswer(),
                faq.getCategory().getName(), faq.getCreatedAt(), faq.getUpdatedAt());
    }
}
