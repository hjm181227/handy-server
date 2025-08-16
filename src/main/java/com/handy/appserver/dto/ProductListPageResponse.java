package com.handy.appserver.dto;

import com.handy.appserver.entity.product.Product;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductListPageResponse {
    private List<ProductListResponse> data;
    private long total;
    private int page;
    private int size;

    public ProductListPageResponse(List<Product> products, long total, int page, int size) {
        this.data = products.stream()
                .map(ProductListResponse::new)
                .collect(Collectors.toList());
        this.total = total;
        this.page = page;
        this.size = size;
    }
} 