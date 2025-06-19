package com.handy.appserver.controller;

import com.handy.appserver.dto.ImageUploadResponse;
import com.handy.appserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.handy.appserver.dto.PresignedUrlRequest;
import com.handy.appserver.dto.PresignedUrlResponse;
import com.handy.appserver.service.S3Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = imageService.uploadImage(file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteImage(@RequestParam String imageUrl) {
        imageService.deleteImage(imageUrl);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/presigned-url", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(@RequestBody PresignedUrlRequest request) {
        String presignedUrl = s3Service.generatePresignedUrl(request.getFileName());
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl));
    }
} 