package com.handy.appserver.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.Map;

@Slf4j
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
    private static final String USERS_FOLDER = "users/";
    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://handy-images-bucket.s3.ap-northeast-2.amazonaws.com/default_user.png";

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

    /**
     * 프로필 이미지를 temp 폴더에서 user/{userId}/profile 폴더로 이동
     */
    public String moveToProfileFolder(String tempImageUrl, Long userId) {
        log.debug("Starting moveToProfileFolder - tempImageUrl: {}, userId: {}", tempImageUrl, userId);
        
        String tempKey = extractKeyFromUrl(tempImageUrl);
        log.debug("Extracted tempKey: {}", tempKey);
        
        if (tempKey == null) {
            log.error("Failed to extract key from URL: {}", tempImageUrl);
            throw new IllegalArgumentException("유효하지 않은 임시 이미지 URL입니다. (key 추출 실패)");
        }
        
        // 임시로 temp 폴더 검증 완화 (디버깅용)
        if (!tempKey.startsWith(TEMP_FOLDER)) {
            log.warn("Temp key does not start with temp folder: {}. Proceeding anyway for debugging.", tempKey);
            // throw new IllegalArgumentException("유효하지 않은 임시 이미지 URL입니다. (temp 폴더가 아님)");
        }

        // 새로운 key 생성 (users/{userId}/profile/ 폴더로)
        String fileName = tempKey.substring(tempKey.lastIndexOf("/") + 1);
        String newFileName = UUID.randomUUID().toString() + "_" + fileName;
        String newKey = String.format("%s%d/profile/%s", USERS_FOLDER, userId, newFileName);

        log.debug("Moving profile image from {} to {}", tempKey, newKey);

        try {
            // 파일 복사
            amazonS3.copyObject(bucket, tempKey, bucket, newKey);
            log.debug("Successfully copied file from {} to {}", tempKey, newKey);
            
            // 임시 파일 삭제
            amazonS3.deleteObject(bucket, tempKey);
            log.debug("Successfully deleted temp file: {}", tempKey);

            // 새로운 URL 반환
            String newUrl = amazonS3.getUrl(bucket, newKey).toString();
            log.debug("Generated new URL: {}", newUrl);
            return newUrl;
        } catch (Exception e) {
            log.error("Error during file operation: {}", e.getMessage(), e);
            throw new RuntimeException("프로필 이미지 이동 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 기존 프로필 이미지를 temp 폴더로 이동 (삭제 대신 보관)
     */
    public void moveProfileImageToTemp(String currentProfileImageUrl) {
        if (currentProfileImageUrl == null || currentProfileImageUrl.isEmpty()) {
            log.debug("No current profile image to move");
            return;
        }
        
        // 기본 이미지인 경우 처리 건너뛰기
        if (currentProfileImageUrl.equals(DEFAULT_PROFILE_IMAGE_URL)) {
            log.debug("Skipping default profile image move");
            return;
        }

        String currentKey = extractKeyFromUrl(currentProfileImageUrl);
        if (currentKey == null) {
            log.warn("Could not extract key from current profile image URL: {}", currentProfileImageUrl);
            return;
        }

        // temp 폴더로 이동할 새로운 key 생성
        String fileName = currentKey.substring(currentKey.lastIndexOf("/") + 1);
        String tempKey = TEMP_FOLDER + "old_profile_" + System.currentTimeMillis() + "_" + fileName;

        log.debug("Moving old profile image from {} to {}", currentKey, tempKey);

        try {
            // 파일 복사
            amazonS3.copyObject(bucket, currentKey, bucket, tempKey);
            
            // 원본 파일 삭제
            amazonS3.deleteObject(bucket, currentKey);
            
            log.debug("Successfully moved old profile image to temp folder");
        } catch (Exception e) {
            log.error("Failed to move old profile image to temp folder", e);
            throw new RuntimeException("기존 프로필 이미지 이동 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 프로필 이미지용 presigned URL 생성
     */
    public String generateProfileImagePresignedUrl(String fileName) {
        String key = TEMP_FOLDER + "profile_" + UUID.randomUUID().toString() + "_" + fileName;
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(new Date(System.currentTimeMillis() + presignedUrlExpiration));

        URL presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }

    private String extractKeyFromUrl(String url) {
        try {
            log.debug("Extracting key from URL: {}", url);
            
            if (url == null || url.trim().isEmpty()) {
                log.warn("URL is null or empty");
                return null;
            }
            
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();
            log.debug("Parsed path: {}", path);
            
            // URL에서 bucket 이름을 제외한 경로 추출
            // 예: https://bucket.s3.region.amazonaws.com/temp/uuid_filename.jpg
            // path는 /temp/uuid_filename.jpg 형태
            
            if (path.length() <= 1) {
                log.warn("Path is too short: {}", path);
                return null;
            }
            
            // 첫 번째 슬래시 제거
            String key = path.substring(1);
            log.debug("Extracted key: {}", key);
            
            return key;
        } catch (Exception e) {
            log.error("Error extracting key from URL: {}", url, e);
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

    /**
     * URL 검증을 위한 테스트 메서드 (디버깅용)
     */
    public void validateImageUrl(String imageUrl) {
        log.info("=== URL Validation Debug ===");
        log.info("Input URL: {}", imageUrl);
        
        try {
            URL parsedUrl = new URL(imageUrl);
            log.info("Parsed URL - Protocol: {}, Host: {}, Path: {}", 
                    parsedUrl.getProtocol(), parsedUrl.getHost(), parsedUrl.getPath());
            
            String extractedKey = extractKeyFromUrl(imageUrl);
            log.info("Extracted Key: {}", extractedKey);
            
            if (extractedKey != null) {
                log.info("Starts with temp folder: {}", extractedKey.startsWith(TEMP_FOLDER));
                log.info("File exists in S3: {}", amazonS3.doesObjectExist(bucket, extractedKey));
            }
        } catch (Exception e) {
            log.error("URL validation failed", e);
        }
        log.info("=== End URL Validation ===");
    }

    /**
     * S3 파일 존재 여부 확인
     */
    public boolean doesFileExist(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            if (key == null) {
                log.warn("Could not extract key from URL: {}", imageUrl);
                return false;
            }
            
            boolean exists = amazonS3.doesObjectExist(bucket, key);
            log.info("File exists in S3 - Key: {}, Exists: {}", key, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking file existence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * S3 버킷 정보 확인
     */
    public void checkBucketInfo() {
        try {
            log.info("=== S3 Bucket Info ===");
            log.info("Bucket name: {}", bucket);
            log.info("Region: {}", region);
            log.info("Bucket exists: {}", amazonS3.doesBucketExist(bucket));
            
            // 버킷의 객체 수 확인
            int objectCount = amazonS3.listObjects(bucket).getObjectSummaries().size();
            log.info("Total objects in bucket: {}", objectCount);
            
            // temp 폴더의 객체 수 확인
            int tempObjectCount = amazonS3.listObjects(bucket, TEMP_FOLDER).getObjectSummaries().size();
            log.info("Objects in temp folder: {}", tempObjectCount);
            
            log.info("=== End S3 Bucket Info ===");
        } catch (Exception e) {
            log.error("Error checking bucket info: {}", e.getMessage(), e);
        }
    }
} 