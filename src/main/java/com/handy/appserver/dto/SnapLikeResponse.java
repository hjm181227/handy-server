package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SnapLikeResponse {
    private Long snapId;
    private Long likeCount;
    private boolean isLiked;
} 