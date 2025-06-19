package com.handy.appserver.service;

import com.handy.appserver.dto.SnapPostRequest;
import com.handy.appserver.dto.SnapPostResponse;
import com.handy.appserver.dto.SnapImageResponse;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.snap.SnapImage;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnapPostService {

    private final SnapPostRepository snapPostRepository;
    private final UserRepository userRepository;

    @Transactional
    public SnapPostResponse createSnapPost(SnapPostRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        SnapPost snapPost = new SnapPost();
        snapPost.setTitle(request.getTitle());
        snapPost.setContent(request.getContent());
        snapPost.setUserId(userId);

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
        return convertToResponse(savedSnapPost, user);
    }

    private SnapPostResponse convertToResponse(SnapPost snapPost, User user) {
        SnapPostResponse response = new SnapPostResponse();
        response.setId(snapPost.getId());
        response.setTitle(snapPost.getTitle());
        response.setContent(snapPost.getContent());
        response.setUserId(snapPost.getUserId());
        response.setUserName(user.getName());
        response.setUserProfileImage(user.getProfileImageUrl());
        response.setCreatedAt(snapPost.getCreatedAt());
        response.setUpdatedAt(snapPost.getUpdatedAt());
        
        List<SnapImageResponse> imageResponses = snapPost.getImages().stream()
                .map(image -> {
                    SnapImageResponse imageResponse = new SnapImageResponse();
                    imageResponse.setId(image.getId());
                    imageResponse.setImageUrl(image.getImageUrl());
                    return imageResponse;
                })
                .collect(Collectors.toList());
        response.setImages(imageResponses);
        
        return response;
    }
} 