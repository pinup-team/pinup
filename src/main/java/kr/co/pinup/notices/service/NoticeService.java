package kr.co.pinup.notices.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.notices.repository.NoticeRepository;
import kr.co.pinup.notices.Notice;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    public List<NoticeResponse> findAll() {
        return noticeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NoticeResponse::new)
                .collect(Collectors.toList());
    }

    public NoticeResponse find(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .map(NoticeResponse::new)
                .orElseThrow(NoticeNotFound::new);
    }

    public void save(NoticeCreateRequest noticeCreate) {
        // TODO : 임시 로직 / merge 후 수정 필요
        List<Member> members = memberRepository.findAll();

        Notice notice = Notice.builder()
                .title(noticeCreate.title())
                .content(noticeCreate.content())
                .member(members.get(0))
                .build();

        noticeRepository.save(notice);
    }

    @Transactional
    public void update(Long noticeId, NoticeUpdateRequest noticeUpdate) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFound::new);

        notice.update(noticeUpdate);
    }

    public void remove(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFound::new);

        noticeRepository.delete(notice);
    }

}
