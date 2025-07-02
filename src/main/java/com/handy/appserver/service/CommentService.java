package com.handy.appserver.service;

import com.handy.appserver.dto.CommentRequest;
import com.handy.appserver.dto.CommentResponse;
import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.like.LikeTargetType;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.CommentRepository;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SnapPostRepository snapPostRepository;
    private final UserRepository userRepository;
    private final LikeService likeService;
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";

    @Transactional
    public CommentResponse createComment(Long snapPostId, CommentRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        SnapPost snapPost = snapPostRepository.findById(snapPostId)
                .orElseThrow(() -> new IllegalArgumentException("스냅 포스트를 찾을 수 없습니다."));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUser(user);
        comment.setSnapPost(snapPost);

        // 답글인 경우 부모 댓글 설정
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
            
            // 부모 댓글이 같은 스냅 포스트에 속하는지 확인
            if (!parent.getSnapPost().getId().equals(snapPostId)) {
                throw new IllegalArgumentException("부모 댓글이 해당 스냅 포스트에 속하지 않습니다.");
            }
            
            comment.setParent(parent);
            comment.setDepth(parent.getDepth() + 1);
        }

        Comment savedComment = commentRepository.save(comment);
        return convertToResponse(savedComment, user);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsBySnapPost(Long snapPostId, Pageable pageable, User currentUser) {
        SnapPost snapPost = snapPostRepository.findById(snapPostId)
                .orElseThrow(() -> new IllegalArgumentException("스냅 포스트를 찾을 수 없습니다."));

        Page<Comment> comments = commentRepository.findBySnapPostAndParentIsNullAndIsActiveTrueOrderByCreatedAtDesc(snapPost, pageable);
        return comments.map(comment -> convertToResponseWithReplies(comment, currentUser));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 댓글 작성자만 수정 가능
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글을 수정할 권한이 없습니다.");
        }

        comment.setContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);
        return convertToResponse(updatedComment, comment.getUser());
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        // 댓글 작성자만 삭제 가능
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글을 삭제할 권한이 없습니다.");
        }

        comment.setActive(false);
        commentRepository.save(comment);
    }

    private CommentResponse convertToResponse(Comment comment, User user) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUserId(user.getId());
        response.setUserName(user.getName());
        String profileImage = (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty())
            ? DEFAULT_PROFILE_IMAGE_URL
            : user.getProfileImageUrl();
        response.setUserProfileImage(profileImage);
        response.setSnapPostId(comment.getSnapPost().getId());
        response.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        response.setDepth(comment.getDepth());
        response.setLikeCount(comment.getLikeCount());
        response.setReportCount(comment.getReportCount());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }

    private CommentResponse convertToResponseWithReplies(Comment comment, User currentUser) {
        CommentResponse response = convertToResponse(comment, comment.getUser());
        
        // 좋아요 상태 설정
        if (currentUser != null && currentUser.getId() != null) {
            boolean isLiked = likeService.isLikedByUser(currentUser, comment.getId(), LikeTargetType.COMMENT);
            response.setLiked(isLiked);
        } else {
            response.setLiked(false);
        }

        // 답글들 조회 및 변환
        List<Comment> replies = commentRepository.findByParentAndIsActiveTrueOrderByCreatedAtAsc(comment);
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> {
                    CommentResponse replyResponse = convertToResponse(reply, reply.getUser());
                    if (currentUser != null && currentUser.getId() != null) {
                        boolean isLiked = likeService.isLikedByUser(currentUser, reply.getId(), LikeTargetType.COMMENT);
                        replyResponse.setLiked(isLiked);
                    } else {
                        replyResponse.setLiked(false);
                    }
                    return replyResponse;
                })
                .collect(Collectors.toList());
        response.setReplies(replyResponses);

        return response;
    }
} 