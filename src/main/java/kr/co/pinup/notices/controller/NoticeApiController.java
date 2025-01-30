package kr.co.pinup.notices.controller;

import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.ForbiddenException;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.notices.service.NoticeService;
import kr.co.pinup.notices.model.dto.NoticeCreateRequest;
import kr.co.pinup.notices.model.dto.NoticeUpdateRequest;
import kr.co.pinup.notices.model.dto.NoticeResponse;
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
    public ResponseEntity<Void> save(@LoginMember MemberInfo memberInfo,
                                     @RequestBody @Valid NoticeCreateRequest request) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        noticeService.save(memberInfo, request);

        return ResponseEntity.status(CREATED).build();
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<Void> update(@LoginMember MemberInfo memberInfo,
                                       @PathVariable Long noticeId,
                                       @RequestBody @Valid NoticeUpdateRequest request) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        noticeService.update(noticeId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(@LoginMember MemberInfo memberInfo, @PathVariable Long noticeId) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        noticeService.remove(noticeId);

        return ResponseEntity.noContent().build();
    }

    private void ensureAuthenticated(MemberInfo memberInfo) {
        if (memberInfo == null) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
    }

    private void ensureAdminRole(MemberInfo memberInfo) {
        if (MemberRole.ROLE_ADMIN != memberInfo.getRole()) {
            throw new ForbiddenException("액세스할 수 있는 권한이 없습니다.");
        }
    }
}
