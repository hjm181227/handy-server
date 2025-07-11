package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SnapPostResponse {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String userName;
    private String userProfileImage;
    private List<String> images;
    private boolean isLiked;
    private int likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 