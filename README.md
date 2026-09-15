# Commerce (갈팡질팡) — 백엔드

Spring Boot 4.1.1 / Java 17 기반 커머스 API 서버. 프론트엔드는 별도 저장소([commerce-frontend](https://github.com/TeamOJOSAMA/commerce-frontend))에 있다.

## 시연 전 준비

### 1. 필요한 것
- JDK 17
- MySQL 8 (로컬에 `commerce` 데이터베이스)
- Redis (인기 상품 캐시용)
- Node.js 20+ (프론트엔드 실행용)

### 2. 브랜치
지금은 **`feat/front` 브랜치**를 써야 한다. `dev`로 보내는 PR이 아직 리뷰 중이라, `dev`에는 오늘 작업한 수정사항(장바구니 중복 담기 방지, 비로그인 상품 조회, 이벤트 할인 표시, 채팅 자동종료 반복 버그 등)이 없다.

```bash
git checkout feat/front
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
mysql -u <DB_USERNAME> -p commerce < db/seed/backfill_missing_events.sql
mysql -u <DB_USERNAME> -p commerce < db/seed/seed_chat_bot.sql
```

- `seed_products.sql` — 상품 220개
- `seed_dummy_data.sql` — 테스트 회원 5명(비밀번호 전부 `test1234`), 쿠폰, 장바구니, 주문, 이벤트 4건
- `backfill_missing_events.sql` — `seed_products.sql`이 "이벤트중" 상태로 만들어둔 상품 중, 실제 이벤트 데이터가 빠진 나머지 상품에 이벤트를 채워준다
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
1. **상품 둘러보기** — 비로그인 상태로 홈/상품목록 진입. "타임세일" 탭에서 이벤트 할인 상품(취소선+할인율) 확인
2. **회원가입 또는 테스트 계정 로그인** (`kim@test.com` / `test1234`)
3. **장바구니** — 상품 담기, 같은 상품 두 번 담아서 중복 행 대신 수량이 합쳐지는 것 확인
4. **주문서 작성 → 결제** — 결제 완료 후 주문 상세에서 이벤트 할인/쿠폰 할인 표시 확인
5. **환불** — 결제 완료 주문에서 "환불 신청" → 주문 상세에 환불 사유 노출
6. **채팅 상담** — "+ 새 문의"로 문의 시작, 메시지 몇 개 주고받기, "상담사 연결" 입력해서 상태 전환 확인 (5분 방치하면 자동 종료 — 데모 중엔 굳이 기다릴 필요 없음)

## 알려진 제약
- **쿠폰 발급/내 쿠폰 조회**는 아직 PR 리뷰 중이라 화면에서는 목업 데이터로만 보인다 (실제 발급 불가)
- `/products/popular`(인기 상품)는 Redis에 5분 캐시된다 — 이벤트 데이터를 막 바꿨다면 반영까지 최대 5분 걸릴 수 있음
- 데모 계정(로그인 화면의 "데모 계정으로 체험하기")은 백엔드 없이 프론트 목업만으로 화면을 둘러볼 수 있게 만든 것이라, 실제 결제·채팅은 동작하지 않는다(안내 문구가 뜬다). 실제 흐름을 보여주려면 반드시 위 테스트 계정으로 로그인할 것
