package com.trustbank.loanapp.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustbank.loanapp.event.LoanStatusChangedEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LoanMessageStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public LoanMessageStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public List<UUID> claim(UUID token) {
        jdbc.update("CALL claim_loan_messages(?, ?)", 10, token);
        return jdbc.query("SELECT id FROM loan_message_outbox WHERE lease_token = ? AND published_at IS NULL "
                + "ORDER BY created_at, id", (rs, row) -> rs.getObject(1, UUID.class), token);
    }

    public List<UUID> pendingForApplication(UUID applicationId) {
        return jdbc.query("SELECT id FROM loan_message_outbox WHERE loan_application_id = ? "
                + "AND published_at IS NULL AND lease_token IS NULL ORDER BY created_at, id",
                (rs, row) -> rs.getObject(1, UUID.class), applicationId);
    }

    public LoanStatusChangedEvent load(UUID id) {
        var rows = jdbc.query("SELECT payload::text FROM loan_message_outbox WHERE id = ?",
                (rs, row) -> rs.getString(1), id);
        if (rows.isEmpty()) return null; // loan deleted after publication
        try {
            return json.readValue(rows.getFirst(), LoanStatusChangedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid stored loan event", ex);
        }
    }

    public void recordNotification(UUID id, String message) {
        jdbc.update("CALL record_loan_notification(?, ?)", id, message);
    }

    public void complete(UUID id, UUID token) {
        jdbc.update("UPDATE loan_message_outbox SET published_at = now(), lease_token = NULL, lease_until = NULL "
                + "WHERE id = ? AND lease_token = ? AND published_at IS NULL", id, token);
    }

    public void retryLater(UUID id, UUID token) {
        jdbc.update("UPDATE loan_message_outbox SET lease_token = NULL, lease_until = NULL, "
                + "available_at = now() + interval '30 seconds' WHERE id = ? AND lease_token = ? "
                + "AND published_at IS NULL", id, token);
    }

    public void completeEmbedded(UUID id) {
        jdbc.update("UPDATE loan_message_outbox SET published_at = now() WHERE id = ? AND consumed_at IS NOT NULL "
                + "AND published_at IS NULL AND lease_token IS NULL", id);
    }

    public boolean requestRetry(UUID id) {
        return jdbc.update("UPDATE loan_message_outbox SET published_at = NULL, lease_token = NULL, "
                + "lease_until = NULL, available_at = now() WHERE id = ? AND consumed_at IS NULL", id) == 1;
    }

    public java.util.Map<String, Object> stats() {
        return jdbc.queryForMap("SELECT count(*) FILTER (WHERE published_at IS NULL) AS pending, "
                + "count(*) FILTER (WHERE published_at IS NOT NULL) AS published, "
                + "count(*) FILTER (WHERE consumed_at IS NOT NULL) AS consumed, "
                + "count(*) FILTER (WHERE published_at IS NOT NULL AND consumed_at IS NULL) AS awaiting_consumer, "
                + "count(*) FILTER (WHERE lease_until > now() AND published_at IS NULL) AS leased "
                + "FROM loan_message_outbox");
    }
}
