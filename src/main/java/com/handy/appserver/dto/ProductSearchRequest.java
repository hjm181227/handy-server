package com.handy.appserver.dto;

import com.handy.appserver.entity.product.ProductSortType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductSearchRequest {
    private String keyword;
    private int page = 1;
    private int size = 10;
    private ProductSortType sort = ProductSortType.CREATED_AT_DESC;
} 