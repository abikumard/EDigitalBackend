package com.contenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public class SellerDtos {

    public record SellerApplicationRequest(
            @NotBlank(message = "Business name is required") String businessName,
            @NotBlank(message = "Account holder name is required") String accountHolderName,
            @NotBlank(message = "Bank account number is required") String bankAccountNumber,
            @NotBlank(message = "IFSC code is required")
            @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Enter a valid IFSC code")
            String ifscCode,
            @NotBlank(message = "Bank name is required") String bankName,
            @NotBlank(message = "PAN number is required")
            @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Enter a valid PAN number")
            String panNumber,
            @NotBlank(message = "Phone number is required") String phone,
            @NotBlank(message = "Address is required") String address
    ) {}

    public record SellerStatusResponse(
            boolean hasApplied,
            String status,
            String businessName,
            String rejectionReason
    ) {}

    public record AdminSellerResponse(
            Long id,
            Long userId,
            String userEmail,
            String userMobile,
            String businessName,
            String accountHolderName,
            String bankAccountNumber,
            String ifscCode,
            String bankName,
            String panNumber,
            String phone,
            String address,
            String status,
            LocalDateTime appliedAt,
            LocalDateTime reviewedAt,
            String rejectionReason
    ) {}

    public record RejectRequest(String reason) {}
}
