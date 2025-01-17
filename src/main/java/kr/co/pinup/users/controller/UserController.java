package kr.co.pinup.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.users.model.UserDto;
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

    @GetMapping("/login")
    public String login() {
        return "/users/login";
    }

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
                .map(session -> userService.login(params, session))
                .map(userInfo -> {
                    request.getSession().setAttribute("userInfo", userInfo);
                    return "redirect:/";
                })
                .orElseGet(() -> handleError(request.getSession(), "세션 생성에 실패했습니다.")); // 세션 없으면 에러 처리
    }

    @GetMapping("/profile")
    public String userProfile(HttpServletRequest request, Model model) {
        return getSession(request)
                .map(session -> {
                    UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
                    if (userInfo == null) {
                        return handleError(session, "UserInfo is null");
                    }
                    UserDto user = userService.findUser(userInfo);
                    if (user == null) {
                        return handleError(session, "User is null");
                    }
                    model.addAttribute("profile", user);
                    return "/users/profile";
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    @GetMapping("/nickname")
    public String makeNickname(HttpServletRequest request, Model model) {
        return getSession(request)
                .map(session -> {
                    model.addAttribute("nickname", userService.makeNickname());
                    return "/users/profile";
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    @PostMapping
    public String update(@ModelAttribute UserDto userDto, HttpServletRequest request, Model model) {
        return getSession(request)
                .map(session -> {
                    UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
                    UserDto user = userService.update(userInfo, userDto);
                    if (user != null) {
                        userInfo.setNickname(userDto.getNickname());
                        session.setAttribute("userInfo", userInfo);
                        model.addAttribute("profile", user);
                        return "redirect:/users/profile";
                    } else {
                        return handleError(session, "user update failed");
                    }
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    @DeleteMapping
    public String delete(@RequestBody UserDto userDto, HttpServletRequest request) {
        System.out.println("UserDto : " + userDto);
        return getSession(request)
                .map(session -> {
                    UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
                    if (userService.delete(userInfo, userDto)) {
                        session.removeAttribute("userInfo");
                        return "redirect:/";
                    } else {
                        return handleError(session, "user delete failed");
                    }
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        return getSession(request)
                .map(session -> {
                    UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
                    boolean response = userService.logout(userInfo.getProvider(), session);
                    return response ? "redirect:/" : handleError(session, "logout_failed");
                })
                .orElseGet(() -> handleError(request.getSession(), "no_session_exists"));
    }

    private Optional<HttpSession> getSession(HttpServletRequest request) {
        return Optional.ofNullable(request.getSession(false))
                .filter(session -> !session.isNew());
    }

    private String handleError(HttpSession session, String message) {
        log.error("Error: {}", message);
        session.setAttribute("error", "error");
        session.setAttribute("message", message);
        return "redirect:/error";
    }
}
