package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserSnapProfileResponse {
    private Long userId;
    private String name;
    private String profileImage;
    private Long followerCount;    // 팔로워 수 (해당 유저를 좋아요한 사용자 수)
    private Long followingCount;   // 팔로잉 수 (해당 유저가 좋아요한 사용자 수)
    private boolean isFollowed;    // 현재 사용자가 해당 유저를 팔로우했는지 여부
} 