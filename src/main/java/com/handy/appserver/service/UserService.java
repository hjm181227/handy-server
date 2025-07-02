package com.handy.appserver.service;

import com.handy.appserver.dto.UserPublicResponse;
import com.handy.appserver.dto.UserSnapProfileResponse;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.user.UserRole;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final LikeService likeService;
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";

    @Transactional
    public User signup(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = new User(email, passwordEncoder.encode(password), name, UserRole.USER);
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다: " + email));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public UserPublicResponse getPublicInfo(Long userId) {
        User user = findById(userId);
        String profileImage = (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())
            ? DEFAULT_PROFILE_IMAGE_URL
            : user.getProfileImageUrl();
        return new UserPublicResponse(
            user.getId(),
            user.getName(),
            profileImage
        );
    }

    @Transactional
    public User updateUser(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        user.update(name);
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        user.updatePassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        user.updateRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updateAuthLevel(Long userId, Integer authLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        user.updateAuthLevel(authLevel);
        return userRepository.save(user);
    }

    @Transactional
    public String updateProfileImage(Long userId, String tempImageUrl) {
        log.debug("Updating profile image for user: {}, tempImageUrl: {}", userId, tempImageUrl);
        
        // URL 검증 추가
        if (tempImageUrl == null || tempImageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("이미지 URL이 제공되지 않았습니다.");
        }
        
        // S3Service의 URL 검증 메서드 호출 (디버깅용)
        s3Service.validateImageUrl(tempImageUrl);
        
        // S3 버킷 정보 확인
        s3Service.checkBucketInfo();
        
        // 파일 존재 여부 확인
        boolean fileExists = s3Service.doesFileExist(tempImageUrl);
        if (!fileExists) {
            log.error("File does not exist in S3: {}", tempImageUrl);
            throw new IllegalArgumentException("S3에 해당 파일이 존재하지 않습니다. 파일 업로드를 다시 시도해주세요.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 기존 프로필 이미지를 temp 폴더로 이동 (기본 이미지가 아닌 경우에만)
        String currentProfileImageUrl = user.getProfileImageUrl();
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty() && !currentProfileImageUrl.equals(DEFAULT_PROFILE_IMAGE_URL)) {
            try {
                s3Service.moveProfileImageToTemp(currentProfileImageUrl);
                log.debug("Moved old profile image to temp folder");
            } catch (Exception e) {
                log.warn("Failed to move old profile image to temp folder: {}", e.getMessage());
                // 기존 이미지 이동 실패해도 새 이미지 설정은 계속 진행
            }
        } else {
            log.debug("Skipping old profile image move - it's either null, empty, or default image");
        }

        // 새로운 이미지를 user/{userId}/profile 폴더로 이동
        String newProfileImageUrl = s3Service.moveToProfileFolder(tempImageUrl, userId);
        
        // 사용자 정보 업데이트
        user.setProfileImageUrl(newProfileImageUrl);
        userRepository.save(user);
        
        log.debug("Profile image updated successfully for user: {}", userId);
        return newProfileImageUrl;
    }

    @Transactional
    public String resetProfileImage(Long userId) {
        log.debug("Resetting profile image for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 기존 프로필 이미지를 temp 폴더로 이동 (기본 이미지가 아닌 경우에만)
        String currentProfileImageUrl = user.getProfileImageUrl();
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty() && !currentProfileImageUrl.equals(DEFAULT_PROFILE_IMAGE_URL)) {
            try {
                s3Service.moveProfileImageToTemp(currentProfileImageUrl);
                log.debug("Moved old profile image to temp folder");
            } catch (Exception e) {
                log.warn("Failed to move old profile image to temp folder: {}", e.getMessage());
                // 기존 이미지 이동 실패해도 초기화는 계속 진행
            }
        } else {
            log.debug("Skipping old profile image move - it's either null, empty, or already default image");
        }

        // 기본 이미지로 설정
        user.setProfileImageUrl(DEFAULT_PROFILE_IMAGE_URL);
        userRepository.save(user);
        
        log.debug("Profile image reset successfully for user: {}", userId);
        return DEFAULT_PROFILE_IMAGE_URL;
    }

    @Transactional(readOnly = true)
    public UserSnapProfileResponse getUserSnapProfileInfo(Long userId, User currentUser) {
        log.debug("Getting snap profile info for user: {}, currentUser: {}", userId, currentUser != null ? currentUser.getId() : "null");
        
        // 1. UserPublic 정보 가져오기
        UserPublicResponse publicInfo = getPublicInfo(userId);
        
        // 2. 팔로워 수 (해당 유저를 좋아요한 사용자 수)
        Long followerCount = (long) likeService.getLikeCount(userId, LikeTargetType.USER);
        
        // 3. 팔로잉 수 (해당 유저가 좋아요한 사용자 수)
        Long followingCount = likeService.getUserFollowingCount(userId);
        
        // 4. 현재 사용자의 팔로우 여부
        boolean isFollowed = false;
        if (currentUser != null && currentUser.getId() != null) {
            isFollowed = likeService.isLikedByUser(currentUser, userId, LikeTargetType.USER);
        }
        
        log.debug("Snap profile info - userId: {}, followerCount: {}, followingCount: {}, isFollowed: {}", 
                userId, followerCount, followingCount, isFollowed);
        
        return new UserSnapProfileResponse(
            publicInfo.getUserId(),
            publicInfo.getName(),
            publicInfo.getProfileImage(),
            followerCount,
            followingCount,
            isFollowed
        );
    }
} 