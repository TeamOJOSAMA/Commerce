package com.example.commerce.domain.refund.entity;

public enum RefundStatus {
    REQUESTED {
        @Override
        public boolean canTransitTo(RefundStatus target) {

            return target == COMPLETED;
        }
    },

    COMPLETED {
        @Override
        public boolean canTransitTo(RefundStatus target) {

            return false;
        }
    };

    public abstract boolean canTransitTo(RefundStatus target);
}