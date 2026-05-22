package com.wanderai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDto {

    @Data
    public static class CreateOrderRequest {
        private String packageName; // BASIC, PRO, ENTERPRISE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderResponse {
        private String orderId;
        private Double amount;
        private String currency;
        private String keyId;
        private String packageName;
        private Integer creditsToAdd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private Double amount;
        private String currency;
        private Integer creditsAdded;
        private String packageName;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditPackage {
        private String name;
        private Integer credits;
        private Double price;
        private String currency;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingHistoryResponse {
        private Integer currentCredits;
        private List<TransactionResponse> transactions;
    }
}
