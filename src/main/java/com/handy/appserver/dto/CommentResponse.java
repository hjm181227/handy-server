package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String userName;
    private String userProfileImage;
    private Long snapPostId;
    private Long parentId;
    private int depth;
    private int likeCount;
    private int reportCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies; // 답글들
} 