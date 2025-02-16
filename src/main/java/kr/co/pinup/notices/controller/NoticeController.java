package kr.co.pinup.notices.controller;

import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.exception.common.ForbiddenException;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
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

    private static final String VIEW_PATH = "views/notices";

    private final NoticeService noticeService;
    private final MemberService memberService;

    @GetMapping
    public String list(@LoginMember MemberInfo memberInfo, Model model) {
        model.addAttribute("profile", getMember(memberInfo));
        model.addAttribute("notices", noticeService.findAll());

        return VIEW_PATH + "/list";
    }

    @GetMapping("/new")
    public String create(@LoginMember MemberInfo memberInfo) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

        return VIEW_PATH + "/create";
    }

    @GetMapping("/{noticeId}")
    public String detail(@LoginMember MemberInfo memberInfo, @PathVariable Long noticeId, Model model) {
        model.addAttribute("profile", getMember(memberInfo));
        model.addAttribute("notice", noticeService.find(noticeId));

        return VIEW_PATH + "/detail";
    }

    @GetMapping("/{noticeId}/update")
    public String update(@LoginMember MemberInfo memberInfo, @PathVariable Long noticeId, Model model) {
        ensureAuthenticated(memberInfo);
        ensureAdminRole(memberInfo);

//        model.addAttribute("profile", memberService.findMember(memberInfo));
        model.addAttribute("notice", noticeService.find(noticeId));

        return VIEW_PATH + "/update";
    }

    private MemberResponse getMember(MemberInfo memberInfo) {
        if (memberInfo != null) {
//            return memberService.findMember(memberInfo);
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
