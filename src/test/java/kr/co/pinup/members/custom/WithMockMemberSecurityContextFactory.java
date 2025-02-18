package kr.co.pinup.members.custom;

import kr.co.pinup.members.model.dto.MemberInfo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {
    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        MemberInfo memberInfo = new MemberInfo(annotation.nickname(), annotation.provider(), annotation.role());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());
        // TODO ACCESSTOKEN 테스트 할때 여기도 넣어줘야함
        context.setAuthentication(authentication);
        return context;
    }
}