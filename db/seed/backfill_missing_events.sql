-- status='ON_EVENT'인데 실제 events 테이블에 진행 중인 이벤트가 없는 상품을 보정한다.
-- products.event_price / products.discount_rate는 예전 스키마에서 남은 미사용 컬럼이지만,
-- seed_products.sql이 채워둔 의도된 할인 값이 그대로 들어있어 그걸 그대로 재사용한다.
INSERT INTO events (product_id, discount_rate, event_price, sold_quantity, total_quantity, start_at, end_at, status, created_at, updated_at)
SELECT
    p.id,
    p.discount_rate,
    p.event_price,
    0,
    p.stock,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    'ACTIVE',
    NOW(),
    NOW()
FROM products p
WHERE p.status = 'ON_EVENT'
  AND p.event_price IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM events e
      WHERE e.product_id = p.id AND e.status = 'ACTIVE'
  );
