-- =============================================================================
-- Bank Loan Application System - core schema (PostgreSQL 16 / Neon)
-- Table names follow diagrams/erd.puml. Additions required by the course brief:
--   * version columns        -> optimistic concurrency control (no long row locks)
--   * expires_at timestamps  -> firm deadlines (OTP codes, loan offers)
--   * idempotency_records    -> at-most-once invocation (duplicate filter + cached reply)
--   * indexes on every hot query path -> soft deadlines for status/queue queries
-- =============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(254) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL
                    CHECK (role IN ('BORROWER','LOAN_OFFICER','CREDIT_ANALYST','BRANCH_MANAGER','ADMIN')),
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    branch          VARCHAR(80),
    avatar_color    VARCHAR(9),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);
CREATE INDEX ix_users_role ON users (role);

-- One-time codes. FIRM deadline: a code presented after expires_at has zero value.
CREATE TABLE verification_codes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose      VARCHAR(30) NOT NULL CHECK (purpose IN ('PHONE_VERIFICATION','PASSWORD_RESET')),
    code_hash    VARCHAR(64) NOT NULL,
    attempts     INT         NOT NULL DEFAULT 0,
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_verification_codes_lookup ON verification_codes (user_id, purpose, created_at DESC);

CREATE TABLE applicant_profiles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    national_id      VARCHAR(14)   NOT NULL,
    date_of_birth    DATE          NOT NULL,
    employment_info  JSONB         NOT NULL DEFAULT '{}'::jsonb,
    income_monthly   NUMERIC(14,2) NOT NULL CHECK (income_monthly > 0),
    address          JSONB         NOT NULL DEFAULT '{}'::jsonb,
    version          BIGINT        NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_applicant_profiles_user UNIQUE (user_id),
    CONSTRAINT uq_applicant_profiles_nin  UNIQUE (national_id)
);

CREATE TABLE loan_products (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(80)   NOT NULL,
    type                  VARCHAR(20)   NOT NULL
                          CHECK (type IN ('PERSONAL','BUSINESS','AGRICULTURE','ASSET_FINANCE','EMERGENCY')),
    interest_rate_annual  NUMERIC(5,2)  NOT NULL CHECK (interest_rate_annual > 0 AND interest_rate_annual <= 100),
    min_amount            NUMERIC(14,2) NOT NULL CHECK (min_amount > 0),
    max_amount            NUMERIC(14,2) NOT NULL,
    min_term_months       INT           NOT NULL CHECK (min_term_months >= 1),
    max_term_months       INT           NOT NULL,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_loan_products_name UNIQUE (name),
    CONSTRAINT ck_loan_products_amounts CHECK (min_amount <= max_amount),
    CONSTRAINT ck_loan_products_terms   CHECK (min_term_months <= max_term_months)
);

CREATE SEQUENCE loan_reference_seq START 1000;

CREATE TABLE loan_applications (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference             VARCHAR(20)   NOT NULL,
    applicant_id          UUID          NOT NULL REFERENCES users (id),
    product_id            UUID          NOT NULL REFERENCES loan_products (id),
    product_type          VARCHAR(20)   NOT NULL,
    amount_requested      NUMERIC(14,2) NOT NULL CHECK (amount_requested > 0),
    amount_approved       NUMERIC(14,2) CHECK (amount_approved IS NULL OR amount_approved > 0),
    interest_rate_annual  NUMERIC(5,2)  NOT NULL,
    term_months           INT           NOT NULL CHECK (term_months >= 1),
    purpose               VARCHAR(200)  NOT NULL,
    status                VARCHAR(30)   NOT NULL
                          CHECK (status IN ('DRAFT','SUBMITTED','PENDING_DOCUMENTS','DOCUMENTS_REJECTED',
                                            'CREDIT_CHECK_PENDING','CREDIT_CHECK_FAILED','CREDIT_CHECK_PASSED',
                                            'UNDER_REVIEW','AWAITING_ADDITIONAL_INFO','APPROVED','REJECTED',
                                            'PENDING_DISBURSEMENT','DISBURSED','ACTIVE','DELINQUENT','CLOSED','DEFAULTED')),
    assigned_officer_id   UUID REFERENCES users (id),
    decision_remarks      VARCHAR(500),
    info_request_message  VARCHAR(500),
    metadata              JSONB         NOT NULL DEFAULT '{}'::jsonb,
    -- FIRM deadline: the borrower must accept an approved offer before this instant.
    offer_expires_at      TIMESTAMPTZ,
    offer_accepted_at     TIMESTAMPTZ,
    submitted_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Optimistic lock: every UPDATE is "... WHERE id = ? AND version = ?".
    version               BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_loan_applications_reference UNIQUE (reference)
);
-- Borrower's "my applications" list (mobile) - index-only ordering, no sort step.
CREATE INDEX ix_loan_applications_applicant ON loan_applications (applicant_id, submitted_at DESC);
-- Staff queues are filtered views over status (dashboard).
CREATE INDEX ix_loan_applications_status ON loan_applications (status, submitted_at DESC);
CREATE INDEX ix_loan_applications_officer ON loan_applications (assigned_officer_id) WHERE assigned_officer_id IS NOT NULL;
-- The offer-expiry sweeper only ever looks at open offers: keep that index tiny.
CREATE INDEX ix_loan_applications_open_offers ON loan_applications (offer_expires_at) WHERE status = 'APPROVED';

-- Timeline of the long-running loan transaction (one row per state change).
CREATE TABLE loan_status_history (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID        NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    from_status          VARCHAR(30),
    to_status            VARCHAR(30) NOT NULL,
    changed_by           UUID REFERENCES users (id),
    note                 VARCHAR(500),
    changed_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_loan_status_history_loan ON loan_status_history (loan_application_id, changed_at);

CREATE TABLE documents (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    doc_type             VARCHAR(30)  NOT NULL
                         CHECK (doc_type IN ('NATIONAL_ID','PAYSLIP','BANK_STATEMENT','COLLATERAL_PROOF','PASSPORT_PHOTO')),
    file_name            VARCHAR(200) NOT NULL,
    file_url             VARCHAR(500),
    content_type         VARCHAR(60)  NOT NULL,
    size_bytes           BIGINT       NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    verification_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                         CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED')),
    verified_by          UUID REFERENCES users (id),
    uploaded_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version              BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_documents_loan ON documents (loan_application_id);
CREATE INDEX ix_documents_pending ON documents (loan_application_id) WHERE verification_status = 'PENDING';

CREATE TABLE credit_checks (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID        NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    bureau_reference     VARCHAR(40) NOT NULL,
    score                INT         NOT NULL CHECK (score BETWEEN 300 AND 850),
    result               VARCHAR(10) NOT NULL CHECK (result IN ('PASSED','FAILED')),
    raw_response         JSONB       NOT NULL DEFAULT '{}'::jsonb,
    checked_by           UUID REFERENCES users (id),      -- NULL = automated worker
    checked_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_credit_checks_loan ON credit_checks (loan_application_id, checked_at DESC);

CREATE TABLE approvals (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    decided_by           UUID         NOT NULL REFERENCES users (id),
    decision             VARCHAR(10)  NOT NULL CHECK (decision IN ('APPROVED','REJECTED')),
    comments             VARCHAR(500),
    decided_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_approvals_loan ON approvals (loan_application_id, decided_at DESC);

CREATE TABLE disbursements (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID          NOT NULL REFERENCES loan_applications (id),
    amount               NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    method               VARCHAR(20)   NOT NULL CHECK (method IN ('BANK_TRANSFER','MOBILE_MONEY')),
    status               VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING','COMPLETED')),
    disbursed_by         UUID REFERENCES users (id),
    disbursed_at         TIMESTAMPTZ,
    -- Last line of defence for at-most-once: money can leave only once per loan.
    CONSTRAINT uq_disbursements_loan UNIQUE (loan_application_id)
);

CREATE TABLE repayment_schedules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id  UUID          NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    installment_no       INT           NOT NULL,
    due_date             DATE          NOT NULL,
    principal_due        NUMERIC(14,2) NOT NULL,
    interest_due         NUMERIC(14,2) NOT NULL,
    amount_due           NUMERIC(14,2) NOT NULL CHECK (amount_due > 0),
    amount_paid          NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    status               VARCHAR(10)   NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','PARTIAL','PAID','OVERDUE')),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_repayment_schedules_installment UNIQUE (loan_application_id, installment_no),
    CONSTRAINT ck_repayment_schedules_not_overpaid CHECK (amount_paid <= amount_due)
);
-- Delinquency sweeper: unpaid installments by due date.
CREATE INDEX ix_repayment_schedules_due ON repayment_schedules (due_date) WHERE status <> 'PAID';

CREATE TABLE repayments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id        UUID          NOT NULL REFERENCES repayment_schedules (id),
    payment_reference  VARCHAR(40)   NOT NULL,
    amount             NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    method             VARCHAR(20)   NOT NULL CHECK (method IN ('BANK_TRANSFER','MOBILE_MONEY')),
    paid_by            UUID REFERENCES users (id),
    paid_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_repayments_schedule ON repayments (schedule_id);
CREATE INDEX ix_repayments_reference ON repayments (payment_reference);

CREATE TABLE notifications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    loan_application_id  UUID REFERENCES loan_applications (id) ON DELETE CASCADE,
    channel              VARCHAR(10)  NOT NULL DEFAULT 'IN_APP' CHECK (channel IN ('IN_APP','SMS','EMAIL')),
    message              VARCHAR(500) NOT NULL,
    sent_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    read_at              TIMESTAMPTZ
);
CREATE INDEX ix_notifications_user ON notifications (user_id, sent_at DESC);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type   VARCHAR(40) NOT NULL,
    entity_id     UUID,
    action        VARCHAR(60) NOT NULL,
    performed_by  UUID REFERENCES users (id),
    performed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    details       JSONB       NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_audit_logs_time ON audit_logs (performed_at DESC);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);

-- At-most-once invocation semantics (BIT2216 fault-tolerance table):
--   duplicate filtering  -> UNIQUE (user_id, idem_key)
--   retransmit the reply -> response_status + response_body are replayed verbatim
-- The row is written in the SAME database transaction as the business change, so
-- "work done" and "reply cached" commit or roll back together.
CREATE TABLE idempotency_records (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    idem_key         VARCHAR(100) NOT NULL,
    operation        VARCHAR(60)  NOT NULL,
    request_hash     VARCHAR(64)  NOT NULL,
    response_status  INT,
    response_body    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_idempotency_records_key UNIQUE (user_id, idem_key)
);
CREATE INDEX ix_idempotency_records_expiry ON idempotency_records (expires_at);
