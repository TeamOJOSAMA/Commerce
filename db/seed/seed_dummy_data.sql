-- 갈팡질팡 개발용 더미 데이터: 테스트 계정 + 장바구니/주문/결제 + 쿠폰 + 이벤트(할인)
-- 비밀번호는 전부 test1234 (BCrypt 해시, Spring BCryptPasswordEncoder와 호환)

-- ============ 1. 테스트 계정 ============
INSERT INTO users (created_at, updated_at, email, name, password, role) VALUES
(NOW(), NOW(), 'kim@test.com', '김철수', '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'USER'),
(NOW(), NOW(), 'lee@test.com', '이영희', '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'USER'),
(NOW(), NOW(), 'park@test.com', '박민수', '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'USER'),
(NOW(), NOW(), 'choi@test.com', '최지은', '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'USER'),
(NOW(), NOW(), 'seller@test.com', '정상인', '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'SELLER');

-- ============ 2. 쿠폰 정책 ============
INSERT INTO coupons (created_at, updated_at, name, discount_rate, minimum_order_amount, maximum_discount_amount, total_quantity, issued_quantity, status, issue_starts_at, issue_ends_at) VALUES
(NOW(), NOW(), '신규가입 10% 할인', 10, 10000, 5000, 100, 12, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY)),
(NOW(), NOW(), '여름 시즌 15% 쿠폰', 15, 30000, 10000, 50, 8, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY)),
(NOW(), NOW(), 'VIP 20% 쿠폰', 20, 50000, 20000, 20, 2, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY)),
(NOW(), NOW(), '지난달 마감 쿠폰', 10, 20000, 8000, 30, 30, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

-- ============ 3. 유저 발급 쿠폰 ============
INSERT INTO user_coupons (created_at, updated_at, user_id, coupon_id, status, issued_at, expires_at, used_at) VALUES
(NOW(), NOW(), (SELECT id FROM users WHERE email='kim@test.com'), (SELECT id FROM coupons WHERE name='신규가입 10% 할인'), 'AVAILABLE', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NULL),
(NOW(), NOW(), (SELECT id FROM users WHERE email='lee@test.com'), (SELECT id FROM coupons WHERE name='여름 시즌 15% 쿠폰'), 'AVAILABLE', NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY), NULL),
(NOW(), NOW(), (SELECT id FROM users WHERE email='park@test.com'), (SELECT id FROM coupons WHERE name='VIP 20% 쿠폰'), 'USED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============ 4. 이벤트(할인) ============
-- ON_EVENT 상태로 시딩된 상품들의 실제 Event 행은 apply_event_discount.sql이 전부 만들어준다
-- (이 스크립트 다음 순서로 실행). 여기서는 그걸로 만들 수 없는 "종료된 이벤트" 하나만
-- 히스토리 확인용으로 남겨둔다. 미니 스커트는 seed_products.sql에서 ON_SALE로 시딩되어
-- apply_event_discount.sql 대상이 아니다.
-- event_type, products.current_event_id는 예전 스키마에서 쓰던 컬럼이라 지금 엔티티에는
-- 없다 - 새로 만든 DB에는 그 컬럼 자체가 없으므로 여기서 참조하면 안 된다.
INSERT INTO events (created_at, updated_at, product_id, discount_rate, event_price, sold_quantity, total_quantity, start_at, end_at, status)
VALUES (NOW(), NOW(), (SELECT id FROM products WHERE name='미니 스커트' AND category='CLOTHING'), 40, 30800, 20, 20, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'ENDED');

-- ============ 5. 장바구니 ============
INSERT INTO carts (created_at, updated_at, user_id) VALUES
(NOW(), NOW(), (SELECT id FROM users WHERE email='kim@test.com')),
(NOW(), NOW(), (SELECT id FROM users WHERE email='lee@test.com'));

INSERT INTO cart_items (created_at, updated_at, cart_id, product_id, quantity) VALUES
(NOW(), NOW(), (SELECT id FROM carts WHERE user_id=(SELECT id FROM users WHERE email='kim@test.com')), (SELECT id FROM products WHERE name='데님 팬츠' AND category='CLOTHING'), 2),
(NOW(), NOW(), (SELECT id FROM carts WHERE user_id=(SELECT id FROM users WHERE email='kim@test.com')), (SELECT id FROM products WHERE name='후드 집업' AND category='CLOTHING'), 1),
(NOW(), NOW(), (SELECT id FROM carts WHERE user_id=(SELECT id FROM users WHERE email='lee@test.com')), (SELECT id FROM products WHERE name='플리스 자켓' AND category='CLOTHING'), 1);

-- ============ 6. 주문 + 주문상품 + 결제 ============
-- 6-1. park: 쿠폰 사용해서 결제 완료된 주문 (CONFIRMED / PAID)
INSERT INTO orders (created_at, updated_at, user_id, order_number, status, total_amount, payment_amount, coupon_discount_amount, user_coupon_id, idempotency_key)
VALUES (
    NOW(), NOW(),
    (SELECT id FROM users WHERE email='park@test.com'),
    'ORD-DUMMY-0001', 'CONFIRMED', 108800, 88800, 20000,
    (SELECT id FROM user_coupons WHERE user_id=(SELECT id FROM users WHERE email='park@test.com') AND coupon_id=(SELECT id FROM coupons WHERE name='VIP 20% 쿠폰')),
    'dummy-idem-0001'
);
SET @order1 = LAST_INSERT_ID();

INSERT INTO order_items (created_at, updated_at, order_id, product_id, product_name, unit_price, quantity, event_id, coupon_discount_share)
VALUES (NOW(), NOW(), @order1, (SELECT id FROM products WHERE name='와이드 슬랙스' AND category='CLOTHING'), '와이드 슬랙스', 108800, 1, NULL, 20000);

INSERT INTO payments (created_at, updated_at, order_id, amount, status, paid_at)
VALUES (NOW(), NOW(), @order1, 88800, 'PAID', NOW());

-- 6-2. choi: 아직 결제 대기 중인 주문 (PAYMENT_PENDING / READY) - 주문취소 버튼 테스트용
INSERT INTO orders (created_at, updated_at, user_id, order_number, status, total_amount, payment_amount, coupon_discount_amount, idempotency_key)
VALUES (
    NOW(), NOW(),
    (SELECT id FROM users WHERE email='choi@test.com'),
    'ORD-DUMMY-0002', 'PAYMENT_PENDING', 51500, 51500, 0,
    'dummy-idem-0002'
);
SET @order2 = LAST_INSERT_ID();

INSERT INTO order_items (created_at, updated_at, order_id, product_id, product_name, unit_price, quantity, event_id, coupon_discount_share)
VALUES (NOW(), NOW(), @order2, (SELECT id FROM products WHERE name='니트 가디건' AND category='CLOTHING'), '니트 가디건', 51500, 1, NULL, 0);

INSERT INTO payments (created_at, updated_at, order_id, amount, status)
VALUES (NOW(), NOW(), @order2, 51500, 'READY');
