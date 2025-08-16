package com.handy.appserver.dto;

import com.handy.appserver.entity.order.OrderItem;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderItemResponse {
    private final Long id;
    private final Long productId;
    private final String productName;
    private final String productMainImageUrl;
    private final Integer quantity;
    private final BigDecimal price;
    private final BigDecimal totalPrice;
    private final String size;
    private final String shape;

    public OrderItemResponse(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.productId = orderItem.getProductId();
        this.productName = orderItem.getProductName();
        this.productMainImageUrl = orderItem.getProductMainImageUrl();
        this.quantity = orderItem.getQuantity();
        this.price = orderItem.getPrice();
        this.totalPrice = orderItem.getTotalPrice();
        this.size = orderItem.getSize();
        this.shape = orderItem.getShape();
    }
}