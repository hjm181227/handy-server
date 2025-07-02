package com.handy.appserver.dto;

import com.handy.appserver.entity.report.ReportReason;
import com.handy.appserver.entity.report.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String content; // 신고 내용 (상세 설명)
} 