package com.handy.appserver.controller;

import com.handy.appserver.dto.CartAddRequest;
import com.handy.appserver.dto.CartItemUpdateRequest;
import com.handy.appserver.dto.CartStateResponse;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartStateResponse> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CartAddRequest request) {
        CartStateResponse response = cartService.addToCart(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartStateResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CartStateResponse response = cartService.getCartState(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartStateResponse> updateCartItemQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long itemId,
            @RequestBody CartItemUpdateRequest request) {
        CartStateResponse response = cartService.updateCartItemQuantity(userDetails.getId(), itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartStateResponse> removeCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long itemId) {
        CartStateResponse response = cartService.removeCartItem(userDetails.getId(), itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getId());
        return ResponseEntity.ok("Cart cleared successfully");
    }
}
