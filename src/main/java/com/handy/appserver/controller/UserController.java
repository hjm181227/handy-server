package com.handy.appserver.controller;

import com.handy.appserver.dto.AuthLevelUpdateRequest;
import com.handy.appserver.dto.UserResponse;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.user.UserRole;
import com.handy.appserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getAuthLevel(),
            user.getRole()
        ));
    }

    @PutMapping("/{userId}/auth-level")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> updateUserAuthLevel(
            @PathVariable Long userId,
            @RequestBody AuthLevelUpdateRequest request) {
        User updatedUser = userService.updateAuthLevel(userId, request.getAuthLevel());
        UserRole updatedRole = request.getAuthLevel() == 100 ? UserRole.USER :
                request.getAuthLevel() == 200 ? UserRole.SELLER : UserRole.ADMIN;
        userService.updateRole(userId, updatedRole);

        return ResponseEntity.ok(new UserResponse(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getName(),
                updatedUser.getAuthLevel(),
                updatedUser.getRole()
        ));
    }
}