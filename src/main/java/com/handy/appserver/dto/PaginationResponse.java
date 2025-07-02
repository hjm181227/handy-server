package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PaginationResponse<T> {
    private List<T> data;
    private PaginationMeta pagination;

    @Getter
    @AllArgsConstructor
    public static class PaginationMeta {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private boolean isFirst;
        private boolean isLast;
    }

    public static <T> PaginationResponse<T> from(Page<T> page) {
        return new PaginationResponse<>(
            page.getContent(),
            new PaginationMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast()
            )
        );
    }
} 