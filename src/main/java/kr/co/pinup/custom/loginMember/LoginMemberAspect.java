package kr.co.pinup.custom.loginMember;

import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.model.dto.MemberInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoginMemberAspect {

    @Around("@annotation(kr.co.pinup.custom.loginMember.LoginMember)")  // @LoginMember 어노테이션이 붙은 메서드에 적용
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        MemberInfo memberInfo = (MemberInfo) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (memberInfo == null) {
            throw new UnauthorizedException("로그인 정보가 없습니다.");
        }

        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof MemberInfo) {
                args[i] = memberInfo;
            }
        }

        return joinPoint.proceed(args);
    }
}