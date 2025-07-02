package com.handy.appserver.controller;

import com.handy.appserver.dto.ImageUploadResponse;
import com.handy.appserver.dto.ImageMoveRequest;
import com.handy.appserver.dto.ImageMoveResponse;
import com.handy.appserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.handy.appserver.dto.PresignedUrlRequest;
import com.handy.appserver.dto.PresignedUrlResponse;
import com.handy.appserver.service.S3Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(@RequestBody PresignedUrlRequest request) {
        String presignedUrl = s3Service.generatePresignedUrl(request.getFileName());
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl));
    }

    @PostMapping("/profile/presigned-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> generateProfileImagePresignedUrl(@RequestBody PresignedUrlRequest request) {
        log.debug("Generating presigned URL for profile image: {}", request.getFileName());
        String presignedUrl = s3Service.generateProfileImagePresignedUrl(request.getFileName());
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl));
    }

    /**
     * S3 이미지 이동 테스트용 API
     * temp 폴더의 이미지를 목적 폴더로 이동
     */
    @PostMapping("/move")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ImageMoveResponse> moveImage(@RequestBody ImageMoveRequest request) {
        try {
            String newUrl = imageService.moveImageInS3(request.getSourceUrl(), request.getTargetKey());
            return ResponseEntity.ok(new ImageMoveResponse(
                request.getSourceUrl(), 
                newUrl, 
                "이미지가 성공적으로 이동되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ImageMoveResponse(
                request.getSourceUrl(), 
                null, 
                "이미지 이동 실패: " + e.getMessage()
            ));
        }
    }
} 