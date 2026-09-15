package com.example.commerce.domain.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// 주문 생성 요청은 컨트롤러에 닿기 전에 DTO에서 거른다.
class CreateOrderRequestValidationTest {

    private static final String VALID_KEY = "checkout-key";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("항목을 선택한 요청은 위반 없이 통과한다")
    void validRequest() {
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(
                new CreateOrderRequest(List.of(1L, 2L), 3L, VALID_KEY, 30_000L));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("항목 목록이 null이면 빈 목록으로 정규화한 뒤 선택을 요구한다")
    void nullCartItemIdsIsNormalizedAndRejected() {
        CreateOrderRequest request = new CreateOrderRequest(null, null, VALID_KEY, null);

        assertThat(request.cartItemIds()).isEmpty();
        assertThat(messagesOf(request)).contains("주문할 장바구니 항목을 선택해 주세요.");
    }

    @Test
    @DisplayName("빈 목록은 전체 장바구니 주문으로 해석하지 않고 거부한다")
    void emptyCartItemIdsIsRejected() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(), null, VALID_KEY, null);

        assertThat(messagesOf(request)).contains("주문할 장바구니 항목을 선택해 주세요.");
    }

    @Test
    @DisplayName("멱등성 키가 없으면 중복 요청을 구분할 수 없으므로 거부한다")
    void blankIdempotencyKeyIsRejected() {
        assertThat(messagesOf(new CreateOrderRequest(List.of(1L), null, " ", null)))
                .contains("멱등성 키는 필수입니다.");
    }

    private List<String> messagesOf(CreateOrderRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }
}
