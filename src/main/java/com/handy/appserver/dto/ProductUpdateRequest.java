package com.handy.appserver.dto;

import com.handy.appserver.entity.product.ProductShape;
import com.handy.appserver.entity.product.ProductSize;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductUpdateRequest {
    private String name;
    private String description;
    private ProductShape shape;
    private boolean shapeChangeable;
    private ProductSize size;
    private boolean sizeChangeable;
    private BigDecimal price;
    private Integer productionDays;
    private List<Long> categoryIds;
    private String mainImageUrl;
    private List<DetailImageRequest> detailImages;
    private boolean customAvailable;
} 