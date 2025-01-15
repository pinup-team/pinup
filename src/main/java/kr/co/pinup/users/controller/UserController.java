package kr.co.pinup.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        log.info("move to login page");
        HttpSession session = request.getSession();
        model.addAttribute("authUri", userService.makeNaverAuthUri(session));
        model.addAttribute("authGoogleUri", userService.makeGoogleAuthUri(session));
        return "users/login";
    }

    @GetMapping("/oauth/naver")
    public String naverLogin(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "state", required = false) String state,
                             @RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "error_description", required = false) String error_description,
                             HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true);

        UserInfo userInfo = userService.oauthNaver(code, state, error, error_description, session);

        if (userInfo == null) {
            model.addAttribute("error", error);
            model.addAttribute("message", error_description);
            return "error";
        } else {
            session.setAttribute("userInfo", userInfo);
            return "redirect:/";
        }
    }

    @GetMapping("/oauth/google")
    public String googleLogin(@RequestParam(value = "code", required = false) String code,
                              @RequestParam(value = "state", required = false) String state,
                              @RequestParam(value = "error", required = false) String error,
                              HttpServletRequest request, Model model) {
        log.info("move to login google");
        HttpSession session = request.getSession(true);

        UserInfo userInfo = userService.oauthGoogle(code, state, error, session);

        if (userInfo == null) {
            model.addAttribute("error", error);
            model.addAttribute("message", "access_denied");
            return "error";
        } else {
            session.setAttribute("userInfo", userInfo);
            return "redirect:/";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, Model model) {
        // HttpSession은 새로운 세션을 만들 수 있기 때문에, 아래 코드 사용함
        HttpSession session = request.getSession(false);

        if (session != null) {
            UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
            if (userInfo != null) {
                String accessToken = userService.logout(session, userInfo.getProvider());
                if (accessToken == null) {
                    model.addAttribute("error", "error");
                    model.addAttribute("message", "logout_failed");
                    return "error";
                }
            } else {
                model.addAttribute("error", "error");
                model.addAttribute("message", "no_user_info_in_session");
                return "error";
            }
        } else {
            model.addAttribute("error", "error");
            model.addAttribute("message", "no_session_exists");
            return "error";
        }
        return "redirect:/";
    }
}
