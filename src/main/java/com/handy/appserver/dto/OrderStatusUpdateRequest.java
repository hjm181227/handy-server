package com.handy.appserver.dto;

import com.handy.appserver.entity.order.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusUpdateRequest {
    private OrderStatus status;
}