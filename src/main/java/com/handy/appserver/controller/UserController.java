package com.handy.appserver.controller;

import com.handy.appserver.dto.AuthLevelUpdateRequest;
import com.handy.appserver.dto.ProfileImageRequest;
import com.handy.appserver.dto.ProfileImageResponse;
import com.handy.appserver.dto.UserResponse;
import com.handy.appserver.dto.UserPublicResponse;
import com.handy.appserver.dto.UserLikeResponse;
import com.handy.appserver.dto.UserSnapProfileResponse;
import com.handy.appserver.dto.PasswordChangeRequest;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.UserService;
import com.handy.appserver.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LikeService likeService;
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";

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
            user.getRole(),
            user.getProfileImageUrl() != null ? user.getProfileImageUrl() : DEFAULT_PROFILE_IMAGE_URL
        ));
    }

    @GetMapping("/{userId}/public")
    public ResponseEntity<UserPublicResponse> getUserPublicInfo(@PathVariable Long userId) {
        UserPublicResponse publicInfo = userService.getPublicInfo(userId);
        return ResponseEntity.ok(publicInfo);
    }

    @GetMapping("/{userId}/snap-profile")
    public ResponseEntity<UserSnapProfileResponse> getUserSnapProfileInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findById(userDetails.getId());
            }
            
            UserSnapProfileResponse profileInfo = userService.getUserSnapProfileInfo(userId, currentUser);
            return ResponseEntity.ok(profileInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/likes")
    public ResponseEntity<UserLikeResponse> getUserLikes(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findById(userDetails.getId());
            } else {
                // 인증되지 않은 사용자의 경우 기본 사용자 정보로 처리
                currentUser = userService.findById(0L); // 존재하지 않는 사용자 ID
            }
            
            UserLikeResponse likeInfo = likeService.getUserLikeInfo(userId, currentUser);
            return ResponseEntity.ok(likeInfo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{userId}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleUserLike(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            User currentUser = userService.findById(userDetails.getId());
            likeService.toggleLike(currentUser, userId, LikeTargetType.USER);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}/auth-level")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> updateUserAuthLevel(
            @PathVariable Long userId,
            @RequestBody AuthLevelUpdateRequest request) {
        User updatedUser = userService.updateAuthLevel(userId, request.getAuthLevel());

        return ResponseEntity.ok(new UserResponse(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getName(),
                updatedUser.getAuthLevel(),
                updatedUser.getRole(),
                updatedUser.getProfileImageUrl() != null ? updatedUser.getProfileImageUrl() : DEFAULT_PROFILE_IMAGE_URL
        ));
    }

    @PostMapping("/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileImageResponse> updateProfileImage(
            @RequestBody ProfileImageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Profile image update request received for user: {}", userDetails.getId());
        log.debug("Request imageUrl: {}", request.getImageUrl());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            String newProfileImageUrl = userService.updateProfileImage(userDetails.getId(), request.getImageUrl());
            
            ProfileImageResponse response = new ProfileImageResponse();
            response.setUserId(userDetails.getId());
            String profileImage = (newProfileImageUrl == null || newProfileImageUrl.isEmpty())
                ? DEFAULT_PROFILE_IMAGE_URL
                : newProfileImageUrl;
            response.setProfileImageUrl(profileImage);
            response.setMessage("프로필 이미지가 성공적으로 업데이트되었습니다.");
            
            log.debug("Profile image updated successfully for user: {}", userDetails.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Profile image update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during profile image update", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileImageResponse> resetProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Profile image reset request received for user: {}", userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            String resetProfileImageUrl = userService.resetProfileImage(userDetails.getId());
            
            ProfileImageResponse response = new ProfileImageResponse();
            response.setUserId(userDetails.getId());
            response.setProfileImageUrl(resetProfileImageUrl);
            response.setMessage("프로필 이미지가 기본 이미지로 초기화되었습니다.");
            
            log.debug("Profile image reset successfully for user: {}", userDetails.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Profile image reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during profile image reset", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            userService.changePassword(userDetails.getId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}