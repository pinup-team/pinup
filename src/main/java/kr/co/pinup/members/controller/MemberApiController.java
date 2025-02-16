package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.OAuthTokenUtils;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<?> loginNaver(@Valid @ModelAttribute NaverLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Naver login process started");
        return loginProcess(params, request, response);
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<?> loginGoogle(@Valid @ModelAttribute GoogleLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        log.info("Google login process started");
        return loginProcess(params, request, response);
    }

    // TODO 로그인 성공, ACCESSTOKEN HEADER에 들어가는 것ㄱ도 확인 완료
    // 로그인 로직 다 긑나고 나서 /으로 안돌아감 이것도 확인해야함
    // 그런데 아직 F12 들어가서 COOKIE에서 REFRESHTOKEN 확인 안되는 중, 이거 들어올 수 잇또록 해야함
    private ResponseEntity<?> loginProcess(OAuthLoginParams params, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);

        Pair<OAuthResponse, OAuthToken> oAuthResponseOAuthTokenPair = memberService.login(params, session);

        OAuthToken oAuthToken = oAuthResponseOAuthTokenPair.getRight();
        if (oAuthToken == null) {
            throw new UnauthorizedException("MemberService : OAuth token is null");
        }
        OAuthResponse oAuthResponse = oAuthResponseOAuthTokenPair.getLeft();
        if (oAuthResponse == null) {
            throw new UnauthorizedException("MemberService : OAuth response is null");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/"));

        // 리프레시 토큰을 HttpOnly 쿠키에 저장
        OAuthTokenUtils.setRefreshTokenToCookie(response, headers, oAuthToken.getRefreshToken());
        /*Cookie refreshTokenCookie = new Cookie("refreshToken", oAuthToken.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);  // 클라이언트에서 접근할 수 없도록 설정
        refreshTokenCookie.setPath("/");  // 쿠키가 유효한 경로 설정
        refreshTokenCookie.setMaxAge(60 * 60 * 24);  // 쿠키의 유효 기간 설정 (예시: 1일)
//        refreshTokenCookie.setSecure(true);  // HTTPS에서만 쿠키가 전송되도록 설정
//
//        // 쿠키를 응답 헤더에 추가
        response.setHeader("Set-Cookie", "refreshToken=" + refreshTokenCookie.getValue() +
                "; HttpOnly; Path=/; Max-Age=86400; SameSite=None");

        // 쿠키에 저장된 refreshToken 확인 (로그로 출력)
        System.out.println("MemberApiController : Refresh Token 쿠키에 저장됨: " + refreshTokenCookie.getValue() + "/" + refreshTokenCookie.getPath() + "/" + refreshTokenCookie.getMaxAge());*/

        // 액세스 토큰을 Authorization 헤더에 추가하여 요청
        OAuthTokenUtils.addAccessTokenToHeader(headers, oAuthToken.getAccessToken());
        /*
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + oAuthToken.getAccessToken());  // Authorization 헤더에 액세스 토큰 추가
//        headers.add(HttpHeaders.SET_COOKIE, cookie.toString()); // 필요없음, addCookie해서 ㄱㅊ*/

        // 1. Authorization 헤더에서 Access Token 추출
        String accessToken = String.valueOf(headers.getFirst("Authorization")); // "Bearer <accessToken>"
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7); // "Bearer " 이후 부분만 추출
        } else {
            throw new OAuthTokenNotFoundException("MemberApiController : 엑세스 토큰 요청에 실패했습니다.");
        }
        System.out.println("MemberApiController : Authorization 헤더에 추가된 액세스 토큰: Bearer " + accessToken);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            System.out.println("MemberApiController Authentication: " + authentication.getName());
        } else {
            System.out.println("MemberApiController Authentication not found");
        }
        MemberInfo memberInfo = (MemberInfo) authentication.getPrincipal();

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
        if (memberService.logout(memberInfo.provider(), OAuthTokenUtils.getAccessTokenFromHeader(request))) {
            request.getSession(false).invalidate();
            HttpHeaders headers = controlHeader("", 0);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body("로그아웃 성공");
        } else {
            return ResponseEntity.badRequest().body("로그아웃 실패");
        }
    }

    private HttpHeaders controlHeader(String accessToken, int age) {
        ResponseCookie cookie = ResponseCookie.from("Authorization", "Bearer " + accessToken)
                .path("/")
                .httpOnly(true)  // 클라이언트에서 접근할 수 없도록 설정
                .secure(false)    // HTTPS에서만 쿠키를 전송하도록 설정
                .sameSite("Strict")  // SameSite 설정
                .maxAge(age)  // 쿠키 만료 시간 설정
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
}
