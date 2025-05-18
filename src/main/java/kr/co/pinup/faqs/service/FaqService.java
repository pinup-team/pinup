package kr.co.pinup.faqs.service;

import kr.co.pinup.faqs.Faq;
import kr.co.pinup.faqs.exception.FaqNotFound;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.repository.FaqRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
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

    public void save(MemberInfo memberInfo, FaqCreateRequest request) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        faqRepository.save(Faq.builder()
                .question(request.question())
                .answer(request.answer())
                .category(request.category())
                .member(member)
                .build());
    }

    public List<FaqResponse> findAll() {
        return faqRepository.findAllByOrderByCreatedAtDescIdDesc()
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
