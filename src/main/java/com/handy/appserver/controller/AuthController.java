package com.handy.appserver.controller;

import com.handy.appserver.dto.LoginRequest;
import com.handy.appserver.dto.LoginResponse;
import com.handy.appserver.entity.User;
import com.handy.appserver.service.JwtService;
import com.handy.appserver.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        User user = userService.signup(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.ok(new SignupResponse(user.getId(), user.getEmail(), user.getName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userService.findByEmail(request.getEmail());
            String token = jwtService.generateToken(user.getEmail());
            
            return ResponseEntity.ok(new LoginResponse(user.getId(), user.getEmail(), user.getName(), token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("로그인 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

@Getter
@Setter
class SignupRequest {
    private String email;
    private String password;
    private String name;
}

@Getter
@AllArgsConstructor
class SignupResponse {
    private Long id;
    private String email;
    private String name;
}

@Getter
@AllArgsConstructor
class ErrorResponse {
    private String message;
}
