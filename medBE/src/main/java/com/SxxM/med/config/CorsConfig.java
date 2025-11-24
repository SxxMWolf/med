package com.sxxm.med.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 외부 Origin 허용 (모든 origin 허용)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 개발 환경: localhost:3000 허용 (프론트엔드)
        // 주의: setAllowedOriginPatterns와 setAllowCredentials(true)는 함께 사용할 수 없음
        // configuration.setAllowedOrigins(Arrays.asList(
        //     "http://localhost:3000",
        //     "http://localhost:3001",
        //     "http://127.0.0.1:3000",
        //     "http://127.0.0.1:3001"
        // ));
        // 프로덕션에서는: Arrays.asList("https://yourdomain.com", "https://www.yourdomain.com")
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        // 주의: 와일드카드 origin과 함께 사용할 수 없으므로 false로 설정
        configuration.setAllowCredentials(false);
        
        // Preflight 요청의 캐시 시간 (초 단위)
        configuration.setMaxAge(3600L);
        
        // 응답 헤더에 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Access-Control-Allow-Origin"));
        
        // Health check 경로에 대해서는 CORS 검증을 완전히 건너뛰도록 설정
        // (Origin 헤더가 없는 요청도 허용)
        CorsConfiguration healthConfig = new CorsConfiguration();
        healthConfig.setAllowedOriginPatterns(Arrays.asList("*")); // 모든 origin 허용 (패턴 사용)
        healthConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS", "HEAD", "POST"));
        healthConfig.setAllowedHeaders(Arrays.asList("*"));
        healthConfig.setAllowCredentials(false); // credentials 불필요
        healthConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/health", healthConfig);
        source.registerCorsConfiguration("/actuator/**", healthConfig);
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

