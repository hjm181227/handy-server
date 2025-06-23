package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageMoveRequest {
    private String sourceUrl;
    private String targetKey;
} 