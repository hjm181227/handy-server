package com.handy.appserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // Allow all origins for development
                // .allowedOrigins(
                //     "http://localhost:8080",
                //     "http://172.29.9.249:8080",
                //     "http://172.30.1.254:8080",
                //     "https://web.postman.co"
                // )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers")
                .exposedHeaders("Authorization")
                .allowCredentials(false)  // Must be false when using wildcard
                .maxAge(3600);
    }
} 