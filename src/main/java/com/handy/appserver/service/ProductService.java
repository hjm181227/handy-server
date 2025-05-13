package com.handy.appserver.service;

import com.handy.appserver.entity.product.*;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.CategoryRepository;
import com.handy.appserver.repository.ProductRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ImageService imageService;

    @Transactional
    public Product createProduct(UserDetails userDetails, String name, ProductShape shape, boolean shapeChangeable,
                                 ProductSize size, boolean sizeChangeable,
                                 BigDecimal price, Integer productionDays, List<Long> categoryIds,
                                 MultipartFile mainImage, List<MultipartFile> detailImages) {
        // 1. 판매자 확인
        User seller = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        
        if (!seller.isSeller()) {
            throw new IllegalStateException("판매자 권한이 없습니다.");
        }

        // 2. 카테고리 확인
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        try {
            // TODO: 이미지 서버 연동 구현
            String mainImageUrl = "temp_main_image_url"; // 임시 URL
            List<String> detailImageUrls = List.of("temp_detail_image_url_1", "temp_detail_image_url_2"); // 임시 URL

            // 3. 상품 생성
            Product product = new Product(name, mainImageUrl, shape, shapeChangeable,
                    size, sizeChangeable, price, productionDays, seller);

            // 4. 카테고리 추가
            categories.forEach(product::addCategory);

            // 5. 상세 이미지 추가
            for (int i = 0; i < detailImageUrls.size(); i++) {
                ProductImage detailImage = new ProductImage(
                        detailImageUrls.get(i),
                        i
                );
                product.addDetailImage(detailImage);
            }

            return productRepository.save(product);
        } catch (Exception e) {
            log.error("상품 생성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("상품 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public Product updateProduct(Long productId, Long sellerId, String name, ProductShape shape,
                               boolean shapeChangeable, ProductSize size, boolean sizeChangeable,
                               BigDecimal price, Integer productionDays, List<Long> categoryIds,
                               MultipartFile mainImage, List<MultipartFile> detailImages) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new IllegalStateException("상품을 수정할 권한이 없습니다.");
        }

        // 카테고리 확인
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        try {
            // TODO: 이미지 서버 연동 구현
            String mainImageUrl = "temp_main_image_url"; // 임시 URL
            List<String> detailImageUrls = List.of("temp_detail_image_url_1", "temp_detail_image_url_2"); // 임시 URL

            // 1. 기존 상세 이미지 제거
            product.getDetailImages().clear();

            // 2. 상품 정보 업데이트
            product.update(name, mainImageUrl, shape, shapeChangeable, size, sizeChangeable, price, productionDays);
            
            // 3. 카테고리 업데이트
            product.getCategories().clear();
            categories.forEach(product::addCategory);

            // 4. 새로운 상세 이미지 추가
            for (int i = 0; i < detailImageUrls.size(); i++) {
                ProductImage detailImage = new ProductImage(
                        detailImageUrls.get(i),
                        i
                );
                product.addDetailImage(detailImage);
            }

            return product;
        } catch (Exception e) {
            log.error("상품 수정 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("상품 수정 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 삭제 (비활성화)
    @Transactional
    public void deactivateProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new IllegalStateException("상품을 삭제할 권한이 없습니다.");
        }

        product.deactivate();
    }

    // 상품 상세 조회
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }

    // 판매자의 상품 목록 조회
    public Page<Product> getSellerProducts(Long sellerId, Pageable pageable) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        return productRepository.findBySeller(seller, pageable);
    }

    // 카테고리별 상품 목록 조회
    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    // 상품명으로 검색
    public Page<Product> searchProductsByName(String name, Pageable pageable) {
        return productRepository.findByNameContaining(name, pageable);
    }

    // 활성화된 상품만 조회
    public Page<Product> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }

    // 여러 상품 조회
    public List<Product> getProductsByIds(List<Long> productIds) {
        return productRepository.findByIds(productIds);
    }
} 