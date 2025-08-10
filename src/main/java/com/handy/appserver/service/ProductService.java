package com.handy.appserver.service;

import com.handy.appserver.dto.DetailImageRequest;
import com.handy.appserver.dto.ProductListResponse;
import com.handy.appserver.dto.ProductListPageResponse;
import com.handy.appserver.dto.ProductSearchResponse;
import com.handy.appserver.entity.product.*;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.entity.user.UserRole;
import com.handy.appserver.repository.CategoryRepository;
import com.handy.appserver.repository.ProductRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            String description,
            ProductShape shape,
            boolean shapeChangeable,
            ProductSize size,
            boolean sizeChangeable,
            BigDecimal price,
            int productionDays,
            boolean customAvailable,
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
                .description(description)
                .mainImageUrl(mainImageUrl) // mainImageUrl을 빌더에서 설정
                .shape(shape)
                .shapeChangeable(shapeChangeable)
                .size(size)
                .sizeChangeable(sizeChangeable)
                .price(price)
                .productionDays(productionDays)
                .customAvailable(customAvailable)
                .build();

        try {
            // 먼저 상품을 저장하여 ID를 생성
            _product = productRepository.save(_product);
            final Long productId = _product.getId();

            // 메인 이미지를 임시 폴더에서 products 폴더로 이동하고 업데이트
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
            String description,
            ProductShape shape,
            boolean shapeChangeable,
            ProductSize size,
            boolean sizeChangeable,
            BigDecimal price,
            int productionDays,
            boolean customAvailable,
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
            product.update(name, description, product.getMainImageUrl(), shape, shapeChangeable,
                    size, sizeChangeable, price, productionDays, customAvailable);

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

    // 추천 상품 목록 조회
    public ProductListPageResponse getProductList(int listNum, String sort, Integer page) {
        // 기본값 설정
        if (listNum <= 0) {
            listNum = 10;
        }
        
        // 페이지 설정 (0부터 시작)
        int pageNumber = (page != null && page > 0) ? page - 1 : 0;
        
        // 정렬 타입 파싱
        ProductSortType sortType;
        try {
            sortType = ProductSortType.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            sortType = ProductSortType.CREATED_AT_DESC; // 기본값: 최신순
        }
        
        // 추천순인 경우 임시로 최신순으로 처리 (추후 추천 알고리즘 구현 시 변경)
        if (sortType == ProductSortType.RECOMMEND) {
            sortType = ProductSortType.CREATED_AT_DESC;
        }
        
        Pageable pageable = PageRequest.of(pageNumber, listNum, Sort.by(Sort.Direction.fromString(sortType.getDirection()), sortType.getField()));
        Page<Product> productPage = productRepository.findByIsActiveTrue(pageable);
        
        return new ProductListPageResponse(
            productPage.getContent(),
            productPage.getTotalElements(),
            pageNumber + 1, // 클라이언트는 1부터 시작하는 페이지 번호를 받음
            listNum
        );
    }

    // 상품 검색 (활성화된 상품만)
    public ProductSearchResponse searchProducts(String keyword, int page, int size, ProductSortType sort) {
        // 페이지는 1부터 시작하므로 0부터 시작하는 Spring Data Page로 변환
        Pageable pageable = createPageable(page - 1, size, sort);
        
        Page<Product> productPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            productPage = productRepository.findByIsActiveTrueAndNameContaining(keyword.trim(), pageable);
        } else {
            productPage = productRepository.findByIsActiveTrue(pageable);
        }
        
        Page<ProductListResponse> responsePage = productPage.map(ProductListResponse::new);
        return ProductSearchResponse.from(responsePage);
    }

    private Pageable createPageable(int page, int size, ProductSortType sort) {
        if (sort == null) {
            sort = ProductSortType.CREATED_AT_DESC; // 기본값: 최신순
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sort.getDirection()), sort.getField()));
    }
}