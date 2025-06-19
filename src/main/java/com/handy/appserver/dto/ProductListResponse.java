package com.handy.appserver.dto;

import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.product.ProductImage;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductListResponse {
    private final Long id;
    private final String name;
    private final String mainImageUrl;
    private final BigDecimal price;
    private final Integer productionDays;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<ProductImageResponse> detailImages;

    public ProductListResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.mainImageUrl = product.getMainImageUrl();
        this.price = product.getPrice();
        this.productionDays = product.getProductionDays();
        this.isActive = product.isActive();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
        this.detailImages = product.getDetailImages().stream()
                .map(ProductImageResponse::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class ProductImageResponse {
        private final Long id;
        private final String imageUrl;
        private final int order;

        public ProductImageResponse(ProductImage productImage) {
            this.id = productImage.getId();
            this.imageUrl = productImage.getImageUrl();
            this.order = productImage.getImageOrder();
        }
    }
} 