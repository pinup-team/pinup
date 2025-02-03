package kr.co.pinup.members.custom;

import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {
    String nickname() default "네이버TestMember";
    OAuthProvider provider() default OAuthProvider.NAVER;
    MemberRole role() default MemberRole.ROLE_USER;
}
