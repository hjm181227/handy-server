package com.handy.appserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImageUploadResponse {
    private String imageUrl;
    private String originalFileName;
    private Long fileSize;

    public ImageUploadResponse(String imageUrl, String originalFileName, Long fileSize) {
        this.imageUrl = imageUrl;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
    }
} 