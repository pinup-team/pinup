package kr.co.pinup.members.controller;

import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberLoginRequest;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
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
}
