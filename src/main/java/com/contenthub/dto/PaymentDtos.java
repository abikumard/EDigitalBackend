package com.contenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentDtos {

    public record CreateOrderRequest(
            @NotNull Long contentId
    ) {}

    public record CreateOrderResponse(
            Long purchaseId,
            String razorpayOrderId,
            long amountInPaise,
            BigDecimal amount,
            String currency,
            String razorpayKeyId,
            boolean alreadyPurchased
    ) {}

    public record VerifyPaymentRequest(
            @NotNull Long purchaseId,
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {}

    public record VerifyPaymentResponse(
            boolean success,
            String message,
            Long contentId
    ) {}

    public record CreateCartOrderResponse(
            String razorpayOrderId,
            long amountInPaise,
            BigDecimal amount,
            String currency,
            String razorpayKeyId,
            int itemCount
    ) {}

    public record VerifyCartPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature
    ) {}

    public record VerifyCartPaymentResponse(
            boolean success,
            String message,
            int itemsUnlocked
    ) {}
}
