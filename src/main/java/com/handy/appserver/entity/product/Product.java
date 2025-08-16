package com.handy.appserver.entity.product;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "products")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Column(name = "main_image_url", nullable = false)
    private String mainImageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductImage> detailImages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductShape shape;

    @Column(name = "shape_changeable", nullable = false)
    private boolean shapeChangeable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductSize size;

    @Column(name = "size_changeable", nullable = false)
    private boolean sizeChangeable;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "production_days", nullable = false)
    private Integer productionDays;

    @Column(name = "custom_available", nullable = false)
    private boolean customAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToMany
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public Product(String name, String description, String mainImageUrl, ProductShape shape, boolean shapeChangeable,
                  ProductSize size, boolean sizeChangeable,
                  BigDecimal price, Integer productionDays, boolean customAvailable, User seller) {
        this.name = name;
        this.description = description;
        this.mainImageUrl = mainImageUrl;
        this.shape = shape;
        this.shapeChangeable = shapeChangeable;
        this.size = size;
        this.sizeChangeable = sizeChangeable;
        this.price = price;
        this.productionDays = productionDays;
        this.customAvailable = customAvailable;
        this.seller = seller;
    }

    public void update(String name, String description, String mainImageUrl, ProductShape shape, boolean shapeChangeable,
                      ProductSize size, boolean sizeChangeable,
                      BigDecimal price, Integer productionDays, boolean customAvailable) {
        this.name = name;
        this.description = description;
        this.mainImageUrl = mainImageUrl;
        this.shape = shape;
        this.shapeChangeable = shapeChangeable;
        this.size = size;
        this.sizeChangeable = sizeChangeable;
        this.price = price;
        this.productionDays = productionDays;
        this.customAvailable = customAvailable;
    }

    public void addDetailImage(ProductImage image) {
        if (detailImages.size() >= 5) {
            throw new IllegalStateException("상세 이미지는 최대 5장까지만 추가할 수 있습니다.");
        }
        detailImages.add(image);
        image.setProduct(this);
    }

    public void updateDetailImages(List<ProductImage> images) {
        if (images.size() > 5) {
            throw new IllegalStateException("상세 이미지는 최대 5장까지만 추가할 수 있습니다.");
        }
        this.detailImages.clear();
        for (ProductImage image : images) {
            addDetailImage(image);
        }
    }

    public void removeDetailImage(ProductImage image) {
        detailImages.remove(image);
        image.setProduct(null);
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    public void removeCategory(Category category) {
        categories.remove(category);
    }

    public void deactivate() {
        this.isActive = false;
    }
} 