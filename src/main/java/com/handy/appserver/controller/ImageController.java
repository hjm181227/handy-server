package com.handy.appserver.controller;

import com.handy.appserver.dto.ImageUploadResponse;
import com.handy.appserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

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
} 