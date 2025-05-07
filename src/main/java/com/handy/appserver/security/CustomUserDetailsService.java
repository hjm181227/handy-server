package com.handy.appserver.security;

import com.handy.appserver.entity.User;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // 기본 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 판매자 권한
        if (user.isSeller()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
        }
        
        // 관리자 권한
        if (user.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
} 