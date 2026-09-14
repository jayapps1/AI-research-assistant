ALTER TABLE research_projects
    ADD COLUMN next_participant_number BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN next_session_number BIGINT NOT NULL DEFAULT 1;

CREATE TABLE ethics_protocols (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','CONDITIONALLY_APPROVED','REJECTED','EXPIRED','WITHDRAWN')),
    institution_name VARCHAR(255),
    review_board_name VARCHAR(255),
    protocol_reference VARCHAR(255),
    risk_level VARCHAR(100),
    risk_description TEXT,
    confidentiality_plan TEXT,
    data_protection_plan TEXT,
    retention_plan TEXT,
    withdrawal_procedure TEXT,
    vulnerable_population_considerations TEXT,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ethics_protocols_project ON ethics_protocols(project_id);
CREATE INDEX idx_ethics_protocols_status ON ethics_protocols(status);

CREATE TABLE ethics_approvals (
    id UUID PRIMARY KEY,
    protocol_id UUID NOT NULL REFERENCES ethics_protocols(id) ON DELETE CASCADE,
    approval_reference VARCHAR(255) NOT NULL,
    approval_date DATE,
    expiry_date DATE,
    approving_body VARCHAR(255),
    conditions TEXT,
    notes TEXT,
    recorded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (approval_date IS NULL OR expiry_date IS NULL OR expiry_date >= approval_date)
);
CREATE INDEX idx_ethics_approvals_protocol ON ethics_approvals(protocol_id);

CREATE TABLE consent_forms (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED')),
    language_code VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_consent_forms_project ON consent_forms(project_id);
CREATE UNIQUE INDEX uk_consent_forms_active_language ON consent_forms(project_id, language_code) WHERE status = 'ACTIVE';

CREATE TABLE consent_form_revisions (
    id UUID PRIMARY KEY,
    consent_form_id UUID NOT NULL REFERENCES consent_forms(id) ON DELETE CASCADE,
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    introduction TEXT NOT NULL,
    study_purpose TEXT NOT NULL,
    procedures TEXT NOT NULL,
    risks TEXT NOT NULL,
    benefits TEXT NOT NULL,
    confidentiality TEXT NOT NULL,
    voluntary_participation TEXT NOT NULL,
    withdrawal_rights TEXT NOT NULL,
    contact_information TEXT,
    data_usage_statement TEXT,
    data_retention_statement TEXT,
    consent_statement TEXT NOT NULL,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(consent_form_id, revision_number)
);
CREATE INDEX idx_consent_revisions_form ON consent_form_revisions(consent_form_id);

CREATE TABLE participants (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    participant_code VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('SCREENED','ELIGIBLE','ENROLLED','COMPLETED','WITHDRAWN','EXCLUDED','LOST_TO_FOLLOW_UP')),
    population_id UUID REFERENCES study_populations(id),
    sampling_plan_id UUID REFERENCES sampling_plans(id),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    enrolled_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, participant_code),
    CHECK (withdrawn_at IS NULL OR withdrawn_at >= enrolled_at)
);
CREATE INDEX idx_participants_project ON participants(project_id);
CREATE INDEX idx_participants_status ON participants(status);

CREATE TABLE participant_identities (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL UNIQUE REFERENCES participants(id) ON DELETE CASCADE,
    encrypted_full_name TEXT,
    encrypted_email TEXT,
    encrypted_phone TEXT,
    encrypted_external_identifier TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE participant_eligibility_assessments (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    eligible BOOLEAN NOT NULL,
    reason_code VARCHAR(100),
    notes TEXT,
    assessed_by UUID NOT NULL REFERENCES users(id),
    assessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_participant_eligibility_participant ON participant_eligibility_assessments(participant_id);

CREATE TABLE participant_consents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    participant_id UUID NOT NULL REFERENCES participants(id),
    consent_revision_id UUID NOT NULL REFERENCES consent_form_revisions(id),
    decision VARCHAR(30) NOT NULL CHECK (decision IN ('CONSENTED','DECLINED','WITHDRAWN')),
    method VARCHAR(30) NOT NULL CHECK (method IN ('WRITTEN','ELECTRONIC','VERBAL','WITNESSED_VERBAL','OTHER')),
    consented_at TIMESTAMP WITH TIME ZONE NOT NULL,
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    witness_code VARCHAR(100),
    recorded_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (withdrawn_at IS NULL OR withdrawn_at >= consented_at)
);
CREATE INDEX idx_participant_consents_project ON participant_consents(project_id);
CREATE INDEX idx_participant_consents_participant ON participant_consents(participant_id);

CREATE TABLE data_collection_sessions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    instrument_id UUID NOT NULL REFERENCES research_instruments(id),
    instrument_revision_number INTEGER NOT NULL CHECK (instrument_revision_number >= 1),
    data_collection_method_id UUID REFERENCES data_collection_methods(id),
    participant_id UUID REFERENCES participants(id),
    type VARCHAR(40) NOT NULL CHECK (type IN ('QUESTIONNAIRE','INTERVIEW','FOCUS_GROUP','OBSERVATION','RECORD_REVIEW','SECONDARY_DATA','OTHER')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','PARTIAL','CANCELLED','INVALIDATED')),
    session_code VARCHAR(40) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    location_or_setting TEXT,
    collected_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, session_code),
    CHECK (started_at IS NULL OR completed_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX idx_sessions_project ON data_collection_sessions(project_id);
CREATE INDEX idx_sessions_participant ON data_collection_sessions(participant_id);
CREATE INDEX idx_sessions_status ON data_collection_sessions(status);

CREATE TABLE data_collection_session_participants (
    session_id UUID NOT NULL REFERENCES data_collection_sessions(id) ON DELETE CASCADE,
    participant_id UUID NOT NULL REFERENCES participants(id),
    PRIMARY KEY(session_id, participant_id)
);

CREATE TABLE instrument_responses (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES data_collection_sessions(id) ON DELETE CASCADE,
    instrument_item_type VARCHAR(50) NOT NULL CHECK (instrument_item_type IN ('QUESTIONNAIRE_ITEM','INTERVIEW_QUESTION','FOCUS_GROUP_QUESTION','OBSERVATION_ITEM')),
    instrument_item_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('TEXT','NUMERIC','BOOLEAN','DATE','OPTION','MULTIPLE_OPTIONS','RAW')),
    text_value TEXT,
    numeric_value NUMERIC,
    boolean_value BOOLEAN,
    date_value DATE,
    option_value VARCHAR(255),
    raw_value TEXT,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by UUID REFERENCES users(id)
);
CREATE INDEX idx_instrument_responses_session ON instrument_responses(session_id);
CREATE INDEX idx_instrument_responses_item ON instrument_responses(instrument_item_type, instrument_item_id);

CREATE TABLE instrument_response_options (
    id UUID PRIMARY KEY,
    response_id UUID NOT NULL REFERENCES instrument_responses(id) ON DELETE CASCADE,
    option_value VARCHAR(255) NOT NULL,
    option_label VARCHAR(500),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    UNIQUE(response_id, option_value)
);

CREATE TABLE research_datasets (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(40) NOT NULL CHECK (source_type IN ('COLLECTED_SESSIONS','IMPORTED_CSV','IMPORTED_XLSX','MANUAL','SECONDARY_DATA','MERGED','OTHER')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','VALIDATING','READY','INVALID','ARCHIVED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_datasets_project ON research_datasets(project_id);

CREATE TABLE dataset_variables (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES research_datasets(id) ON DELETE CASCADE,
    variable_name VARCHAR(120) NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('STRING','INTEGER','DECIMAL','BOOLEAN','DATE','DATETIME','CATEGORY','ORDINAL','TEXT')),
    measurement_level VARCHAR(30) NOT NULL CHECK (measurement_level IN ('NOMINAL','ORDINAL','INTERVAL','RATIO','TEXT','UNKNOWN')),
    nullable BOOLEAN NOT NULL DEFAULT TRUE,
    unit VARCHAR(80),
    missing_value_code VARCHAR(80),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    source_instrument_item_id UUID,
    objective_id UUID REFERENCES research_objectives(id),
    research_question_id UUID REFERENCES research_questions(id),
    hypothesis_id UUID REFERENCES research_hypotheses(id),
    conceptual_variable_id UUID REFERENCES conceptual_variables(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(dataset_id, variable_name),
    UNIQUE(dataset_id, display_order),
    CHECK (variable_name ~ '^[A-Za-z][A-Za-z0-9_]*$')
);
CREATE INDEX idx_dataset_variables_dataset ON dataset_variables(dataset_id);

CREATE TABLE dataset_variable_categories (
    id UUID PRIMARY KEY,
    variable_id UUID NOT NULL REFERENCES dataset_variables(id) ON DELETE CASCADE,
    code VARCHAR(255) NOT NULL,
    label VARCHAR(500) NOT NULL,
    numeric_value NUMERIC,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    UNIQUE(variable_id, code),
    UNIQUE(variable_id, display_order)
);

CREATE TABLE dataset_records (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES research_datasets(id) ON DELETE CASCADE,
    row_number BIGINT NOT NULL CHECK (row_number >= 1),
    participant_id UUID REFERENCES participants(id),
    source_session_id UUID REFERENCES data_collection_sessions(id),
    external_record_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(dataset_id, row_number)
);
CREATE INDEX idx_dataset_records_dataset ON dataset_records(dataset_id);

CREATE TABLE dataset_values (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL REFERENCES dataset_records(id) ON DELETE CASCADE,
    variable_id UUID NOT NULL REFERENCES dataset_variables(id) ON DELETE CASCADE,
    string_value VARCHAR(1000),
    integer_value BIGINT,
    decimal_value NUMERIC,
    boolean_value BOOLEAN,
    date_value DATE,
    date_time_value TIMESTAMP WITH TIME ZONE,
    text_value TEXT,
    category_code VARCHAR(255),
    missing BOOLEAN NOT NULL DEFAULT FALSE,
    missing_reason VARCHAR(40) CHECK (missing_reason IS NULL OR missing_reason IN ('NOT_PROVIDED','NOT_APPLICABLE','REFUSED','UNKNOWN','IMPORT_ERROR','OTHER')),
    UNIQUE(record_id, variable_id),
    CHECK (missing = TRUE OR missing_reason IS NULL)
);
CREATE INDEX idx_dataset_values_record ON dataset_values(record_id);
CREATE INDEX idx_dataset_values_variable ON dataset_values(variable_id);

CREATE TABLE dataset_import_jobs (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES research_datasets(id) ON DELETE CASCADE,
    format VARCHAR(20) NOT NULL CHECK (format IN ('CSV','XLSX')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('UPLOADED','PROFILING','AWAITING_MAPPING','VALIDATING','IMPORTING','COMPLETED','COMPLETED_WITH_ERRORS','FAILED','CANCELLED')),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500),
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    checksum_sha256 VARCHAR(64) NOT NULL,
    total_rows INTEGER,
    imported_rows INTEGER,
    rejected_rows INTEGER,
    error_code VARCHAR(100),
    error_message TEXT,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT
);
CREATE INDEX idx_dataset_import_jobs_dataset ON dataset_import_jobs(dataset_id);
CREATE INDEX idx_dataset_import_jobs_status ON dataset_import_jobs(status);

CREATE TABLE dataset_import_column_mappings (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES dataset_import_jobs(id) ON DELETE CASCADE,
    source_column VARCHAR(255) NOT NULL,
    variable_id UUID REFERENCES dataset_variables(id),
    proposed_variable_name VARCHAR(120),
    target_type VARCHAR(30) NOT NULL CHECK (target_type IN ('STRING','INTEGER','DECIMAL','BOOLEAN','DATE','DATETIME','CATEGORY','ORDINAL','TEXT')),
    measurement_level VARCHAR(30) NOT NULL CHECK (measurement_level IN ('NOMINAL','ORDINAL','INTERVAL','RATIO','TEXT','UNKNOWN')),
    ignored BOOLEAN NOT NULL DEFAULT FALSE,
    transformation TEXT,
    UNIQUE(job_id, source_column)
);

CREATE TABLE dataset_validation_issues (
    id UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES research_datasets(id) ON DELETE CASCADE,
    import_job_id UUID REFERENCES dataset_import_jobs(id) ON DELETE CASCADE,
    row_number BIGINT,
    variable_id UUID,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO','WARNING','ERROR')),
    code VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    rejected_value_snapshot TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_dataset_validation_issues_dataset ON dataset_validation_issues(dataset_id);
CREATE INDEX idx_dataset_validation_issues_import ON dataset_validation_issues(import_job_id);
