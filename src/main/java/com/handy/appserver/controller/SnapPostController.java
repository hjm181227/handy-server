package com.handy.appserver.controller;

import com.handy.appserver.dto.SnapPostRequest;
import com.handy.appserver.dto.SnapPostResponse;
import com.handy.appserver.dto.PaginationResponse;
import com.handy.appserver.dto.SnapLikeResponse;
import com.handy.appserver.dto.SnapPostWithLikeInfoResponse;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.SnapPostService;
import com.handy.appserver.service.LikeService;
import com.handy.appserver.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/snap")
@RequiredArgsConstructor
public class SnapPostController {

    private final SnapPostService snapPostService;
    private final LikeService likeService;
    private final UserService userService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SnapPostResponse> createSnapPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SnapPostRequest request) {
        
        log.debug("Creating snap post for user: {}", userDetails != null ? userDetails.getId() : "null");
        log.debug("Request: title={}, content={}, imagesCount={}", 
                request.getTitle(), request.getContent(), 
                request.getImages() != null ? request.getImages().size() : 0);
        
        if (userDetails == null) {
            log.warn("userDetails is null");
            return ResponseEntity.status(401).build();
        }
        
        try {
            SnapPostResponse snapPost = snapPostService.createSnapPost(request, userDetails.getId());
            log.debug("Snap post created successfully with id: {}", snapPost.getId());
            return ResponseEntity.ok(snapPost);
        } catch (Exception e) {
            log.error("Error creating snap post: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<SnapPostResponse> getSnapPost(
            @PathVariable("id") Long snapPostId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("Getting snap post by id: {}", snapPostId);
        
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findById(userDetails.getId());
            }
            
            SnapPostResponse snapPost = snapPostService.getSnapPostById(snapPostId, currentUser);
            return ResponseEntity.ok(snapPost);
        } catch (IllegalArgumentException e) {
            log.warn("Snap post not found or inactive: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{snapId}/likes")
    public ResponseEntity<SnapLikeResponse> getSnapLikes(
            @PathVariable Long snapId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Getting snap likes for snapId: {}, user: {}", snapId, userDetails != null ? userDetails.getId() : "null");
        
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findById(userDetails.getId());
            } else {
                // 인증되지 않은 사용자의 경우 기본 사용자 정보로 처리
                currentUser = userService.findById(0L); // 존재하지 않는 사용자 ID
            }
            
            SnapLikeResponse likeInfo = likeService.getSnapLikeInfo(snapId, currentUser);
            return ResponseEntity.ok(likeInfo);
        } catch (IllegalArgumentException e) {
            log.warn("Snap like info not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{snapId}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleSnapLike(
            @PathVariable Long snapId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Toggling snap like for snapId: {}, user: {}", snapId, userDetails.getId());
        
        if (userDetails == null) {
            log.warn("userDetails is null in toggleSnapLike");
            return ResponseEntity.status(401).build();
        }
        
        try {
            User currentUser = userService.findById(userDetails.getId());
            log.debug("Found current user: {}", currentUser.getId());
            
            boolean result = likeService.toggleLike(currentUser, snapId, LikeTargetType.SNAP);
            log.debug("Toggle like result: {}", result);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Failed to toggle snap like: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error in toggleSnapLike: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<PaginationResponse<SnapPostResponse>> getSnapList(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Getting snap list - userId: {}, page: {}, size: {}, currentUser: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize(), 
                userDetails != null ? userDetails.getId() : "null");
        
        User currentUser = null;
        if (userDetails != null) {
            currentUser = userService.findById(userDetails.getId());
        }
        
        Page<SnapPostResponse> snapPosts;
        if (userId != null) {
            snapPosts = snapPostService.getSnapPostsByUserId(userId, pageable, currentUser);
        } else {
            snapPosts = snapPostService.getAllSnapPosts(pageable, currentUser);
        }
        
        return ResponseEntity.ok(PaginationResponse.from(snapPosts));
    }

    /**
     * 특정 사용자의 SnapPost 목록을 효율적으로 조회 (좋아요 정보 포함)
     */
    @GetMapping("/user/{userId}/posts")
    public ResponseEntity<List<SnapPostWithLikeInfoResponse>> getUserSnapPostsWithLikeInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Getting user snap posts with like info - userId: {}, currentUser: {}", 
                userId, userDetails != null ? userDetails.getId() : "null");
        
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findById(userDetails.getId());
            }
            
            List<SnapPostWithLikeInfoResponse> snapPosts = snapPostService.getUserSnapPostsWithLikeInfo(userId, currentUser);
            return ResponseEntity.ok(snapPosts);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get user snap posts: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
} 