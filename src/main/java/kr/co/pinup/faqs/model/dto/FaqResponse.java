package kr.co.pinup.faqs.model.dto;

import kr.co.pinup.faqs.Faq;
import kr.co.pinup.users.MemberResponse;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FaqResponse(Long id, String question, String answer, String category,
                          MemberResponse member, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public FaqResponse(Faq faq) {
        this(faq.getId(), faq.getQuestion(), faq.getAnswer(), faq.getCategory().getName(),
                MemberResponse.toResponse(faq.getMember()),
                faq.getCreatedAt(), faq.getUpdatedAt());
    }
}
