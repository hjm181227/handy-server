package com.handy.appserver.dto;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.report.ReportReason;
import com.handy.appserver.entity.report.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest extends BaseTimeEntity {
    private Long targetId;
    private ReportTargetType targetType; // SNAP, USER, COMMENT
    private ReportReason reason;
    private String content; // 선택적 상세 설명
} 