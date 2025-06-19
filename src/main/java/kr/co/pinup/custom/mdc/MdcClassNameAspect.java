package kr.co.pinup.custom.mdc;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MdcClassNameAspect {

    @Pointcut("execution(* kr.co.pinup..*Service.*(..)) || execution(* kr.co.pinup..*Controller.*(..))")
    public void loggableMethods() {}

    @Around("loggableMethods()")
    public Object logMdc(ProceedingJoinPoint joinPoint) throws Throwable {
        String prevClass = MDC.get("className");
        String prevMethod = MDC.get("methodName");

        try {
            MDC.put("className", joinPoint.getTarget().getClass().getSimpleName());
            MDC.put("methodName", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        } finally {
            // 복원
            if (prevClass != null) MDC.put("className", prevClass); else MDC.remove("className");
            if (prevMethod != null) MDC.put("methodName", prevMethod); else MDC.remove("methodName");
        }
    }

}