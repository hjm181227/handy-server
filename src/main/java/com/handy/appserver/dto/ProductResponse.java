package com.handy.appserver.dto;

import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.product.ProductImage;
import com.handy.appserver.entity.product.ProductShape;
import com.handy.appserver.entity.product.ProductSize;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String mainImageUrl;
    private final List<ProductImageResponse> detailImages;
    private final ProductShape shape;
    private final boolean shapeChangeable;
    private final ProductSize size;
    private final boolean sizeChangeable;
    private final BigDecimal price;
    private final Integer productionDays;
    private final Long sellerId;
    private final String sellerName;
    private final List<Long> categoryIds;
    private final boolean isActive;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.mainImageUrl = product.getMainImageUrl();
        this.detailImages = product.getDetailImages().stream()
                .map(ProductImageResponse::new)
                .collect(Collectors.toList());
        this.shape = product.getShape();
        this.shapeChangeable = product.isShapeChangeable();
        this.size = product.getSize();
        this.sizeChangeable = product.isSizeChangeable();
        this.price = product.getPrice();
        this.productionDays = product.getProductionDays();
        this.sellerId = product.getSeller().getId();
        this.sellerName = product.getSeller().getName();
        this.categoryIds = product.getCategories().stream()
                .map(category -> category.getId())
                .collect(Collectors.toList());
        this.isActive = product.isActive();
    }
}

@Getter
class ProductImageResponse {
    private final Long id;
    private final String imageUrl;
    private final Integer imageOrder;

    public ProductImageResponse(ProductImage image) {
        this.id = image.getId();
        this.imageUrl = image.getImageUrl();
        this.imageOrder = image.getImageOrder();
    }
} 