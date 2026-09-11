package com.contenthub.controller;

import com.contenthub.dto.PayoutDtos.PayoutResponse;
import com.contenthub.dto.PayoutDtos.RequestPayoutRequest;
import com.contenthub.dto.SellerDtos.PublisherAnalyticsResponse;
import com.contenthub.dto.SellerDtos.SellerApplyRequest;
import com.contenthub.dto.SellerDtos.SellerProfileResponse;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.SellerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publisher")
public class PublisherPayoutController {

    private final SellerService sellerService;

    public PublisherPayoutController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/profile")
    public ResponseEntity<SellerProfileResponse> getProfile(Authentication authentication) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(sellerService.getProfile(currentUserId));
    }

    @PostMapping("/profile")
    public ResponseEntity<SellerProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody SellerApplyRequest req) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(sellerService.updateProfile(currentUserId, req));
    }

    @GetMapping("/analytics")
    public ResponseEntity<PublisherAnalyticsResponse> getAnalytics(Authentication authentication) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(sellerService.getAnalytics(currentUserId));
    }

    @PostMapping("/payout/request")
    public ResponseEntity<PayoutResponse> requestPayout(
            Authentication authentication,
            @RequestBody RequestPayoutRequest req) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(sellerService.requestPayout(currentUserId, req));
    }

    @GetMapping("/payout/history")
    public ResponseEntity<List<PayoutResponse>> getPayoutHistory(Authentication authentication) {
        Long currentUserId = requireUserId(authentication);
        return ResponseEntity.ok(sellerService.getPayoutHistory(currentUserId));
    }

    private Long requireUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal.getUserId();
    }
}
