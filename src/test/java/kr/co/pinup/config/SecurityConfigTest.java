package kr.co.pinup.config;

import kr.co.pinup.custom.filter.AccessTokenValidationFilter;
import kr.co.pinup.custom.filter.SessionExpirationFilter;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.members.service.MemberService;
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

@TestConfiguration
public class SecurityConfigTest {

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf(AbstractHttpConfigurer::disable)
//                .httpBasic(AbstractHttpConfigurer::disable)
//                .formLogin(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
//                        .requestMatchers("/**").permitAll()
//                        .anyRequest().authenticated()
//                );
//
//        return http.build();
//    }
@Bean
public SessionExpirationFilter sessionExpirationFilter() {
    return new SessionExpirationFilter();
}

    @Bean
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
                    session.maximumSessions(1).expiredUrl("/");
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
