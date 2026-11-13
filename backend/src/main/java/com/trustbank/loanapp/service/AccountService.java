package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.Notification;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.ApplicantProfileRepository;
import com.trustbank.loanapp.repo.NotificationRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.web.dto.AuthDtos.ChangePasswordRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.ProfileDto;
import com.trustbank.loanapp.web.dto.AuthDtos.ProfileRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UpdateMeRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.UserDto;
import com.trustbank.loanapp.web.dto.DtoMapper;
import com.trustbank.loanapp.web.dto.LoanDtos.NotificationDto;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "My account" operations: own user record, KYC profile and notifications. */
@Service
public class AccountService {

    private final UserRepository users;
    private final ApplicantProfileRepository profiles;
    private final NotificationRepository notifications;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(UserRepository users, ApplicantProfileRepository profiles,
            NotificationRepository notifications, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return DtoMapper.user(requireUser(userId));
    }

    @Transactional
    public UserDto updateMe(UUID userId, UpdateMeRequest request) {
        User user = requireUser(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        return DtoMapper.user(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Current password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(UUID userId) {
        return profiles.findByUserId(userId)
                .map(p -> DtoMapper.profile(p, false))
                .orElseThrow(() -> ApiException.notFound("Profile"));
    }

    @Transactional
    public ProfileDto saveProfile(UUID userId, ProfileRequest request) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (request.dateOfBirth().isAfter(today.minusYears(18))) {
            throw ApiException.rule("UNDERAGE", "You must be at least 18 years old");
        }
        String nin = request.nationalId().toUpperCase(Locale.ROOT);
        if (profiles.existsByNationalIdAndUserIdNot(nin, userId)) {
            throw ApiException.conflict("NIN_TAKEN", "This National ID is already registered");
        }
        ApplicantProfile profile = profiles.findByUserId(userId).orElseGet(() -> {
            ApplicantProfile created = new ApplicantProfile();
            created.setUserId(userId);
            return created;
        });
        profile.setNationalId(nin);
        profile.setDateOfBirth(request.dateOfBirth());
        Map<String, String> employment = new LinkedHashMap<>();
        employment.put("employer", request.employmentInfo().employer());
        employment.put("jobTitle", request.employmentInfo().jobTitle());
        employment.put("employmentType", request.employmentInfo().employmentType());
        profile.setEmploymentInfo(employment);
        profile.setIncomeMonthly(request.incomeMonthly());
        Map<String, String> address = new LinkedHashMap<>();
        address.put("district", request.address().district());
        address.put("city", request.address().city());
        address.put("street", request.address().street());
        profile.setAddress(address);
        profile.setUpdatedAt(clock.instant());
        return DtoMapper.profile(profiles.save(profile), false);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> notifications(UUID userId) {
        return notifications.findByUserIdOrderBySentAtDesc(userId, PageRequest.of(0, 100)).stream()
                .map(DtoMapper::notification)
                .toList();
    }

    @Transactional
    public NotificationDto markRead(UUID userId, UUID notificationId) {
        // Looked up by (id, owner): one user can never touch another user's notification.
        Notification notification = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(clock.instant());
        }
        return DtoMapper.notification(notification);
    }

    private User requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }
}
