package com.handy.appserver.service;

import com.handy.appserver.dto.SnapLikeResponse;
import com.handy.appserver.dto.UserLikeResponse;
import com.handy.appserver.dto.SnapPostWithLikeInfoResponse;
import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.like.Like;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.CommentRepository;
import com.handy.appserver.repository.LikeRepository;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 여러 SnapPost의 좋아요 정보를 효율적으로 조회
     * @param snapPosts SnapPost 엔티티 리스트
     * @param currentUser 현재 로그인한 사용자 (null 가능)
     * @return 좋아요 정보가 포함된 SnapPost 응답 리스트
     */
    @Transactional(readOnly = true)
    public List<SnapPostWithLikeInfoResponse> getSnapPostsWithLikeInfo(List<SnapPost> snapPosts, User currentUser) {
        if (snapPosts == null || snapPosts.isEmpty()) {
            return new ArrayList<>();
        }

        // SnapPost ID 리스트 추출
        List<Long> snapIds = snapPosts.stream()
                .map(SnapPost::getId)
                .collect(Collectors.toList());

        log.debug("Getting like info for {} snap posts", snapIds.size());

        // 1. 모든 SnapPost의 좋아요 수를 한 번에 조회
        Map<Long, Long> likeCountMap = getLikeCountMap(snapIds);

        // 2. 현재 사용자가 좋아요한 SnapPost ID 목록을 한 번에 조회
        Set<Long> likedSnapIds = getLikedSnapIds(snapIds, currentUser);

        // 3. SnapPost 엔티티를 응답 DTO로 변환
        return snapPosts.stream()
                .map(snapPost -> convertToSnapPostWithLikeInfo(snapPost, likeCountMap, likedSnapIds))
                .collect(Collectors.toList());
    }

    /**
     * SnapPost ID 리스트에 대한 좋아요 수 맵 생성
     */
    private Map<Long, Long> getLikeCountMap(List<Long> snapIds) {
        List<Object[]> results = likeRepository.countLikesBySnapIds(snapIds, LikeTargetType.SNAP);
        
        Map<Long, Long> likeCountMap = new HashMap<>();
        for (Object[] result : results) {
            Long snapId = (Long) result[0];
            Long count = (Long) result[1];
            likeCountMap.put(snapId, count);
        }
        
        // 좋아요가 없는 SnapPost는 0으로 설정
        for (Long snapId : snapIds) {
            likeCountMap.putIfAbsent(snapId, 0L);
        }
        
        log.debug("Like count map: {}", likeCountMap);
        return likeCountMap;
    }

    /**
     * 현재 사용자가 좋아요한 SnapPost ID 목록 조회
     */
    private Set<Long> getLikedSnapIds(List<Long> snapIds, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return new HashSet<>();
        }
        
        Set<Long> likedSnapIds = likeRepository.findLikedSnapIdsByUser(currentUser, snapIds, LikeTargetType.SNAP);
        log.debug("User {} liked snap IDs: {}", currentUser.getId(), likedSnapIds);
        return likedSnapIds;
    }

    /**
     * SnapPost 엔티티를 좋아요 정보가 포함된 응답 DTO로 변환
     */
    private SnapPostWithLikeInfoResponse convertToSnapPostWithLikeInfo(
            SnapPost snapPost, 
            Map<Long, Long> likeCountMap, 
            Set<Long> likedSnapIds) {
        
        Long snapId = snapPost.getId();
        Long likeCount = likeCountMap.getOrDefault(snapId, 0L);
        boolean isLiked = likedSnapIds.contains(snapId);
        
        // 이미지 URL 리스트 추출
        List<String> imageUrls = snapPost.getImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());
        
        // 프로필 이미지 처리 (기본 이미지 적용)
        String profileImage = snapPost.getUser().getProfileImageUrl();
        if (profileImage == null || profileImage.isEmpty()) {
            profileImage = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";
        }
        
        return new SnapPostWithLikeInfoResponse(
            snapPost.getId(),
            snapPost.getTitle(),
            snapPost.getContent(),
            snapPost.getUser().getId(),
            snapPost.getUser().getName(),
            profileImage,
            imageUrls,
            snapPost.getCreatedAt(),
            snapPost.getUpdatedAt(),
            likeCount,
            isLiked
        );
    }
}