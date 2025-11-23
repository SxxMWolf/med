package com.SxxM.med.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("이메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", to, e);
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    public void sendUsernameEmail(String to, String username) {
        String subject = "아이디 찾기 결과";
        String text = String.format("""
                안녕하세요.
                
                요청하신 아이디 찾기 결과입니다.
                
                아이디: %s
                
                감사합니다.
                """, username);
        sendEmail(to, subject, text);
    }
    
    public void sendTemporaryPasswordEmail(String to, String temporaryPassword) {
        String subject = "임시 비밀번호 발급";
        String text = String.format("""
                안녕하세요.
                
                요청하신 임시 비밀번호가 발급되었습니다.
                
                임시 비밀번호: %s
                
                로그인 후 비밀번호를 변경해주시기 바랍니다.
                
                감사합니다.
                """, temporaryPassword);
        sendEmail(to, subject, text);
    }
}

