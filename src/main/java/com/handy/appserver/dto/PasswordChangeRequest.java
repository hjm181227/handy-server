package com.handy.appserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
} 