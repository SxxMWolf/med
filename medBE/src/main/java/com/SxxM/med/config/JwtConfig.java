package com.SxxM.med.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtConfig {
    
    @Value("${jwt.secret:your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}")
    private String secret;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty() || secret.equals("your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm")) {
            System.err.println("⚠️ WARNING: JWT_SECRET이 설정되지 않았거나 기본값입니다!");
            System.err.println("⚠️ JWT 토큰 생성이 실패할 수 있습니다. JWT_SECRET 환경변수를 설정하세요.");
        } else {
            System.out.println("✅ JWT_SECRET이 설정되었습니다 (길이: " + secret.length() + ")");
        }
    }
    
    @Value("${jwt.expiration:86400000}") // 24시간
    private Long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateToken(String username) {
        try {
            if (secret == null || secret.isEmpty() || secret.equals("your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm")) {
                throw new IllegalStateException("JWT_SECRET이 설정되지 않았습니다. JWT_SECRET 환경변수를 설정하세요.");
            }
            
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);
            
            return Jwts.builder()
                    .subject(username)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("JWT 토큰 생성 실패: " + e.getMessage(), e);
        }
    }
    
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("토큰이 만료되었습니다", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new RuntimeException("토큰 서명이 유효하지 않습니다. JWT_SECRET을 확인하세요", e);
        } catch (Exception e) {
            throw new RuntimeException("토큰 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            boolean isValid = (tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token));
            return isValid;
        } catch (Exception e) {
            // 토큰 파싱 실패 시 false 반환
            return false;
        }
    }
}

