package com.handy.appserver.dto;

import com.handy.appserver.entity.report.ReportReason;
import com.handy.appserver.entity.report.ReportStatus;
import com.handy.appserver.entity.report.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String content;
    private ReportStatus status;
    private String processMemo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 