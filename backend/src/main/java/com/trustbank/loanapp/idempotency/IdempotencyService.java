package com.trustbank.loanapp.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.common.ApiException;
import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.IdempotencyRecord;
import com.trustbank.loanapp.repo.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * At-most-once invocation semantics for money-moving requests (BIT2216 fault-tolerance table):
 *
 * <pre>
 *   retransmit request  -> the client retries with the SAME Idempotency-Key
 *   duplicate filtering -> UNIQUE (user_id, idem_key) in idempotency_records
 *   retransmit reply    -> the stored JSON reply is returned, the action is NOT run again
 * </pre>
 *
 * <p>The key row, the business change and the cached reply are written in ONE database transaction.
 * So there are exactly two outcomes, even if the server crashes halfway:
 * <ul>
 *   <li>commit: the loan exists and its reply is cached. Any retry gets that reply.</li>
 *   <li>rollback: nothing exists, the key is free. A retry runs the action for the first time.</li>
 * </ul>
 * There is no state in which the work was done but the server has forgotten it (which is what would
 * create a duplicate loan), and none in which the key is burnt but the work was never done.
 *
 * <p>This class has no mutable fields: all per-request data lives in local variables, so concurrent
 * request threads cannot see each other's values.
 */
@Service
public class IdempotencyService {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private static final Pattern KEY_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{16,100}$");

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final AppProperties props;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper objectMapper, AppProperties props,
            Clock clock) {
        this.records = records;
        this.objectMapper = objectMapper;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Runs {@code action} at most once for this (user, key).
     *
     * @param operation name of the endpoint, so a key cannot be replayed against a different operation
     * @param request   the request payload; its hash detects "same key, different request"
     */
    @Transactional
    public <T> IdempotentResult<T> execute(UUID userId, String key, String operation, Object request,
            Class<T> replyType, Supplier<T> action) {
        if (key == null || !KEY_FORMAT.matcher(key).matches()) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be 16-100 characters (letters, digits, '-' or '_'); a UUID works well");
        }
        String requestHash = sha256(operation + "\n" + toJson(request));
        Instant now = clock.instant();

        int claimed = records.claim(UUID.randomUUID(), userId, key, operation, requestHash, now,
                now.plus(props.idempotency().ttl()));

        if (claimed == 0) {
            // Duplicate: the first request already committed (otherwise claim() would have inserted).
            IdempotencyRecord existing = records.findByUserIdAndIdemKey(userId, key)
                    .orElseThrow(() -> ApiException.conflict("IDEMPOTENCY_RETRY", "Please retry the request"));
            if (!existing.getOperation().equals(operation) || !existing.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                        "This Idempotency-Key was already used for a different request");
            }
            if (existing.getResponseBody() == null) {
                throw ApiException.conflict("IDEMPOTENCY_RETRY", "Please retry the request");
            }
            return new IdempotentResult<>(fromJson(existing.getResponseBody(), replyType), true);
        }

        // First time: do the work. If it throws, the whole transaction (key row included) rolls back.
        T reply = action.get();
        records.storeReply(userId, key, HttpStatus.CREATED.value(), toJson(reply));
        return new IdempotentResult<>(reply, false);
    }

    /** Housekeeping: keys only need to outlive the client's retry window. */
    @Transactional
    public int purgeExpired() {
        return records.deleteExpired(clock.instant());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialise idempotent payload", ex);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read cached reply", ex);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
