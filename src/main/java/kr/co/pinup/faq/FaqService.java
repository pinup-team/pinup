package kr.co.pinup.faq;

import kr.co.pinup.faq.domain.Faq;
import kr.co.pinup.faq.exception.FaqNotFound;
import kr.co.pinup.faq.request.FaqCreate;
import kr.co.pinup.faq.response.FaqResponse;
import kr.co.pinup.notice.request.FaqUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    public void save(FaqCreate request) {
        FaqCategory category = FaqCategory.valueOf(request.category().toUpperCase());

        Faq faq = Faq.builder()
                .question(request.question())
                .answer(request.answer())
                .category(category)
                .build();

        faqRepository.save(faq);
    }

    public List<FaqResponse> findAll() {
        return faqRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(FaqResponse::new)
                .collect(Collectors.toList());
    }

    public FaqResponse find(Long faqId) {
        return faqRepository.findById(faqId)
                .map(FaqResponse::new)
                .orElseThrow(FaqNotFound::new);
    }

    @Transactional
    public void update(Long faqId, FaqUpdate request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(FaqNotFound::new);

        faq.update(request);
    }

    public void remove(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(FaqNotFound::new);

        faqRepository.delete(faq);
    }
}
