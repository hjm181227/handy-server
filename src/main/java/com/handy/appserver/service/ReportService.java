package com.handy.appserver.service;

import com.handy.appserver.dto.ReportRequest;
import com.handy.appserver.dto.ReportResponse;
import com.handy.appserver.entity.comment.Comment;
import com.handy.appserver.entity.report.Report;
import com.handy.appserver.entity.report.ReportReason;
import com.handy.appserver.entity.report.ReportStatus;
import com.handy.appserver.entity.report.ReportTargetType;
import com.handy.appserver.entity.snap.SnapPost;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.repository.CommentRepository;
import com.handy.appserver.repository.ReportRepository;
import com.handy.appserver.repository.SnapPostRepository;
import com.handy.appserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final SnapPostRepository snapPostRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse createReport(ReportRequest request, Long reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("신고자를 찾을 수 없습니다."));

        // 신고 대상 존재 여부 확인
        validateTarget(request.getTargetType(), request.getTargetId());

        // 이미 신고했는지 확인
        if (reportRepository.existsByReporterAndTargetTypeAndTargetId(reporter, request.getTargetType(), request.getTargetId())) {
            throw new IllegalArgumentException("이미 신고한 대상입니다.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setContent(request.getContent());
        report.setStatus(ReportStatus.PENDING);

        Report savedReport = reportRepository.save(report);
        
        // 신고 대상의 신고 수 증가
        updateTargetReportCount(request.getTargetType(), request.getTargetId());

        return convertToResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByTarget(ReportTargetType targetType, Long targetId, Pageable pageable) {
        Page<Report> reports = reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable);
        return reports.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByReporter(Long reporterId, Pageable pageable) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("신고자를 찾을 수 없습니다."));
        
        Page<Report> reports = reportRepository.findByReporterOrderByCreatedAtDesc(reporter, pageable);
        return reports.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return reports.map(this::convertToResponse);
    }

    @Transactional
    public ReportResponse processReport(Long reportId, ReportStatus status, String processMemo, Long processorId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다."));

        User processor = null;
        if (processorId != null) {
            processor = userRepository.findById(processorId)
                    .orElseThrow(() -> new IllegalArgumentException("처리자를 찾을 수 없습니다."));
        }

        report.setStatus(status);
        report.setProcessor(processor);
        report.setProcessMemo(processMemo);

        Report updatedReport = reportRepository.save(report);
        return convertToResponse(updatedReport);
    }

    private void validateTarget(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case SNAP -> {
                if (!snapPostRepository.existsById(targetId)) {
                    throw new IllegalArgumentException("존재하지 않는 스냅 포스트입니다.");
                }
            }
            case USER -> {
                if (!userRepository.existsById(targetId)) {
                    throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
                }
            }
            case COMMENT -> {
                if (!commentRepository.existsById(targetId)) {
                    throw new IllegalArgumentException("존재하지 않는 댓글입니다.");
                }
            }
        }
    }

    private void updateTargetReportCount(ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case SNAP -> {
                SnapPost snapPost = snapPostRepository.findById(targetId).orElse(null);
                if (snapPost != null) {
                    // SnapPost에 신고 수 필드가 있다면 업데이트
                    // 현재는 SnapPost에 신고 수 필드가 없으므로 주석 처리
                    // snapPost.setReportCount(snapPost.getReportCount() + 1);
                    // snapPostRepository.save(snapPost);
                }
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId).orElse(null);
                if (comment != null) {
                    comment.setReportCount(comment.getReportCount() + 1);
                    commentRepository.save(comment);
                }
            }
            case USER -> {
                // User에 신고 수 필드가 있다면 업데이트
                // 현재는 User에 신고 수 필드가 없으므로 주석 처리
                // User user = userRepository.findById(targetId).orElse(null);
                // if (user != null) {
                //     user.setReportCount(user.getReportCount() + 1);
                //     userRepository.save(user);
                // }
            }
        }
    }

    private ReportResponse convertToResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporter().getId());
        response.setReporterName(report.getReporter().getName());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setReason(report.getReason());
        response.setContent(report.getContent());
        response.setStatus(report.getStatus());
        response.setProcessMemo(report.getProcessMemo());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        return response;
    }
} 