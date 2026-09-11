package com.contenthub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayoutDtos {

    public record RequestPayoutRequest(
            BigDecimal amount,
            String payoutMethod,
            String payoutDetails
    ) {}

    public record PayoutResponse(
            Long id,
            BigDecimal amount,
            String payoutMethod,
            String payoutDetails,
            String status,
            String transactionRef,
            LocalDateTime requestedAt,
            LocalDateTime processedAt
    ) {}

    public record PublisherWalletSummary(
            BigDecimal totalEarnings,
            BigDecimal platformFeePaid,
            BigDecimal availableBalance,
            BigDecimal paidOutAmount,
            long totalSalesCount
    ) {}
}
