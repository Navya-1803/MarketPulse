package com.marketpulse.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static HttpStatus unauthorizedStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
