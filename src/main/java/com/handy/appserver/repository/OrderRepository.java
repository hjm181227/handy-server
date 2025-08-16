package com.handy.appserver.repository;

import com.handy.appserver.entity.order.Order;
import com.handy.appserver.entity.order.OrderStatus;
import com.handy.appserver.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN Product p ON oi.productId = p.id WHERE p.seller.id = :sellerId ORDER BY o.createdAt DESC")
    Page<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
    
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN Product p ON oi.productId = p.id WHERE p.seller.id = :sellerId ORDER BY o.createdAt DESC")
    List<Order> findOrdersBySellerId(@Param("sellerId") Long sellerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.orderItems oi JOIN Product p ON oi.productId = p.id WHERE p.seller.id = :sellerId")
    Long countOrdersBySellerId(@Param("sellerId") Long sellerId);
}