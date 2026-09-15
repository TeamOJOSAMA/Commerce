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
// test 프로파일은 H2 + 인메모리 캐시(spring.cache.type=simple)라 외부 MySQL·Redis 없이 돈다.
// Redis 직렬화까지 확인하려면 로컬 Redis를 띄우고 프로파일 없이 실행한다.
@SpringBootTest
@ActiveProfiles("test")
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
