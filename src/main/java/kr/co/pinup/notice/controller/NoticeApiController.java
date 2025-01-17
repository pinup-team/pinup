package kr.co.pinup.notice.controller;

import jakarta.validation.Valid;
import kr.co.pinup.notice.NoticeService;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeApiController {

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
    public ResponseEntity<Void> save(@RequestBody @Valid NoticeCreate request) {
        log.info("save= {}", request);
        noticeService.save(request);

        return ResponseEntity.status(CREATED).build();
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<Void> update(@PathVariable Long noticeId, @RequestBody @Valid NoticeUpdate request) {
        noticeService.update(noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(@PathVariable Long noticeId) {
        noticeService.remove(noticeId);

        return ResponseEntity.noContent().build();
    }
}
