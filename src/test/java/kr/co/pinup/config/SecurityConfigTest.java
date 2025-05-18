package kr.co.pinup.config;

import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthApiClient;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.security.SecurityUtil;
import kr.co.pinup.security.filter.AccessTokenValidationFilter;
import kr.co.pinup.security.filter.SessionExpirationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@TestConfiguration
public class SecurityConfigTest {
    @Bean
    public SecurityUtil securityUtil() {
        return new SecurityUtil();
    }

    @Bean
    public OAuthService oAuthService(List<OAuthApiClient> clients) {
        return new OAuthService(clients);
    }

    @Bean(name = "testSessionExpirationFilter")
    public SessionExpirationFilter sessionExpirationFilter(SecurityUtil securityUtil) {
        return new SessionExpirationFilter(securityUtil);
    }

    @Bean(name = "testAccessTokenValidationFilter")
    public AccessTokenValidationFilter accessTokenValidationFilter(MemberService memberService, SecurityUtil securityUtil) {
        return new AccessTokenValidationFilter(memberService, securityUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SessionExpirationFilter sessionExpirationFilter,
                                                   AccessTokenValidationFilter accessTokenValidationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                    session.invalidSessionUrl("/");
                    session.maximumSessions(1)
                            .expiredUrl("/");
                })
                .securityContext(securityContext -> securityContext
                        .requireExplicitSave(false)
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                .addFilterBefore(sessionExpirationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(accessTokenValidationFilter, SessionExpirationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("X-Requested-With", "Content-Type", "Authorization", "X-XSRF-token"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
