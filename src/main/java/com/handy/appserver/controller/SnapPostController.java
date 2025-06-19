package com.handy.appserver.controller;

import com.handy.appserver.dto.SnapPostRequest;
import com.handy.appserver.dto.SnapPostResponse;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.SnapPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/snap")
@RequiredArgsConstructor
public class SnapPostController {

    private final SnapPostService snapPostService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SnapPostResponse> createSnapPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SnapPostRequest request) {
        
        SnapPostResponse snapPost = snapPostService.createSnapPost(request, userDetails.getId());
        return ResponseEntity.ok(snapPost);
    }
} 