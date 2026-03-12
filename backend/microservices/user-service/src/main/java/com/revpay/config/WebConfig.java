package com.revpay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS is now handled globally at the API Gateway level.
        // Microservice-level CORS is disabled to avoid header duplicates.
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Idempotency interceptor removed for microservices Phase 2 decomposition
    }
}