package kr.co.pinup.users.controller;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.users.model.*;
import kr.co.pinup.users.model.enums.Provider;
import kr.co.pinup.users.model.TokenReponse;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        log.info("move to login page");
        model.addAttribute("authUri", userService.makeNaverAuthUri(session));
        model.addAttribute("authGoogleUri", userService.makeGoogleAuthUri(session));
        return "users/login";
    }

    @GetMapping("/login/oauth/naver")
    public String naverLogin(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "state", required = false) String state,
                             @RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "error_description", required = false) String error_description,
                             HttpSession session, Model model) {
        log.info("move to login naver");
        UserInfo userInfo = userService.oauthNaver(code, state, error, error_description, session);

        if(userInfo == null) {
            model.addAttribute("error", error);
            model.addAttribute("message", error_description);
            return "error";
        } else {
            model.addAttribute("userInfo", userInfo);
            return "index";
        }
    }

    @GetMapping("/oauth/google")
    public String googleLogin(@RequestParam(value = "code", required = false) String code,
                              @RequestParam(value = "error", required = false) String error,
                              HttpSession session, Model model) {
        log.info("move to login naver");
        UserInfo userInfo = userService.oauthGoogle(code, error, session);

        if(userInfo == null) {
            model.addAttribute("error", error);
            model.addAttribute("message", "access_denied");
            return "error";
        } else {
            model.addAttribute("userInfo", userInfo);
            return "index";
        }
    }
}
