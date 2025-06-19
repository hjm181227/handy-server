package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SnapPostRequest {
    private String title;
    private String content;
    private List<SnapImageRequest> images;
}
