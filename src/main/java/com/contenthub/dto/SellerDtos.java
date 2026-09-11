package com.contenthub.dto;

import com.contenthub.entity.Seller;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SellerDtos {

    public record SellerApplicationRequest(
            @NotBlank String businessName,
            @NotBlank String accountHolderName,
            @NotBlank String bankAccountNumber,
            @NotBlank String ifscCode,
            @NotBlank String bankName,
            @NotBlank String panNumber,
            @NotBlank String phone,
            @NotBlank String address
    ) {}

    public record SellerApplyRequest(
            String businessName,
            String penName,
            String bio,
            String accountHolderName,
            String bankAccountNumber,
            String ifscCode,
            String bankName,
            String upiId,
            String panNumber,
            String phone,
            String address
    ) {}

    public record SellerStatusResponse(
            String status,
            String businessName,
            String rejectionReason,
            LocalDateTime appliedAt,
            LocalDateTime reviewedAt
    ) {
        public static SellerStatusResponse of(Seller s) {
            return new SellerStatusResponse(
                    s.getStatus(),
                    s.getBusinessName(),
                    s.getRejectionReason(),
                    s.getAppliedAt(),
                    s.getReviewedAt()
            );
        }
    }

    public record AdminSellerResponse(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            String businessName,
            String accountHolderName,
            String bankAccountNumber,
            String ifscCode,
            String bankName,
            String panNumber,
            String phone,
            String address,
            String status,
            String rejectionReason,
            LocalDateTime appliedAt,
            LocalDateTime reviewedAt
    ) {
        public static AdminSellerResponse of(Seller s) {
            return new AdminSellerResponse(
                    s.getId(),
                    s.getUser().getId(),
                    s.getUser().getName(),
                    s.getUser().getEmail(),
                    s.getBusinessName(),
                    s.getAccountHolderName(),
                    s.getBankAccountNumber(),
                    s.getIfscCode(),
                    s.getBankName(),
                    s.getPanNumber(),
                    s.getPhone(),
                    s.getAddress(),
                    s.getStatus(),
                    s.getRejectionReason(),
                    s.getAppliedAt(),
                    s.getReviewedAt()
            );
        }
    }

    public record RejectRequest(String reason) {}

    public record SellerProfileResponse(
            Long id,
            String businessName,
            String penName,
            String bio,
            String avatarUrl,
            String accountHolderName,
            String bankAccountNumber,
            String ifscCode,
            String bankName,
            String upiId,
            String panNumber,
            String phone,
            String address,
            String status,
            BigDecimal totalEarnings,
            BigDecimal availableBalance,
            BigDecimal paidOutAmount,
            LocalDateTime appliedAt
    ) {
        public static SellerProfileResponse of(Seller s) {
            return new SellerProfileResponse(
                    s.getId(),
                    s.getBusinessName(),
                    s.getPenName(),
                    s.getBio(),
                    s.getAvatarUrl(),
                    s.getAccountHolderName(),
                    s.getBankAccountNumber(),
                    s.getIfscCode(),
                    s.getBankName(),
                    s.getUpiId(),
                    s.getPanNumber(),
                    s.getPhone(),
                    s.getAddress(),
                    s.getStatus(),
                    s.getTotalEarnings(),
                    s.getAvailableBalance(),
                    s.getPaidOutAmount(),
                    s.getAppliedAt()
            );
        }
    }

    public record PublisherAnalyticsResponse(
            BigDecimal grossRevenue,
            BigDecimal platformCommission3Percent,
            BigDecimal netAuthorEarnings97Percent,
            long totalUnitsSold,
            BigDecimal availableWalletBalance
    ) {}
}
