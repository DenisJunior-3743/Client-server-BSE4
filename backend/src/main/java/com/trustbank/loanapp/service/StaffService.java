package com.trustbank.loanapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.AuditLog;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.event.AuditEvent;
import com.trustbank.loanapp.repo.AuditLogRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffCreatedResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.StaffRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UserDto;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.AuditLogDto;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin operations: staff accounts and the audit trail. */
@Service
public class StaffService {

    private static final Set<Role> STAFF_ROLES =
            EnumSet.of(Role.LOAN_OFFICER, Role.CREDIT_ANALYST, Role.BRANCH_MANAGER, Role.ADMIN);
    private static final String[] AVATAR_COLORS = {"#1E3A8A", "#0F766E", "#B45309", "#7C3AED", "#BE123C", "#0369A1"};
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public StaffService(UserRepository users, AuditLogRepository auditLogs, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events, ObjectMapper objectMapper) {
        this.users = users;
        this.auditLogs = auditLogs;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listStaff() {
        return users.findByActiveTrueAndRoleInOrderByCreatedAtAsc(STAFF_ROLES).stream().map(DtoMapper::user).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getStaff(UUID staffId) {
        return DtoMapper.user(requireActiveStaff(staffId));
    }

    @Transactional
    public StaffCreatedResponse createStaff(UUID adminId, StaffRequest request, String initialPassword) {
        requireActiveAdminForMutation(adminId);
        if (!STAFF_ROLES.contains(request.role())) {
            throw ApiException.rule("NOT_A_STAFF_ROLE", "Staff accounts cannot have the BORROWER role");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        String temporaryPassword = initialPassword == null ? temporaryPassword() : null;
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encodePassword(initialPassword == null ? temporaryPassword : initialPassword));
        user.setRole(request.role());
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setBranch(request.branch().trim());
        user.setPhoneVerified(true);
        user.setAvatarColor(AVATAR_COLORS[Math.floorMod(email.hashCode(), AVATAR_COLORS.length)]);
        user = users.save(user);
        events.publishEvent(new AuditEvent("USER", user.getId(), "STAFF_CREATED", adminId,
                Map.of("role", user.getRole().name(), "email", user.getEmail())));
        return new StaffCreatedResponse(DtoMapper.user(user), temporaryPassword);
    }

    @Transactional
    public void setStaffCredentials(UUID adminId, UUID staffId, String password) {
        requireActiveAdminForMutation(adminId);
        User user = requireActiveStaff(staffId);
        user.setPasswordHash(encodePassword(password));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        events.publishEvent(new AuditEvent("USER", staffId, "STAFF_PASSWORD_CHANGED", adminId,
                Map.of("email", user.getEmail())));
    }

    private String encodePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.badRequest("PASSWORD_TOO_LONG", "Password must be no more than 72 UTF-8 bytes");
        }
        return passwordEncoder.encode(password);
    }

    @Transactional
    public UserDto updateStaff(UUID adminId, UUID staffId, StaffRequest request) {
        requireActiveAdminForMutation(adminId);
        User user = requireActiveStaff(staffId);
        if (!STAFF_ROLES.contains(request.role())) {
            throw ApiException.rule("NOT_A_STAFF_ROLE", "Staff accounts cannot have the BORROWER role");
        }
        if (adminId.equals(staffId) && request.role() != Role.ADMIN) {
            throw ApiException.conflict("SELF_ROLE_CHANGE", "You cannot remove your own administrator role");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setBranch(request.branch().trim());
        // Password and past workflow/audit references remain unchanged.
        events.publishEvent(new AuditEvent("USER", staffId, "STAFF_UPDATED", adminId,
                Map.of("role", user.getRole().name(), "email", user.getEmail())));
        return DtoMapper.user(user);
    }

    @Transactional
    public void deleteStaff(UUID adminId, UUID staffId) {
        requireActiveAdminForMutation(adminId);
        if (adminId.equals(staffId)) {
            throw ApiException.conflict("SELF_DELETE", "You cannot delete your own administrator account");
        }
        User user = requireStaff(staffId);
        if (!user.isActive()) return; // DELETE is safe to repeat.
        // Retain the identity referenced by loans, decisions and audit rows; revoke access instead.
        user.setActive(false);
        events.publishEvent(new AuditEvent("USER", staffId, "STAFF_DELETED", adminId,
                Map.of("role", user.getRole().name(), "email", user.getEmail())));
    }

    private void requireActiveAdminForMutation(UUID adminId) {
        if (users.findActiveByRoleForUpdate(Role.ADMIN).stream().noneMatch(admin -> admin.getId().equals(adminId))) {
            throw ApiException.forbidden("An active administrator account is required");
        }
    }

    private User requireStaff(UUID staffId) {
        return users.findById(staffId).filter(user -> STAFF_ROLES.contains(user.getRole()))
                .orElseThrow(() -> ApiException.notFound("Staff member"));
    }

    private User requireActiveStaff(UUID staffId) {
        User user = requireStaff(staffId);
        if (!user.isActive()) throw ApiException.notFound("Staff member");
        return user;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> auditLog(int limit) {
        List<AuditLog> rows = auditLogs.findAllByOrderByPerformedAtDesc(
                PageRequest.of(0, Math.max(1, Math.min(limit, 500))));
        Map<UUID, String> names = users
                .findAllById(rows.stream().map(AuditLog::getPerformedBy).filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getFullName));
        return rows.stream()
                .map(row -> new AuditLogDto(row.getId(), row.getEntityType(), row.getEntityId(), row.getAction(),
                        row.getPerformedBy(),
                        row.getPerformedBy() == null ? "System" : names.getOrDefault(row.getPerformedBy(), "Unknown"),
                        row.getPerformedAt(), describe(row.getDetails())))
                .toList();
    }

    private String describe(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return "";
        }
    }

    /** 12 characters with at least one upper, one lower and one digit (meets the password rule). */
    private String temporaryPassword() {
        StringBuilder sb = new StringBuilder("Tb7x");
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
