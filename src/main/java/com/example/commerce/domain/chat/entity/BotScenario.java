package com.example.commerce.domain.chat.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BotScenario {

    DELIVERY("배송", "배송 조회는 마이페이지 > 주문내역에서 확인하실 수 있습니다."),
    REFUND("환불", "환불은 상품 수령 후 7일 이내 신청 가능합니다."),
    EXCHANGE("교환", "교환 정책 안내 준비 중입니다."),
    COUPON("쿠폰", "쿠폰은 주문당 1장만 사용할 수 있으며, 쿠폰별 최소 주문금액과 최대 할인금액 조건이 적용될 수 있습니다."),
    PAYMENT("결제", "결제가 완료되지 않은 경우 주문 상태를 확인한 후 다시 시도해주세요. 지속적으로 실패할 경우 상담사 연결을 이용해주세요.");

    public static final String ESCALATE_REPLY = "상담원을 연결하고 있습니다. 잠시만 기다려주세요.";
    public static final String CLOSE_CONFIRM_REPLY =
            "상담을 종료하시겠습니까? 종료하시려면 '종료', 문의를 이어가시려면 '계속'을 입력해주세요.";
    public static final String CLOSED_REPLY = "상담을 종료합니다. 감사합니다.";
    public static final String CONTINUE_REPLY = "네, 계속 도와드리겠습니다. 무엇을 도와드릴까요?";
    public static final String AUTO_CLOSED_REPLY =
            "5분 동안 문의가 없어 상담을 자동 종료합니다. 감사합니다.";

    private static final String FALLBACK_REPLY =
            "죄송합니다. 이해하지 못했습니다. 상담원 연결을 원하시면 '상담사 연결'을 입력해주세요.";

    private final String keyword;
    private final String reply;

    BotScenario(String keyword, String reply) {
        this.keyword = keyword;
        this.reply = reply;
    }

    // 첫 번째로 매칭되는 시나리오의 답변을 반환한다. 매칭되지 않으면 상담원 연결을 안내한다
    public static String replyTo(String message) {
        return Arrays.stream(values())
                .filter(scenario -> message.contains(scenario.keyword))
                .findFirst()
                .map(BotScenario::getReply)
                .orElse(FALLBACK_REPLY);
    }
}