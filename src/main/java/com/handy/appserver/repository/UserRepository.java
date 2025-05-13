package com.handy.appserver.repository;

import com.handy.appserver.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.products WHERE u.id = :id")
    Optional<User> findByIdWithProducts(@Param("id") Long id);
    
    @Query("SELECT u FROM User u WHERE u.role = 'SELLER'")
    List<User> findAllSellers();
} 