package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
 
@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String presignedUrl;
} 