package com.handy.appserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImageMoveResponse {
    private String originalUrl;
    private String newUrl;
    private String message;

    public ImageMoveResponse(String originalUrl, String newUrl, String message) {
        this.originalUrl = originalUrl;
        this.newUrl = newUrl;
        this.message = message;
    }
} 