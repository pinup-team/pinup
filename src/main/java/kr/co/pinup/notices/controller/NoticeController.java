package kr.co.pinup.notices.controller;

import kr.co.pinup.notices.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notices", noticeService.findAll());

        return "views/notices/list";
    }

    @GetMapping("/new")
    public String create() {
        return "views/notices/create";
    }

    @GetMapping("/{noticeId}")
    public String detail(@PathVariable Long noticeId, Model model) {
        model.addAttribute("notice", noticeService.find(noticeId));

        return "views/notices/detail";
    }

    @GetMapping("/{noticeId}/update")
    public String update(@PathVariable Long noticeId, Model model) {
        model.addAttribute("notice", noticeService.find(noticeId));

        return "views/notices/update";
    }
}
