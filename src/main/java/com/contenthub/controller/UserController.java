package com.contenthub.controller;

import com.contenthub.dto.ContentDtos.ContentResponse;
import com.contenthub.entity.Purchase;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.repository.PurchaseRepository;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final PurchaseRepository purchaseRepository;

    public UserController(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @GetMapping("/purchases")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ContentResponse>> myPurchases(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        List<ContentResponse> items = purchaseRepository
                .findByUser_IdAndStatusOrderByCreatedAtDesc(principal.getUserId(), Purchase.Status.SUCCESS)
                .stream()
                .map(p -> ContentResponse.of(p.getContent(), true, p.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(items);
    }
}
