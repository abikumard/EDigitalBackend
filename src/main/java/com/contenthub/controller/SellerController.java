package com.contenthub.controller;

import com.contenthub.dto.SellerDtos.SellerApplicationRequest;
import com.contenthub.dto.SellerDtos.SellerStatusResponse;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/apply")
    public ResponseEntity<SellerStatusResponse> apply(@Valid @RequestBody SellerApplicationRequest req,
                                                        Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        return ResponseEntity.ok(sellerService.apply(principal.getUserId(), req));
    }

    @GetMapping("/status")
    public ResponseEntity<SellerStatusResponse> status(Authentication authentication) {
        AuthPrincipal principal = requireUser(authentication);
        return ResponseEntity.ok(sellerService.myStatus(principal.getUserId()));
    }

    private AuthPrincipal requireUser(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal;
    }
}
