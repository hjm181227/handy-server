package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String email;
    private String name;
    private String token;
} 