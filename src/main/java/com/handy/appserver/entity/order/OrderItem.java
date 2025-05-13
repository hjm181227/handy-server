package com.handy.appserver.entity.order;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "order_items")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String shape;

    public OrderItem(Product product, Integer quantity, String size, String shape) {
        this.product = product;
        this.quantity = quantity;
        this.size = size;
        this.shape = shape;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
} 