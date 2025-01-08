package kr.co.pinup.notice;

import jakarta.validation.Valid;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public List<NoticeResponse> findAll() {
        return noticeService.findAll();
    }

    @GetMapping("/{noticeId}")
    public NoticeResponse find(@PathVariable Long noticeId) {
        return noticeService.find(noticeId);
    }

    @PostMapping
    public void save(@RequestBody @Valid NoticeCreate request) {
        noticeService.save(request);
    }

    @PutMapping("/{noticeId}")
    public void update(@PathVariable Long noticeId, @RequestBody @Valid NoticeUpdate request) {
        noticeService.update(noticeId, request);
    }

    @DeleteMapping("/{noticeId}")
    public void delete(@PathVariable Long noticeId) {
        noticeService.delete(noticeId);
    }
}
