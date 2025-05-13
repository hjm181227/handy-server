package com.handy.appserver.dto;

import com.handy.appserver.entity.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private Integer authLevel;
    private UserRole role;
}