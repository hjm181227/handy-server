package com.handy.appserver.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        System.out.println("Using AWS profile: handy");
        return ProfileCredentialsProvider.create("handy");
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        System.out.println("S3Client built with profile: handy");
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of("ap-northeast-2"))
                .build();
    }

    @Bean
    public AmazonS3 amazonS3() {
        if (accessKey == null || secretKey == null || region == null) {
            throw new IllegalStateException("AWS credentials not found in application.yml or environment variables");
        }

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }
} 