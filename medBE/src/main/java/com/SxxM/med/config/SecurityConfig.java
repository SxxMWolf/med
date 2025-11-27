package com.sxxm.med.config;

import com.sxxm.med.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health/**",
                                "/actuator/**",
                                "/api/auth/register",
                                "/api/auth/login", 
                                "/api/auth/find-username", 
                                "/api/auth/find-password",
                                "/api/analysis/**",  // 분석 API 전체 허용
                                "/api/medications/search",
                                "/api/medications/search/batch",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        )
                        .permitAll()
                        // 게시글 조회(GET)는 공개, 작성/수정/삭제는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**")
                        .permitAll()
                        .requestMatchers("/api/posts/**")
                        .authenticated()
                        // 댓글 조회(GET)는 공개, 작성/수정/삭제는 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/comments/**")
                        .permitAll()
                        .requestMatchers("/api/comments/**")
                        .authenticated()
                        .requestMatchers("/api/users/**")
                        .authenticated() // 사용자 및 알러지 관리 API는 인증 필요
                        .anyRequest()
                        .authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

