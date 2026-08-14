package com.contenthub.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDtos {

    public record AdminLoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record DashboardStatsResponse(
            long totalUsers,
            long totalPurchases,
            BigDecimal totalRevenue,
            long totalContentItems
    ) {}

    public record AdminUserResponse(
            Long id,
            String email,
            String mobile,
            String name,
            LocalDateTime joinedAt,
            LocalDateTime lastLoginAt,
            long purchaseCount,
            List<AdminPurchaseResponse> purchases
    ) {}

    public record AdminPurchaseResponse(
            Long purchaseId,
            String userEmail,
            Long contentId,
            String contentTitle,
            String contentType,
            BigDecimal amount,
            String status,
            LocalDateTime purchasedAt
    ) {}
}
