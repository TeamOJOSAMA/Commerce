package com.example.commerce.domain.product.service;

import com.example.commerce.domain.product.dto.ProductResponse;
import com.example.commerce.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// @Cacheable은 AOP 프록시로 동작하므로 Mockito 단위 테스트로는 캐싱 여부를 검증할 수 없어 스프링 컨텍스트를 띄운다.
// test2 프로파일은 실제 MySQL(commerce_locktest 스키마)과 Redis(localhost:6379)를 쓴다.
// 캐시가 Redis를 실제로 타는지(직렬화 포함) 검증하므로 MySQL·Redis가 떠 있어야 한다 (docker compose up -d mysql redis).
// 호스트에서 돌릴 때 .env를 셸에 읽었다면 REDIS_HOST=localhost로 덮어야 한다.
@SpringBootTest
@ActiveProfiles("test2")
class ProductServiceCacheTest {

    private static final String CACHE_NAME = "popularProducts";

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void clearCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("같은 페이지를 두 번 조회하면 두 번째부터는 리포지토리를 다시 호출하지 않는다")
    void getPopularProducts_secondCallHitsCache() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProductResponse> first = productService.getPopularProducts(pageable);
        Page<ProductResponse> second = productService.getPopularProducts(pageable);

        assertThat(second.getContent()).isEqualTo(first.getContent());
        // 서비스는 두 번 호출했지만, 리포지토리 실제 조회는 한 번만 일어나야 한다 (=캐시 히트).
        verify(productRepository, times(1)).findPopularProducts(pageable);
    }

    @Test
    @DisplayName("캐시 매니저에 popularProducts 캐시가 실제로 등록된다")
    void popularProductsCache_isRegistered() {
        Pageable pageable = PageRequest.of(0, 10);

        productService.getPopularProducts(pageable);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get("0_10")).isNotNull();
    }
}
