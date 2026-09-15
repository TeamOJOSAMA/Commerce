package com.example.commerce.domain.refund.service;

import com.example.commerce.common.exception.BusinessException;
import com.example.commerce.common.exception.ErrorCode;
import com.example.commerce.domain.order.entity.Order;
import com.example.commerce.domain.order.entity.OrderItem;
import com.example.commerce.domain.payment.entity.Payment;
import com.example.commerce.domain.payment.repository.PaymentRepository;
import com.example.commerce.domain.refund.dto.RefundRequest;
import com.example.commerce.domain.refund.entity.Refund;
import com.example.commerce.domain.refund.entity.RefundItem;
import com.example.commerce.domain.refund.entity.RefundType;
import com.example.commerce.domain.refund.repository.RefundItemRepository;
import com.example.commerce.domain.refund.repository.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final long PAYMENT_ID = 1L;
    private static final long ITEM_A_ID = 10L;
    private static final long ITEM_B_ID = 11L;
    private static final long OWNER_ID = 100L;
    private static final long OTHER_USER_ID = 200L;

    @Mock private RefundRepository refundRepository;
    @Mock private RefundItemRepository refundItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private Payment payment;
    @Mock private Order order;
    @Mock private OrderItem orderItemA;
    @Mock private OrderItem orderItemB;

    @InjectMocks
    private RefundService refundService;

    @BeforeEach
    void setup() {
        lenient().when(payment.getId()).thenReturn(PAYMENT_ID);
        lenient().when(payment.isPaid()).thenReturn(true);
        lenient().when(payment.getOrder()).thenReturn(order);
        lenient().when(order.getUserId()).thenReturn(OWNER_ID);
        lenient().when(order.getOrderItems()).thenReturn(List.of(orderItemA, orderItemB));
        lenient().when(orderItemA.getId()).thenReturn(ITEM_A_ID);
        lenient().when(orderItemA.getQuantity()).thenReturn(2);
        lenient().when(orderItemA.getPaidAmount()).thenReturn(1_800L);
        lenient().when(orderItemB.getId()).thenReturn(ITEM_B_ID);
        lenient().when(orderItemB.getQuantity()).thenReturn(1);
        lenient().when(orderItemB.getPaidAmount()).thenReturn(2_700L);

        lenient().when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
        // 기본값: 아직 환불된 수량 없음. 개별 테스트가 필요하면 재정의한다.
        lenient().when(refundItemRepository.findAllByOrderItem_IdIn(any())).thenReturn(List.of());
        lenient().when(refundRepository.saveAndFlush(any(Refund.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("전액 환불 - 주문 전체 항목을 전량 환불하고 총액은 항목 실결제액의 합")
    void createRefund_full() {
        var request = new RefundRequest(PAYMENT_ID, RefundType.FULL, "단순 변심", null);

        var response = refundService.createRefund(OWNER_ID, request);

        assertThat(response.refundType()).isEqualTo(RefundType.FULL);
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalRefundAmount()).isEqualTo(1_800L + 2_700L);
    }

    @Test
    @DisplayName("결제 소유자가 아니면 FORBIDDEN_ACCESS")
    void createRefund_notOwner() {
        given(order.getUserId()).willReturn(OWNER_ID);

        assertThatThrownBy(() -> refundService.createRefund(OTHER_USER_ID, new RefundRequest(PAYMENT_ID, RefundType.FULL, "x", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_ACCESS);
    }

    @Test
    @DisplayName("부분 환불 - 지정한 항목/수량만 환불")
    void createRefund_partial() {
        var request = new RefundRequest(PAYMENT_ID, RefundType.PARTIAL, "일부 반품",
                List.of(new RefundRequest.Item(ITEM_A_ID, 1)));

        var response = refundService.createRefund(OWNER_ID, request);

        assertThat(response.refundType()).isEqualTo(RefundType.PARTIAL);
        assertThat(response.items()).singleElement()
                .satisfies(i -> {
                    assertThat(i.orderItemId()).isEqualTo(ITEM_A_ID);
                    assertThat(i.quantity()).isEqualTo(1);
                });
        // 실결제액 1,800의 2개 중 1개 → 1,800 × 1 ÷ 2
        assertThat(response.totalRefundAmount()).isEqualTo(900L);
    }

    @Test
    @DisplayName("존재하지 않는 결제 - PAYMENT_NOT_FOUND")
    void createRefund_paymentNotFound() {
        given(paymentRepository.findByIdForUpdate(PAYMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.createRefund(OWNER_ID, new RefundRequest(PAYMENT_ID, RefundType.FULL, "x", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 전 항목이 환불된 결제 - 전액 환불 재요청은 REFUND_NOT_ALLOWED")
    void createRefund_alreadyFullyRefunded() {
        List<RefundItem> alreadyRefunded = List.of(new RefundItem(orderItemA, 2), new RefundItem(orderItemB, 1));
        given(refundItemRepository.findAllByOrderItem_IdIn(any())).willReturn(alreadyRefunded);

        assertThatThrownBy(() -> refundService.createRefund(OWNER_ID, new RefundRequest(PAYMENT_ID, RefundType.FULL, "x", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_NOT_ALLOWED);
    }

    @Test
    @DisplayName("부분 환불을 시점을 나눠 여러 번 요청할 수 있다 - 첫 요청에서 남은 만큼만 환불하면 두 번째 요청도 허용")
    void createRefund_partial_canRepeatAfterPreviousPartialRefund() {
        // itemA는 수량 2 중 1개가 이미 환불된 상태
        List<RefundItem> alreadyRefunded = List.of(new RefundItem(orderItemA, 1));
        given(refundItemRepository.findAllByOrderItem_IdIn(any())).willReturn(alreadyRefunded);

        var request = new RefundRequest(PAYMENT_ID, RefundType.PARTIAL, "나머지도 반품",
                List.of(new RefundRequest.Item(ITEM_A_ID, 1)));

        var response = refundService.createRefund(OWNER_ID, request);

        assertThat(response.items()).singleElement()
                .satisfies(i -> {
                    assertThat(i.orderItemId()).isEqualTo(ITEM_A_ID);
                    assertThat(i.quantity()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("부분 환불 - 이미 환불된 수량을 감안한 잔여 수량을 초과하면 REFUND_ITEM_NOT_FOUND")
    void createRefund_partial_exceedsRemainingAfterPreviousRefund() {
        // itemA는 수량 2 중 1개가 이미 환불된 상태라 남은 건 1개뿐
        List<RefundItem> alreadyRefunded = List.of(new RefundItem(orderItemA, 1));
        given(refundItemRepository.findAllByOrderItem_IdIn(any())).willReturn(alreadyRefunded);

        var request = new RefundRequest(PAYMENT_ID, RefundType.PARTIAL, "x",
                List.of(new RefundRequest.Item(ITEM_A_ID, 2)));

        assertThatThrownBy(() -> refundService.createRefund(OWNER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("부분 환불 - 주문에 없는 항목 ID - REFUND_ITEM_NOT_FOUND")
    void createRefund_partial_itemNotFound() {
        var request = new RefundRequest(PAYMENT_ID, RefundType.PARTIAL, "x", List.of(new RefundRequest.Item(999L, 1)));

        assertThatThrownBy(() -> refundService.createRefund(OWNER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("부분 환불 - 주문 수량 초과 - REFUND_ITEM_NOT_FOUND (RefundItem 생성자 검증")
    void createRefund_partial_quantityExceeds() {
        var request = new RefundRequest(PAYMENT_ID, RefundType.PARTIAL, "x", List.of(new RefundRequest.Item(ITEM_A_ID, 99)));

        assertThatThrownBy(() -> refundService.createRefund(OWNER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.REFUND_ITEM_NOT_FOUND);
    }

}
