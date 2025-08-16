package com.handy.appserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreateRequest {
    private String deliveryAddress;
    private String deliveryPhoneNumber;
    private String deliveryName;
}