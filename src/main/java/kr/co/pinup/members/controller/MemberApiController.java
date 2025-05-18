package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
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
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        return loginProcess(params, request, response);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        return loginProcess(params, request, response);
    }

    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);

        Triple<OAuthResponse, OAuthToken, String> triple = memberService.login(params, session);

        OAuthToken oAuthToken = triple.getMiddle();

        if (oAuthToken == null) {
            throw new OAuthTokenRequestException("OAuth token is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/"));

        if (oAuthToken.getRefreshToken() == null) {
            throw new OAuthTokenRequestException("Refresh token is empty");
        }
        securityUtil.setRefreshTokenToCookie(response, oAuthToken.getRefreshToken());

        // 한글 URL 인코딩 처리해 쿠키에 메시지를 저장
        String encodedMessage = URLEncoder.encode(triple.getRight(), StandardCharsets.UTF_8);
        ResponseCookie messageCookie = ResponseCookie.from("loginMessage", encodedMessage)
                .path("/")
                .maxAge(5)
                .httpOnly(false)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, messageCookie.toString());

        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .build();
    }

    @GetMapping(value = "/nickname", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> makeNickname(@LoginMember MemberInfo memberInfo) {
        return Optional.ofNullable(memberInfo).map(user -> {
            String nickname = memberService.makeNickname();
            return ResponseEntity.ok(nickname);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("로그인 정보가 없습니다."));
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        MemberResponse updatedMemberResponse = memberService.update(memberInfo, memberRequest);
        if (updatedMemberResponse != null && updatedMemberResponse.getNickname().equals(memberRequest.nickname())) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("닉네임이 변경되었습니다.");
        } else {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("닉네임 변경 실패");
        }
    }

    @DeleteMapping
    public ResponseEntity<?> disable(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        if (memberService.disable(memberInfo, memberRequest)) {
            return ResponseEntity.ok().body("탈퇴 성공");
        } else {
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
