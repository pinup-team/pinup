package kr.co.pinup.members.custom;

import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MockSecurityContextTest {
    @Test
    @WithMockMember
    public void testMethod() {
        MemberInfo memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);  // ✅ authentication이 null이 아니어야 함
        assertNotNull(authentication.getPrincipal());  // ✅ principal도 null이 아니어야 함
        assertEquals(memberInfo, authentication.getPrincipal());
        assertEquals("valid-access-token", authentication.getDetails().toString());
    }
}
