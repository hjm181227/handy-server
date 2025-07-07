package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLikeResponse {
    private Long userId;
    private Long likeCount;
    private boolean isLiked;
} 