package com.contenthub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sellers")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "pen_name", length = 200)
    private String penName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "account_holder_name", nullable = false, length = 200)
    private String accountHolderName;

    @Column(name = "bank_account_number", nullable = false, length = 30)
    private String bankAccountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 15)
    private String ifscCode;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "pan_number", nullable = false, length = 15)
    private String panNumber;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "total_earnings", precision = 12, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "paid_out_amount", precision = 12, scale = 2)
    private BigDecimal paidOutAmount = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 12, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    // PENDING | APPROVED | REJECTED
    @Column(nullable = false, length = 20)
    private String status = "APPROVED";

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) this.status = "APPROVED";
        if (this.totalEarnings == null) this.totalEarnings = BigDecimal.ZERO;
        if (this.paidOutAmount == null) this.paidOutAmount = BigDecimal.ZERO;
        if (this.availableBalance == null) this.availableBalance = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPenName() { return penName != null ? penName : businessName; }
    public void setPenName(String penName) { this.penName = penName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getTotalEarnings() { return totalEarnings != null ? totalEarnings : BigDecimal.ZERO; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getPaidOutAmount() { return paidOutAmount != null ? paidOutAmount : BigDecimal.ZERO; }
    public void setPaidOutAmount(BigDecimal paidOutAmount) { this.paidOutAmount = paidOutAmount; }

    public BigDecimal getAvailableBalance() { return availableBalance != null ? availableBalance : BigDecimal.ZERO; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
