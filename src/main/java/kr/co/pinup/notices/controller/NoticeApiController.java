package kr.co.pinup.notices.controller;

import jakarta.validation.Valid;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.service.NoticeService;
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
    public ResponseEntity<Void> save(@RequestBody @Valid NoticeCreateRequest request) {
        noticeService.save(request);

        return ResponseEntity.status(CREATED).build();
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<Void> update(@PathVariable Long noticeId, @RequestBody @Valid NoticeUpdateRequest request) {
        noticeService.update(noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(@PathVariable Long noticeId) {
        noticeService.remove(noticeId);

        return ResponseEntity.noContent().build();
    }
}
