-- Keep routine table references in the schema where Flyway installed them.
-- This also makes isolated test schemas safe when caller search paths differ.
ALTER FUNCTION enqueue_loan_status_message() SET search_path FROM CURRENT;
ALTER FUNCTION audit_loan_status_change() SET search_path FROM CURRENT;
ALTER PROCEDURE claim_loan_messages(INT, UUID) SET search_path FROM CURRENT;
ALTER PROCEDURE record_loan_notification(UUID, TEXT) SET search_path FROM CURRENT;
