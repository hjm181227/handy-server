package com.handy.appserver.entity.cart;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "cart_items")
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private String shape;

    public CartItem(Product product, Integer quantity, String size, String shape) {
        this.product = product;
        this.quantity = quantity;
        this.size = size;
        this.shape = shape;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }
} 