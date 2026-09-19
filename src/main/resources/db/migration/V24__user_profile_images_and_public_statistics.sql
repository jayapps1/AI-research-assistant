-- ============================================================
-- AI RESEARCH ASSISTANT
-- V24 - USER PROFILE IMAGES AND PUBLIC STATISTICS
-- ============================================================

-- ------------------------------------------------------------
-- 1. USER PROFILE IMAGES
-- ------------------------------------------------------------
CREATE TABLE user_profile_images (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    width INTEGER,
    height INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_profile_images_status CHECK (status IN ('ACTIVE', 'REPLACED', 'DELETED'))
);

CREATE UNIQUE INDEX uk_user_profile_images_active
    ON user_profile_images(user_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_user_profile_images_user ON user_profile_images(user_id, status);

-- ------------------------------------------------------------
-- 2. PUBLIC STATISTICS
-- ------------------------------------------------------------
CREATE TABLE public_statistics (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    label VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    value_source VARCHAR(30) NOT NULL,
    manual_value VARCHAR(100),
    system_metric VARCHAR(80),
    prefix VARCHAR(20),
    suffix VARCHAR(20),
    icon_key VARCHAR(60),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_public_statistics_source CHECK (value_source IN ('MANUAL', 'SYSTEM_DERIVED')),
    CONSTRAINT chk_public_statistics_metric CHECK (system_metric IS NULL OR system_metric IN (
        'TOTAL_RESEARCH_PROJECTS',
        'TOTAL_ACTIVE_USERS',
        'TOTAL_DOCUMENTS_PROCESSED',
        'TOTAL_WORKSPACES',
        'TOTAL_COMPLETED_REPORT_EXPORTS'
    ))
);

CREATE INDEX idx_public_statistics_nav ON public_statistics(enabled, display_order);
CREATE INDEX idx_public_statistics_code ON public_statistics(code);

-- Seed initial default public statistics
INSERT INTO public_statistics (id, code, label, description, value_source, manual_value, system_metric, prefix, suffix, icon_key, enabled, featured, display_order, created_at, updated_at)
VALUES
('00000000-0000-0000-0000-000000000401', 'research-projects', 'Research Projects', 'Total research studies initiated and managed', 'SYSTEM_DERIVED', NULL, 'TOTAL_RESEARCH_PROJECTS', NULL, '+', 'BookOpen', TRUE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000402', 'documents-processed', 'Documents Processed', 'Academic papers, datasets, and literature extracts synthesized', 'SYSTEM_DERIVED', NULL, 'TOTAL_DOCUMENTS_PROCESSED', NULL, '+', 'FileText', TRUE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000403', 'active-researchers', 'Researchers Supported', 'Faculty, postdocs, and graduate scholars using the platform', 'SYSTEM_DERIVED', NULL, 'TOTAL_ACTIVE_USERS', NULL, '+', 'Users', TRUE, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000404', 'academic-institutions', 'Research Teams & Labs', 'Collaborative academic groups and institutional departments', 'SYSTEM_DERIVED', NULL, 'TOTAL_WORKSPACES', NULL, '+', 'Building', TRUE, TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000405', 'evidence-grounding', 'Citation Verification', 'Grounded empirical verification and hallucination screening', 'MANUAL', '100%', NULL, NULL, NULL, 'ShieldCheck', TRUE, FALSE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
