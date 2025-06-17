package kr.co.pinup.faqs.controller;

import jakarta.validation.Valid;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqApiController {

    private final FaqService faqService;

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Void> save(@AuthenticationPrincipal MemberInfo memberInfo,
                                     @RequestBody @Valid FaqCreateRequest request) {
        log.debug("save method FaqCreateRequest={}", request);

        faqService.save(memberInfo, request);

        return ResponseEntity.status(CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<FaqResponse>> findAll() {
        return ResponseEntity.ok(faqService.findAll());
    }

    @GetMapping("/{faqId}")
    public ResponseEntity<FaqResponse> find(@PathVariable Long faqId) {
        log.debug("find method faqId={}", faqId);

        return ResponseEntity.ok(faqService.find(faqId));
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @PutMapping("/{faqId}")
    public ResponseEntity<Void> update(@PathVariable Long faqId, @RequestBody @Valid FaqUpdateRequest request) {
        log.debug("update method faqId={}, FaqUpdateRequest={}", faqId, request);

        faqService.update(faqId, request);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{faqId}")
    public ResponseEntity<Void> delete(@PathVariable Long faqId) {
        log.debug("delete method faqId={}", faqId);

        faqService.remove(faqId);

        return ResponseEntity.noContent().build();
    }

}
