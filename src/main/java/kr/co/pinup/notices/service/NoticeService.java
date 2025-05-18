package kr.co.pinup.notices.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.notices.Notice;
import kr.co.pinup.notices.exception.NoticeNotFound;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final MemberRepository memberRepository;
    private final NoticeRepository noticeRepository;

    public List<NoticeResponse> findAll() {
        return noticeRepository.findAllByIsDeletedFalseOrderByCreatedAtDescIdDesc()
                .stream()
                .map(NoticeResponse::new)
                .collect(Collectors.toList());
    }

    public NoticeResponse find(Long noticeId) {
        return noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .map(NoticeResponse::new)
                .orElseThrow(NoticeNotFound::new);
    }

    public void save(MemberInfo memberInfo, NoticeCreateRequest noticeCreate) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        Notice notice = Notice.builder()
                .title(noticeCreate.title())
                .content(noticeCreate.content())
                .member(member)
                .build();

        noticeRepository.save(notice);
    }

    @Transactional
    public void update(Long noticeId, NoticeUpdateRequest noticeUpdate) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFound::new);

        notice.update(noticeUpdate);
    }

    @Transactional
    public void remove(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFound::new);

        notice.changeDeleted(true);
    }

}
