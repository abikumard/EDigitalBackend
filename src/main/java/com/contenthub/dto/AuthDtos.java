package com.contenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record SignupRequest(
            @NotBlank(message = "Please enter your email or mobile number") String identifier,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be at least 8 characters") String password,
            @NotBlank(message = "Please confirm your password") String confirmPassword
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Please enter your email or mobile number") String identifier,
            @NotBlank(message = "Please enter your password") String password
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String email,
            String mobile,
            String name,
            String role
    ) {}
}
