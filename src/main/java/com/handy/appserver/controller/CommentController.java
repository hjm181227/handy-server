package com.handy.appserver.controller;

import com.handy.appserver.dto.CommentRequest;
import com.handy.appserver.dto.CommentResponse;
import com.handy.appserver.dto.PaginationResponse;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.CommentService;
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

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    private final LikeService likeService;

    @PostMapping("/snap/{snapPostId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long snapPostId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Creating comment for snapPostId: {}, user: {}", snapPostId, userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            CommentResponse comment = commentService.createComment(snapPostId, request, userDetails.getId());
            return ResponseEntity.ok(comment);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create comment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/snap/{snapPostId}")
    public ResponseEntity<PaginationResponse<CommentResponse>> getCommentsBySnapPost(
            @PathVariable Long snapPostId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Getting comments for snapPostId: {}, user: {}", snapPostId, 
                userDetails != null ? userDetails.getId() : "null");
        
        User currentUser = null;
        if (userDetails != null) {
            currentUser = userService.findById(userDetails.getId());
        }
        
        try {
            Page<CommentResponse> comments = commentService.getCommentsBySnapPost(snapPostId, pageable, currentUser);
            return ResponseEntity.ok(PaginationResponse.from(comments));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get comments: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Updating comment: {}, user: {}", commentId, userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            CommentResponse comment = commentService.updateComment(commentId, request, userDetails.getId());
            return ResponseEntity.ok(comment);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update comment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Deleting comment: {}, user: {}", commentId, userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            commentService.deleteComment(commentId, userDetails.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete comment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{commentId}/likes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> toggleCommentLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Toggling comment like for commentId: {}, user: {}", commentId, userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            User currentUser = userService.findById(userDetails.getId());
            likeService.toggleLike(currentUser, commentId, LikeTargetType.COMMENT);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to toggle comment like: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 