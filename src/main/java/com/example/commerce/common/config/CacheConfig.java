package com.example.commerce.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        // 커스텀 ObjectMapper 대신 Spring Boot가 구성해주는 기본 직렬화를 사용한다.
        // (Jackson 3 전환기라 spring-data-redis의 Jackson 직렬화기와 버전이 안 맞음)
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 인기상품 순위는 실시간일 필요 없음
                .disableCachingNullValues();
    }
}