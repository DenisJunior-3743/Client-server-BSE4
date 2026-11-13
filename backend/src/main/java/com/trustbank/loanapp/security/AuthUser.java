package com.trustbank.loanapp.security;

import com.trustbank.loanapp.domain.Role;
import java.util.UUID;

/**
 * The authenticated caller, rebuilt from the JWT on every request. Nothing about the caller is kept
 * in server memory between requests (stateless server, Comer 2.3.6).
 */
public record AuthUser(UUID id, String email, Role role) {

    public boolean isStaff() {
        return role != Role.BORROWER;
    }
}
