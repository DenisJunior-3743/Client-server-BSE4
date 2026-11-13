package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.IdempotencyRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    /**
     * Claims the key. Returns 1 if this request is the first to use it, 0 if the key is already
     * taken. If a concurrent request holds the key in an uncommitted transaction, PostgreSQL makes
     * this INSERT wait for that transaction to finish (bounded by lock_timeout), so two retries can
     * never both run the business action.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_records (id, user_id, idem_key, operation, request_hash, created_at, expires_at)
            VALUES (:id, :userId, :idemKey, :operation, :requestHash, :createdAt, :expiresAt)
            ON CONFLICT (user_id, idem_key) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("id") UUID id, @Param("userId") UUID userId, @Param("idemKey") String idemKey,
            @Param("operation") String operation, @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt, @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query("update IdempotencyRecord r set r.responseStatus = :status, r.responseBody = :body "
            + "where r.userId = :userId and r.idemKey = :idemKey")
    int storeReply(@Param("userId") UUID userId, @Param("idemKey") String idemKey, @Param("status") int status,
            @Param("body") String body);

    Optional<IdempotencyRecord> findByUserIdAndIdemKey(UUID userId, String idemKey);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
