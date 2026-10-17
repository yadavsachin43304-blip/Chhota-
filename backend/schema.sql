-- CHHOTU AI — PostgreSQL Schema & Migrations (Deliverable #6)
-- Enforces multi-tenant owner isolation, append-only audit logs, and remote job state.

CREATE TABLE IF NOT EXISTS owners (
    owner_uid VARCHAR(128) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    wake_word VARCHAR(64) NOT NULL DEFAULT 'छोटू',
    voice_embedding_sha256 VARCHAR(128),
    failed_auth_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS agent_tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    owner_uid VARCHAR(128) NOT NULL REFERENCES owners(owner_uid) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    user_request TEXT NOT NULL,
    intent_category VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(32) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by_owner BOOLEAN NOT NULL DEFAULT FALSE,
    affected_resource VARCHAR(1000) NOT NULL,
    reversible BOOLEAN NOT NULL DEFAULT TRUE,
    verification_result VARCHAR(2000) NOT NULL,
    plan_summary TEXT NOT NULL,
    execution_target VARCHAR(32) NOT NULL DEFAULT 'LOCAL_DEVICE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_owner_created
    ON agent_tasks(owner_uid, created_at DESC);

CREATE TABLE IF NOT EXISTS business_projects (
    project_id VARCHAR(64) PRIMARY KEY,
    owner_uid VARCHAR(128) NOT NULL REFERENCES owners(owner_uid) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(64) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    estimated_expense_inr BIGINT NOT NULL DEFAULT 0,
    actual_revenue_inr BIGINT NOT NULL DEFAULT 0,
    summary TEXT NOT NULL,
    next_action VARCHAR(500) NOT NULL,
    deadline_text VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS secure_file_records (
    file_id VARCHAR(64) PRIMARY KEY,
    owner_uid VARCHAR(128) NOT NULL REFERENCES owners(owner_uid) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    folder VARCHAR(100) NOT NULL,
    tags VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    encrypted_with_keystore BOOLEAN NOT NULL DEFAULT TRUE,
    storage_location VARCHAR(32) NOT NULL,
    ai_summary TEXT NOT NULL,
    sha256_checksum VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS security_audit_logs (
    log_id VARCHAR(64) PRIMARY KEY,
    owner_uid VARCHAR(128) NOT NULL REFERENCES owners(owner_uid) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    actor_identity VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    task_ref_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
