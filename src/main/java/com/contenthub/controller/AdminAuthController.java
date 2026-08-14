package com.contenthub.controller;

import com.contenthub.dto.AdminDtos.AdminLoginRequest;
import com.contenthub.dto.AuthDtos.AuthResponse;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.JwtUtil;
import com.contenthub.security.LoginRateLimiter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final JwtUtil jwtUtil;
    private final LoginRateLimiter rateLimiter;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminAuthController(JwtUtil jwtUtil, LoginRateLimiter rateLimiter) {
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AdminLoginRequest req) {
        String rateLimitKey = "admin:" + req.email();
        rateLimiter.checkAllowed(rateLimitKey);

        if (!adminEmail.equalsIgnoreCase(req.email().trim()) || !adminPassword.equals(req.password())) {
            rateLimiter.recordFailure(rateLimitKey);
            throw new UnauthorizedException("Invalid admin email or password.");
        }

        rateLimiter.recordSuccess(rateLimitKey);
        String token = jwtUtil.generateAdminToken(adminEmail);
        return ResponseEntity.ok(new AuthResponse(token, null, adminEmail, null, "Admin", "ADMIN"));
    }
}
