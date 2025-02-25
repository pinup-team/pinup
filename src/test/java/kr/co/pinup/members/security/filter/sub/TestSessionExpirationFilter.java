package kr.co.pinup.members.security.filter.sub;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.filter.SessionExpirationFilter;

import java.io.IOException;

public class TestSessionExpirationFilter extends SessionExpirationFilter {

    public TestSessionExpirationFilter() {
        super();
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // super 호출하여 부모 클래스의 로직을 그대로 사용할 수 있습니다.
        super.doFilterInternal(request, response, chain);
    }
}