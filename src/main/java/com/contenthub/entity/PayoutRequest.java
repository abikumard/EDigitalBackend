package com.contenthub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_requests")
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_method", length = 50)
    private String payoutMethod = "BANK_TRANSFER"; // BANK_TRANSFER, UPI

    @Column(name = "payout_details", length = 300)
    private String payoutDetails;

    // PENDING, PROCESSED, REJECTED
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "transaction_ref", length = 150)
    private String transactionRef;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPayoutMethod() { return payoutMethod; }
    public void setPayoutMethod(String payoutMethod) { this.payoutMethod = payoutMethod; }

    public String getPayoutDetails() { return payoutDetails; }
    public void setPayoutDetails(String payoutDetails) { this.payoutDetails = payoutDetails; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
