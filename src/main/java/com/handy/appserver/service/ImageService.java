package com.handy.appserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    private final S3Client s3Client;

    /**
     * temp 폴더에 있는 이미지를 목적 폴더로 이동 (copy+delete)
     * @param sourceUrl temp에 업로드된 S3 URL
     * @param targetKey 이동할 S3 key (예: products/main/123/abc.jpg)
     * @return 이동 후 최종 S3 URL
     */
    public String moveImageInS3(String sourceUrl, String targetKey) {
        String sourceKey = extractKeyFromUrl(sourceUrl);
        try {
            // 1. copy
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(targetKey)
                    .build();
            s3Client.copyObject(copyReq);
            // 2. delete
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(sourceKey)
                    .build();
            s3Client.deleteObject(delReq);
            // 3. return new url
            return generateS3Url(targetKey);
        } catch (S3Exception e) {
            log.error("S3 이미지 이동 실패: {} -> {}", sourceKey, targetKey, e);
            throw new RuntimeException("S3 이미지 이동 실패", e);
        }
    }

    private String extractKeyFromUrl(String url) {
        // https://{bucket}.s3.{region}.amazonaws.com/{key} 형식에서 key만 추출
        try {
            URL u = new URL(url);
            String path = u.getPath();
            if (path.startsWith("/")) path = path.substring(1);
            return path;
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 S3 URL: " + url, e);
        }
    }

    private String generateS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }
} 