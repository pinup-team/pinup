package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.exception.MemberServiceException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.*;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/members", produces = "application/json;charset=UTF-8")
public class MemberApiController {

    private final MemberService memberService;
    private final SecurityUtil securityUtil;
    private final AppLogger appLogger;

    @GetMapping("/oauth/naver")
    public ResponseEntity<?> loginNaver(@Valid @ModelAttribute NaverLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        appLogger.info(new InfoLog("Login with Naver"));
        return loginProcess(params, request, response);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        appLogger.info(new InfoLog("Login with Google"));
        return loginProcess(params, request, response);
    }

    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        appLogger.info(new InfoLog("Login with OAuth Start"));
        HttpSession session = request.getSession(true);

        try {
            Triple<OAuthResponse, OAuthToken, String> triple = memberService.oauthLogin(params, session);
            appLogger.info(new InfoLog("OAuth 로그인 성공 - provider=" + params.oAuthProvider() + ", email=" + triple.getLeft().getEmail()));

            securityUtil.setRefreshTokenToCookie(response, triple.getMiddle().getRefreshToken());

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/"));

            String encodedMessage = URLEncoder.encode(triple.getRight(), StandardCharsets.UTF_8);
            response.addHeader(HttpHeaders.SET_COOKIE,
                    createCookie("loginMessage", encodedMessage, 5, false).toString());

            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        } catch (MemberBadRequestException e) {
            appLogger.error(new ErrorLog("OAuth 로그인 실패 - provider: " + params.oAuthProvider(), e));

            throw new MemberServiceException(params.oAuthProvider().getDisplayName() + " 로그인에 실패하였습니다.\n" + e.getMessage());
        } catch (Exception e) {
            appLogger.error(new ErrorLog("OAuth 로그인 실패 - provider: " + params.oAuthProvider(), e));
            throw new OAuthTokenRequestException("OAuth 로그인 실패");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberLoginRequest member, HttpServletRequest request, HttpServletResponse response) {
        appLogger.info(new InfoLog("Login By Pinup"));
        HttpSession session = request.getSession(true);

        try {
            Pair<Member, String> pair = memberService.login(member, session);
            appLogger.info(new InfoLog("자체 로그인 성공 - provider=" + member.providerType() + ", email=" + member.email()));
            securityUtil.setRefreshTokenToCookie(response, null);
            return ResponseEntity.ok(pair.getRight());
        } catch (MemberServiceException | MemberNotFoundException | UnauthorizedException e) {
            appLogger.warn(new WarnLog("자체 로그인 실패 - provider: " + member.providerType())
                    .setStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                    .addDetails("reason", e.getMessage()));
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("자체 로그인 실패 - provider: " + member.providerType(), e));
            throw new MemberNotFoundException("로그인에 실패하였습니다.");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateEmail(@RequestParam("email") @Valid String email) {
        appLogger.info(new InfoLog("이메일 중복 확인 요청: " + email));

        return memberService.validateEmail(email)
                ? ResponseEntity.ok("가입 가능한 이메일입니다.")
                : ResponseEntity.status(HttpStatus.CONFLICT).body("이미 가입된 이메일입니다.");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody MemberRequest member, HttpServletResponse response) {
        appLogger.info(new InfoLog("register By Pinup"));

        try {
            Pair<Member, String> pair = memberService.register(member);

            if (pair == null || pair.getLeft() == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("회원가입에 실패했습니다.\n이미 존재하는 이메일일 수 있습니다.");
            }

            return ResponseEntity.ok("회원가입이 완료되었습니다.\n로그인 화면으로 이동합니다.");
        } catch (MemberBadRequestException e) {
            appLogger.error(new ErrorLog("회원가입 실패", e));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("회원가입에 실패했습니다.\n" + e.getMessage());
        } catch (Exception e) {
            appLogger.error(new ErrorLog("회원가입 실패", e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류로 인해 회원가입에 실패했습니다.");
        }
    }

    @GetMapping(value = "/nickname", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> makeNickname(HttpServletRequest request) {
        appLogger.info(new InfoLog("Make Random Nickname"));

        String referer = request.getHeader("Referer");

        if (referer != null && referer.contains("/members/profile")) {
            MemberInfo memberInfo = securityUtil.getMemberInfo();
            if (memberInfo == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("로그인 정보가 없습니다.");
            }
            String nickname = memberService.makeNickname();
            return ResponseEntity.ok(nickname);
        }

        if (referer != null && referer.contains("/members/register")) {
            String nickname = memberService.makeNickname();
            return ResponseEntity.ok(nickname);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("허용되지 않은 요청입니다.");
    }

    @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        appLogger.info(new InfoLog("사용자 정보 수정 요청 - 기존 닉네임=" + memberInfo.getUsername() + ", 변경할 닉네임=" + memberRequest.nickname()));

        MemberResponse updatedMemberResponse = memberService.update(memberInfo, memberRequest);
        if (updatedMemberResponse != null && updatedMemberResponse.getNickname().equals(memberRequest.nickname())) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("사용자 정보 수정되었습니다.").build());
        } else {
            appLogger.warn(new WarnLog("사용자 정보 수정 실패 - 기존 닉네임=" + memberInfo.getUsername() + ", 요청 닉네임=" + memberRequest.nickname()).setStatus("400"));
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("사용자 정보 수정에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> disable(@LoginMember MemberInfo memberInfo, @Validated @RequestBody MemberRequest memberRequest) {
        appLogger.info(new InfoLog("사용자 탈퇴 요청 - 닉네임=" + memberInfo.getUsername() + ", 이메일=" + memberRequest.email()));

        if (memberService.disable(memberInfo, memberRequest)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("탈퇴되었습니다. 이용해주셔서 감사합니다.").build());
        } else {
            appLogger.warn(new WarnLog("회원 탈퇴 실패 - nickname: " + memberInfo.getUsername()).setStatus("400"));
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("탈퇴에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo) {
        appLogger.info(new InfoLog("로그아웃 요청 - nickname=" + memberInfo.getUsername()));

        if (memberService.logout(memberInfo.provider(), securityUtil.getAccessTokenFromSecurityContext())) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(200).message("로그아웃되었습니다.").build());
        } else {
            appLogger.warn(new WarnLog("로그아웃 실패 - nickname: " + memberInfo.getUsername()).setStatus("400"));
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(MemberApiResponse.builder().code(400).message("로그아웃에 실패하였습니다.\n관리자에게 문의해주세요.").build());
        }
    }

    private ResponseCookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(httpOnly)
                .build();
    }

}