package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/members", produces = "application/json;charset=UTF-8")
public class MemberApiController {

    private final MemberService memberService;
    private final SecurityUtil securityUtil;

    @GetMapping("/oauth/naver")
    public ResponseEntity<?> loginNaver(@Valid @ModelAttribute NaverLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Naver login process started");
        return loginProcess(params, request, response);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Google login process started");
        return loginProcess(params, request, response);
    }

    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);

        Pair<OAuthResponse, OAuthToken> oAuthResponseOAuthTokenPair = memberService.login(params, session);

        OAuthToken oAuthToken = oAuthResponseOAuthTokenPair.getRight();

        if(oAuthToken == null) {
            throw new OAuthTokenRequestException("OAuth token is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/"));

        if (oAuthToken.getRefreshToken() == null) {
            throw new OAuthTokenRequestException("Refresh token is empty");
        }
        securityUtil.setRefreshTokenToCookie(response, oAuthToken.getRefreshToken());

        log.debug("Login successful: {}", securityUtil.getMemberInfo());
        return ResponseEntity.status(302)
                .headers(headers)
                .build();
    }

    // todo nickname 추천해주는거?
    @GetMapping("/nickname")
    public String makeNickname(@LoginMember MemberInfo memberInfo, Model model) {
        return Optional.ofNullable(memberInfo).map(user -> {
            model.addAttribute("nickname", memberService.makeNickname());
            return "views/members/profile";
        }).orElseGet(() -> {
            model.addAttribute("message", "로그인 정보가 없습니다.");
            return "error";
        });
    }

    @PatchMapping
    public ResponseEntity<?> update(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        MemberResponse updatedMember = memberService.update(memberInfo, memberRequest);

        log.debug("Nickname updated to: {}", updatedMember.getNickname());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest,
                                    HttpSession session) {
        boolean isDeleted = memberService.delete(memberInfo, memberRequest);
        if (isDeleted) {
            session.invalidate();
            log.info("Member deleted successfully");
            return ResponseEntity.ok().body("탈퇴 성공");
        } else {
            log.warn("Member deletion failed");
            return ResponseEntity.badRequest().body("사용자 탈퇴 실패");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo) {
        if (memberService.logout(memberInfo.provider(), securityUtil.getAccessTokenFromSecurityContext())) {
            return ResponseEntity.ok()
                    .body("로그아웃 성공");
        } else {
            return ResponseEntity.badRequest().body("로그아웃 실패");
        }
    }
}
