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
//         TODO 도희 :accessToken header에 들어가기 성공, 당분간 session에서 처리
//        session.removeAttribute("accessToken");
//
//        System.out.println("Set-Cookie Header: " + headers.get(HttpHeaders.SET_COOKIE));
//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create("/"));
        HttpHeaders headers = controlCookie(accessToken, 3600);
        headers.setLocation(URI.create("/"));

        log.info("Login successful: {}", memberInfo);
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

        log.info("Nickname updated to: {}", updatedMember.getNickname());
        return ResponseEntity.ok("닉네임이 변경되었습니다.");
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@Validated @RequestBody MemberRequest memberRequest, @LoginMember MemberInfo memberInfo,
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
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo, HttpServletRequest request) {
        if (memberService.logout(memberInfo.provider(), request)) {
            HttpHeaders headers = controlCookie("", 0);
            return ResponseEntity.ok()
//                    .headers(headers)
                    .body("로그아웃 성공");
        } else {
            return ResponseEntity.badRequest().body("로그아웃 실패");
        }
    }

    private HttpHeaders controlCookie(String accessToken, int age) {
        ResponseCookie cookie = ResponseCookie.from("Authorization", accessToken)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite("Strict")
                .maxAge(age)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
}
