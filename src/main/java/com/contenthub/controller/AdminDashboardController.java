package com.contenthub.controller;

import com.contenthub.entity.PayoutRequest;
import com.contenthub.entity.Purchase;
import com.contenthub.repository.ContentItemRepository;
import com.contenthub.repository.PayoutRequestRepository;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.repository.SellerRepository;
import com.contenthub.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ContentItemRepository contentItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final PayoutRequestRepository payoutRequestRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    SellerRepository sellerRepository,
                                    ContentItemRepository contentItemRepository,
                                    PurchaseRepository purchaseRepository,
                                    PayoutRequestRepository payoutRequestRepository) {
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.contentItemRepository = contentItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.payoutRequestRepository = payoutRequestRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAdminMetrics() {
        long totalUsers = userRepository.count();
        long totalPublishers = sellerRepository.count();
        long totalBooks = contentItemRepository.count();

        List<Purchase> allPurchases = purchaseRepository.findAll();
        BigDecimal grossVolume = BigDecimal.ZERO;
        BigDecimal platform3PercentProfit = BigDecimal.ZERO;

        for (Purchase p : allPurchases) {
            if (p.getStatus() == Purchase.Status.SUCCESS) {
                grossVolume = grossVolume.add(p.getAmount());
                BigDecimal fee = p.getPlatformFee() != null ? p.getPlatformFee() : p.getAmount().multiply(new BigDecimal("0.03"));
                platform3PercentProfit = platform3PercentProfit.add(fee);
            }
        }

        List<PayoutRequest> pendingPayouts = payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalPublishers", totalPublishers,
                "totalBooks", totalBooks,
                "grossVolume", grossVolume,
                "platform3PercentProfit", platform3PercentProfit,
                "pendingPayoutCount", pendingPayouts.size()
        ));
    }

    @GetMapping("/payouts/pending")
    public ResponseEntity<List<PayoutRequest>> getPendingPayouts() {
        return ResponseEntity.ok(payoutRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING"));
    }

    @PostMapping("/payouts/{id}/approve")
    public ResponseEntity<Map<String, String>> approvePayout(
            @PathVariable Long id,
            @RequestParam(value = "transactionRef", required = false) String transactionRef) {
        PayoutRequest pr = payoutRequestRepository.findById(id).orElseThrow();
        pr.setStatus("PROCESSED");
        pr.setTransactionRef(transactionRef != null ? transactionRef : "BANK_TXN_" + System.currentTimeMillis());
        pr.setProcessedAt(LocalDateTime.now());
        payoutRequestRepository.save(pr);
        return ResponseEntity.ok(Map.of("message", "Payout approved and marked processed."));
    }
}
