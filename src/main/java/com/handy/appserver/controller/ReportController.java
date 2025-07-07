package com.handy.appserver.controller;

import com.handy.appserver.dto.ReportRequest;
import com.handy.appserver.dto.ReportResponse;
import com.handy.appserver.dto.PaginationResponse;
import com.handy.appserver.entity.report.ReportStatus;
import com.handy.appserver.entity.user.User;
import com.handy.appserver.security.CustomUserDetails;
import com.handy.appserver.service.ReportService;
import com.handy.appserver.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReport(
            @RequestBody ReportRequest request) {
        reportService.createReport(request);
        return ResponseEntity.ok(Map.of("message", "신고가 접수되었습니다."));
    }

    @GetMapping("/target")
    public ResponseEntity<PaginationResponse<ReportResponse>> getReportsByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Getting reports for target - type: {}, id: {}", targetType, targetId);
        
        try {
            com.handy.appserver.entity.report.ReportTargetType type = 
                    com.handy.appserver.entity.report.ReportTargetType.valueOf(targetType.toUpperCase());
            Page<ReportResponse> reports = reportService.getReportsByTarget(type, targetId, pageable);
            return ResponseEntity.ok(PaginationResponse.from(reports));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get reports: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginationResponse<ReportResponse>> getMyReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Getting my reports for user: {}", userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            Page<ReportResponse> reports = reportService.getReportsByReporter(userDetails.getId(), pageable);
            return ResponseEntity.ok(PaginationResponse.from(reports));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get my reports: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaginationResponse<ReportResponse>> getReportsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Getting reports by status: {}", status);
        
        try {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            Page<ReportResponse> reports = reportService.getReportsByStatus(reportStatus, pageable);
            return ResponseEntity.ok(PaginationResponse.from(reports));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get reports by status: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{reportId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> processReport(
            @PathVariable Long reportId,
            @RequestParam String status,
            @RequestParam(required = false) String processMemo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.debug("Processing report: {}, status: {}, processor: {}", reportId, status, userDetails.getId());
        
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            ReportResponse report = reportService.processReport(reportId, reportStatus, processMemo, userDetails.getId());
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to process report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 