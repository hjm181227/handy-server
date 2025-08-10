package com.handy.appserver.entity.product;

public enum ProductSortType {
    CREATED_AT_DESC("createdAt", "desc"),  // 최신순
    UPDATED_AT_DESC("updatedAt", "desc"),  // 업데이트순
    PRICE_ASC("price", "asc"),             // 가격 낮은순
    PRICE_DESC("price", "desc"),           // 가격 높은순
    RECOMMEND("recommend", "desc");        // 추천순

    private final String field;
    private final String direction;

    ProductSortType(String field, String direction) {
        this.field = field;
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public String getDirection() {
        return direction;
    }
} 