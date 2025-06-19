package com.handy.appserver.repository;

import com.handy.appserver.entity.snap.SnapPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapPostRepository extends JpaRepository<SnapPost, Long> {
} 