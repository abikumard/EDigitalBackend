package com.contenthub.security;

import org.springframework.security.core.Authentication;

public class CurrentUser {

    public static AuthPrincipal from(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal ap) {
            return ap;
        }
        return null; // anonymous / unauthenticated request
    }
}
