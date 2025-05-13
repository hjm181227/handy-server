package com.handy.appserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 재시도 간격을 1초로 설정
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // 최대 3번까지 재시도
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                3, // 최대 재시도 횟수
                Collections.singletonMap(Exception.class, true) // 모든 예외에 대해 재시도
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
} 