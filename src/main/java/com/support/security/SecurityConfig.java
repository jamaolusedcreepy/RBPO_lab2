package com.support.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Регистрация не требует CSRF-токена (первый запрос)
                .ignoringRequestMatchers("/api/auth/register")
            )
            .authorizeHttpRequests(auth -> auth

                // --- Публичные ---
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                // --- SLA: просмотр — любой авторизованный, управление — только ADMIN ---
                .requestMatchers(HttpMethod.GET, "/api/slas/**").authenticated()
                .requestMatchers("/api/slas/**").hasRole("ADMIN")

                // --- Категории: просмотр — любой авторизованный, управление — только ADMIN ---
                .requestMatchers(HttpMethod.GET, "/api/categories/**").authenticated()
                .requestMatchers("/api/categories/**").hasRole("ADMIN")

                // --- Пользователи (domain): просмотр — ADMIN/AGENT, управление — ADMIN ---
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // --- Агенты: просмотр — ADMIN/AGENT, управление — ADMIN ---
                .requestMatchers(HttpMethod.GET, "/api/agents/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/api/agents/**").hasRole("ADMIN")

                // --- Тикеты ---
                // Создать тикет — USER или ADMIN
                .requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("ADMIN", "USER")
                // Просматривать — все авторизованные
                .requestMatchers(HttpMethod.GET, "/api/tickets/**").authenticated()
                // Изменять — AGENT или ADMIN
                .requestMatchers(HttpMethod.PUT, "/api/tickets/**").hasAnyRole("ADMIN", "AGENT")
                // Удалять — только ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")

                // --- Отчёты и бизнес-операции — AGENT или ADMIN ---
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "AGENT")

                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Готовая почва для будущей JWT-аутентификации
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
