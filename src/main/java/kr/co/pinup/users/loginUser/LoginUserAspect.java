package kr.co.pinup.users.loginUser;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.pinup.users.error.UnauthorizedException;
import kr.co.pinup.users.model.UserInfo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoginUserAspect {

    @Around("@annotation(LoginUser)")  // @LoginUser 어노테이션이 붙은 메서드에 적용
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        UserInfo userInfo = (UserInfo) request.getSession().getAttribute("userInfo");

        if (userInfo == null) {
            throw new UnauthorizedException("로그인 정보가 없습니다.");
        }

        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof UserInfo) {
                args[i] = userInfo;
            }
        }

        return joinPoint.proceed(args);
    }
}