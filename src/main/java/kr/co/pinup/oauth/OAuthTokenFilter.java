package kr.co.pinup.oauth;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// TODO FILTER를 통해서 ACCESSTOKEN 만료 확인 및 갱신하기
@WebFilter("/api/*") // API 경로에 적용
public class OAuthTokenFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 초기화 로직
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String accessToken = httpRequest.getHeader("Authorization"); // 헤더에서 accessToken 가져오기
        String refreshToken = httpRequest.getHeader("Refresh-Token"); // 헤더에서 refreshToken 가져오기

        if (accessToken != null && isAccessTokenExpired(accessToken)) {
            // accessToken이 만료되었을 경우, refreshToken을 사용해 새로운 accessToken 발급
            if (refreshToken != null && !isRefreshTokenExpired(refreshToken)) {
                String newAccessToken = refreshAccessToken(refreshToken);
                if (newAccessToken != null) {
                    httpResponse.setHeader("Authorization", "Bearer " + newAccessToken);
                    // 새로운 accessToken을 요청 헤더에 다시 설정하여 처리
                    chain.doFilter(request, response); // 다음 필터로 요청 전달
                } else {
                    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
                }
            } else {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token is missing or invalid");
            }
        } else {
            chain.doFilter(request, response); // 정상적인 요청 처리
        }
    }

    private boolean isAccessTokenExpired(String accessToken) {
        // accessToken 만료 여부 체크하는 로직 (JWT 등의 경우 payload에서 만료시간 확인)
        return false;
    }

    private boolean isRefreshTokenExpired(String refreshToken) {
        // refreshToken 만료 여부 체크하는 로직
        return false;
    }

    private String refreshAccessToken(String refreshToken) {
        // refreshToken을 사용하여 새로운 accessToken을 발급받는 로직 (OAuth 서버 호출)
        return "newAccessToken";
    }

    @Override
    public void destroy() {
        // 종료 처리 로직
    }
}
