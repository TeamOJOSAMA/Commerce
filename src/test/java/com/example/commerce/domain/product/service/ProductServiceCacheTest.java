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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// 실제 스프링 컨텍스트 + Redis가 필요하다 (로컬 Redis, DB 실행 중이어야 함).
// @Cacheable은 AOP 프록시로 동작하므로 Mockito 단위 테스트로는 캐싱 여부를 검증할 수 없다.
@SpringBootTest
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
        verify(productRepository, times(1)).findAllByOrderByViewCountDesc(pageable);
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
