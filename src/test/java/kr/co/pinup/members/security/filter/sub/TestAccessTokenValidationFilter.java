package kr.co.pinup.members.security.filter.sub;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.security.SecurityUtil;
import kr.co.pinup.security.filter.AccessTokenValidationFilter;

import java.io.IOException;

public class TestAccessTokenValidationFilter extends AccessTokenValidationFilter {

    public TestAccessTokenValidationFilter(MemberService memberService, SecurityUtil securityUtil) {
        super(memberService, securityUtil);
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        super.doFilterInternal(request, response, filterChain);
    }
}