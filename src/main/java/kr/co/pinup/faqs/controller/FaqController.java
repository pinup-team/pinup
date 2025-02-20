package kr.co.pinup.faqs.controller;

import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.ForbiddenException;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private static final String VIEW_PATH = "views/faqs";

    private final FaqService faqService;
    private final MemberService memberService;

    @GetMapping
    public String list(@LoginMember MemberInfo memberInfo, Model model) {
        model.addAttribute("profile", getMember(memberInfo));
        model.addAttribute("faqs", faqService.findAll());

        return VIEW_PATH + "/list";
    }

    @GetMapping("/new")
    public String create(@LoginMember MemberInfo memberInfo, Model model) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        model.addAttribute("category", getFaqCategoryToMap());

        return VIEW_PATH + "/create";
    }

    @GetMapping("/{faqId}/update")
    public String update(@LoginMember MemberInfo memberInfo, @PathVariable Long faqId, Model model) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        model.addAttribute("profile", memberService.findMember(memberInfo));
        model.addAttribute("category", getFaqCategoryToMap());
        model.addAttribute("faq", faqService.find(faqId));

        return VIEW_PATH + "/update";
    }

    private Map<String, String> getFaqCategoryToMap() {
        return Arrays.stream(FaqCategory.values())
                .collect(Collectors.toMap(Enum::name, FaqCategory::getName));
    }

    private MemberResponse getMember(MemberInfo memberInfo) {
        if (memberInfo != null) {
            return memberService.findMember(memberInfo);
        }

        return null;
    }

    // TODO Security 적용 전까지는 공통으로 쓰일 것 같아서 리팩토링 시 공통을 빼보면 좋을 것 같다
    private void ensureAuthenticated(MemberInfo memberInfo) {
        if (memberInfo == null) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
    }

    private void ensureAdminRole(MemberInfo memberInfo) {
        if (MemberRole.ROLE_ADMIN != memberInfo.role()) {
            throw new ForbiddenException("액세스할 수 있는 권한이 없습니다.");
        }
    }
}
