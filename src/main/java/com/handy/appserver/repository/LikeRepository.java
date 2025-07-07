package com.handy.appserver.repository;

import com.handy.appserver.entity.like.Like;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndTargetIdAndTargetType(User user, Long targetId, LikeTargetType targetType);

    Long countByTargetIdAndTargetType(Long targetId, LikeTargetType targetType);

    boolean existsByUserAndTargetIdAndTargetType(User user, Long targetId, LikeTargetType targetType);

    /**
     * 여러 SnapPost의 좋아요 수를 한 번에 조회
     */
    @Query("SELECT l.targetId, COUNT(l) FROM Like l " +
           "WHERE l.targetId IN :snapIds AND l.targetType = :targetType " +
           "GROUP BY l.targetId")
    List<Object[]> countLikesBySnapIds(@Param("snapIds") List<Long> snapIds, @Param("targetType") LikeTargetType targetType);

    /**
     * 현재 사용자가 좋아요한 SnapPost ID 목록 조회
     */
    @Query("SELECT l.targetId FROM Like l " +
           "WHERE l.user = :user AND l.targetId IN :snapIds AND l.targetType = :targetType")
    Set<Long> findLikedSnapIdsByUser(@Param("user") User user, @Param("snapIds") List<Long> snapIds, @Param("targetType") LikeTargetType targetType);
}

