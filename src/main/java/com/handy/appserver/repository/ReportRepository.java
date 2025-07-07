package com.handy.appserver.repository;

import com.handy.appserver.entity.report.Report;
import com.handy.appserver.entity.report.ReportStatus;
import com.handy.appserver.entity.report.ReportTargetType;
import com.handy.appserver.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 특정 대상에 대한 신고들 조회
    Page<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ReportTargetType targetType, Long targetId, Pageable pageable);

    // 사용자가 신고한 내역 조회
    Page<Report> findByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);

    // 신고 상태별 조회
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    // 특정 대상에 대한 신고 수 조회
    Long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 사용자가 특정 대상을 이미 신고했는지 확인
    boolean existsByReporterAndTargetTypeAndTargetId(User reporter, ReportTargetType targetType, Long targetId);

    // 특정 대상에 대한 신고 수 조회 (상태별)
    @Query("SELECT COUNT(r) FROM Report r WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.status = :status")
    Long countByTargetTypeAndTargetIdAndStatus(@Param("targetType") ReportTargetType targetType, 
                                              @Param("targetId") Long targetId, 
                                              @Param("status") ReportStatus status);
} 