-- A history insert, its audit entry, and its outgoing message commit together.
-- Existing history is deliberately not replayed: that would notify borrowers about old changes.
CREATE TABLE loan_message_outbox (
    id UUID PRIMARY KEY REFERENCES loan_status_history(id) ON DELETE CASCADE,
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lease_token UUID,
    lease_until TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX ix_loan_message_outbox_pending ON loan_message_outbox(available_at, created_at)
    WHERE published_at IS NULL;
ALTER TABLE notifications ADD COLUMN source_event_id UUID;
CREATE UNIQUE INDEX uq_notifications_source_event ON notifications(source_event_id)
    WHERE source_event_id IS NOT NULL;

CREATE FUNCTION enqueue_loan_status_message() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO loan_message_outbox(id, loan_application_id, payload)
    SELECT NEW.id, NEW.loan_application_id, jsonb_build_object(
        'applicationId', a.id, 'reference', a.reference, 'applicantId', a.applicant_id,
        'from', NEW.from_status, 'to', NEW.to_status, 'actorId', NEW.changed_by,
        'note', NEW.note, 'occurredAt', NEW.changed_at)
    FROM loan_applications a WHERE a.id = NEW.loan_application_id;
    RETURN NEW;
END;
$$;
CREATE TRIGGER loan_status_message_trigger AFTER INSERT ON loan_status_history
    FOR EACH ROW EXECUTE FUNCTION enqueue_loan_status_message();

CREATE FUNCTION audit_loan_status_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO audit_logs(entity_type, entity_id, action, performed_by, performed_at, details)
    SELECT 'LOAN_APPLICATION', NEW.loan_application_id, 'STATUS_' || NEW.to_status,
        NEW.changed_by, NEW.changed_at, jsonb_build_object(
            'reference', a.reference, 'from', NEW.from_status, 'to', NEW.to_status, 'note', NEW.note)
    FROM loan_applications a WHERE a.id = NEW.loan_application_id;
    RETURN NEW;
END;
$$;
CREATE TRIGGER loan_status_audit_trigger AFTER INSERT ON loan_status_history
    FOR EACH ROW EXECUTE FUNCTION audit_loan_status_change();

-- Short claim transaction; never hold database locks while waiting on RabbitMQ.
CREATE PROCEDURE claim_loan_messages(IN p_limit INT, IN p_token UUID)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_limit NOT BETWEEN 1 AND 10 OR p_token IS NULL THEN
        RAISE EXCEPTION 'Invalid message claim' USING ERRCODE = '22023';
    END IF;
    WITH selected AS (
        SELECT id FROM loan_message_outbox
        WHERE published_at IS NULL AND available_at <= now()
          AND (lease_until IS NULL OR lease_until < now())
        ORDER BY created_at, id LIMIT p_limit FOR UPDATE SKIP LOCKED
    )
    UPDATE loan_message_outbox o SET lease_token = p_token,
        lease_until = now() + interval '120 seconds', attempts = attempts + 1
    FROM selected s WHERE o.id = s.id;
END;
$$;

-- Atomic inbox/deduplication + notification. Redelivery of an event is harmless.
-- No COMMIT inside procedures: the caller owns the transaction.
CREATE PROCEDURE record_loan_notification(IN p_event_id UUID, IN p_message TEXT)
LANGUAGE plpgsql AS $$
DECLARE event_payload JSONB;
BEGIN
    UPDATE loan_message_outbox SET consumed_at = now()
        WHERE id = p_event_id AND consumed_at IS NULL RETURNING payload INTO event_payload;
    IF NOT FOUND THEN RETURN; END IF;
    IF p_message IS NOT NULL THEN
        INSERT INTO notifications(user_id, loan_application_id, message, source_event_id)
        VALUES ((event_payload->>'applicantId')::uuid,
                (event_payload->>'applicationId')::uuid, p_message, p_event_id);
    END IF;
END;
$$;
