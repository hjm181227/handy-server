package com.handy.appserver.entity.order;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_items")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 주문 시점 상품 정보 완전 스냅샷 - Product 참조 제거
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "product_main_image_url")
    private String productMainImageUrl;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column
    private String size;

    @Column
    private String shape;

    public OrderItem(Product product, Integer quantity, BigDecimal price) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.productMainImageUrl = product.getMainImageUrl();
        this.quantity = quantity;
        this.price = price;
    }

    public OrderItem(Product product, Integer quantity, BigDecimal price, String size, String shape) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.productMainImageUrl = product.getMainImageUrl();
        this.quantity = quantity;
        this.price = price;
        this.size = size;
        this.shape = shape;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
} 