package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.*;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginRequest", MemberLoginRequest.builder().providerType(OAuthProvider.PINUP).build());
        return "views/members/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", MemberRequest.builder().providerType(OAuthProvider.PINUP).build());
        return "views/members/register";
    }

    @GetMapping("/profile")
    public String memberProfile(@LoginMember MemberInfo memberInfo, Model model) {
        MemberResponse memberResponse = memberService.findMember(memberInfo);

        if (memberInfo.getProvider() == OAuthProvider.PINUP) {
            MemberRequest updateRequest = MemberRequest.builder()
                    .name(memberResponse.getName())
                    .email(memberResponse.getEmail())
                    .password(null)
                    .nickname(memberResponse.getNickname())
                    .providerType(memberResponse.getProviderType())
                    .build();
            model.addAttribute("profile", updateRequest);
        } else {
            model.addAttribute("profile", memberResponse);
        }

        return "views/members/profile";
    }

    @GetMapping("/verify")
    public String verify() {
        return "views/members/verify";
    }

    @GetMapping("/password")
    public String password(HttpSession session, Model model) {
        String email = (String) session.getAttribute("verifiedEmail");

        if (email == null) {
            // 세션에 이메일이 없으면 본인인증 페이지로 리다이렉트
            return "redirect:/members/verify";
        }

        model.addAttribute("resetRequest", MemberPasswordRequest.builder().email(email).providerType(OAuthProvider.PINUP).build());
        return "views/members/password";
    }
}
