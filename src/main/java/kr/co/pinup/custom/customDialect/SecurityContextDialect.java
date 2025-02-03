package kr.co.pinup.custom.customDialect;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Collections;
import java.util.Set;

// TODO securitycontext에서 값 꺼내서 커스텀태그로 만들기
@Component
public class SecurityContextDialect extends AbstractDialect {

    public SecurityContextDialect() {
        super("sec"); // Dialect를 위한 접두어 설정
    }

//    @Override
//    public String getPrefix() {
//        return "sec"; // 템플릿에서 사용할 접두어
//    }
//
//    @Override
//    public Set<IExpressionObjectFactory> getExpressionObjectFactories() {
//        Set<IExpressionObjectFactory> factories = new HashSet<>();
//        factories.add(new SecurityContextFactory());
//        return factories;
//    }

    // SecurityContextFactory 클래스
    private static class SecurityContextFactory implements IExpressionObjectFactory {

        @Override
        public Set<String> getAllExpressionObjectNames() {
            return Collections.singleton("securityContext");  // 템플릿에서 'securityContext' 이름으로 접근
        }

        @Override
        public Object buildObject(IExpressionContext context, String expressionObjectName) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return authentication.getPrincipal(); // 인증된 사용자 정보 반환
            }
            return null;
        }

        @Override
        public boolean isCacheable(String expressionObjectName) {
            return false;
        }
    }
}
