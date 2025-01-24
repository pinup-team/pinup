package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.custom.LoginMember;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping(value = "/members", produces = "application/json;charset=UTF-8")
public class MemberController {
    private final MemberService memberService;
    private final HttpSession session;

    @GetMapping("/login")
    public String login() {
        return "members/login";
    }

    @GetMapping("/oauth/naver")
    public String loginNaver(@Valid @ModelAttribute NaverLoginParams params,
//                             BindingResult bindingResult,
                             HttpServletRequest request, Model model) {
        log.info("move to login naver");
        return loginProcess(params, request, model);
    }

    @GetMapping("/oauth/google")
    public String loginGoogle(@Valid @ModelAttribute GoogleLoginParams params,
                              HttpServletRequest request, Model model) {
        log.info("move to login google");
        return loginProcess(params, request, model);
    }

    private String loginProcess(OAuthLoginParams params, HttpServletRequest request, Model model) {
        try {
            return Optional.ofNullable(request.getSession(true))
                    .map(session -> memberService.login(params, session))
                    .map(userInfo -> {
                        request.getSession().setAttribute("userInfo", userInfo);
                        return "redirect:/";
                    })
                    .orElseGet(() -> {
                        model.addAttribute("message", "세션 생성에 실패했습니다.");
                        return "error";
                    });
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth 인증 실패: {}", e.getMessage());
            model.addAttribute("message", "OAuth 인증에 실패했습니다.");
            return "error";
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage());
            model.addAttribute("message", "로그인 중 예기치 못한 오류가 발생했습니다.");
            return "error";
        }
    }

    @GetMapping("/profile")
    public String userProfile(@LoginMember MemberInfo memberInfo, Model model) {
        try {
            MemberResponse memberResponse = memberService.findUser(memberInfo);
            if (memberResponse == null) {
                log.error("User not found for: {}", memberInfo);
                model.addAttribute("message", "사용자를 찾을 수 없습니다.");
                return "error";
            }
            model.addAttribute("profile", memberResponse);
            return "members/profile";
        } catch (IllegalArgumentException e) {
            log.error("Error occurred while fetching user: {}", e.getMessage());
            model.addAttribute("message", e.getMessage());
            return "error";
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage());
            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    // todo nickname 추천해주는거?
    @GetMapping("/nickname")
    public String makeNickname(@LoginMember MemberInfo memberInfo, Model model) {
        return Optional.ofNullable(memberInfo)
                .map(user -> {
                    model.addAttribute("nickname", memberService.makeNickname());
                    return "members/profile";
                })
                .orElseGet(() -> {
                    model.addAttribute("message", "로그인 정보가 없습니다.");
                    return "error";
                });
    }

    @PatchMapping
    public ResponseEntity<?> update(@Validated @RequestBody MemberRequest memberRequest, @LoginMember MemberInfo memberInfo) {
        try {
            MemberResponse updatedUser = memberService.update(memberInfo, memberRequest);
            if (updatedUser == null) {
                throw new IllegalArgumentException("업데이트된 사용자 정보가 없습니다.");
            }
            memberInfo = new MemberInfo(updatedUser.getNickname(), memberInfo.getProvider(), memberInfo.getRole());
            session.setAttribute("userInfo", memberInfo);

            return ResponseEntity.ok("닉네임이 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody MemberRequest memberRequest, @LoginMember MemberInfo memberInfo) {
        try {
            boolean isDeleted = memberService.delete(memberInfo, memberRequest);

            if (isDeleted) {
                session.removeAttribute("memberInfo");
                session.invalidate();
                return ResponseEntity.ok("탈퇴 성공");
            } else {
                return ResponseEntity.badRequest().body("사용자 탈퇴 실패");
            }
        } catch (MemberNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginMember MemberInfo memberInfo) {
        return Optional.ofNullable(memberInfo)
                .map(user -> {
                    boolean response = memberService.logout(user.getProvider(), session);
                    return response ? ResponseEntity.ok("로그아웃 성공") : ResponseEntity.badRequest().body("로그아웃 실패");
                })
                .orElseGet(() -> {
                    return ResponseEntity.status(401).body("로그인 정보가 없습니다.");
                });
    }
}
