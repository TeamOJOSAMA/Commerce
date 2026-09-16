-- 프론트 "할인상품"(구 "타임세일") 화면에 뜨는 모든 ON_EVENT 상품에 균일하게 5% 할인을 건다.
-- 1) 진행 중인 이벤트가 아직 없는 ON_EVENT 상품에는 새 이벤트를 5%로 만들고
-- 2) 이미 이벤트가 있는 상품(할인율이 제각각이었을 수 있음)도 전부 5%로 맞춘다.
-- 몇 번을 다시 실행해도 "ON_EVENT 상품 전체 = 5% 할인"이 되도록 멱등하게 짰다.

INSERT INTO events (product_id, discount_rate, event_price, sold_quantity, total_quantity, start_at, end_at, status, created_at, updated_at)
SELECT
    p.id,
    5,
    ROUND(p.price * 0.95),
    0,
    p.stock,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    'ACTIVE',
    NOW(),
    NOW()
FROM products p
WHERE p.status = 'ON_EVENT'
  AND NOT EXISTS (
      SELECT 1 FROM events e
      WHERE e.product_id = p.id AND e.status = 'ACTIVE'
  );

UPDATE events e
JOIN products p ON p.id = e.product_id
SET e.discount_rate = 5,
    e.event_price = ROUND(p.price * 0.95),
    e.updated_at = NOW()
WHERE e.status = 'ACTIVE'
  AND p.status = 'ON_EVENT';
