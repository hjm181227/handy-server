package com.handy.appserver.entity.comment;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글 내용
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 댓글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 댓글이 달린 스냅 포스트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snap_post_id", nullable = false)
    private SnapPost snapPost;

    // 부모 댓글 (답글인 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 자식 댓글들 (답글들)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    // 댓글 활성화 상태
    @Column(nullable = false)
    private boolean isActive = true;

    // 댓글 깊이 (0: 최상위 댓글, 1: 답글, 2: 답글의 답글...)
    @Column(nullable = false)
    private int depth = 0;

    // 좋아요 수
    @Column(nullable = false)
    private int likeCount = 0;

    // 신고 수
    @Column(nullable = false)
    private int reportCount = 0;
} 