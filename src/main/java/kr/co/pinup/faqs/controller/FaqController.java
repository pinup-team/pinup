package kr.co.pinup.faqs.controller;

import kr.co.pinup.faqs.model.enums.FaqCategory;
import kr.co.pinup.faqs.service.FaqService;
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

    private final FaqService faqService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("faqs", faqService.findAll());

        return "views/faqs/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("category", getFaqCategoryToMap());

        return "views/faqs/create";
    }

    @GetMapping("/{faqId}/update")
    public String update(@PathVariable Long faqId, Model model) {
        model.addAttribute("category", getFaqCategoryToMap());
        model.addAttribute("faq", faqService.find(faqId));

        return "views/faqs/update";
    }

    private Map<String, String> getFaqCategoryToMap() {
        return Arrays.stream(FaqCategory.values())
                .collect(Collectors.toMap(Enum::name, FaqCategory::getName));
    }
}
