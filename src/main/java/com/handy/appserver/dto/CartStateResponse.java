package com.handy.appserver.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartStateResponse {
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}
