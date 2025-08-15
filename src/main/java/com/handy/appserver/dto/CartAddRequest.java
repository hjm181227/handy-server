package com.handy.appserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartAddRequest {
    private Long productId;
    private Integer quantity;
}
