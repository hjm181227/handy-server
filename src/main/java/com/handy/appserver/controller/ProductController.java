package com.handy.appserver.controller;

import com.handy.appserver.dto.ProductCreateRequest;
import lombok.extern.slf4j.Slf4j;
import com.handy.appserver.dto.ProductUpdateRequest;
import com.handy.appserver.dto.ProductResponse;
import com.handy.appserver.dto.ProductListResponse;
import com.handy.appserver.dto.ProductListPageResponse;
import com.handy.appserver.dto.ProductSearchRequest;
import com.handy.appserver.dto.ProductSearchResponse;
import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.product.ProductShape;
import com.handy.appserver.entity.product.ProductSize;
import com.handy.appserver.entity.product.ProductSortType;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ProductCreateRequest request) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        // 클라이언트 요청 데이터 로깅
        log.info("=== Product Create Request ===");
        log.info("User ID: {}", userDetails.getId());
        log.info("Product Name: {}", request.getName());
        log.info("Description: {}", request.getDescription());
        log.info("Shape: {}", request.getShape());
        log.info("Shape Changeable: {}", request.isShapeChangeable());
        log.info("Size: {}", request.getSize());
        log.info("Size Changeable: {}", request.isSizeChangeable());
        log.info("Price: {}", request.getPrice());
        log.info("Production Days: {}", request.getProductionDays());
        log.info("Custom Available: {}", request.isCustomAvailable());
        log.info("Category IDs: {}", request.getCategoryIds());
        log.info("Main Image URL: {}", request.getMainImageUrl());
        log.info("Detail Images Count: {}", request.getDetailImages() != null ? request.getDetailImages().size() : 0);
        
        if (request.getDetailImages() != null && !request.getDetailImages().isEmpty()) {
            for (int i = 0; i < request.getDetailImages().size(); i++) {
                var detailImage = request.getDetailImages().get(i);
                log.info("Detail Image {}: URL={}, Description={}", 
                    i + 1, detailImage.getImageUrl(), detailImage.getDescription());
            }
        }
        log.info("================================");
        
        Product product = productService.createProduct(
            userDetails.getId(),
            request.getName(),
            request.getDescription(),
            request.getShape(),
            request.isShapeChangeable(),
            request.getSize(),
            request.isSizeChangeable(),
            request.getPrice(),
            request.getProductionDays(),
            request.isCustomAvailable(), // customAvailable 기본값 false
            request.getCategoryIds(),
            request.getMainImageUrl(),
            request.getDetailImages()
        );
        
        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 상품 수정
    @PutMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ProductUpdateRequest request) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        Product product = productService.updateProduct(
                productId,
                userDetails.getId(),
                request.getName(),
                request.getDescription(),
                request.getShape(),
                request.isShapeChangeable(),
                request.getSize(),
                request.isSizeChangeable(),
                request.getPrice(),
                request.getProductionDays(),
                request.isCustomAvailable(),
                request.getCategoryIds(),
                request.getMainImageUrl(),
                request.getDetailImages()
        );

        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 상품 삭제 (비활성화)
    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        productService.deactivateProduct(productId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 판매자의 상품 목록 조회
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ProductListResponse>> getSellerProducts(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ProductSortType sort,
            @RequestParam(required = false) String keyword) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Product> products = productService.getSellerProducts(sellerId, keyword, pageable);
        Page<ProductListResponse> response = products.map(ProductListResponse::new);
        return ResponseEntity.ok(response);
    }

    // 카테고리별 상품 목록 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductListResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ProductSortType sort,
            @RequestParam(required = false) String keyword) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Product> products = productService.getProductsByCategory(categoryId, keyword, pageable);
        Page<ProductListResponse> response = products.map(ProductListResponse::new);
        return ResponseEntity.ok(response);
    }

    // 상품 검색 (새로운 API)
    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(@RequestBody ProductSearchRequest request) {
        ProductSearchResponse response = productService.searchProducts(
            request.getKeyword(), 
            request.getPage(), 
            request.getSize(), 
            request.getSort()
        );
        return ResponseEntity.ok(response);
    }

    // 상품명으로 검색 (기존 API - 하위 호환성 유지)
    @GetMapping("/search")
    public ResponseEntity<Page<ProductListResponse>> searchProductsByName(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ProductSortType sort) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Product> products = productService.searchProductsByName(keyword, pageable);
        Page<ProductListResponse> response = products.map(ProductListResponse::new);
        return ResponseEntity.ok(response);
    }

    // 추천 상품 목록 조회 (getProductList API)
    @GetMapping("/list")
    public ResponseEntity<ProductListPageResponse> getProductList(
            @RequestParam(defaultValue = "10") int listNum,
            @RequestParam(defaultValue = "CREATED_AT_DESC") String sort,
            @RequestParam(required = false) Integer page) {
        
        ProductListPageResponse response = productService.getProductList(listNum, sort, page);
        return ResponseEntity.ok(response);
    }

    // 활성화된 상품만 조회
    @GetMapping("/active")
    public ResponseEntity<Page<ProductListResponse>> getActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ProductSortType sort,
            @RequestParam(required = false) String keyword) {
        
        Pageable pageable = createPageable(page, size, sort);
        Page<Product> products = productService.getActiveProducts(keyword, pageable);
        Page<ProductListResponse> response = products.map(ProductListResponse::new);
        return ResponseEntity.ok(response);
    }

    // 여러 상품 조회
    @PostMapping("/batch")
    public ResponseEntity<List<ProductListResponse>> getProductsByIds(@RequestBody List<Long> productIds) {
        List<Product> products = productService.getProductsByIds(productIds);
        List<ProductListResponse> response = products.stream()
                .map(ProductListResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private Pageable createPageable(int page, int size, ProductSortType sort) {
        if (sort == null) {
            sort = ProductSortType.CREATED_AT_DESC; // 기본값: 최신순
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sort.getDirection()), sort.getField()));
    }
} 