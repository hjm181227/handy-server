package com.handy.appserver.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.presigned-url.expiration}")
    private long presignedUrlExpiration;

    private static final String TEMP_FOLDER = "temp/";
    private static final String PRODUCTS_FOLDER = "products/";

    public String generatePresignedUrl(String fileName) {
        String key = TEMP_FOLDER + UUID.randomUUID().toString() + "_" + fileName;
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(new Date(System.currentTimeMillis() + presignedUrlExpiration));

        URL presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }

    public String moveToProductsFolder(String tempImageUrl, Long productId, boolean isMainImage) {
        // tempImageUrl에서 key 추출
        String tempKey = extractKeyFromUrl(tempImageUrl);
        if (tempKey == null || !tempKey.startsWith(TEMP_FOLDER)) {
            throw new IllegalArgumentException("유효하지 않은 임시 이미지 URL입니다.");
        }

        // 새로운 key 생성 (products/{productId}/main 또는 details 폴더로)
        String fileName = tempKey.substring(tempKey.lastIndexOf("/") + 1);
        String subFolder = isMainImage ? "main" : "details";
        // UUID만 사용하여 파일명 생성 (순서 정보 제외)
        String newFileName = UUID.randomUUID().toString() + "_" + fileName;
        String newKey = String.format("%s%d/%s/%s", PRODUCTS_FOLDER, productId, subFolder, newFileName);

        // 파일 복사
        amazonS3.copyObject(bucket, tempKey, bucket, newKey);
        
        // 임시 파일 삭제
        amazonS3.deleteObject(bucket, tempKey);

        // 새로운 URL 반환
        return amazonS3.getUrl(bucket, newKey).toString();
    }

    private String extractKeyFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();
            // URL에서 bucket 이름을 제외한 경로 추출
            return path.substring(path.indexOf("/", 1) + 1);
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteProductImages(Long productId) {
        String productPrefix = String.format("%s%d/", PRODUCTS_FOLDER, productId);
        amazonS3.listObjects(bucket, productPrefix).getObjectSummaries()
                .forEach(object -> amazonS3.deleteObject(bucket, object.getKey()));
    }

    public void deleteImage(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        if (key != null) {
            amazonS3.deleteObject(bucket, key);
        }
    }
} 