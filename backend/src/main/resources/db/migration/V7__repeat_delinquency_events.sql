-- Both event types use the existing durable relay. Preserve the V5 foreign-key
-- relationship and event IDs for all previously recorded status events.
ALTER TABLE loan_message_outbox
    ADD COLUMN event_type TEXT NOT NULL DEFAULT 'LOAN_STATUS_CHANGED',
    ADD COLUMN source_history_id UUID;
UPDATE loan_message_outbox SET source_history_id = id;
ALTER TABLE loan_message_outbox
    DROP CONSTRAINT loan_message_outbox_id_fkey,
    ALTER COLUMN source_history_id SET NOT NULL,
    ADD FOREIGN KEY (source_history_id) REFERENCES loan_status_history(id) ON DELETE CASCADE,
    ADD UNIQUE (source_history_id, event_type),
    ADD CHECK (
        (event_type = 'LOAN_STATUS_CHANGED' AND id = source_history_id)
        OR (event_type = 'REPEAT_DELINQUENCY' AND id <> source_history_id)
    );

CREATE OR REPLACE FUNCTION enqueue_loan_status_message() RETURNS trigger
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    INSERT INTO loan_message_outbox(id, source_history_id, loan_application_id, payload)
    SELECT NEW.id, NEW.id, NEW.loan_application_id, jsonb_build_object(
        'applicationId', a.id, 'reference', a.reference, 'applicantId', a.applicant_id,
        'from', NEW.from_status, 'to', NEW.to_status, 'actorId', NEW.changed_by,
        'note', NEW.note, 'occurredAt', NEW.changed_at)
    FROM loan_applications a WHERE a.id = NEW.loan_application_id;
    RETURN NEW;
END;
$$;

CREATE INDEX ix_loan_status_history_delinquency ON loan_status_history(loan_application_id, changed_at, id)
    WHERE from_status = 'ACTIVE' AND to_status = 'DELINQUENT';

CREATE PROCEDURE claim_loan_events(IN p_limit INT, IN p_token UUID, IN p_event_type TEXT)
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    IF p_limit NOT BETWEEN 1 AND 10 OR p_token IS NULL THEN
        RAISE EXCEPTION 'Invalid message claim' USING ERRCODE = '22023';
    END IF;
    WITH selected AS (
        SELECT id FROM loan_message_outbox
        WHERE published_at IS NULL AND available_at <= now()
          AND (lease_until IS NULL OR lease_until < now())
          AND (p_event_type IS NULL OR event_type = p_event_type)
        ORDER BY created_at, id LIMIT p_limit FOR UPDATE SKIP LOCKED
    )
    UPDATE loan_message_outbox o SET lease_token = p_token,
        lease_until = now() + interval '120 seconds', attempts = attempts + 1
    FROM selected s WHERE o.id = s.id;
END;
$$;

-- An older relay can still run while V7 deploys. It only knows the status routing key.
CREATE OR REPLACE PROCEDURE claim_loan_messages(IN p_limit INT, IN p_token UUID)
LANGUAGE sql SET search_path FROM CURRENT AS $$
    CALL claim_loan_events(p_limit, p_token, 'LOAN_STATUS_CHANGED');
$$;

-- The current transition is already committed when RabbitMQ delivers it.
-- Find the most recent earlier transition on the same loan within 30 elapsed days.
CREATE PROCEDURE detect_repeat_delinquency(IN p_event_id UUID)
LANGUAGE sql SET search_path FROM CURRENT AS $$
    INSERT INTO loan_message_outbox(id, source_history_id, loan_application_id, event_type, payload)
    SELECT gen_random_uuid(), current_event.id, current_event.loan_application_id, 'REPEAT_DELINQUENCY',
           jsonb_build_object('previousEventId', previous_event.id, 'previousAt', previous_event.changed_at,
                              'currentEventId', current_event.id, 'currentAt', current_event.changed_at)
    FROM loan_status_history current_event
    CROSS JOIN LATERAL (
        SELECT h.id, h.changed_at
        FROM loan_status_history h
        WHERE h.loan_application_id = current_event.loan_application_id
          AND h.from_status = 'ACTIVE' AND h.to_status = 'DELINQUENT'
          AND h.changed_at >= current_event.changed_at - interval '720 hours'
          AND (h.changed_at, h.id) < (current_event.changed_at, current_event.id)
        ORDER BY h.changed_at DESC, h.id DESC
        LIMIT 1
    ) previous_event
    WHERE current_event.id = p_event_id
      AND current_event.from_status = 'ACTIVE' AND current_event.to_status = 'DELINQUENT'
    ON CONFLICT (source_history_id, event_type) DO NOTHING;
$$;

-- Commit the alert and the delivery marker together. Use the assigned active
-- officer, or a single active administrator if the assignment is unavailable.
CREATE PROCEDURE record_repeat_delinquency_alert(IN p_event_id UUID)
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
DECLARE
    application_id UUID;
    loan_reference TEXT;
    recipient_id UUID;
BEGIN
    UPDATE loan_message_outbox
    SET consumed_at = now()
    WHERE id = p_event_id AND event_type = 'REPEAT_DELINQUENCY' AND consumed_at IS NULL
    RETURNING loan_application_id INTO application_id;
    IF NOT FOUND THEN RETURN; END IF;

    SELECT reference INTO loan_reference FROM loan_applications WHERE id = application_id;
    SELECT u.id INTO recipient_id
    FROM loan_applications a
    JOIN users u ON u.active AND (
        (u.id = a.assigned_officer_id AND u.role = 'LOAN_OFFICER') OR u.role = 'ADMIN'
    )
    WHERE a.id = application_id
    ORDER BY (u.id IS DISTINCT FROM a.assigned_officer_id), u.created_at, u.id
    LIMIT 1;
    IF recipient_id IS NULL THEN
        RAISE EXCEPTION 'No active staff recipient for repeat delinquency alert';
    END IF;

    INSERT INTO notifications(user_id, loan_application_id, message, source_event_id)
    VALUES (recipient_id, application_id,
            'Loan ' || loan_reference || ' entered delinquency twice within 30 days. Please follow up.',
            p_event_id);
END;
$$;
