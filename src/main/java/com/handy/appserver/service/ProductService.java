package com.handy.appserver.service;

import com.handy.appserver.dto.DetailImageRequest;
import com.handy.appserver.dto.ProductCreateRequest;
import com.handy.appserver.entity.product.*;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.user.UserRole;
import com.handy.appserver.repository.CategoryRepository;
import com.handy.appserver.repository.ProductRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ImageService imageService;
    private final S3Service s3Service;

    @Transactional
    public Product createProduct(
            Long sellerId,
            String name,
            ProductShape shape,
            boolean shapeChangeable,
            ProductSize size,
            boolean sizeChangeable,
            BigDecimal price,
            int productionDays,
            List<Long> categoryIds,
            String mainImageUrl,
            List<DetailImageRequest> detailImages) {
        
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        if (!seller.getRole().equals(UserRole.SELLER)) {
            throw new IllegalArgumentException("판매자만 상품을 등록할 수 있습니다.");
        }

        // 카테고리 검증
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        Product _product = Product.builder()
                .seller(seller)
                .name(name)
                .shape(shape)
                .shapeChangeable(shapeChangeable)
                .size(size)
                .sizeChangeable(sizeChangeable)
                .price(price)
                .productionDays(productionDays)
                .build();

        try {
            // 먼저 상품을 저장하여 ID를 생성
            _product = productRepository.save(_product);
            final Long productId = _product.getId();

            // 메인 이미지를 임시 폴더에서 products 폴더로 이동
            if (mainImageUrl != null) {
                String finalMainImageUrl = s3Service.moveToProductsFolder(mainImageUrl, productId, true);
                _product.setMainImageUrl(finalMainImageUrl);
            }

            // 상세 이미지를 임시 폴더에서 products 폴더로 이동
            if (detailImages != null && !detailImages.isEmpty()) {
                List<ProductImage> productImages = detailImages.stream()
                        .map(detailImage -> {
                            String finalImageUrl = s3Service.moveToProductsFolder(detailImage.getImageUrl(), productId, false);
                            return ProductImage.builder()
                                    .imageUrl(finalImageUrl)
                                    .description(detailImage.getDescription())
                                    .build();
                        })
                        .collect(Collectors.toList());
                
                // 이미지 순서 설정
                for (int i = 0; i < productImages.size(); i++) {
                    productImages.get(i).updateImageOrder(i);
                }
                
                _product.updateDetailImages(productImages);
            }

            return productRepository.save(_product);
        } catch (Exception e) {
            // 실패 시 업로드된 이미지 정리
            if (_product.getId() != null) {
                s3Service.deleteProductImages(_product.getId());
            }
            throw new RuntimeException("상품 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public Product updateProduct(
            Long productId,
            Long sellerId,
            String name,
            ProductShape shape,
            boolean shapeChangeable,
            ProductSize size,
            boolean sizeChangeable,
            BigDecimal price,
            int productionDays,
            List<Long> categoryIds,
            String mainImageUrl,
            List<DetailImageRequest> detailImages) {
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("상품을 수정할 권한이 없습니다.");
        }

        // 카테고리 검증
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 카테고리가 포함되어 있습니다.");
        }

        try {
            // 기존 이미지 URL 저장
            String oldMainImageUrl = product.getMainImageUrl();
            List<String> oldDetailImageUrls = product.getDetailImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());

            // 메인 이미지 업데이트
            if (mainImageUrl != null && !mainImageUrl.equals(oldMainImageUrl)) {
                String finalMainImageUrl = s3Service.moveToProductsFolder(mainImageUrl, productId, true);
                product.setMainImageUrl(finalMainImageUrl);
                // 기존 메인 이미지 삭제
                s3Service.deleteImage(oldMainImageUrl);
            }

            // 상세 이미지 업데이트
            if (detailImages != null) {
                List<ProductImage> newDetailImages = detailImages.stream()
                        .map(detailImage -> {
                            String finalImageUrl = s3Service.moveToProductsFolder(detailImage.getImageUrl(), productId, false);
                            return ProductImage.builder()
                                    .imageUrl(finalImageUrl)
                                    .description(detailImage.getDescription())
                                    .build();
                        })
                        .collect(Collectors.toList());

                // 기존 상세 이미지 삭제
                oldDetailImageUrls.forEach(s3Service::deleteImage);
                
                // 새로운 상세 이미지 설정
                product.updateDetailImages(newDetailImages);
            }

            // 상품 정보 업데이트
            product.update(name, product.getMainImageUrl(), shape, shapeChangeable,
                    size, sizeChangeable, price, productionDays);

            // 카테고리 업데이트
            product.getCategories().clear();
            categories.forEach(product::addCategory);

            return productRepository.save(product);
        } catch (Exception e) {
            // 실패 시 업로드된 이미지 정리
            if (mainImageUrl != null && !mainImageUrl.equals(product.getMainImageUrl())) {
                s3Service.deleteImage(mainImageUrl);
            }
            if (detailImages != null) {
                detailImages.forEach(image -> s3Service.deleteImage(image.getImageUrl()));
            }
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
    public Page<Product> getSellerProducts(Long sellerId, String keyword, Pageable pageable) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.findBySellerAndNameContaining(seller, keyword.trim(), pageable);
        }
        return productRepository.findBySeller(seller, pageable);
    }

    // 카테고리별 상품 목록 조회
    public Page<Product> getProductsByCategory(Long categoryId, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.findByCategoryIdAndNameContaining(categoryId, keyword.trim(), pageable);
        }
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    // 상품명으로 검색
    public Page<Product> searchProductsByName(String keyword, Pageable pageable) {
        return productRepository.findByNameContaining(keyword.trim(), pageable);
    }

    // 활성화된 상품만 조회
    public Page<Product> getActiveProducts(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.findByIsActiveTrueAndNameContaining(keyword.trim(), pageable);
        }
        return productRepository.findByIsActiveTrue(pageable);
    }

    // 여러 상품 조회
    public List<Product> getProductsByIds(List<Long> productIds) {
        return productRepository.findByIds(productIds);
    }
}