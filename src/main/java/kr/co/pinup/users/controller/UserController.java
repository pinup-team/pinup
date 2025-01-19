package kr.co.pinup.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.oauth.OAuthLoginParams;
import kr.co.pinup.users.oauth.google.GoogleLoginParams;
import kr.co.pinup.users.oauth.naver.NaverLoginParams;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.loginUser.LoginUser;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final HttpSession session;

    @GetMapping("/login")
    public String login() {
        return "/users/login";
    }

    @GetMapping("/oauth/naver")
    public String loginNaver(@ModelAttribute NaverLoginParams params,
                             HttpServletRequest request, Model model) {
        log.info("move to login naver");
        return loginProcess(params, request, model);
    }

    @GetMapping("/oauth/google")
    public String loginGoogle(@ModelAttribute GoogleLoginParams params,
                              HttpServletRequest request, Model model) {
        log.info("move to login google");
        return loginProcess(params, request, model);
    }

    private String loginProcess(OAuthLoginParams params, HttpServletRequest request, Model model) {
        try {
            return Optional.ofNullable(request.getSession(true))
                    .map(session -> userService.login(params, session))
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
    public String userProfile(@LoginUser UserInfo userInfo, Model model) {
        return Optional.ofNullable(userInfo)
                .map(user -> {
                    UserDto userDto = userService.findUser(user);
                    if (userDto == null) {
                        log.error("User not found for: {}", user);
                        model.addAttribute("message", "사용자를 찾을 수 없습니다.");
                        return "error";
                    }
                    model.addAttribute("profile", userDto);
                    return "/users/profile";
                })
                .orElseGet(() -> {
                    log.error("User info not found in session");
                    model.addAttribute("message", "로그인 정보가 없습니다.");
                    return "error";
                });
    }

    // todo nickname 추천해주는거?
    @GetMapping("/nickname")
    public String makeNickname(@LoginUser UserInfo userInfo, Model model) {
        return Optional.ofNullable(userInfo)
                .map(user -> {
                    model.addAttribute("nickname", userService.makeNickname());
                    return "/users/profile";
                })
                .orElseGet(() -> {
                    model.addAttribute("message", "로그인 정보가 없습니다.");
                    return "error";
                });
    }

    @PatchMapping
    public ResponseEntity<?> update(@RequestBody UserDto userDto, @LoginUser UserInfo userInfo) {
        return Optional.ofNullable(userInfo)
                .map(user -> {
                    UserDto updatedUser = userService.update(user, userDto);
                    if (updatedUser != null) {
                        user.setNickname(userDto.getNickname());
                        session.setAttribute("userInfo", user);
                        return ResponseEntity.ok("닉네임이 변경되었습니다.");
                    } else {
                        return ResponseEntity.badRequest().body("중복된 닉네임입니다.");
                    }
                })
                .orElseGet(() -> ResponseEntity.status(403).body("로그인 정보가 없습니다."));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody UserDto userDto, @LoginUser UserInfo userInfo) {
        return Optional.ofNullable(userInfo)
                .map(user -> {
                    if (userService.delete(user, userDto)) {
                        session.removeAttribute("userInfo");
                        session.invalidate();
                        return ResponseEntity.ok("탈퇴 성공");
                    } else {
                        return ResponseEntity.badRequest().body("사용자 탈퇴 실패");
                    }
                })
                .orElseGet(() -> {
                    return ResponseEntity.status(403).body("로그인 정보가 없습니다.");
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@LoginUser UserInfo userInfo) {
        return Optional.ofNullable(userInfo)
                .map(user -> {
                    boolean response = userService.logout(user.getProvider(), session);
                    return response ? ResponseEntity.ok("로그아웃 성공") : ResponseEntity.badRequest().body("로그아웃 실패");
                })
                .orElseGet(() -> {
                    return ResponseEntity.status(403).body("로그인 정보가 없습니다.");
                });
    }
}
