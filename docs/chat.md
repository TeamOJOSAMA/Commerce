# 실시간 CS 문의 채팅

담당: 최정이

고객이 CS 문의를 등록하면 먼저 자동응답 봇이 안내하고, 해결되지 않으면 상담원에게 연결되는 구조입니다.

```
채팅방 생성 → BOT_HANDLING (봇 응대)
                  ↓ 고객이 "상담사 연결" 입력
              WAITING (상담원 연결 대기)
                  ↓ 관리자가 픽업
              IN_PROGRESS (1:1 실시간 상담)
                  ↓
              COMPLETED (완료)
```

---

## 1. 왜 WebSocket인가

HTTP는 요청이 있어야 응답이 오는 단방향 구조입니다. 서버에 새 메시지가 도착해도 클라이언트가 먼저 물어보지 않으면 알 수 없습니다.

이걸 HTTP로 흉내내려면 폴링밖에 없는데, 1초마다 요청하면 **메시지가 없어도 요청이 계속 나갑니다.** 접속자가 100명이면 초당 100건이고, 대부분은 "새 메시지 없음"이라는 빈 응답입니다. 매 요청마다 헤더와 쿠키가 오가고 커넥션을 새로 맺는 비용도 그대로 듭니다.

WebSocket은 **한 번 연결하면 그 연결을 유지**합니다. 서버가 먼저 보낼 수 있고, 핸드셰이크 이후에는 HTTP 헤더 없이 프레임만 오갑니다. 채팅처럼 언제 올지 모르는 데이터를 주고받는 데 적합합니다.

| | HTTP 폴링 | WebSocket |
| --- | --- | --- |
| 방향 | 클라이언트 요청 필수 | 양방향 |
| 연결 | 요청마다 맺고 끊음 | 한 번 맺고 유지 |
| 오버헤드 | 매 요청 헤더·쿠키 | 최초 핸드셰이크만 |
| 지연 | 폴링 주기만큼 | 즉시 |

---

## 2. 순수 WebSocket의 한계와 STOMP

발제 요구에 따라 순수 WebSocket으로 먼저 구현한 뒤 STOMP로 전환했습니다.

### 순수 WebSocket에서 겪은 문제

```java
public class EchoWebSocketHandler extends TextWebSocketHandler {

    // 브로커가 없으니 세션을 직접 들고 있어야 한다
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 구독 개념이 없어 모든 세션에 직접 전송
        for (WebSocketSession connectedSession : sessions) {
            connectedSession.sendMessage(new TextMessage(message.getPayload()));
        }
    }
}
```

브라우저 창 두 개를 띄우고 메시지를 보내니 **양쪽 모두에 도착했습니다.** 채팅방이라는 개념이 없으니 당연한 결과입니다.

채팅방을 구분하려면 `Map<Long, Set<WebSocketSession>>` 같은 구조를 직접 만들고, 입장 시 추가·퇴장 시 제거·비정상 종료 처리까지 전부 구현해야 합니다. 메시지 형식도 정해진 게 없어서 "이 메시지가 대화인지 입장 알림인지"를 매번 파싱해서 판단해야 합니다.

### STOMP 적용 후

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/sub", "/user");
    registry.setApplicationDestinationPrefixes("/pub");
}
```

- 세션 관리가 사라졌습니다. 브로커가 구독자 목록을 관리합니다.
- 서버는 `convertAndSend("/sub/chat-rooms/1", message)` 한 줄이면 그 방 구독자에게만 전달됩니다.
- `CONNECT`, `SUBSCRIBE`, `SEND` 같은 명령 체계가 있어 메시지 성격을 프로토콜 수준에서 구분할 수 있습니다.
- `@MessageMapping`으로 HTTP의 `@RequestMapping`처럼 라우팅할 수 있습니다.

### SUBSCRIBE와 SEND

| 명령 | 방향 | 역할 |
| --- | --- | --- |
| `SUBSCRIBE` | 클라이언트 → 서버 | 특정 destination의 메시지를 받겠다고 등록. 브로커가 구독자 목록에 추가 |
| `SEND` | 클라이언트 → 서버 | 서버로 메시지 발행. `@MessageMapping` 메서드로 라우팅 |

이 프로젝트에서는 `SEND`로 받은 메시지를 저장한 뒤, 서버가 `/sub/chat-rooms/{chatRoomId}`로 브로드캐스트합니다. 그 방을 `SUBSCRIBE`한 사람만 받습니다.

---

## 3. WebSocket 인증 — 왜 Filter가 아니라 Interceptor인가

프로젝트에 이미 `JwtAuthFilter`가 있었지만 WebSocket에는 쓸 수 없었습니다.

### HTTP Filter가 동작하지 않는 이유

**핸드셰이크 단계에서 헤더를 실을 수 없습니다.** 브라우저의 WebSocket API에는 커스텀 헤더를 넣는 방법이 없고, SockJS도 마찬가지입니다.

**필터는 핸드셰이크 요청 한 번만 지나갑니다.** 프로토콜이 전환된 뒤 오가는 STOMP 프레임은 서블릿 필터 체인을 거치지 않습니다. 이미 HTTP가 아니기 때문입니다.

### ChannelInterceptor로 해결

STOMP에는 연결 직후 클라이언트가 보내는 `CONNECT` 프레임이 있고, 여기에는 헤더를 실을 수 있습니다.

```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        // CONNECT 시점에 한 번만 인증. 이후 SEND 는 인증된 세션이 유지된다
        accessor.setUser(authenticate(accessor));
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
        validateSubscribable(accessor);
    }

    return message;
}
```

| | HTTP Filter | ChannelInterceptor |
| --- | --- | --- |
| 가로채는 대상 | 서블릿 요청 | STOMP 메시지 |
| 동작 시점 | 매 HTTP 요청 | CONNECT / SUBSCRIBE / SEND 프레임 |
| 헤더 접근 | HTTP 헤더 | STOMP native 헤더 |

`/api/ws-stomp` 경로는 `permitAll`로 열려 있지만, 실제 인증은 그 다음 단계인 CONNECT에서 이루어집니다.

### 클라이언트가 senderId를 보내지 않는다

인증이 CONNECT에서 끝나므로, 메시지 payload는 이것뿐입니다.

```json
{ "content": "배송이 언제 오나요?" }
```

발신자는 `accessor.setUser()`로 심어둔 `Principal`에서 꺼냅니다.

```java
@MessageMapping("/chat-rooms/{chatRoomId}/messages")
public void sendMessage(@DestinationVariable Long chatRoomId,
                        ChatMessageSendRequest request,
                        Principal principal) {
    AuthUser authUser = extractAuthUser(principal);
    // ...
}
```

클라이언트가 `senderId`를 보내는 구조였다면 남의 ID를 적어 보내는 위조가 가능했을 것입니다.

### SUBSCRIBE 권한 검증

destination의 방 번호만 바꾸면 타인의 상담 내용을 볼 수 있으므로, 구독 시점에도 검증합니다.

```java
if (!chatRoomRepository.existsAccessibleBy(chatRoomId, authUser.getUserId(), isAdmin)) {
        throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
}
```

고객은 본인이 생성한 문의만, 관리자는 모든 문의를 구독할 수 있습니다.

---

## 4. 채팅 도메인 설계

### 메시지 → 채팅방 단방향 참조

```java
@Entity
public class ChatMessage extends BaseEntity {

    // 메시지 -> 채팅방 단방향 (양방향 X)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;
}
```

`ChatRoom`에 `List<ChatMessage>`를 두지 않았습니다.

**메시지는 무한히 쌓입니다.** 컬렉션을 양방향으로 두면 채팅방을 조회하는 것만으로 전체 메시지를 끌고 올 위험이 생깁니다. LAZY로 두어도 실수로 `getMessages()`를 호출하면 수만 건이 로딩됩니다.

**메시지 조회는 항상 페이징을 동반합니다.** 전체를 다 읽을 일이 없으므로 컬렉션으로 들고 있을 이유가 없습니다. 필요한 만큼만 Repository로 조회하는 편이 안전합니다.

**연관관계 편의 메서드도 불필요합니다.** 양방향이면 양쪽을 모두 맞춰줘야 하는데, 단방향이면 메시지를 저장하는 것으로 끝납니다.

### 인덱스

```sql
CREATE INDEX idx_chat_messages_room_id ON chat_messages (chat_room_id, chat_message_id);
CREATE INDEX idx_chat_rooms_status_created ON chat_rooms (inquiry_status, created_at);
CREATE INDEX idx_chat_rooms_user_created ON chat_rooms (user_id, created_at);
```

메시지 조회는 `WHERE chat_room_id = ? AND chat_message_id < ?` 형태라 두 컬럼의 복합 인덱스가 필요합니다.
관리자의 상태별 필터 조회(`WHERE inquiry_status = 'WAITING'`)도 빈번해 별도 인덱스를 두었습니다.

### 담당 관리자 추적

초기 스키마에는 문의를 등록한 고객(`user_id`)만 있었습니다. 관리자가 여러 명일 때 **누가 응대 중인 문의인지 구분할 수 없고**, 상태를 변경한 사람과 실제 응대자가 달라질 수 있어 `assignee_id`를 추가했습니다.

`WAITING → IN_PROGRESS` 전이 시 요청한 관리자가 자동으로 담당자로 지정됩니다.

```java
public void assignTo(User admin) {
    inquiryStatus.validateTransitionTo(InquiryStatus.IN_PROGRESS);
    this.assignee = admin;
    this.inquiryStatus = InquiryStatus.IN_PROGRESS;
}
```

---

## 5. 상태 전이 규칙

### 왜 ENUM 안에 두었는가

상태 검증을 Service에 `if`문으로 흩어놓으면, 나중에 다른 경로로 상태를 바꾸는 코드가 생겼을 때 규칙이 뚫립니다. 전이 규칙은 상태 자신이 알아야 할 정보라고 판단해 ENUM 내부에 허용 목록으로 두었습니다.

```java
public enum InquiryStatus {

    BOT_HANDLING, WAITING, IN_PROGRESS, COMPLETED;

    // 허용 목록 방식이라 명시하지 않은 전이는 전부 차단된다
    private static final Map<InquiryStatus, Set<InquiryStatus>> ALLOWED_TRANSITIONS = Map.of(
            BOT_HANDLING, Set.of(WAITING, COMPLETED),
            WAITING, Set.of(IN_PROGRESS, COMPLETED),
            IN_PROGRESS, Set.of(COMPLETED),
            COMPLETED, Set.of()
    );

    public void validateTransitionTo(InquiryStatus target) {
        if (!ALLOWED_TRANSITIONS.get(this).contains(target)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}
```

### 역방향 전이를 어떻게 막는가

**금지 목록이 아니라 허용 목록**으로 관리했습니다. 금지 목록은 새 상태가 추가될 때마다 빠뜨릴 위험이 있지만, 허용 목록은 명시하지 않은 전이가 전부 차단됩니다.

`COMPLETED`의 허용 대상은 빈 집합이므로 완료된 문의는 어떤 상태로도 돌아갈 수 없습니다.

```
PATCH /api/v1/chat-rooms/1/status  { "inquiryStatus": "WAITING" }
→ 400 CHAT_003 허용되지 않은 상태 전이입니다.
```

### 전이 주체 분리

`COMPLETED`로 가는 길은 여러 개지만, 아무나 아무 전이나 할 수는 없습니다.

| 전이 | 주체 | 경로 |
| --- | --- | --- |
| `BOT_HANDLING → WAITING` | 고객 | `POST /chat-rooms/{id}/escalate` 또는 "상담사 연결" 입력 |
| `BOT_HANDLING → COMPLETED` | 고객 / 시스템 | "종료" 입력 또는 5분 방치 시 자동 |
| `WAITING → IN_PROGRESS` | 관리자 | `PATCH /chat-rooms/{id}/status` |
| `IN_PROGRESS → COMPLETED` | 관리자 | `PATCH /chat-rooms/{id}/status` |

고객이 호출하는 유일한 전이(`escalate`)는 상태 변경 API와 분리했습니다. 하나의 엔드포인트에서 역할별로 분기하면 권한 검사가 복잡해지기 때문입니다.

---

## 6. 커서 기반 페이징

### Offset 방식을 쓰지 않은 이유

```sql
SELECT * FROM chat_messages ORDER BY id DESC LIMIT 20 OFFSET 20;
```

**실시간으로 데이터가 쌓이는 환경에서 중복과 누락이 발생합니다.** 1페이지를 본 뒤 새 메시지가 하나 들어오면 전체가 한 칸씩 밀려서, 2페이지에서 이미 본 메시지를 다시 보게 됩니다.

**OFFSET이 커질수록 느려집니다.** DB는 건너뛸 행을 실제로 다 읽고 버립니다. `OFFSET 10000`이면 10,020건을 읽고 20건만 반환합니다.

### 커서 방식

```java
@Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.sender
        WHERE m.chatRoom.id = :chatRoomId
          AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
        """)
List<ChatMessage> findSliceBefore(@Param("chatRoomId") Long chatRoomId,
                                  @Param("cursor") Long cursor,
                                  Pageable pageable);
```

기준점이 "몇 번째"가 아니라 "어느 ID 이후"이므로 새 데이터가 들어와도 흔들리지 않습니다. 인덱스를 타고 바로 시작 지점으로 이동하므로 페이지가 뒤로 가도 성능이 일정합니다.

| | Offset | Cursor |
| --- | --- | --- |
| 기준 | 건너뛸 개수 | 마지막으로 본 ID |
| 데이터 추가 시 | 중복·누락 발생 | 영향 없음 |
| 깊은 페이지 성능 | 선형적으로 저하 | 일정 |
| 임의 페이지 접근 | 가능 | 불가능 (순차 탐색만) |

채팅은 "5페이지로 점프"할 일이 없으므로 커서 방식의 단점이 문제되지 않습니다.

### hasNext 판정

`COUNT` 쿼리를 추가로 날리지 않고, `size + 1`개를 조회해 초과분 유무로 판단합니다.

```java
Pageable pageable = PageRequest.of(0, size + 1);
List<ChatMessage> messages = chatMessageRepository.findSliceBefore(chatRoomId, cursor, pageable);

boolean hasNext = messages.size() > size;
if (hasNext) {
messages = messages.subList(0, size);
}
```

### N+1 방지

`sender`는 `@ManyToOne(LAZY)`이므로 메시지 20건을 조회하면 발신자 이름을 꺼낼 때 쿼리가 20번 더 발생합니다. `JOIN FETCH`로 함께 로딩해 한 번에 해결했습니다.

**ToOne 관계라 페이징과 함께 써도 안전합니다.** `@OneToMany` 컬렉션에 fetch join을 걸면 Hibernate가 전체를 메모리로 읽은 뒤 잘라내므로(`HHH000104`) 위험하지만, ToOne은 그렇지 않습니다.

---

## 7. 재연결과 메시지 복구

### 문제

네트워크가 끊기면 브로커는 해당 세션을 구독자 목록에서 제거합니다. 재연결하더라도 **끊겨 있던 동안 발행된 메시지는 받을 수 없습니다.** 브로커가 미수신 메시지를 보관하지 않기 때문입니다.

### lastMessageId 기반 복구

클라이언트가 마지막으로 받은 메시지 ID를 기억해두고, 재연결 후 그 이후 메시지를 조회합니다.

```javascript
stompClient.subscribe("/sub/chat-rooms/" + roomId, (message) => {
    const body = JSON.parse(message.body);
    lastMessageId = body.chatMessageId;   // 수신할 때마다 갱신
});

// 재연결 성공 콜백에서 호출
async function recoverMissedMessages() {
    if (lastMessageId === null) return;

    const url = BASE_URL + "/v1/chat-rooms/" + roomId + "/messages"
        + "?cursor=" + lastMessageId + "&size=50&direction=AFTER";
    // ...
}
```

### 조회 API 재사용

과거 조회와 복구 조회는 **부등호 방향만 다릅니다.** 별도 API를 만들지 않고 `direction` 파라미터로 분기했습니다.

| 용도 | 조건 | 정렬 |
| --- | --- | --- |
| 과거 조회 (스크롤 업) | `id < cursor` | 최신순 |
| 복구 조회 (재연결) | `id > cursor` | 과거순 |

### 자동 재연결

```javascript
stompClient.connect({ Authorization: token }, () => {
    subscribeAll();
    recoverMissedMessages();
}, () => {
    // 연결이 끊기거나 실패하면 재연결을 시도한다
    if (isManualDisconnect) return;
    setTimeout(openSocket, RECONNECT_DELAY_MS);
});
```

`socket.onclose`에 핸들러를 걸면 STOMP.js가 내부에서 덮어써 호출되지 않습니다. `connect()`의 에러 콜백에서 처리해야 합니다.

---

## 8. 입장/퇴장 시스템 메시지

별도 API 없이 STOMP 이벤트로 처리했습니다.

| 이벤트 | 처리 |
| --- | --- |
| `SessionSubscribeEvent` | `ENTER` 메시지 저장 후 해당 방에 발행 |
| `SessionDisconnectEvent` | `LEAVE` 메시지 저장 후 해당 방에 발행 |

### 비정상 종료를 어떻게 처리하는가

퇴장 API를 만들면 **브라우저를 그냥 닫는 경우를 잡을 수 없습니다.** 사용자가 API를 호출해주지 않기 때문입니다.

`SessionDisconnectEvent`는 명시적 종료든 네트워크 단절이든 브라우저 종료든 관계없이 발생하므로, 별도 처리 없이 모든 경우를 포괄합니다.

### 세션-채팅방 매핑

연결 해제 시점에는 destination 정보가 없어 어느 방에 있었는지 알 수 없습니다. 구독 시점에 매핑을 기록해둡니다.

```java
private final Map<String, Long> sessionRooms = new ConcurrentHashMap<>();

// 같은 세션이 이미 입장했다면 중복 발송하지 않는다
if (sessionRooms.putIfAbsent(accessor.getSessionId(), chatRoomId) != null) {
        return;
        }
```

한 번 연결에 메시지·에러·상태 세 경로를 구독하므로, `putIfAbsent`로 최초 구독만 입장으로 처리합니다.

---

## 9. 자동응답 봇

### 시나리오 관리

DB 테이블 대신 ENUM으로 관리했습니다. 관리자가 시나리오를 편집하는 요구사항이 없어, 테이블과 CRUD API 4개를 추가하는 비용이 과하다고 판단했습니다.

```java
public enum BotScenario {

    DELIVERY("배송", "배송 조회는 마이페이지 > 주문내역에서 확인하실 수 있습니다."),
    REFUND("환불", "환불은 상품 수령 후 7일 이내 신청 가능합니다."),
    // ...

    public static String replyTo(String message) {
        return Arrays.stream(values())
                .filter(scenario -> message.contains(scenario.keyword))
                .findFirst()
                .map(BotScenario::getReply)
                .orElse(FALLBACK_REPLY);
    }
    }
```

### 상태로 봇과 사람을 구분

```java
private void replyIfBotTurn(Long chatRoomId, String content) {
    if (!chatBotService.isBotTurn(chatMessageService.getChatRoom(chatRoomId))) {
        return;   // 상담원 연결 이후에는 사람이 답한다
    }
    // ...
}
```

`BOT_HANDLING` 상태에서만 반응합니다. 고객이 "상담사 연결"을 입력해 `WAITING`으로 전이되는 순간부터 봇은 비활성화됩니다. 별도 플래그 없이 **기존 상태값 하나로 모드가 갈리므로** 조건이 흩어지지 않습니다.

### 방치 채팅방 자동 종료

```java
@Scheduled(fixedDelayString = "${chat.cleanup-interval-ms}")
public void closeIdleChatRooms() {
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(idleTimeoutMinutes);
    chatMessageRepository.findIdleChatRoomIds(threshold).forEach(this::closeAndNotify);
}
```

봇 응대 중인 방이 5분간 메시지 없이 방치되면 자동으로 종료합니다. 마지막 메시지 시각은 `GROUP BY` + `HAVING MAX(created_at)`으로 구합니다.

### 봇 계정

`chat_messages.sender_id`가 `NOT NULL`이므로 봇 메시지에도 발신자가 필요합니다. 컬럼을 nullable로 바꾸면 `JOIN FETCH`를 `LEFT JOIN FETCH`로 변경해야 하고 DTO마다 null 체크가 붙으므로, **봇 전용 계정을 시드로 두는 쪽**을 택했습니다. FK 제약과 기존 쿼리를 그대로 유지할 수 있습니다.

---

## 10. STOMP 예외 처리

### 문제

`SEND`는 단방향이라 `@MessageMapping` 메서드에서 예외가 발생해도 **클라이언트에 아무것도 전달되지 않습니다.** 서버 로그에는 예외가 남지만 사용자는 메시지가 전송되지 않은 이유를 알 수 없습니다.

HTTP라면 `@RestControllerAdvice`가 400 응답을 만들어주지만, STOMP에는 요청-응답 짝이 없습니다.

### 해결

에러 전용 destination을 두고 예외 발생 시 그쪽으로 발행합니다.

```java
try {
ChatMessageResponse message = chatMessageService.sendMessage(...);
broadcast(chatRoomId, message);
replyIfBotTurn(chatRoomId, request.content());
        } catch (BusinessException e) {
sendError(chatRoomId, e);   // /sub/chat-rooms/{id}/errors
}
```

```json
{ "code": "CHAT_004", "message": "이미 완료된 문의입니다." }
```

`@SendToUser("/queue/errors")`로 요청자에게만 보내는 방식을 먼저 시도했으나 전달되지 않아, 방 단위 경로로 대체했습니다. CS 문의는 고객과 상담원 2인 대화라 실질적 문제는 없으나, 다인 채팅에서는 개인 큐 방식이 적합합니다.

---

## 11. 구현하지 않은 항목 — unread count

이번 범위에서는 구현하지 않았습니다. CS 문의는 고객 1명과 상담원 1명의 대화라 안 읽은 메시지 수가 크게 의미 없고, 상담원이 문의를 픽업하는 시점에 전체를 읽기 때문입니다.

구현한다면 `ChatMember` 테이블에 `last_read_message_id`를 두는 방식을 택했을 것입니다.

```sql
SELECT COUNT(*) FROM chat_messages
WHERE chat_room_id = ? AND chat_message_id > ?
```

별도 카운터 컬럼을 두고 증감시키는 방법도 있지만, **동시에 여러 메시지가 오갈 때 정합성이 깨질 수 있습니다.** 읽은 위치만 기록하고 개수는 조회 시점에 계산하면 그런 문제가 없고, `(chat_room_id, chat_message_id)` 인덱스를 그대로 활용할 수 있습니다.

---

## API 목록

| 기능 | Method | Path | 권한 |
| --- | --- | --- | --- |
| CS 채팅방 생성 | POST | `/api/v1/chat-rooms` | 고객 |
| 내 문의 목록 조회 | GET | `/api/v1/chat-rooms/me` | 고객 |
| 전체 문의 목록 조회 | GET | `/api/v1/chat-rooms` | 관리자 |
| 문의 상태 변경 | PATCH | `/api/v1/chat-rooms/{id}/status` | 관리자 |
| 상담사 연결 요청 | POST | `/api/v1/chat-rooms/{id}/escalate` | 고객 |
| 메시지 내역 조회 | GET | `/api/v1/chat-rooms/{id}/messages` | 참여자 |
| 메시지 발행 | SEND | `/pub/chat-rooms/{id}/messages` | 참여자 |
| 메시지 구독 | SUBSCRIBE | `/sub/chat-rooms/{id}` | 참여자 |
| 상태 변경 구독 | SUBSCRIBE | `/sub/chat-rooms/{id}/status` | 참여자 |
| 에러 구독 | SUBSCRIBE | `/sub/chat-rooms/{id}/errors` | 참여자 |

## ErrorCode

| 코드 | HTTP | 메시지 |
| --- | --- | --- |
| `CHAT_001` | 404 | 채팅방을 찾을 수 없습니다. |
| `CHAT_002` | 403 | 해당 채팅방에 접근할 권한이 없습니다. |
| `CHAT_003` | 400 | 허용되지 않은 상태 전이입니다. |
| `CHAT_004` | 400 | 이미 완료된 문의입니다. |
| `CHAT_005` | 400 | 유효하지 않은 커서 값입니다. |
| `CHAT_006` | 400 | 존재하지 않는 문의 상태입니다. |
| `CHAT_007` | 401 | 웹소켓 인증에 실패했습니다. |