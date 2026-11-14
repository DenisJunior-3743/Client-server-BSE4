-- Existing tokens without a version are treated as version 0 until the next password change.
ALTER TABLE users ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0
    CHECK (credential_version >= 0);
