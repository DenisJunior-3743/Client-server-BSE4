package com.trustbank.loanapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.event.AuditEvent;
import com.trustbank.loanapp.repo.AuditLogRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StaffServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final StaffService service = new StaffService(users, mock(AuditLogRepository.class), encoder, events, new ObjectMapper());
    final UUID adminId = UUID.randomUUID(), staffId = UUID.randomUUID();
    User admin, member;

    @BeforeEach void setup() {
        admin = user(adminId, Role.ADMIN);
        member = user(staffId, Role.LOAN_OFFICER);
        when(users.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(List.of(admin));
        when(users.findById(adminId)).thenReturn(Optional.of(admin));
        when(users.findById(staffId)).thenReturn(Optional.of(member));
    }

    private User user(UUID id, Role role) {
        User user = new User();
        user.setId(id); user.setRole(role); user.setFullName("Original Name");
        user.setEmail(id + "@bank.com"); user.setPhone("0771234567");
        user.setBranch("Main"); user.setPasswordHash("existing-hash");
        return user;
    }

    private StaffRequest request(Role role) {
        return new StaffRequest(" Updated Name ", " UPDATED@bank.com ", "0781234567", role, " New Branch ");
    }

    @Test void directoryUsesOnlyActiveStaff() {
        when(users.findByActiveTrueAndRoleInOrderByCreatedAtAsc(anyCollection())).thenReturn(List.of(member));
        assertEquals(staffId, service.listStaff().getFirst().id());
        verify(users, never()).findByRoleInOrderByCreatedAtAsc(anyCollection());
    }

    @Test void editsDetailsAndRoleWithoutResettingPassword() {
        var updated = service.updateStaff(adminId, staffId, request(Role.CREDIT_ANALYST));
        assertEquals("Updated Name", updated.fullName());
        assertEquals("updated@bank.com", updated.email());
        assertEquals("New Branch", updated.branch());
        assertEquals(Role.CREDIT_ANALYST, updated.role());
        assertEquals("0781234567", updated.phone());
        assertEquals("existing-hash", member.getPasswordHash());
        verifyNoInteractions(encoder);
        verify(events).publishEvent(argThat((Object event) -> event instanceof AuditEvent audit
                && audit.action().equals("STAFF_UPDATED") && audit.entityId().equals(staffId)));
    }

    @Test void duplicateEmailDoesNotChangeTheAccount() {
        when(users.existsByEmail("updated@bank.com")).thenReturn(true);
        assertEquals("EMAIL_TAKEN", assertThrows(ApiException.class,
                () -> service.updateStaff(adminId, staffId, request(Role.ADMIN))).getCode());
        assertEquals(Role.LOAN_OFFICER, member.getRole());
        assertEquals("Original Name", member.getFullName());
        verifyNoInteractions(events);
    }

    @Test void unchangedEmailIsAllowed() {
        var input = new StaffRequest("New Name", member.getEmail(), member.getPhone(), member.getRole(), "Main");
        service.updateStaff(adminId, staffId, input);
        verify(users, never()).existsByEmail(anyString());
    }

    @Test void adminCanEditOwnDetailsButCannotDemoteOrDeleteSelf() {
        service.updateStaff(adminId, adminId, request(Role.ADMIN));
        assertEquals("SELF_ROLE_CHANGE", assertThrows(ApiException.class,
                () -> service.updateStaff(adminId, adminId, request(Role.LOAN_OFFICER))).getCode());
        assertEquals("SELF_DELETE", assertThrows(ApiException.class,
                () -> service.deleteStaff(adminId, adminId)).getCode());
        assertTrue(admin.isActive());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test void cannotAssignBorrowerRoleThroughStaffApi() {
        assertEquals("NOT_A_STAFF_ROLE", assertThrows(ApiException.class,
                () -> service.updateStaff(adminId, staffId, request(Role.BORROWER))).getCode());
    }

    @Test void borrowerIdentityCannotBeEditedOrDeletedAsStaff() {
        member.setRole(Role.BORROWER);
        assertEquals(404, assertThrows(ApiException.class, () -> service.getStaff(staffId)).getStatus().value());
        assertThrows(ApiException.class, () -> service.updateStaff(adminId, staffId, request(Role.ADMIN)));
        assertThrows(ApiException.class, () -> service.deleteStaff(adminId, staffId));
        assertTrue(member.isActive());
        verifyNoInteractions(events);
    }

    @Test void deletionRevokesAccessAndKeepsIdentityAndHistoryReferences() {
        service.deleteStaff(adminId, staffId);
        assertFalse(member.isActive());
        assertEquals(staffId, member.getId());
        assertEquals("existing-hash", member.getPasswordHash());
        service.deleteStaff(adminId, staffId);
        verify(events, times(1)).publishEvent(argThat((Object event) -> event instanceof AuditEvent audit
                && audit.action().equals("STAFF_DELETED")));
        verify(users, never()).delete(any());
        verify(users, never()).deleteById(any());
    }

    @Test void deletedStaffCannotBeReadOrEdited() {
        member.setActive(false);
        assertEquals(404, assertThrows(ApiException.class, () -> service.getStaff(staffId)).getStatus().value());
        assertEquals(404, assertThrows(ApiException.class,
                () -> service.updateStaff(adminId, staffId, request(Role.ADMIN))).getStatus().value());
    }

    @Test void administratorRevokedWhileWaitingForLockCannotMutateAccounts() {
        when(users.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(List.of());
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.updateStaff(adminId, staffId, request(Role.ADMIN))).getStatus().value());
        assertThrows(ApiException.class, () -> service.deleteStaff(adminId, staffId));
        assertThrows(ApiException.class, () -> service.createStaff(adminId, request(Role.ADMIN), null));
        assertTrue(member.isActive());
        verifyNoInteractions(events);
    }

    @Test void credentialsChangeOnlyPasswordAndInvalidateEarlierSessions() {
        String email = member.getEmail();
        when(encoder.encode("NewPassword123")).thenReturn("new-bcrypt-hash");
        member.setCredentialVersion(4);
        service.setStaffCredentials(adminId, staffId, "NewPassword123");
        assertEquals("new-bcrypt-hash", member.getPasswordHash());
        assertEquals(5, member.getCredentialVersion());
        assertEquals(email, member.getEmail());
        assertEquals("Original Name", member.getFullName());
        assertEquals(Role.LOAN_OFFICER, member.getRole());
        assertEquals("Main", member.getBranch());
        verify(events).publishEvent(argThat((Object event) -> event instanceof AuditEvent audit
                && audit.action().equals("STAFF_PASSWORD_CHANGED")
                && audit.details().equals(java.util.Map.of("email", email))));
    }

    @Test void credentialsCannotBeSetForBorrowerDeletedOrByRevokedAdmin() {
        member.setRole(Role.BORROWER);
        assertThrows(ApiException.class, () -> service.setStaffCredentials(adminId, staffId, "NewPassword123"));
        member.setRole(Role.LOAN_OFFICER); member.setActive(false);
        assertThrows(ApiException.class, () -> service.setStaffCredentials(adminId, staffId, "NewPassword123"));
        member.setActive(true);
        when(users.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(List.of());
        assertThrows(ApiException.class, () -> service.setStaffCredentials(adminId, staffId, "NewPassword123"));
        verifyNoInteractions(encoder, events);
    }

    @Test void bcryptByteLimitRejectsMultibytePasswordsWithoutChangingCredentials() {
        String tooManyBytes = "Aa1" + "é".repeat(35);
        assertEquals("PASSWORD_TOO_LONG", assertThrows(ApiException.class,
                () -> service.setStaffCredentials(adminId, staffId, tooManyBytes)).getCode());
        assertEquals("existing-hash", member.getPasswordHash());
        assertEquals(0, member.getCredentialVersion());
        verifyNoInteractions(encoder, events);
    }

    @Test void creationAcceptsChosenPasswordWithoutEchoingIt() {
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode("ChosenPassword123")).thenReturn("chosen-hash");
        var result = service.createStaff(adminId, request(Role.LOAN_OFFICER), "ChosenPassword123");
        assertNull(result.temporaryPassword());
        assertEquals("updated@bank.com", result.user().email());
        verify(users).save(argThat(user -> user.getPasswordHash().equals("chosen-hash")));
    }

    @Test void creationStillGeneratesPasswordWhenOmitted() {
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode(anyString())).thenReturn("generated-hash");
        var result = service.createStaff(adminId, request(Role.LOAN_OFFICER), null);
        assertNotNull(result.temporaryPassword());
        assertTrue(result.temporaryPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$"));
        verify(encoder).encode(result.temporaryPassword());
    }

    @Test void missingStaffReturnsNotFound() {
        when(users.findById(staffId)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ApiException.class, () -> service.deleteStaff(adminId, staffId)).getStatus().value());
    }
}
