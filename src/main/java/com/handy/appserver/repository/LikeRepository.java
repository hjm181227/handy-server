package com.handy.appserver.repository;

import com.handy.appserver.entity.like.Like;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndTargetIdAndTargetType(User user, Long targetId, LikeTargetType targetType);

    Long countByTargetIdAndTargetType(Long targetId, LikeTargetType targetType);

    boolean existsByUserAndTargetIdAndTargetType(User user, Long targetId, LikeTargetType targetType);
}

