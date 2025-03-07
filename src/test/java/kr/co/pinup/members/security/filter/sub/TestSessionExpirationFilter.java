package kr.co.pinup.members.security.filter.sub;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.security.SecurityUtil;
import kr.co.pinup.security.filter.SessionExpirationFilter;

import java.io.IOException;

public class TestSessionExpirationFilter extends SessionExpirationFilter {

    public TestSessionExpirationFilter(SecurityUtil securityUtil) {
        super(securityUtil);
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        super.doFilterInternal(request, response, chain);
    }
}