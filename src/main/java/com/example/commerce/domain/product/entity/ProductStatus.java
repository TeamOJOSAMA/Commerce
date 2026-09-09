package com.example.commerce.domain.product.entity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProductStatus {
    ON_SALE {
        @Override
        public boolean canTransitTo(ProductStatus target) {return target == SOLDOUT || target == ON_EVENT;}
    },

    SOLDOUT {
        @Override
        public boolean canTransitTo(ProductStatus target) {return false ;}
    },

    ON_EVENT {
        @Override
        public boolean canTransitTo(ProductStatus target) {return target == SOLDOUT || target == ON_SALE;}
    };


   public abstract boolean canTransitTo(ProductStatus target);
}
