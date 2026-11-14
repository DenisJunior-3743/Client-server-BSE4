package com.trustbank.loanapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.*;
import com.trustbank.loanapp.service.AccountService;
import com.trustbank.loanapp.service.StaffService;
import com.trustbank.loanapp.web.dto.AuthDtos.ChangePasswordRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CredentialLifecycleTest {
    static final String SECRET = "local-test-signing-secret-at-least-32-bytes";
    final UserRepository users = mock(UserRepository.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final JwtService jwt = new JwtService(new AppProperties(
            new AppProperties.Jwt(SECRET, Duration.ofHours(1), "test"),
            null, null, null, null, null, null, null, false), Clock.systemUTC());
    final JwtAuthFilter filter = new JwtAuthFilter(jwt, users);
    final UUID adminId = UUID.randomUUID();
    final User member = new User();
    final StaffService staff = new StaffService(users, mock(AuditLogRepository.class), encoder,
            mock(ApplicationEventPublisher.class), new ObjectMapper());

    CredentialLifecycleTest() {
        User admin = new User(); admin.setId(adminId); admin.setRole(Role.ADMIN);
        member.setId(UUID.randomUUID()); member.setRole(Role.LOAN_OFFICER);
        member.setEmail("staff@bank.com"); member.setPasswordHash(encoder.encode("Original123"));
        when(users.findById(member.getId())).thenReturn(Optional.of(member));
        when(users.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(admin));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    Authentication authenticate(String token) throws Exception {
        SecurityContextHolder.clearContext();
        var request = new MockHttpServletRequest("GET", "/api/v1/me");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test void resetInvalidatesOldTokensAndNewPasswordProducesAcceptedTokens() throws Exception {
        String before = jwt.issue(member);
        assertNotNull(authenticate(before));
        staff.setStaffCredentials(adminId, member.getId(), "Replacement123");
        assertTrue(encoder.matches("Replacement123", member.getPasswordHash()));
        assertFalse(encoder.matches("Original123", member.getPasswordHash()));
        assertNotEquals("Replacement123", member.getPasswordHash());
        assertNull(authenticate(before));
        String after = jwt.issue(member);
        assertEquals(1, jwt.verify(after).orElseThrow().credentialVersion());
        assertNotNull(authenticate(after));
    }

    @Test void preMigrationTokensWorkOnlyUntilFirstPasswordChange() throws Exception {
        String legacy = Jwts.builder().issuer("test").subject(member.getId().toString())
                .claim("email", member.getEmail()).claim("role", member.getRole().name())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertEquals(0, jwt.verify(legacy).orElseThrow().credentialVersion());
        assertNotNull(authenticate(legacy));
        staff.setStaffCredentials(adminId, member.getId(), "Replacement123");
        assertNull(authenticate(legacy));
    }

    @Test void existingSelfServicePasswordChangeAlsoRevokesEarlierTokens() throws Exception {
        String before = jwt.issue(member);
        var accounts = new AccountService(users, mock(ApplicantProfileRepository.class),
                mock(NotificationRepository.class), encoder, Clock.systemUTC());
        accounts.changePassword(member.getId(), new ChangePasswordRequest("Original123", "Replacement123"));
        assertNull(authenticate(before));
        assertNotNull(authenticate(jwt.issue(member)));
    }
}
