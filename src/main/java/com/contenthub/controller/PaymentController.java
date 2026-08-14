package com.contenthub.controller;

import com.contenthub.dto.PaymentDtos.CreateCartOrderResponse;
import com.contenthub.dto.PaymentDtos.CreateOrderRequest;
import com.contenthub.dto.PaymentDtos.CreateOrderResponse;
import com.contenthub.dto.PaymentDtos.VerifyCartPaymentRequest;
import com.contenthub.dto.PaymentDtos.VerifyCartPaymentResponse;
import com.contenthub.dto.PaymentDtos.VerifyPaymentRequest;
import com.contenthub.dto.PaymentDtos.VerifyPaymentResponse;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.AuthPrincipal;
import com.contenthub.security.CurrentUser;
import com.contenthub.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest req, Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(paymentService.createOrder(userId, req.contentId()));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPaymentResponse> verify(@Valid @RequestBody VerifyPaymentRequest req, Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(paymentService.verifyPayment(userId, req));
    }

    @PostMapping("/create-cart-order")
    public ResponseEntity<CreateCartOrderResponse> createCartOrder(Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(paymentService.createCartOrder(userId));
    }

    @PostMapping("/verify-cart")
    public ResponseEntity<VerifyCartPaymentResponse> verifyCart(@Valid @RequestBody VerifyCartPaymentRequest req, Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(paymentService.verifyCartPayment(userId, req));
    }

    // Razorpay calls this server-to-server; verified via X-Razorpay-Signature, not JWT.
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawPayload,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId(Authentication authentication) {
        AuthPrincipal principal = CurrentUser.from(authentication);
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Please log in to continue.");
        }
        return principal.getUserId();
    }
}
