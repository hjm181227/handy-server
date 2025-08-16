package com.handy.appserver.dto;

import com.handy.appserver.entity.order.Order;
import com.handy.appserver.entity.order.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderResponse {
    private final Long id;
    private final Long userId;
    private final OrderStatus status;
    private final BigDecimal totalAmount;
    private final String deliveryAddress;
    private final String deliveryPhoneNumber;
    private final String deliveryName;
    private final List<OrderItemResponse> orderItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUser().getId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.deliveryAddress = order.getDeliveryAddress();
        this.deliveryPhoneNumber = order.getDeliveryPhoneNumber();
        this.deliveryName = order.getDeliveryName();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemResponse::new)
                .collect(Collectors.toList());
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }
}