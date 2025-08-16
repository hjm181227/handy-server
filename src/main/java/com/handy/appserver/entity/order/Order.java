package com.handy.appserver.entity.order;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "orders")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_phone_number")
    private String deliveryPhoneNumber;

    @Column(name = "delivery_name")
    private String deliveryName;

    public Order(User user, BigDecimal totalAmount) {
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalAmount = totalAmount;
    }

    public Order(User user, BigDecimal totalAmount, String deliveryAddress, String deliveryPhoneNumber, String deliveryName) {
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalAmount = totalAmount;
        this.deliveryAddress = deliveryAddress;
        this.deliveryPhoneNumber = deliveryPhoneNumber;
        this.deliveryName = deliveryName;
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void updateDeliveryInfo(String deliveryAddress, String deliveryPhoneNumber, String deliveryName) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryPhoneNumber = deliveryPhoneNumber;
        this.deliveryName = deliveryName;
    }

    public BigDecimal calculateTotalAmount() {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
} 