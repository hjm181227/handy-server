package com.handy.appserver.service;

import com.handy.appserver.dto.CartAddRequest;
import com.handy.appserver.dto.CartItemResponse;
import com.handy.appserver.dto.CartItemUpdateRequest;
import com.handy.appserver.dto.CartStateResponse;
import com.handy.appserver.entity.cart.Cart;
import com.handy.appserver.entity.cart.CartItem;
import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.exception.CartException;
import com.handy.appserver.repository.CartItemRepository;
import com.handy.appserver.repository.CartRepository;
import com.handy.appserver.repository.ProductRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartStateResponse addToCart(Long userId, CartAddRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CartException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CartException("Product not found"));

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart(user);
                    return cartRepository.save(newCart);
                });

        // 같은 상품이 이미 있는지 확인
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(
                cart.getId(), request.getProductId());

        if (existingItem.isPresent()) {
            // 기존 아이템 수량 증가
            CartItem item = existingItem.get();
            item.updateQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // 새 아이템 추가
            CartItem newItem = new CartItem(product, request.getQuantity());
            cart.addCartItem(newItem);
            cartItemRepository.save(newItem);
        }

        return getCartState(userId);
    }

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CartException("User not found"));
                    return new Cart(user);
                });
    }

    public CartStateResponse getCartState(Long userId) {
        Cart cart = getCartByUserId(userId);

        List<CartItem> cartItems = cart.getId() != null ? 
                cartItemRepository.findByCartId(cart.getId()) : 
                List.of();
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartStateResponse.builder()
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }

    @Transactional
    public CartStateResponse updateCartItemQuantity(Long userId, Long itemId, CartItemUpdateRequest request) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartException("Cart item not found"));

        // 사용자의 장바구니 아이템인지 확인
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new CartException("Unauthorized access to cart item");
        }

        cartItem.updateQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return getCartState(userId);
    }

    @Transactional
    public CartStateResponse removeCartItem(Long userId, Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartException("Cart item not found"));

        // 사용자의 장바구니 아이템인지 확인
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new CartException("Unauthorized access to cart item");
        }

        cartItemRepository.delete(cartItem);

        return getCartState(userId);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartException("Cart not found"));

        cartItemRepository.deleteByCartId(cart.getId());
    }

    private CartItemResponse convertToCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productMainImageUrl(product.getMainImageUrl())
                .quantity(cartItem.getQuantity())
                .totalPrice(totalPrice)
                .build();
    }
}
