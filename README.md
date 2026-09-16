# Commerce (갈팡질팡) — 백엔드

Spring Boot 4.1.1 / Java 17 기반 커머스 API 서버. 프론트엔드는 별도 저장소([commerce-frontend](https://github.com/TeamOJOSAMA/commerce-frontend))에 있다.

## 프로젝트 소개
상품 조회부터 장바구니, 주문/결제, 쿠폰·이벤트 할인, 전액/부분 환불, 실시간 채팅 상담까지 커머스 서비스의 한 사이클을 구현한 프로젝트다.

- **상품** — 카테고리별 조회, QueryDSL 기반 검색
- **장바구니 / 주문 / 결제** — 같은 상품 중복 담기 시 수량 합산, 쿠폰·이벤트 할인 적용
- **쿠폰 / 이벤트(할인)** — 발급 수량 제한이 있는 쿠폰, 기간 한정 할인 이벤트
- **환불** — 전액/부분 환불, 한 결제에 여러 번 부분 환불 가능(누적 이력 관리)
- **채팅 상담** — 실시간 문의, 상담사 연결, 일정 시간 미응답 시 자동 종료

## 기술 스택
- **Backend**: Java 17, Spring Boot 4.1.1, Spring Web MVC
- **Data**: Spring Data JPA, QueryDSL, MySQL, Redis(캐시)
- **인증**: Spring Security, JWT(jjwt)
- **실시간**: WebSocket(STOMP) + SockJS (채팅)
- **동시성 제어**: 비관적 락(`@Lock(PESSIMISTIC_WRITE)`) — 재고·쿠폰 등 동시 요청 처리
- **API 문서**: springdoc-openapi
- **Frontend**: React 19, Vite, Zustand, Tailwind CSS, Axios

## 도메인 구성
9개 도메인으로 나뉘어 있고, 도메인마다 엔티티·서비스·레포지토리를 독립적으로 두되 서로는 ID(FK)로만 참조한다.

| 도메인 | 핵심 엔티티 | 책임 |
|---|---|---|
| `user` | `User` | 회원 정보, 권한(`UserRole`: USER/SELLER/ADMIN) |
| `auth` | `AuthUser` | 로그인, JWT 발급 — `User`를 감싼 인증 주체 |
| `product` | `Product` | 상품 정보, 카테고리·상태(`ProductStatus`) 관리, 조회수 |
| `event` | `Event` | 상품 1건에 걸리는 기간 한정 할인. `Product`를 FK로 참조하되 역방향 참조는 없음 — "지금 적용 중인 이벤트"는 `findByProduct_IdAndStatus`로 그때그때 조회 |
| `coupon` | `Coupon`, `UserCoupon` | 쿠폰 정책과 사용자별 발급 이력을 분리(정책 재사용, 발급 수량 제한) |
| `cart` | `Cart`, `CartItem` | 사용자 1명당 장바구니 1개(`Cart.user_id` UNIQUE), 상품별 수량 |
| `order` | `Order`, `OrderItem` | 주문 스냅샷(주문 시점 가격·쿠폰 할인 배분액을 `OrderItem`에 그대로 저장) |
| `payment` | `Payment` | 결제 상태(`PaymentStatus`), 주문과 1:1에 가까운 N:1 |
| `refund` | `Refund`, `RefundItem` | 한 결제에 여러 번의 부분 환불 허용, 환불 항목 단위로 수량·금액 관리 |
| `chat` | `ChatRoom`, `ChatMessage` | 1:1 문의 채팅방, 담당자 배정(`assignee_id`), 챗봇 시나리오(`BotScenario`) |

`order`/`coupon`/`cart` 세 도메인엔 `facade` 패키지가 따로 있다. 여러 도메인의 서비스를 한 트랜잭션으로 묶어야 하는 유스케이스(주문 생성 시 재고 차감+쿠폰 사용+결제 생성 등)를 서비스 계층 대신 파사드가 조립해서, 각 도메인 서비스는 자기 도메인 로직만 알면 되게 했다.

## 패키지 구조
```
com.example.commerce
├── common/                 # 도메인에 안 묶이는 공통 관심사
│   ├── config/              # Security, JPA Auditing, Redis, Cache, WebConfig, Swagger, QueryDSL, Scheduling
│   ├── entity/              # BaseEntity(createdAt/updatedAt 공통 필드)
│   ├── exception/           # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── filter/               # JwtAuthFilter
│   ├── jwt/                  # JwtProvider
│   └── response/            # ApiResponse, ApiErrorResponse, PageResponse 공통 응답 포맷
└── domain/
    └── {도메인}/
        ├── controller/       # REST 엔드포인트
        ├── facade/           # (일부 도메인) 여러 서비스를 조립하는 트랜잭션 경계
        ├── service/          # 도메인 로직, 단일 애그리거트 트랜잭션
        ├── repository/       # Spring Data JPA + QueryDSL(Custom/Impl)
        ├── entity/           # JPA 엔티티, 상태(enum)
        └── dto/              # 요청/응답 DTO(record)
```
도메인 내부는 전부 이 레이어드 구조를 따르고, 도메인 간에는 서로의 엔티티를 직접 참조하지 않고 ID와 파사드/서비스 호출로만 연결한다(예: `Order`는 `Product`를 참조하지 않고 `OrderItem`에 상품 스냅샷만 저장).

## 설계 포인트
- **동시성 제어** — 낙관적 락(`@Version`) 대신 `@Lock(PESSIMISTIC_WRITE)` + `findByIdForUpdate`/`findAllByIdsForUpdate` 패턴을 `Product`/`Order`/`Payment`/`Refund`/`Coupon`/`UserCoupon` 6개 레포지토리에 일관되게 적용했다. 재고 차감·쿠폰 발급처럼 실패 시 재시도보다 "먼저 온 요청이 끝날 때까지 대기"가 자연스러운 케이스라 비관적 락을 선택했다.
- **트랜잭션 경계** — 서비스는 자기 도메인 하나의 트랜잭션만 책임지고, 여러 도메인을 넘나드는 유스케이스는 `facade`가 조립한다.
- **주문 스냅샷** — `OrderItem`은 주문 시점의 상품명·단가·쿠폰 할인 배분액을 그대로 저장한다. 이후 상품 가격이 바뀌거나 쿠폰이 만료돼도 이미 만들어진 주문 내역은 변하지 않는다.
- **캐시** — `/products/popular`처럼 실시간성이 필요 없는 조회만 Redis 캐시(5분 TTL)를 태운다.
- **스케줄러** — 쿠폰 만료(`CouponExpirationScheduler`), 이벤트 종료(`EventExpireScheduler`), 상품 조회수 Redis→DB 동기화(`ViewCountSyncScheduler`)를 각 도메인 안에 두어, 배치성 작업도 도메인 경계를 넘지 않게 했다.
- **실시간 채팅** — WebSocket(STOMP) + SockJS. Redis Pub/Sub는 쓰지 않고 `SimpMessagingTemplate`으로 서버 프로세스 안에서 바로 구독자에게 전달한다(다중 인스턴스 확장은 고려 대상 아님).

## 시연 전 준비

### 1. 필요한 것
- JDK 17
- MySQL 8, Redis — 직접 설치하거나, `docker-compose.yml`로 한 번에 띄울 수 있다:
  ```bash
  docker compose up -d mysql redis
  ```
- Node.js 20+ (프론트엔드 실행용)

### 2. 브랜치
**`dev` 브랜치**를 쓰면 된다. 장바구니 중복 담기 방지, 이벤트 할인 표시, 채팅 자동종료 버그 수정, 쿠폰 발급까지 전부 반영돼 있다.

```bash
git checkout dev
git pull
```

### 3. 환경변수
IntelliJ 실행 설정(Run Configuration)의 Environment Variables에 아래를 채운다.

| 변수 | 설명 | 비고 |
|---|---|---|
| `DB_USERNAME` | MySQL 계정 | 필수 |
| `DB_PASSWORD` | MySQL 비밀번호 | 필수 |
| `CHAT_BOT_USER_ID` | 챗봇이 쓸 계정의 users.id | 아래 4번 참고. 기본값(2)이 실제 봇 계정이 아니면 봇이 일반 회원 이름으로 메시지를 보낸다 |
| `DB_HOST` | MySQL 호스트 | 기본 `localhost` |
| `REDIS_HOST` | Redis 호스트 | 기본 `localhost` |
| `JWT_SECRET` | JWT 서명 키 | 기본값 있음(개발용) |

### 4. 더미 데이터 넣기
`db/seed/` 폴더의 SQL을 **이 순서로** 실행한다 (MySQL Workbench, `mysql` CLI 등 편한 걸로).

```bash
mysql -u <DB_USERNAME> -p commerce < db/seed/seed_products.sql
mysql -u <DB_USERNAME> -p commerce < db/seed/seed_dummy_data.sql
mysql -u <DB_USERNAME> -p commerce < db/seed/apply_event_discount.sql
mysql -u <DB_USERNAME> -p commerce < db/seed/seed_chat_bot.sql
```

- `seed_products.sql` — 상품 220개
- `seed_dummy_data.sql` — 테스트 회원 5명(비밀번호 전부 `test1234`), 쿠폰, 장바구니, 주문, 이벤트 4건
- `apply_event_discount.sql` — "할인상품" 화면에 뜨는 상품(`status='ON_EVENT'`) 전체에 이벤트가 없으면 만들고, 있으면 덮어써서 균일하게 5% 할인을 건다. 여러 번 실행해도 안전하다
- `seed_chat_bot.sql` — 챗봇 전용 계정 생성. **마지막 줄 조회 결과(`bot_user_id`)를 `CHAT_BOT_USER_ID` 환경변수에 넣고 백엔드를 (재)시작해야 한다**

테스트 계정: `kim@test.com` / `lee@test.com` / `park@test.com` / `choi@test.com` (일반 회원), `seller@test.com`(판매자) — 비밀번호 전부 `test1234`.

### 5. 실행
IntelliJ에서 `CommerceApplication` 실행(디버그 가능). 또는:

```bash
./gradlew bootRun
```

포트 `8080`.

## 프론트엔드까지 같이 켜기
[commerce-frontend](https://github.com/TeamOJOSAMA/commerce-frontend) 저장소를 받아서:

```bash
npm install
npm run dev
```

`http://localhost:5173`. `/api`는 `localhost:8080`으로 프록시된다(별도 설정 불필요).

## 시연 시나리오 예시
1. **상품 둘러보기** — 비로그인 상태로 홈/상품목록 진입. "할인상품" 탭에서 이벤트 할인 상품(취소선+할인율) 확인
2. **회원가입 또는 테스트 계정 로그인** (`kim@test.com` / `test1234`)
3. **장바구니** — 상품 담기, 같은 상품 두 번 담아서 중복 행 대신 수량이 합쳐지는 것 확인
4. **주문서 작성 → 결제** — 결제 완료 후 주문 상세에서 이벤트 할인/쿠폰 할인 표시 확인
5. **환불** — 결제 완료 주문에서 "환불 신청" → 주문 상세에 환불 사유 노출
6. **채팅 상담** — "+ 새 문의"로 문의 시작, 메시지 몇 개 주고받기, "상담사 연결" 입력해서 상태 전환 확인 (5분 방치하면 자동 종료 — 데모 중엔 굳이 기다릴 필요 없음)

## 알려진 제약
- `/products/popular`(인기 상품)는 Redis에 5분 캐시된다 — 이벤트 데이터를 막 바꿨다면 반영까지 최대 5분 걸릴 수 있음
- 데모 계정(로그인 화면의 "데모 계정으로 체험하기")은 백엔드 없이 프론트 목업만으로 화면을 둘러볼 수 있게 만든 것이라, 실제 결제·채팅은 동작하지 않는다(안내 문구가 뜬다). 실제 흐름을 보여주려면 반드시 위 테스트 계정으로 로그인할 것
- 예전 코드로 이미 DB를 한 번 띄워봤다면 `refunds` 테이블에 `uk_refund_payment_id`(결제당 환불 1건 제약)가 남아있을 수 있다. `ddl-auto: update`는 기존 제약을 자동으로 지우지 않으므로, 부분환불을 여러 번 테스트하다 원인 불명의 오류가 나면 `ALTER TABLE refunds DROP INDEX uk_refund_payment_id;` 실행하거나 DB를 새로 만들 것
- 같은 이유로 아주 오래전 DB라면 `events` 테이블에 지금 코드는 안 쓰는 `event_type` 컬럼이 남아있을 수 있다. MySQL이 ENUM 컬럼을 암묵적으로 채워주긴 하지만(첫 값으로), 신경 쓰이면 `ALTER TABLE events DROP COLUMN event_type;`로 지워도 된다
