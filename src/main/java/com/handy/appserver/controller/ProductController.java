package com.handy.appserver.controller;

import com.handy.appserver.dto.ProductCreateRequest;
import com.handy.appserver.dto.ProductUpdateRequest;
import com.handy.appserver.dto.ProductResponse;
import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.product.ProductShape;
import com.handy.appserver.entity.product.ProductSize;
import com.handy.appserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute ProductCreateRequest request,
            @RequestParam("mainImage") MultipartFile mainImage,
            @RequestParam(value = "detailImages", required = false) List<MultipartFile> detailImages) {
        
        Product product = productService.createProduct(
            userDetails,
            request.getName(),
            request.getShape(),
            request.isShapeChangeable(),
            request.getSize(),
            request.isSizeChangeable(),
            request.getPrice(),
            request.getProductionDays(),
            request.getCategoryIds(),
            mainImage,
            detailImages
        );
        
        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 상품 수정
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("request") ProductUpdateRequest request,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages) {
        // TODO: userDetails에서 sellerId 추출 로직 구현
        Long sellerId = 1L; // 임시 sellerId

        Product product = productService.updateProduct(
                productId,
                sellerId,
                request.getName(),
                request.getShape(),
                request.isShapeChangeable(),
                request.getSize(),
                request.isSizeChangeable(),
                request.getPrice(),
                request.getProductionDays(),
                request.getCategoryIds(),
                mainImage,
                detailImages
        );

        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 상품 삭제 (비활성화)
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: userDetails에서 sellerId 추출 로직 구현
        Long sellerId = 1L; // 임시 sellerId

        productService.deactivateProduct(productId, sellerId);
        return ResponseEntity.ok().build();
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(new ProductResponse(product));
    }

    // 판매자의 상품 목록 조회
    @GetMapping("/seller")
    public ResponseEntity<Page<Product>> getSellerProducts(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        // TODO: userDetails에서 sellerId 추출 로직 구현
        Long sellerId = 1L; // 임시 sellerId

        Page<Product> products = productService.getSellerProducts(sellerId, pageable);
        return ResponseEntity.ok(products);
    }

    // 카테고리별 상품 목록 조회
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Product>> getProductsByCategory(
            @PathVariable Long categoryId,
            Pageable pageable) {
        Page<Product> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    // 상품명으로 검색
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam String name,
            Pageable pageable) {
        Page<Product> products = productService.searchProductsByName(name, pageable);
        return ResponseEntity.ok(products);
    }

    // 활성화된 상품만 조회
    @GetMapping("/active")
    public ResponseEntity<Page<Product>> getActiveProducts(Pageable pageable) {
        Page<Product> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(products);
    }
    // 여러 상품 조회
    @PostMapping("/batch")
    public ResponseEntity<List<Product>> getProductsByIds(@RequestBody List<Long> productIds) {
        List<Product> products = productService.getProductsByIds(productIds);
        return ResponseEntity.ok(products);
    }
} 