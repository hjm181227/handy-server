package com.handy.appserver.service;

import com.handy.appserver.dto.NoticeCreateRequest;
import com.handy.appserver.dto.NoticeListResponse;
import com.handy.appserver.dto.NoticeResponse;
import com.handy.appserver.dto.NoticeUpdateRequest;
import com.handy.appserver.entity.notice.Notice;
import com.handy.appserver.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        
        Notice savedNotice = noticeRepository.save(notice);
        return new NoticeResponse(savedNotice);
    }

    public NoticeListResponse getAllNotices(int page, int size) {
        // 페이지는 1부터 시작하므로 0부터 시작하는 Spring Data Page로 변환
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notice> noticePage = noticeRepository.findAll(pageable);
        
        Page<NoticeResponse> responsePage = noticePage.map(NoticeResponse::new);
        return NoticeListResponse.from(responsePage);
    }

    public NoticeResponse getNoticeById(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + noticeId));
        return new NoticeResponse(notice);
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + noticeId));
        
        notice.update(request.getTitle(), request.getContent());
        return new NoticeResponse(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다. ID: " + noticeId));
        
        noticeRepository.delete(notice);
    }
} 