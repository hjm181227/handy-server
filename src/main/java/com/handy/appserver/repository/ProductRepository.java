package com.handy.appserver.repository;

import com.handy.appserver.entity.product.Product;
import com.handy.appserver.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 판매자의 상품 목록 조회
    Page<Product> findBySeller(User seller, Pageable pageable);
    
    // 판매자와 상품명으로 상품 검색
    Page<Product> findBySellerAndNameContaining(User seller, String keyword, Pageable pageable);
    
    // 카테고리별 상품 목록 조회
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
    
    // 카테고리와 상품명으로 상품 검색
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.name LIKE %:keyword%")
    Page<Product> findByCategoryIdAndNameContaining(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);
    
    // 상품명으로 검색
    Page<Product> findByNameContaining(String keyword, Pageable pageable);
    
    // 판매자와 카테고리로 상품 검색
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE p.seller = :seller AND c.id = :categoryId")
    Page<Product> findBySellerAndCategoryId(@Param("seller") User seller, @Param("categoryId") Long categoryId, Pageable pageable);
    
    // 활성화된 상품만 조회
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    // 활성화된 상품명으로 상품 검색
    Page<Product> findByIsActiveTrueAndNameContaining(String keyword, Pageable pageable);
    
    // 판매자의 활성화된 상품만 조회
    Page<Product> findBySellerAndIsActiveTrue(User seller, Pageable pageable);
    
    // 상품 ID 목록으로 상품 조회
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findByIds(@Param("ids") List<Long> ids);
} 