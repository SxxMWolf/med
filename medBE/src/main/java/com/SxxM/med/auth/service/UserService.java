package com.sxxm.med.auth.service;

import com.sxxm.med.auth.entity.User;
import com.sxxm.med.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public void findUsername(String email) {
        // 보안을 위해 이메일이 존재하든 존재하지 않든 동일한 메시지를 응답
        // 실제로는 이메일이 존재하면 발송하고, 존재하지 않으면 발송하지 않지만
        // 사용자에게는 동일한 응답을 반환
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            emailService.sendUsernameEmail(email, user.getUsername());
        }
        // 이메일이 존재하지 않아도 예외를 던지지 않음
    }
    
    public void changeNickname(String username, String newNickname) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        user.setNickname(newNickname);
        userRepository.save(user);
    }
}

