package com.handy.appserver.repository;

import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 스냅 포스트의 최상위 댓글들 조회 (답글 제외)
    Page<Comment> findBySnapPostAndParentIsNullAndIsActiveTrueOrderByCreatedAtDesc(SnapPost snapPost, Pageable pageable);

    // 특정 댓글의 답글들 조회
    List<Comment> findByParentAndIsActiveTrueOrderByCreatedAtAsc(Comment parent);

    // 사용자가 작성한 댓글들 조회
    Page<Comment> findByUserAndIsActiveTrueOrderByCreatedAtDesc(User user, Pageable pageable);

    // 스냅 포스트의 전체 댓글 수 조회 (활성화된 것만)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.snapPost = :snapPost AND c.isActive = true")
    Long countBySnapPostAndIsActiveTrue(@Param("snapPost") SnapPost snapPost);

    // 사용자가 작성한 댓글 수 조회
    Long countByUserAndIsActiveTrue(User user);
} 