package kr.co.pinup.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/oauth/naver")
    public String loginNaver(@ModelAttribute NaverLoginParams params,
                             HttpServletRequest request) {
        log.info("move to login naver");
        return loginProcess(params, request);
    }

    @GetMapping("/oauth/google")
    public String loginGoogle(@ModelAttribute GoogleLoginParams params,
                              HttpServletRequest request) {
        log.info("move to login google");
        return loginProcess(params, request);
    }

    private String loginProcess(OAuthLoginParams params, HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(true))
                .map(session -> loginUser(params, session))
                .orElseThrow(() -> new IllegalStateException("세션 생성에 실패했습니다."));
    }

    private String loginUser(OAuthLoginParams params, HttpSession session) {
        return Optional.ofNullable(userService.login(params, session))
                .map(userInfo -> {
                    session.setAttribute("userInfo", userInfo);
                    return "redirect:/";
                })
                .orElseGet(() -> handleError(session, "로그인 실패"));
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(false))
                .filter(session -> !session.isNew())
                .map(session -> {
                    UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
                    if(userInfo != null) {
                        boolean response = userService.logout(userInfo.getProvider(), session);
                        if(!response){
                            handleError(session, "logout_failed");
                        }
                    } else {
                        handleError(session, "no_user_info_in_session");
                    }
                    return "redirect:/";
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    private String handleError(HttpSession session, String message) {
        log.error("Error: {}", message);
        session.setAttribute("error", "error");
        session.setAttribute("message", message);
        return "redirect:/error";
    }
}
