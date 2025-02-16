package kr.co.pinup.members.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthTokenUtils;
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
    public String login() {
        return "views/members/login";
    }

    @GetMapping("/profile")
    public String memberProfile(@LoginMember MemberInfo memberInfo, Model model, HttpServletRequest request) {
        MemberResponse memberResponse = memberService.findMember(memberInfo, request);
        model.addAttribute("profile", memberResponse);
        return "views/members/profile";
    }
}
