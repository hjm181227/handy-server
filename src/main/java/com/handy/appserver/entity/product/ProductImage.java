package com.handy.appserver.entity.product;

import com.handy.appserver.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_images")
public class ProductImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = true)
    private String description;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private com.handy.appserver.entity.product.Product product;

    @OrderColumn(name = "image_order")
    @Column(name = "image_order")
    private Integer imageOrder;

    @Builder
    public ProductImage(String imageUrl, String description, Integer imageOrder) {
        this.imageUrl = imageUrl;
        this.description = description;
        this.imageOrder = imageOrder;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateImageOrder(Integer imageOrder) {
        this.imageOrder = imageOrder;
    }
}