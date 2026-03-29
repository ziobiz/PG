package com.pg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenAuthFilter tokenAuthFilter;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;

    public SecurityConfig(TokenAuthFilter tokenAuthFilter, ApiAuthenticationEntryPoint apiAuthenticationEntryPoint) {
        this.tokenAuthFilter = tokenAuthFilter;
        this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
    }

    /**
     * /api/** 전용: formLogin 없음 → LoginUrl 302 가 끼어들 여지 제거(ERR_TOO_MANY_REDIRECTS 방지).
     * 미인증 시 항상 401. 로그인은 {@code permitAll}( {@code web.ignoring} 금지 — CORS 필터 미적용 방지).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**");
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/login")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/me")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/dev/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/org/branding")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/org/portalByHost")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pay/chillpay/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/open/pg-notify/**")).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(Customizer.withDefaults());
        http.addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 화면·폼 로그인 ( /api/** 는 위 체인이 먼저 매칭됨 ) */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(auth -> auth
                // 이 체인에 /api/** 가 들어오면 안 됨(위 api 체인이 먼저 매칭). 방어용.
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).denyAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/"),
                    AntPathRequestMatcher.antMatcher("/login"),
                    AntPathRequestMatcher.antMatcher("/main"),
                    AntPathRequestMatcher.antMatcher("/index.html"),
                    AntPathRequestMatcher.antMatcher("/login.html"),
                    AntPathRequestMatcher.antMatcher("/favicon.ico"),
                    AntPathRequestMatcher.antMatcher("/robots.txt"),
                    AntPathRequestMatcher.antMatcher("/pay"),
                    AntPathRequestMatcher.antMatcher("/pay/**"),
                    AntPathRequestMatcher.antMatcher("/css/**"),
                    AntPathRequestMatcher.antMatcher("/js/**"),
                    AntPathRequestMatcher.antMatcher("/images/**"),
                    AntPathRequestMatcher.antMatcher("/uploads/**"),
                    AntPathRequestMatcher.antMatcher("/*.html"),
                    AntPathRequestMatcher.antMatcher("/h2-console/**")
                ).permitAll()
                .anyRequest().authenticated());
        httpSecurity.exceptionHandling(ex -> ex.authenticationEntryPoint(apiAuthenticationEntryPoint));
        httpSecurity.formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/main", true)
                .permitAll());
        httpSecurity.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        httpSecurity.csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/api/**")));
        httpSecurity.cors(Customizer.withDefaults());
        httpSecurity.addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
