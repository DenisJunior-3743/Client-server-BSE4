package com.trustbank.loanapp.service;

import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.domain.VerificationCode;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.repo.VerificationCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time verification codes with a FIRM deadline (real-time transactions lecture): a correct code
 * presented after {@code expires_at} is rejected, because a late answer has zero value.
 */
@Service
public class OtpService {

    public enum Outcome { VERIFIED, WRONG_CODE, EXPIRED, TOO_MANY_ATTEMPTS, NO_CODE }

    public record Issued(String code, Instant expiresAt) {}

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final VerificationCodeRepository codes;
    private final UserRepository users;
    private final AppProperties.Otp props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(VerificationCodeRepository codes, UserRepository users, AppProperties props, Clock clock) {
        this.codes = codes;
        this.users = users;
        this.props = props.otp();
        this.clock = clock;
    }

    @Transactional
    public Issued issue(User user) {
        // Any earlier unused code is retired, so only the newest one can ever succeed.
        codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(),
                VerificationCode.PHONE_VERIFICATION).ifPresent(old -> old.setConsumedAt(clock.instant()));

        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant now = clock.instant();
        VerificationCode row = new VerificationCode();
        row.setUserId(user.getId());
        row.setPurpose(VerificationCode.PHONE_VERIFICATION);
        row.setCodeHash(hash(user.getId(), code));
        row.setCreatedAt(now);
        row.setExpiresAt(now.plus(props.ttl()));
        codes.save(row);

        // SMS gateway stub. The code itself is only logged in dev-echo mode.
        if (props.devEcho()) {
            log.info("[dev] verification code for {} is {}", user.getPhone(), code);
        } else {
            log.info("Verification code sent to phone ending {}", lastDigits(user.getPhone()));
        }
        return new Issued(code, row.getExpiresAt());
    }

    /**
     * Returns an outcome instead of throwing, so the attempt counter is committed even when the
     * answer is "wrong code".
     */
    @Transactional
    public Outcome verify(UUID userId, String code) {
        Optional<VerificationCode> latest = codes.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                userId, VerificationCode.PHONE_VERIFICATION);
        if (latest.isEmpty()) {
            return Outcome.NO_CODE;
        }
        VerificationCode row = latest.get();
        Instant now = clock.instant();
        if (!now.isBefore(row.getExpiresAt())) {
            return Outcome.EXPIRED;                       // firm deadline
        }
        if (row.getAttempts() >= props.maxAttempts()) {
            return Outcome.TOO_MANY_ATTEMPTS;
        }
        row.setAttempts(row.getAttempts() + 1);
        boolean matches = MessageDigest.isEqual(
                row.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(userId, code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return Outcome.WRONG_CODE;
        }
        row.setConsumedAt(now);
        users.findById(userId).ifPresent(user -> user.setPhoneVerified(true));
        return Outcome.VERIFIED;
    }

    public boolean devEcho() {
        return props.devEcho();
    }

    private static String hash(UUID userId, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((userId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String lastDigits(String phone) {
        return phone == null || phone.length() < 3 ? "***" : phone.substring(phone.length() - 3);
    }
}
