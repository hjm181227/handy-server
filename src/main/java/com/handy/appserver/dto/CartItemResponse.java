package com.handy.appserver.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productMainImageUrl;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
}
