package com.handy.appserver.entity.report;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // 신고 대상 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    // 신고 대상 ID
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason;

    // 신고 내용 (상세 설명)
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // 신고 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    // 처리자 (관리자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    // 처리 메모
    @Column(name = "process_memo", columnDefinition = "TEXT")
    private String processMemo;
} 