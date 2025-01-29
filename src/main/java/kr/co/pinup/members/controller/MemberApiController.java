package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    @GetMapping("/oauth/naver")
    public ResponseEntity<?> loginNaver(@Valid @ModelAttribute NaverLoginParams params, HttpServletRequest request) {
        log.info("Naver login process started");
        return loginProcess(params, request);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request) {
        log.info("Google login process started");
        return loginProcess(params, request);
    }

    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        MemberInfo memberInfo = memberService.login(params, session);
        session.setAttribute("memberInfo", memberInfo);

        String accessToken = session.getAttribute("accessToken").toString();
        session.removeAttribute("accessToken");

        ResponseCookie cookie = ResponseCookie.from("Authorization", accessToken)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite("Strict")
                .maxAge(3600)
                .build();

        log.info("Login successful: {}", memberInfo);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/"));
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());

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
    public ResponseEntity<?> update(@Validated @RequestBody MemberRequest memberRequest, @LoginMember MemberInfo memberInfo,
                                    HttpSession session) {
        MemberResponse updatedMember = memberService.update(memberInfo, memberRequest);
        session.setAttribute("memberInfo", new MemberInfo(updatedMember.getNickname(), memberInfo.getProvider(), memberInfo.getRole()));

        log.info("Nickname updated to: {}", updatedMember.getNickname());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody MemberRequest memberRequest, @LoginMember MemberInfo memberInfo,
                                    HttpSession session) {
        boolean isDeleted = memberService.delete(memberInfo, memberRequest);
        if (isDeleted) {
            session.invalidate();
            log.info("Member deleted successfully");
            return ResponseEntity.ok("탈퇴 성공");
        } else {
            log.warn("Member deletion failed");
            return ResponseEntity.badRequest().body("사용자 탈퇴 실패");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo, HttpServletRequest request) {
        return Optional.ofNullable(memberInfo).map(member -> {
            boolean response = memberService.logout(member.getProvider(), request);

            ResponseCookie deleteCookie = ResponseCookie.from("Authorization", "")
                    .path("/")
                    .httpOnly(false)
                    .secure(false)
                    .sameSite("Strict")
                    .maxAge(0)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, deleteCookie.toString());

            return response ? ResponseEntity.ok().headers(headers).body("로그아웃 성공")
                    : ResponseEntity.badRequest().body("로그아웃 실패");
        }).orElseGet(() -> {
            return ResponseEntity.status(401).body("로그인 정보가 없습니다.");
        });
    }
}
