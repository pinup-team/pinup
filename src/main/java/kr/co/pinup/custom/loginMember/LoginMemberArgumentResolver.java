package kr.co.pinup.custom.loginMember;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.model.dto.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(LoginMember.class) != null;
    }

    @Override
    public MemberInfo resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // CHECK 이미 인증되어 있지 않은데 처리해야할까?
        if(authentication instanceof AnonymousAuthenticationToken) {
            System.out.println("Anonymous AuthenticationToken");
            return null;
        }

        if (authentication == null || ! (authentication.getPrincipal() instanceof MemberInfo)) {
            throw new UnauthorizedException("로그인 정보가 없습니다.");
        }

        return (MemberInfo) authentication.getPrincipal();
    }
}