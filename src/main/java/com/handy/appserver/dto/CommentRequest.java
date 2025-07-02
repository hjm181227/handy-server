package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {
    private String content;
    private Long parentId; // 답글인 경우 부모 댓글 ID
} 