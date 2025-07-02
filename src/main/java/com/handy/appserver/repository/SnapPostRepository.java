package com.handy.appserver.repository;

import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapPostRepository extends JpaRepository<SnapPost, Long> {
    
    // 활성화된 모든 스냅 포스트 조회 (최신순)
    Page<SnapPost> findByIsActiveTrue(Pageable pageable);
    
    // 특정 사용자의 활성화된 스냅 포스트 조회 (최신순)
    Page<SnapPost> findByUserAndIsActiveTrue(User user, Pageable pageable);
} 