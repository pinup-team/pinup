package kr.co.pinup.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 이 메서드는 정적 자원에 대해 보안을 적용하지 않도록 설정한다.
     * 정적 자원은 보통 HTML, CSS, JavaScript, 이미지 파일 등을 의미하며, 이들에 대해 보안을 적용하지 않음으로써 성능을 향상시킬 수 있다.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    /**
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll()
//                                .requestMatchers("/resources/**", "/static/**", "/templates/**").permitAll()
//                                .requestMatchers("/", "/members/login", "/api/members/oauth/**").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login").permitAll()
                )
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                    session.invalidSessionUrl("/");
                    // maximumSessions()는 별도의 하위 DSL을 통해 설정합니다.
                    session.maximumSessions(1)
                            .expiredUrl("/");
                })
//                .sessionManagement(session -> session
//                        .invalidSessionUrl("/")
//                        .maximumSessions(1)
//                        .expiredUrl("/")
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                );

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
