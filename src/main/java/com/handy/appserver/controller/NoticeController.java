package com.handy.appserver.controller;

import com.handy.appserver.dto.NoticeCreateRequest;
import com.handy.appserver.dto.NoticeListResponse;
import com.handy.appserver.dto.NoticeResponse;
import com.handy.appserver.dto.NoticeUpdateRequest;
import com.handy.appserver.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(@RequestBody NoticeCreateRequest request) {
        NoticeResponse response = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<NoticeListResponse> getAllNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        NoticeListResponse notices = noticeService.getAllNotices(page, size);
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNoticeById(@PathVariable Long noticeId) {
        NoticeResponse notice = noticeService.getNoticeById(noticeId);
        return ResponseEntity.ok(notice);
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateRequest request) {
        NoticeResponse response = noticeService.updateNotice(noticeId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
} 