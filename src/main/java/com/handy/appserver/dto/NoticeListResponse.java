package com.handy.appserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeListResponse {
    private List<NoticeResponse> data;
    private long total;
    private int page;
    private int size;

    public static NoticeListResponse from(Page<NoticeResponse> page) {
        return new NoticeListResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getNumber() + 1, // Spring Data Page는 0부터 시작하므로 1을 더함
            page.getSize()
        );
    }
} 