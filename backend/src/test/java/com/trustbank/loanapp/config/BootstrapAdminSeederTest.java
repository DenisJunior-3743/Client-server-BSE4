package com.trustbank.loanapp.config;

import com.trustbank.loanapp.domain.*;
import com.trustbank.loanapp.repo.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BootstrapAdminSeederTest {
    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final BootstrapAdminSeeder seeder = new BootstrapAdminSeeder(users, encoder, jdbc, "admin@bank.com", "Admin123");
    @Test void createsVerifiedAdminWithHashedPassword() {
        when(users.findByEmail("admin@bank.com")).thenReturn(Optional.empty());
        when(encoder.encode("Admin123")).thenReturn("bcrypt-hash");
        seeder.run(null);
        verify(users).saveAndFlush(argThat(u -> u.getRole() == Role.ADMIN && u.isActive() && u.isPhoneVerified()
                && u.getEmail().equals("admin@bank.com") && u.getPasswordHash().equals("bcrypt-hash")));
        verify(jdbc).execute(contains("pg_advisory_xact_lock"));
    }
    @Test void restartNeverResetsExistingAdminPassword() {
        User admin = new User(); admin.setRole(Role.ADMIN); admin.setPasswordHash("changed-hash");
        when(users.findByEmail("admin@bank.com")).thenReturn(Optional.of(admin));
        seeder.run(null);
        assertEquals("changed-hash", admin.getPasswordHash());
        verifyNoInteractions(encoder);
        verify(users, never()).saveAndFlush(any());
    }
    @Test void restartDoesNotRecreateRenamedOrRemovedBootstrapAccountWhenAnotherAdminExists() {
        when(users.countByRoleIn(anyCollection())).thenReturn(1L);
        seeder.run(null);
        verify(users, never()).findByEmail(anyString());
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }
    @Test void neverPromotesAnExistingBorrower() {
        User borrower = new User(); borrower.setRole(Role.BORROWER);
        when(users.findByEmail("admin@bank.com")).thenReturn(Optional.of(borrower));
        assertThrows(IllegalStateException.class, () -> seeder.run(null));
        verify(users, never()).saveAndFlush(any());
    }
}
