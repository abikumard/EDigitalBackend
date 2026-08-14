package com.contenthub.controller;

import com.contenthub.dto.AuthDtos.AuthResponse;
import com.contenthub.dto.AuthDtos.LoginRequest;
import com.contenthub.dto.AuthDtos.SignupRequest;
import com.contenthub.dto.CommonDtos.MessageResponse;
import com.contenthub.entity.User;
import com.contenthub.exception.AppExceptions.UnauthorizedException;
import com.contenthub.security.JwtUtil;
import com.contenthub.security.LoginRateLimiter;
import com.contenthub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter rateLimiter;

    public AuthController(UserService userService, JwtUtil jwtUtil, LoginRateLimiter rateLimiter) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest req) {
        userService.signup(req.identifier(), req.password(), req.confirmPassword());
        return ResponseEntity.ok(new MessageResponse("Account created. Please log in to continue."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        String rateLimitKey = "user:" + req.identifier();
        rateLimiter.checkAllowed(rateLimitKey);
        try {
            User user = userService.authenticate(req.identifier(), req.password());
            rateLimiter.recordSuccess(rateLimitKey);
            String tokenSubject = user.getEmail() != null ? user.getEmail() : user.getMobile();
            String token = jwtUtil.generateUserToken(user.getId(), tokenSubject);
            return ResponseEntity.ok(new AuthResponse(
                    token, user.getId(), user.getEmail(), user.getMobile(), user.getName(), "USER"
            ));
        } catch (UnauthorizedException e) {
            rateLimiter.recordFailure(rateLimitKey);
            throw e;
        }
    }
}
