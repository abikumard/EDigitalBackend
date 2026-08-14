package com.contenthub.security;

public class AuthPrincipal {

    private final Long userId;
    private final String email;
    private final String role;

    public AuthPrincipal(Long userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
