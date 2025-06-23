package com.handy.appserver.controller;

import com.handy.appserver.dto.SnapPostRequest;
import com.handy.appserver.dto.SnapPostResponse;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.SnapPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/snap")
@RequiredArgsConstructor
public class SnapPostController {

    private final SnapPostService snapPostService;

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
} 