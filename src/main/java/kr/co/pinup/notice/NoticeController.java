package kr.co.pinup.notice;

import jakarta.validation.Valid;
import kr.co.pinup.notice.request.NoticeCreate;
import kr.co.pinup.notice.request.NoticeUpdate;
import kr.co.pinup.notice.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> save(@RequestBody @Valid NoticeCreate request) {
        noticeService.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<Void> update(@PathVariable Long noticeId, @RequestBody @Valid NoticeUpdate request) {
        noticeService.update(noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(@PathVariable Long noticeId) {
        noticeService.delete(noticeId);

        return ResponseEntity.noContent().build();
    }
}
