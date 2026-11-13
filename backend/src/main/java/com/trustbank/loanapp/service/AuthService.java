package com.trustbank.loanapp.service;

import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.event.AuditEvent;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.security.JwtService;
import com.trustbank.loanapp.web.dto.AuthDtos.LoginRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.LoginResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.OtpIssuedResponse;
import com.trustbank.loanapp.web.dto.AuthDtos.RegisterRequest;
import com.trustbank.loanapp.web.dto.AuthDtos.RegisterResponse;
import com.trustbank.loanapp.web.dto.DtoMapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Authentication duty. Stateless: a successful login hands the client a signed token and the server
 * remembers nothing.
 *
 * <p>Password hashing (BCrypt, deliberately slow) is done OUTSIDE any database transaction, so a
 * pooled connection is never held while the CPU grinds.
 */
@Service
public class AuthService {

    private static final String[] AVATAR_COLORS = {"#1E3A8A", "#0F766E", "#B45309", "#7C3AED", "#BE123C", "#0369A1"};

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final OtpService otp;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt, OtpService otp,
            ApplicationEventPublisher events, TransactionTemplate tx) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.otp = otp;
        this.events = events;
        this.tx = tx;
        this.dummyHash = passwordEncoder.encode("no-such-account");
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        String passwordHash = passwordEncoder.encode(request.password());   // slow, no connection held

        return tx.execute(status -> {
            if (users.existsByEmail(email)) {
                throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
            }
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRole(Role.BORROWER);
            user.setFullName(request.fullName().trim());
            user.setPhone(request.phone());
            user.setAvatarColor(AVATAR_COLORS[Math.floorMod(email.hashCode(), AVATAR_COLORS.length)]);
            user = users.save(user);

            OtpService.Issued issued = otp.issue(user);
            events.publishEvent(new AuditEvent("USER", user.getId(), "BORROWER_REGISTERED", user.getId(), Map.of()));
            return new RegisterResponse(DtoMapper.user(user), issued.expiresAt(), otp.devEcho() ? issued.code() : null);
        });
    }

    public OtpIssuedResponse resendOtp(String rawEmail) {
        // Same reply whether or not the account exists: do not reveal who has an account.
        return users.findByEmail(normalise(rawEmail))
                .filter(user -> !user.isPhoneVerified())
                .map(otp::issue)
                .map(issued -> new OtpIssuedResponse(issued.expiresAt(), otp.devEcho() ? issued.code() : null))
                .orElseGet(() -> new OtpIssuedResponse(null, null));
    }

    public void verifyOtp(String rawEmail, String code) {
        User user = users.findByEmail(normalise(rawEmail))
                .orElseThrow(() -> ApiException.badRequest("INVALID_CODE", "The code is not correct"));
        switch (otp.verify(user.getId(), code)) {
            case VERIFIED -> { }
            case EXPIRED -> throw ApiException.deadlineExpired("This code has expired. Request a new one.");
            case TOO_MANY_ATTEMPTS -> throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Too many wrong attempts. Request a new code.");
            case WRONG_CODE, NO_CODE -> throw ApiException.badRequest("INVALID_CODE", "The code is not correct");
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmail(normalise(request.email())).orElse(null);
        // Always run BCrypt, even for an unknown email, so timing does not reveal which emails exist.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);
        if (user == null || !passwordOk || !user.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        if (user.getRole() == Role.BORROWER && !user.isPhoneVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PHONE_NOT_VERIFIED",
                    "Verify your phone number with the code we sent before signing in");
        }
        events.publishEvent(new AuditEvent("USER", user.getId(), "LOGIN", user.getId(), Map.of()));
        return new LoginResponse(jwt.issue(user), "Bearer", jwt.ttlSeconds(), DtoMapper.user(user));
    }

    static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
