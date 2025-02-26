package kr.co.pinup.faqs.controller;

import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.service.FaqService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
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
    public String list(Model model) {
        model.addAttribute("category", getFaqCategoryToMap());
        model.addAttribute("faqs", faqService.findAll());

        return VIEW_PATH + "/list";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @GetMapping("/new")
    public String create(@LoginMember MemberInfo memberInfo, Model model) {
        model.addAttribute("category", getFaqCategoryToMap());

        return VIEW_PATH + "/create";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_ADMIN')")
    @GetMapping("/{faqId}/update")
    public String update(@LoginMember MemberInfo memberInfo, @PathVariable Long faqId, Model model) {
        model.addAttribute("profile", memberService.findMember(memberInfo));
        model.addAttribute("category", getFaqCategoryToMap());
        model.addAttribute("faq", faqService.find(faqId));

        return VIEW_PATH + "/update";
    }

    private Map<FaqCategory, String> getFaqCategoryToMap() {
        return Arrays.stream(FaqCategory.values())
                .collect(Collectors.toMap(Function.identity(), FaqCategory::getName));
    }

}
