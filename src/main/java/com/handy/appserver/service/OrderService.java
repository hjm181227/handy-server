package com.handy.appserver.service;

import com.handy.appserver.dto.OrderCreateRequest;
import com.handy.appserver.entity.cart.Cart;
import com.handy.appserver.entity.cart.CartItem;
import com.handy.appserver.entity.order.Order;
import com.handy.appserver.entity.order.OrderItem;
import com.handy.appserver.entity.order.OrderStatus;
import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.OrderItemRepository;
import com.handy.appserver.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public Order createOrderFromCart(Long userId, OrderCreateRequest request) {
        User user = userService.findById(userId);
        // Cart 조회 로직 수정 필요 - CartService에서 적절한 메서드 사용
        Cart cart = cartService.getCartByUserId(userId);
        
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어있습니다.");
        }

        BigDecimal totalAmount = calculateCartTotal(cart);
        Order order = new Order(user, totalAmount, 
                              request.getDeliveryAddress(), 
                              request.getDeliveryPhoneNumber(), 
                              request.getDeliveryName());

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            OrderItem orderItem = new OrderItem(
                product, 
                cartItem.getQuantity(), 
                product.getPrice(),
                product.getSize().name(),
                product.getShape().name()
            );
            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(order.calculateTotalAmount());
        Order savedOrder = orderRepository.save(order);

        cartService.clearCart(userId);
        
        log.info("Order created: {} for user: {}", savedOrder.getId(), userId);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> getSellerOrders(Long sellerId, Pageable pageable) {
        return orderRepository.findOrdersBySellerId(sellerId, pageable);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Long sellerId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        
        // OrderItem에서 productId로 Product를 조회해서 판매자 확인
        boolean isSellerOrder = order.getOrderItems().stream()
            .anyMatch(item -> {
                Product product = productService.getProduct(item.getProductId());
                return product.getSeller().getId().equals(sellerId);
            });
        
        if (!isSellerOrder) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다.");
        }

        order.updateStatus(status);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order {} status updated to {} by seller {}", orderId, status, sellerId);
        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("취소할 수 없는 주문 상태입니다.");
        }

        order.updateStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order {} cancelled by user {}", orderId, userId);
        return savedOrder;
    }

    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getCartItems().stream()
            .map(cartItem -> cartItem.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Long getUserOrderCount(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getSellerOrderCount(Long sellerId) {
        return orderRepository.countOrdersBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}