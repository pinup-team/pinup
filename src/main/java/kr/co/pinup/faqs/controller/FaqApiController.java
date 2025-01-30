package kr.co.pinup.faqs.controller;

import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.ForbiddenException;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.faqs.model.dto.FaqCreateRequest;
import kr.co.pinup.faqs.model.dto.FaqResponse;
import kr.co.pinup.faqs.model.dto.FaqUpdateRequest;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@Slf4j
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqApiController {

    private final FaqService faqService;

    @PostMapping
    public ResponseEntity<Void> save(@LoginMember MemberInfo memberInfo,
                                     @RequestBody @Valid FaqCreateRequest request) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        faqService.save(memberInfo, request);

        return ResponseEntity.status(CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<FaqResponse>> findAll() {
        return ResponseEntity.ok(faqService.findAll());
    }

    @GetMapping("/{faqId}")
    public ResponseEntity<FaqResponse> find(@PathVariable Long faqId) {
        return ResponseEntity.ok(faqService.find(faqId));
    }

    @PutMapping("/{faqId}")
    public ResponseEntity<Void> update(@LoginMember MemberInfo memberInfo,
                                       @PathVariable Long faqId,
                                       @RequestBody @Valid FaqUpdateRequest request) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        faqService.update(faqId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{faqId}")
    public ResponseEntity<Void> delete(@LoginMember MemberInfo memberInfo, @PathVariable Long faqId) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        faqService.remove(faqId);

        return ResponseEntity.noContent().build();
    }

    private void ensureAuthenticated(MemberInfo memberInfo) {
        if (memberInfo == null) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
    }

    private void ensureAdminRole(MemberInfo memberInfo) {
        if (MemberRole.ROLE_ADMIN != memberInfo.role()) {
            throw new ForbiddenException("액세스할 수 있는 권한이 없습니다.");
        }
    }
}
