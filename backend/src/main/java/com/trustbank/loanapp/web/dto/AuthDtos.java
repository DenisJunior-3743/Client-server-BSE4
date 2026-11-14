package com.trustbank.loanapp.web.dto;

import com.trustbank.loanapp.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Request/reply messages for identity and profile endpoints. These records are the marshalling
 * contract (the "stubs" of our REST RPC): Jackson turns them into machine-independent JSON.
 *
 * <p>The validation rules repeat docs/PROJECT-PLAN.md §10. The clients check them first to save a
 * round trip; the server checks them again because a client can never be trusted.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public static final String PHONE_REGEX = "^(07\\d{8}|\\+2567\\d{8})$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters with a number and both cases";

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email(message = "Enter a valid email address") @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number") String phone,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password) {}

    /** {@code devOtp} is only ever filled when app.otp.dev-echo is on (local demos without an SMS gateway). */
    public record RegisterResponse(UserDto user, Instant otpExpiresAt, String devOtp) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {}

    public record OtpVerifyRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code") String code) {}

    public record OtpResendRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record OtpIssuedResponse(Instant otpExpiresAt, String devOtp) {}

    /** Public view of a user. There is deliberately no password field of any kind. */
    public record UserDto(UUID id, String email, String fullName, String phone, Role role, String branch,
            String avatarColor, Instant createdAt, boolean phoneVerified) {}

    public record UpdateMeRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number") String phone) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {}

    public record EmploymentInfo(
            @NotBlank @Size(max = 120) String employer,
            @NotBlank @Size(max = 80) String jobTitle,
            @NotBlank @Pattern(regexp = "FORMAL|SELF_EMPLOYED|INFORMAL") String employmentType) {}

    public record Address(
            @NotBlank @Size(max = 60) String district,
            @NotBlank @Size(max = 60) String city,
            @NotBlank @Size(max = 120) String street) {}

    public record ProfileRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{14}$", message = "National ID must be 14 characters")
            String nationalId,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull @Valid EmploymentInfo employmentInfo,
            @NotNull @Positive(message = "Enter a valid income amount") BigDecimal incomeMonthly,
            @NotNull @Valid Address address) {}

    public record ProfileDto(UUID id, UUID userId, String nationalId, LocalDate dateOfBirth,
            Map<String, String> employmentInfo, BigDecimal incomeMonthly, Map<String, String> address) {}

    public record StaffRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number") String phone,
            @NotNull Role role,
            @NotBlank @Size(max = 80) String branch) {}

    public record StaffCreateRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number") String phone,
            @NotNull Role role,
            @NotBlank @Size(max = 80) String branch,
            @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password) {
        public StaffRequest details() {
            return new StaffRequest(fullName, email, phone, role, branch);
        }
    }

    /** The account's existing email is the username; this request can change only its password. */
    public record StaffCredentialsRequest(
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password) {}

    /** Generated password is shown once; null when the admin supplied the initial password. */
    public record StaffCreatedResponse(UserDto user, String temporaryPassword) {}
}
