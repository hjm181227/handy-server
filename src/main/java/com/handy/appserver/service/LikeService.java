package com.handy.appserver.service;

import com.handy.appserver.dto.SnapLikeResponse;
import com.handy.appserver.dto.UserLikeResponse;
import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.like.Like;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.CommentRepository;
import com.handy.appserver.repository.LikeRepository;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final SnapPostRepository snapPostRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public boolean toggleLike(User user, Long targetId, LikeTargetType targetType) {
        log.debug("Toggling like - user: {}, targetId: {}, targetType: {}", 
                user != null ? user.getId() : "null", targetId, targetType);
        
        switch (targetType) {
            case SNAP -> {
                // SnapPost가 존재하는지 확인
                if (!snapPostRepository.existsById(targetId)) {
                    log.warn("SnapPost not found with id: {}", targetId);
                    throw new IllegalArgumentException("존재하지 않는 SnapPost입니다.");
                }
                log.debug("SnapPost exists with id: {}", targetId);
            }
            case USER -> {
                if (!userRepository.existsById(targetId)) {
                    log.warn("User not found with id: {}", targetId);
                    throw new IllegalArgumentException("존재하지 않는 유저입니다.");
                }
                log.debug("User exists with id: {}", targetId);
            }
            case COMMENT -> {
                if (!commentRepository.existsById(targetId)) {
                    log.warn("Comment not found with id: {}", targetId);
                    throw new IllegalArgumentException("존재하지 않는 댓글입니다.");
                }
                log.debug("Comment exists with id: {}", targetId);
            }
        }

        Optional<Like> existing = likeRepository.findByUserAndTargetIdAndTargetType(user, targetId, targetType);
        log.debug("Existing like found: {}", existing.isPresent());
        
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            log.debug("Like deleted for user: {}, targetId: {}, targetType: {}", user.getId(), targetId, targetType);
            
            // 좋아요 수 감소
            updateLikeCount(targetId, targetType, -1);
            
            return false;
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setTargetId(targetId);
            like.setTargetType(targetType);
            Like savedLike = likeRepository.save(like);
            log.debug("Like created with id: {} for user: {}, targetId: {}, targetType: {}", 
                    savedLike.getId(), user.getId(), targetId, targetType);
            
            // 좋아요 수 증가
            updateLikeCount(targetId, targetType, 1);
            
            return true;
        }
    }

    private void updateLikeCount(Long targetId, LikeTargetType targetType, int delta) {
        switch (targetType) {
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId).orElse(null);
                if (comment != null) {
                    comment.setLikeCount(comment.getLikeCount() + delta);
                    commentRepository.save(comment);
                }
            }
            // SNAP과 USER는 현재 엔티티에 likeCount 필드가 없으므로 주석 처리
            // case SNAP -> {
            //     SnapPost snapPost = snapPostRepository.findById(targetId).orElse(null);
            //     if (snapPost != null) {
            //         snapPost.setLikeCount(snapPost.getLikeCount() + delta);
            //         snapPostRepository.save(snapPost);
            //     }
            // }
            // case USER -> {
            //     User user = userRepository.findById(targetId).orElse(null);
            //     if (user != null) {
            //         user.setLikeCount(user.getLikeCount() + delta);
            //         userRepository.save(user);
            //     }
            // }
        }
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(User user, Long targetId, LikeTargetType targetType) {
        if (user == null || user.getId() == null) {
            return false;
        }
        return likeRepository.existsByUserAndTargetIdAndTargetType(user, targetId, targetType);
    }

    @Transactional(readOnly = true)
    public SnapLikeResponse getSnapLikeInfo(Long snapId, User currentUser) {
        // SnapPost 존재 여부 확인
        if (!snapPostRepository.existsById(snapId)) {
            throw new IllegalArgumentException("존재하지 않는 SnapPost입니다.");
        }

        // 좋아요 수 조회
        Long likeCount = likeRepository.countByTargetIdAndTargetType(snapId, LikeTargetType.SNAP);
        
        // 현재 사용자가 좋아요를 눌렀는지 확인
        boolean isLiked = isLikedByUser(currentUser, snapId, LikeTargetType.SNAP);

        return new SnapLikeResponse(snapId, likeCount, isLiked);
    }

    @Transactional(readOnly = true)
    public UserLikeResponse getUserLikeInfo(Long userId, User currentUser) {
        // 대상 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("존재하지 않는 유저입니다.");
        }

        // 좋아요 수 조회
        Long likeCount = likeRepository.countByTargetIdAndTargetType(userId, LikeTargetType.USER);
        
        // 현재 사용자가 좋아요를 눌렀는지 확인
        boolean isLiked = isLikedByUser(currentUser, userId, LikeTargetType.USER);

        return new UserLikeResponse(userId, likeCount, isLiked);
    }

    @Transactional(readOnly = true)
    public int getLikeCount(Long targetId, LikeTargetType targetType) {
        Long count = likeRepository.countByTargetIdAndTargetType(targetId, targetType);
        return count != null ? count.intValue() : 0;
    }

    @Transactional(readOnly = true)
    public Long getUserFollowingCount(Long userId) {
        // 해당 유저가 다른 사용자들을 좋아요한 수 (팔로잉 수)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Long followingCount = likeRepository.countByTargetIdAndTargetType(user.getId(), LikeTargetType.USER);
        log.debug("User {} following count: {}", userId, followingCount);
        return followingCount != null ? followingCount : 0L;
    }
}