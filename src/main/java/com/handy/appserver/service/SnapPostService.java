package com.handy.appserver.service;

import com.handy.appserver.dto.SnapPostRequest;
import com.handy.appserver.dto.SnapPostResponse;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.snap.SnapImage;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnapPostService {

    private final SnapPostRepository snapPostRepository;
    private final UserRepository userRepository;
    private final LikeService likeService;
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";

    @Transactional
    public SnapPostResponse createSnapPost(SnapPostRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        SnapPost snapPost = new SnapPost();
        snapPost.setTitle(request.getTitle());
        snapPost.setContent(request.getContent());
        snapPost.setUser(user);

        // 이미지 처리
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<SnapImage> images = request.getImages().stream()
                    .map(imageRequest -> {
                        SnapImage image = new SnapImage();
                        image.setSnapPost(snapPost);
                        image.setImageUrl(imageRequest.getImageUrl());
                        return image;
                    })
                    .collect(Collectors.toList());
            snapPost.setImages(images);
        }

        SnapPost savedSnapPost = snapPostRepository.save(snapPost);
        return convertToResponse(savedSnapPost, user, user);
    }

    @Transactional(readOnly = true)
    public SnapPostResponse getSnapPostById(Long snapPostId, User currentUser) {
        SnapPost snapPost = snapPostRepository.findById(snapPostId)
                .orElseThrow(() -> new IllegalArgumentException("스냅 게시글을 찾을 수 없습니다."));
        
        if (!snapPost.isActive()) {
            throw new IllegalArgumentException("비활성화된 스냅 게시글입니다.");
        }
        
        return convertToResponse(snapPost, snapPost.getUser(), currentUser);
    }

    @Transactional(readOnly = true)
    public Page<SnapPostResponse> getAllSnapPosts(Pageable pageable, User currentUser) {
        Page<SnapPost> snapPosts = snapPostRepository.findByIsActiveTrue(pageable);
        return snapPosts.map(snapPost -> convertToResponse(snapPost, snapPost.getUser(), currentUser));
    }

    @Transactional(readOnly = true)
    public Page<SnapPostResponse> getSnapPostsByUserId(Long userId, Pageable pageable, User currentUser) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Page<SnapPost> snapPosts = snapPostRepository.findByUserAndIsActiveTrue(user, pageable);
        return snapPosts.map(snapPost -> convertToResponse(snapPost, user, currentUser));
    }

    private SnapPostResponse convertToResponse(SnapPost snapPost, User user, User currentUser) {
        SnapPostResponse response = new SnapPostResponse();
        response.setId(snapPost.getId());
        response.setTitle(snapPost.getTitle());
        response.setContent(snapPost.getContent());
        response.setUserId(user.getId());
        response.setUserName(user.getName());
        String profileImage = (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())
            ? DEFAULT_PROFILE_IMAGE_URL
            : user.getProfileImageUrl();
        response.setUserProfileImage(profileImage);
        response.setCreatedAt(snapPost.getCreatedAt());
        response.setUpdatedAt(snapPost.getUpdatedAt());
        
        List<String> imageUrls = snapPost.getImages().stream()
                .map(SnapImage::getImageUrl)
                .collect(Collectors.toList());
        response.setImages(imageUrls);
        
        // 좋아요 수 계산
        int likeCount = likeService.getLikeCount(snapPost.getId(), LikeTargetType.SNAP);
        response.setLikeCount(likeCount);
        
        // 좋아요 상태 설정
        if (currentUser != null && currentUser.getId() != null) {
            boolean isLiked = likeService.isLikedByUser(currentUser, snapPost.getId(), LikeTargetType.SNAP);
            response.setLiked(isLiked);
        } else {
            response.setLiked(false);
        }
        
        return response;
    }
} 