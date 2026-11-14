package com.trustbank.loanapp.security;

import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {
    final JwtService jwt = mock(JwtService.class);
    final UserRepository users = mock(UserRepository.class);
    final JwtAuthFilter filter = new JwtAuthFilter(jwt, users);
    final UUID id = UUID.randomUUID();

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void request() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/staff");
        request.addHeader("Authorization", "Bearer signed-token");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    }

    @Test void oldTokenUsesCurrentRoleAndEmail() throws Exception {
        when(jwt.verify("signed-token")).thenReturn(Optional.of(new AuthUser(id, "old@bank.com", Role.ADMIN)));
        User user = new User(); user.setId(id); user.setEmail("new@bank.com"); user.setRole(Role.LOAN_OFFICER);
        when(users.findById(id)).thenReturn(Optional.of(user));
        request();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(new AuthUser(id, "new@bank.com", Role.LOAN_OFFICER), authentication.getPrincipal());
        assertEquals("ROLE_LOAN_OFFICER", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test void deletedAccountCannotUseAnUnexpiredToken() throws Exception {
        when(jwt.verify("signed-token")).thenReturn(Optional.of(new AuthUser(id, "staff@bank.com", Role.ADMIN)));
        User user = new User(); user.setId(id); user.setActive(false); user.setRole(Role.ADMIN);
        when(users.findById(id)).thenReturn(Optional.of(user));
        request();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void missingAccountCannotUseAnUnexpiredToken() throws Exception {
        when(jwt.verify("signed-token")).thenReturn(Optional.of(new AuthUser(id, "staff@bank.com", Role.ADMIN)));
        when(users.findById(id)).thenReturn(Optional.empty());
        request();
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void invalidTokenDoesNotQueryAccounts() throws Exception {
        when(jwt.verify("signed-token")).thenReturn(Optional.empty());
        request();
        verifyNoInteractions(users);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
