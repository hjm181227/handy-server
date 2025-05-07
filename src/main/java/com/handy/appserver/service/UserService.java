package com.handy.appserver.service;

import com.handy.appserver.entity.User;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다: " + email));
    }

    @Transactional
    public User updateAuthLevel(Long userId, Integer authLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        if (authLevel < 100) {
            throw new IllegalArgumentException("권한 레벨은 100 이상이어야 합니다.");
        }
        
        user.setAuthLevel(authLevel);
        return userRepository.save(user);
    }
} 