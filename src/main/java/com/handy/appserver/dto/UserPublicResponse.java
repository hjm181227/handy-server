package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPublicResponse {
    private Long userId;
    private String name;
    private String profileImage;
} 