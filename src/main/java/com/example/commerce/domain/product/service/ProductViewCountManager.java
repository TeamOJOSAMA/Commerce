package com.example.commerce.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductViewCountManager {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "product:viewcount:";

    public void increase(Long productId) {
        redisTemplate.opsForValue().increment(KEY_PREFIX + productId);
    }

   public long getAndClear(Long productId) {
       String value = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + productId);
       return value != null ? Long.parseLong(value) : 0L;
   }

    public Set<String> getAllKeys() {
        return redisTemplate.keys(KEY_PREFIX + "*");
    }

    public String getKeyPrefix() {
        return KEY_PREFIX;
    }
}