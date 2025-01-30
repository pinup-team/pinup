package kr.co.pinup.custom.loginMember;

import kr.co.pinup.members.model.dto.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(LoginMember.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // HttpSession에서 로그인된 사용자 정보를 가져옴
        MemberInfo memberInfo = (MemberInfo) webRequest.getAttribute("memberInfo", WebRequest.SCOPE_SESSION);
        if (memberInfo == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 없습니다.");
        }
        return memberInfo;
    }
}
