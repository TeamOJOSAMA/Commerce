-- 챗봇 전용 계정. chat.bot-user-id(application.yml, 기본값 2)가 이 계정의 id를
-- 가리켜야 봇 메시지가 실제 회원이 아닌 "갈팡봇" 이름으로 나간다.
-- 이미 있으면 다시 넣지 않는다(재실행 안전).
INSERT INTO users (created_at, updated_at, email, name, password, role)
SELECT NOW(), NOW(), 'bot@galpangjilpang.com', '갈팡봇',
       '$2b$10$pqGTRSYxTNTb/JjpMqiVkOcyhSX1N0EW2urdNZ06NC91qMDkPYqQO', 'USER'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'bot@galpangjilpang.com'
);

-- 이 id를 CHAT_BOT_USER_ID 환경변수로 넣어야 한다. 기본값(2)과 다르면 반드시 설정할 것.
SELECT id AS bot_user_id FROM users WHERE email = 'bot@galpangjilpang.com';
