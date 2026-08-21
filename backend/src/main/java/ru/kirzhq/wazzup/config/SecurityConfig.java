package ru.kirzhq.wazzup.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookiePath("/");
        // Axios автоматически подставляет содержимое XSRF-TOKEN в заголовок
        // X-XSRF-TOKEN. Spring возвращает в API маскированный токен, поэтому
        // для явно передаваемого значения используем отдельное имя заголовка.
        csrfTokens.setHeaderName("X-CSRF-TOKEN");

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        .ignoringRequestMatchers(
                                "/api/auth/login",
                                "/api/partner/webhook"
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/csrf",
                                "/api/partner/oauth/callback",
                                "/api/partner/webhook",
                                "/api/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/settings/api-key").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(
                                        HttpStatus.UNAUTHORIZED.value(),
                                        "Требуется авторизация"
                                )
                        )
                )
                .build();
    }
}
