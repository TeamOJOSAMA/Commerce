package com.example.commerce.domain.product.scheduler;

import com.example.commerce.domain.product.repository.ProductRepository;
import com.example.commerce.domain.product.service.ProductViewCountManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final ProductViewCountManager viewCountManager;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelay = 300_000)   // 5분마다
    @Transactional
    public void syncToDb() {
        Set<String> keys = viewCountManager.getAllKeys();
        if (keys == null || keys.isEmpty()) {
            return;
        }

        String prefix = viewCountManager.getKeyPrefix();

        for (String key : keys) {
            Long productId = Long.parseLong(key.substring(prefix.length()));
            long count = viewCountManager.getPending(productId);

            if (count > 0) {
                productRepository.increaseViewCountBy(productId, count);
                viewCountManager.clear(productId);
            }
        }
    }
}