package com.fitmap.config;

import com.github.catomat0.oauthhelper.jwt.OahJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.web.AuthenticationEntryPoint;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OahJwtAuthenticationFilter jwtFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = (req, res, ex) -> res.sendError(401, "Unauthorized");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsSource()))
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 인증 불필요
                .requestMatchers(
                    "/api/v1/facilities/**",
                    "/api/v1/auth/oauth2/**",   // 소셜 로그인 리다이렉트
                    "/api/v1/auth/refresh",      // 토큰 갱신
                    "/api/v1/auth/signup",       // 회원가입 완료
                    "/actuator/health",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**"
                ).permitAll()
                // 인증 필요
                .requestMatchers(
                    "/api/v1/auth/logout",
                    "/api/v1/users/**",
                    "/api/v1/analyses/**",
                    "/api/v1/reports/**",
                    "/api/v1/payments/**",
                    "/api/v1/credits/**"
                ).authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        List<String> origins = List.of(allowedOriginsRaw.split(","));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
