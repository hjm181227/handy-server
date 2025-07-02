package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileImageResponse {
    private Long userId;
    private String profileImageUrl;
    private String message;
} 