package com.handy.appserver.repository;

import com.handy.appserver.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderId(Long orderId);
    
    // OrderItem에서 Product 참조를 제거했으므로 이 메서드는 사용하지 않음
    // 대신 OrderRepository.findOrdersBySellerId() 사용
    // List<OrderItem> findByProductSellerId(@Param("sellerId") Long sellerId);
    
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.productId = :productId")
    Long countByProductId(@Param("productId") Long productId);
}