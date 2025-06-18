package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberApiResponse;
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
        log.debug("Login with Naver");
        return loginProcess(params, request, response);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Login with Google");
        return loginProcess(params, request, response);
    }

    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Login with OAuth Start");
        HttpSession session = request.getSession(true);

        Triple<OAuthResponse, OAuthToken, String> triple = null;
        try {
            triple = memberService.login(params, session);
            log.info("OAuth 로그인 성공: provider={}, email={}", params.oAuthProvider(), triple.getLeft().getEmail());
        } catch (Exception e) {
            log.warn("OAuth 로그인 실패 - provider: {}", params.oAuthProvider(), e);
            throw new OAuthTokenRequestException("OAuth 로그인 실패");
        }

        OAuthToken oAuthToken = triple.getMiddle();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/"));
        securityUtil.setRefreshTokenToCookie(response, oAuthToken.getRefreshToken());

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
        log.info("Make Random Nickname");

        return Optional.ofNullable(memberInfo).map(user -> {
            String nickname = memberService.makeNickname();
            return ResponseEntity.ok(nickname);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("로그인 정보가 없습니다."));
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        log.info("사용자 닉네임 변경 요청 - 기존 닉네임={}, 변경할 닉네임={}", memberInfo.getUsername(), memberRequest.nickname());

        MemberResponse updatedMemberResponse = memberService.update(memberInfo, memberRequest);
        if (updatedMemberResponse != null && updatedMemberResponse.getNickname().equals(memberRequest.nickname())) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("닉네임이 변경되었습니다.").build());
        } else {
            log.warn("닉네임 변경 실패 - 기존 닉네임: {}, 요청 닉네임: {}", memberInfo.getUsername(), memberRequest.nickname());
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("닉네임 변경에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> disable(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        log.info("사용자 탈퇴 요청 - 닉네임={}, 이메일={}", memberInfo.getUsername(), memberRequest.email());

        if (memberService.disable(memberInfo, memberRequest)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("탈퇴되었습니다. 이용해주셔서 감사합니다.").build());
        } else {
            log.warn("회원 탈퇴 실패 - nickname: {}", memberInfo.getUsername());
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("탈퇴에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo) {
        log.info("로그아웃 요청 - nickname={}", memberInfo.getUsername());

        if (memberService.logout(memberInfo.provider(), securityUtil.getAccessTokenFromSecurityContext())) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("로그아웃에 성공하였습니다.").build());
        } else {
            log.warn("로그아웃 실패 - nickname: {}", memberInfo.getUsername());
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("로그아웃에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }
}