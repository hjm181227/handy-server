package com.handy.appserver.service;

import com.handy.appserver.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    @Value("${image.server.url}")
    private String imageServerUrl;

    @Value("${image.server.api-key}")
    private String apiKey;

    @Async
    public CompletableFuture<ImageUploadResponse> uploadImageAsync(MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> uploadImage(file));
    }

    public ImageUploadResponse uploadImage(MultipartFile file) {
        return retryTemplate.execute(context -> {
            try {
                log.info("이미지 업로드 시도 (시도 횟수: {})", context.getRetryCount() + 1);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                headers.set("X-API-Key", apiKey);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                });

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<ImageUploadResponse> response = restTemplate.postForEntity(
                        imageServerUrl + "/upload",
                        requestEntity,
                        ImageUploadResponse.class
                );

                log.info("이미지 업로드 성공: {}", response.getBody().getImageUrl());
                return response.getBody();
            } catch (IOException e) {
                log.error("이미지 업로드 실패 (시도 횟수: {}): {}", context.getRetryCount() + 1, e.getMessage());
                throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
            }
        });
    }

    public List<ImageUploadResponse> uploadImages(List<MultipartFile> files) {
        List<CompletableFuture<ImageUploadResponse>> futures = files.stream()
                .map(this::uploadImageAsync)
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Async
    public CompletableFuture<Void> deleteImageAsync(String imageUrl) {
        return CompletableFuture.runAsync(() -> deleteImage(imageUrl));
    }

    public void deleteImage(String imageUrl) {
        retryTemplate.execute(context -> {
            try {
                log.info("이미지 삭제 시도 (시도 횟수: {})", context.getRetryCount() + 1);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-Key", apiKey);

                HttpEntity<?> requestEntity = new HttpEntity<>(headers);

                restTemplate.delete(
                        imageServerUrl + "/delete?url=" + imageUrl,
                        requestEntity
                );

                log.info("이미지 삭제 성공: {}", imageUrl);
                return null;
            } catch (Exception e) {
                log.error("이미지 삭제 실패 (시도 횟수: {}): {}", context.getRetryCount() + 1, e.getMessage());
                throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다.", e);
            }
        });
    }

    public void deleteImages(List<String> imageUrls) {
        List<CompletableFuture<Void>> futures = imageUrls.stream()
                .map(this::deleteImageAsync)
                .collect(Collectors.toList());

        futures.forEach(CompletableFuture::join);
    }
} 