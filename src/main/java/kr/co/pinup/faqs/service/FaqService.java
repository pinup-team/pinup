package kr.co.pinup.faqs.service;

import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.faqs.Faq;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.users.Member;
import kr.co.pinup.users.MemberRepository;
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
    private final MemberRepository memberRepository;

    public void save(FaqCreateRequest request) {
        // TODO : 임시 로직 / merge 후 수정 필요
        List<Member> members = memberRepository.findAll();

        FaqCategory category = FaqCategory.valueOf(request.category().toUpperCase());

        Faq faq = Faq.builder()
                .question(request.question())
                .answer(request.answer())
                .category(category)
                .member(members.get(0))
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
    public void update(Long faqId, FaqUpdateRequest request) {
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
