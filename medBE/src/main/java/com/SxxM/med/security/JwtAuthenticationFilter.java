package com.sxxm.med.security;

import com.sxxm.med.config.JwtConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtConfig jwtConfig;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // OPTIONS 요청 (CORS preflight)은 필터 건너뛰기
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = getTokenFromRequest(request);
        
        if (token != null) {
            try {
                log.info("JWT 토큰 수신: URI={}, 토큰 길이={}", request.getRequestURI(), token.length());
                String username = jwtConfig.getUsernameFromToken(token);
                log.info("JWT 토큰에서 username 추출: {}", username);
                
                if (jwtConfig.validateToken(token, username)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, new ArrayList<>());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("JWT 인증 성공: username={}, URI={}", username, request.getRequestURI());
                } else {
                    log.warn("JWT 토큰 검증 실패: username={}, URI={}", username, request.getRequestURI());
                }
            } catch (Exception e) {
                log.error("JWT 토큰 처리 중 오류 발생: URI={}, 에러={}", request.getRequestURI(), e.getMessage(), e);
                // 토큰 파싱 실패 시 인증하지 않고 계속 진행 (401/403은 Security가 처리)
            }
        } else {
            log.warn("Authorization 헤더에 토큰이 없습니다: URI={}, 헤더={}", 
                    request.getRequestURI(), request.getHeader("Authorization"));
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/health") ||
               path.startsWith("/actuator/");
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

