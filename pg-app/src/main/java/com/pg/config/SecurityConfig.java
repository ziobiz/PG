package com.pg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/login"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(auth -> auth
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/login")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/me")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/dev/**")).permitAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/"),
                    AntPathRequestMatcher.antMatcher("/login"),
                    AntPathRequestMatcher.antMatcher("/main"),
                    AntPathRequestMatcher.antMatcher("/index.html"),
                    AntPathRequestMatcher.antMatcher("/login.html"),
                    // 브라우저 기본 favicon 요청이 302 /login 으로 가면 Location 이 http 로 잡혀 Mixed Content 유발 (CF Flexible 등)
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
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/org/branding")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pay/chillpay/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/open/pg-notify/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()
                .anyRequest().authenticated());
        httpSecurity.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                apiAuthenticationEntryPoint,
                AntPathRequestMatcher.antMatcher("/api/**")));
        httpSecurity.formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/main", true)
                .permitAll());
        httpSecurity.logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        httpSecurity.csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/api/**")));
        httpSecurity.addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
