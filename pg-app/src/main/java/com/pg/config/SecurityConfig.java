package com.pg.config;

import com.pg.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenAuthFilter tokenAuthFilter;
    private final ApiAcceptFilter apiAcceptFilter;

    public SecurityConfig(AuthService authService) {
        this.tokenAuthFilter = new TokenAuthFilter(authService);
        this.apiAcceptFilter = new ApiAcceptFilter();
    }

    /** API 로그인 전용 - formLogin 없이 JSON 로그인만 허용 (302 방지) */
    @Bean
    @org.springframework.core.annotation.Order(0)
    public SecurityFilterChain apiAuthFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(AntPathRequestMatcher.antMatcher("/api/auth/login"))
            .addFilterBefore(apiAcceptFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/login")))
            .formLogin(f -> f.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiAcceptFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/"),
                    AntPathRequestMatcher.antMatcher("/login"),
                    AntPathRequestMatcher.antMatcher("/main"),
                    AntPathRequestMatcher.antMatcher("/index.html"),
                    AntPathRequestMatcher.antMatcher("/login.html"),
                    AntPathRequestMatcher.antMatcher("/pay"),
                    AntPathRequestMatcher.antMatcher("/pay/**"),
                    AntPathRequestMatcher.antMatcher("/css/**"),
                    AntPathRequestMatcher.antMatcher("/js/**"),
                    AntPathRequestMatcher.antMatcher("/images/**"),
                    AntPathRequestMatcher.antMatcher("/*.html"),
                    AntPathRequestMatcher.antMatcher("/h2-console/**")
                ).permitAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/auth/me")
                ).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pay/chillpay/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/main", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/api/**")))
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new ApiAuthenticationEntryPoint(),
                    AntPathRequestMatcher.antMatcher("/api/**")
                )
            );
        return http.build();
    }
}
