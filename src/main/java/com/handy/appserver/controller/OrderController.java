package com.handy.appserver.controller;

import com.handy.appserver.dto.OrderCreateRequest;
import com.handy.appserver.dto.OrderResponse;
import com.handy.appserver.dto.OrderStatusUpdateRequest;
import com.handy.appserver.entity.order.Order;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderCreateRequest request) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Order order = orderService.createOrderFromCart(userDetails.getId(), request);
            return ResponseEntity.ok(new OrderResponse(order));
        } catch (IllegalArgumentException e) {
            log.warn("Order creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Order order = orderService.getOrder(orderId, userDetails.getId());
            return ResponseEntity.ok(new OrderResponse(order));
        } catch (IllegalArgumentException e) {
            log.warn("Order access failed: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getUserOrders(userDetails.getId(), pageable);
        Page<OrderResponse> response = orders.map(OrderResponse::new);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seller-orders")
    @PreAuthorize("isAuthenticated() and (hasAuthority('ROLE_SELLER') or hasAuthority('ROLE_ADMIN'))")
    public ResponseEntity<Page<OrderResponse>> getSellerOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getSellerOrders(userDetails.getId(), pageable);
        Page<OrderResponse> response = orders.map(OrderResponse::new);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("isAuthenticated() and (hasAuthority('ROLE_SELLER') or hasAuthority('ROLE_ADMIN'))")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Order order = orderService.updateOrderStatus(orderId, userDetails.getId(), request.getStatus());
            return ResponseEntity.ok(new OrderResponse(order));
        } catch (IllegalArgumentException e) {
            log.warn("Order status update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Order order = orderService.cancelOrder(orderId, userDetails.getId());
            return ResponseEntity.ok(new OrderResponse(order));
        } catch (IllegalArgumentException e) {
            log.warn("Order cancellation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getOrderStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "buyer") String type) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Long count;
        if ("seller".equals(type)) {
            count = orderService.getSellerOrderCount(userDetails.getId());
        } else {
            count = orderService.getUserOrderCount(userDetails.getId());
        }

        return ResponseEntity.ok(Map.of("count", count));
    }

    // TODO: 결제 처리 기능
    @PostMapping("/{orderId}/payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> processPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        // TODO: 실제 결제 처리 로직 구현 필요
        // - 결제 게이트웨이 연동 (토스페이먼츠, 아임포트 등)
        // - 결제 성공/실패 처리
        // - 주문 상태 업데이트
        
        log.info("Payment processing requested for order {} by user {}", orderId, userDetails.getId());
        
        return ResponseEntity.ok(Map.of(
            "message", "결제 처리 기능은 아직 구현되지 않았습니다.",
            "status", "TODO",
            "orderId", orderId.toString()
        ));
    }
}