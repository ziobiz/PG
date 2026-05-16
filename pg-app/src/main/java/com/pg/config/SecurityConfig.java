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
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

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
     * 가맹점 외부 사이트 iframe 에서 열리는 공개 결제·챗봇 HTML.
     * 기본 {@code X-Frame-Options: DENY} 를 끄지 않으면 타 도메인 iframe 에서 "연결 거부"에 가까운 오류로 보일 수 있습니다.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain embeddablePublicContentChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new OrRequestMatcher(
                AntPathRequestMatcher.antMatcher("/chatbot-pay"),
                AntPathRequestMatcher.antMatcher("/chatbot-pay/**"),
                AntPathRequestMatcher.antMatcher("/chatbot-pay.html"),
                AntPathRequestMatcher.antMatcher("/pay"),
                AntPathRequestMatcher.antMatcher("/pay/**"),
                AntPathRequestMatcher.antMatcher("/pay.html"),
                AntPathRequestMatcher.antMatcher("/pay-result.html")));
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        http.cors(Customizer.withDefaults());
        return http.build();
    }

    /**
     * /api/** 전용: formLogin 없음 → LoginUrl 302 가 끼어들 여지 제거(ERR_TOO_MANY_REDIRECTS 방지).
     * 미인증 시 항상 401. 로그인은 {@code permitAll}( {@code web.ignoring} 금지 — CORS 필터 미적용 방지).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // String 기반 matcher는 환경에 따라 MVC matcher로 해석되어 매칭이 어긋날 수 있어 명시적으로 AntPath 사용
        http.securityMatcher(AntPathRequestMatcher.antMatcher("/api/**"));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/login")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/me")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/dev/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/org/branding")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/org/portalByHost")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pub/chatbot/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pub/login-notice")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pay/chillpay/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pay/jpay/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/open/pg-notify/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/middleware/notify/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/middleware/v1/pg/**")).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        http.csrf(AbstractHttpConfigurer::disable);
        // /api/** 는 SPA/외부 호출이 많아 기본 로그인 폼 리다이렉트(302) 방지 필수
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
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
        // /api/** 는 API 체인에서만 처리 (웹 체인에서 로그인 리다이렉트가 끼어들지 않게 완전 제외)
        httpSecurity.securityMatcher(new NegatedRequestMatcher(AntPathRequestMatcher.antMatcher("/api/**")));
        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/"),
                    AntPathRequestMatcher.antMatcher("/login"),
                    AntPathRequestMatcher.antMatcher("/main"),
                    AntPathRequestMatcher.antMatcher("/index.html"),
                    AntPathRequestMatcher.antMatcher("/login.html"),
                    /* URL 결제 ChillPay 복귀 — 패턴 *.html 과 무관하게 미인증 허용(로그인 리다이렉트 방지) */
                    AntPathRequestMatcher.antMatcher("/pay-result.html"),
                    AntPathRequestMatcher.antMatcher("/pay.html"),
                    AntPathRequestMatcher.antMatcher("/favicon.ico"),
                    AntPathRequestMatcher.antMatcher("/robots.txt"),
                    AntPathRequestMatcher.antMatcher("/pay"),
                    AntPathRequestMatcher.antMatcher("/pay/**"),
                    AntPathRequestMatcher.antMatcher("/chatbot-pay"),
                    AntPathRequestMatcher.antMatcher("/chatbot-pay/**"),
                    AntPathRequestMatcher.antMatcher("/v1/embed-chatbot/**"),
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
