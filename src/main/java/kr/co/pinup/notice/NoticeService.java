package kr.co.pinup.notice;

import jakarta.transaction.Transactional;
import kr.co.pinup.notice.domain.Notice;
import kr.co.pinup.notice.exception.NoticeNotFound;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
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

    public void save(NoticeCreate noticeCreate) {
        Notice notice = Notice.builder()
                .title(noticeCreate.title())
                .content(noticeCreate.content())
                .build();

        noticeRepository.save(notice);
    }

    @Transactional
    public void update(Long noticeId, NoticeUpdate noticeUpdate) {
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
