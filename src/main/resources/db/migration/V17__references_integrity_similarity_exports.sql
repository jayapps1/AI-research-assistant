CREATE TABLE reference_entries (
    id UUID PRIMARY KEY,
    type VARCHAR(60) NOT NULL CHECK (type IN ('JOURNAL_ARTICLE','BOOK','BOOK_CHAPTER','CONFERENCE_PAPER','THESIS','DISSERTATION','REPORT','WEB_PAGE','DATASET','GOVERNMENT_DOCUMENT','STANDARD','NEWSPAPER_ARTICLE','MAGAZINE_ARTICLE','WORKING_PAPER','PREPRINT','SOFTWARE','OTHER')),
    title TEXT NOT NULL,
    container_title TEXT,
    publication_year INTEGER,
    publication_month INTEGER CHECK (publication_month IS NULL OR publication_month BETWEEN 1 AND 12),
    publication_day INTEGER CHECK (publication_day IS NULL OR publication_day BETWEEN 1 AND 31),
    volume VARCHAR(80),
    issue VARCHAR(80),
    pages VARCHAR(120),
    publisher VARCHAR(255),
    publisher_place VARCHAR(255),
    edition VARCHAR(120),
    institution VARCHAR(255),
    conference_name VARCHAR(255),
    abstract_text TEXT,
    language_code VARCHAR(20),
    doi VARCHAR(500),
    normalized_doi VARCHAR(500),
    url TEXT,
    isbn VARCHAR(80),
    issn VARCHAR(80),
    pmid VARCHAR(80),
    arxiv_id VARCHAR(120),
    metadata_status VARCHAR(40) NOT NULL CHECK (metadata_status IN ('COMPLETE','PARTIAL','NEEDS_REVIEW','VERIFIED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reference_entries_normalized_doi ON reference_entries(normalized_doi);
CREATE INDEX idx_reference_entries_title ON reference_entries(title);

CREATE TABLE reference_authors (
    id UUID PRIMARY KEY,
    reference_id UUID NOT NULL REFERENCES reference_entries(id) ON DELETE CASCADE,
    family_name VARCHAR(255),
    given_name VARCHAR(255),
    literal_name VARCHAR(500),
    orcid VARCHAR(80),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    role VARCHAR(40) NOT NULL CHECK (role IN ('AUTHOR','EDITOR','TRANSLATOR','ORGANIZATION','OTHER')),
    UNIQUE(reference_id, role, display_order)
);
CREATE INDEX idx_reference_authors_reference ON reference_authors(reference_id);

CREATE TABLE reference_identifiers (
    id UUID PRIMARY KEY,
    reference_id UUID NOT NULL REFERENCES reference_entries(id) ON DELETE CASCADE,
    identifier_type VARCHAR(40) NOT NULL,
    identifier_value VARCHAR(500) NOT NULL,
    normalized_value VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reference_identifiers_reference ON reference_identifiers(reference_id);

CREATE TABLE project_references (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    reference_id UUID NOT NULL REFERENCES reference_entries(id),
    citation_key VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED','NEEDS_REVIEW')),
    added_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, reference_id),
    UNIQUE(project_id, citation_key)
);
CREATE INDEX idx_project_references_project ON project_references(project_id);

CREATE TABLE reference_source_links (
    id UUID PRIMARY KEY,
    reference_id UUID NOT NULL REFERENCES reference_entries(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id),
    document_version_id UUID REFERENCES document_versions(id),
    type VARCHAR(60) NOT NULL CHECK (type IN ('FULL_TEXT','METADATA_SOURCE','SUPPLEMENTARY_MATERIAL','DATASET','OTHER')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reference_import_jobs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    format VARCHAR(40) NOT NULL CHECK (format IN ('RIS','BIBTEX','ENDNOTE_XML','ENDNOTE_TAGGED')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('UPLOADED','PARSING','PREVIEW_READY','IMPORTING','COMPLETED','COMPLETED_WITH_WARNINGS','FAILED')),
    original_filename VARCHAR(500) NOT NULL,
    storage_key VARCHAR(1000),
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    detected_entries INTEGER,
    imported_entries INTEGER,
    skipped_entries INTEGER,
    duplicate_entries INTEGER,
    failed_entries INTEGER,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(100),
    error_message TEXT
);

CREATE TABLE reference_import_items (
    id UUID PRIMARY KEY,
    import_job_id UUID NOT NULL REFERENCES reference_import_jobs(id) ON DELETE CASCADE,
    item_ordinal INTEGER NOT NULL CHECK (item_ordinal >= 1),
    parsed_json TEXT NOT NULL,
    classification VARCHAR(40) NOT NULL CHECK (classification IN ('NEW','EXACT_DUPLICATE','POSSIBLE_DUPLICATE','INVALID')),
    duplicate_reference_id UUID REFERENCES reference_entries(id),
    metadata_warnings TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(import_job_id, item_ordinal)
);

CREATE TABLE similarity_checks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    target_type VARCHAR(60) NOT NULL CHECK (target_type IN ('REPORT','REPORT_CHAPTER','REPORT_SECTION','LITERATURE_REVIEW','DISCUSSION','CUSTOM_TEXT')),
    target_id UUID,
    custom_text TEXT,
    provider VARCHAR(40) NOT NULL CHECK (provider IN ('LOCAL_PROJECT','EXTERNAL_PROVIDER','NONE')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('REQUESTED','RUNNING','COMPLETED','FAILED')),
    overall_similarity_percent DOUBLE PRECISION,
    provider_report_reference VARCHAR(500),
    requested_by UUID NOT NULL REFERENCES users(id),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(100),
    error_message TEXT
);

CREATE TABLE similarity_matches (
    id UUID PRIMARY KEY,
    check_id UUID NOT NULL REFERENCES similarity_checks(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id),
    document_version_id UUID REFERENCES document_versions(id),
    page_id UUID REFERENCES document_pages(id),
    chunk_id UUID REFERENCES document_chunks(id),
    matched_text TEXT NOT NULL,
    source_text_snapshot TEXT NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL,
    type VARCHAR(60) NOT NULL CHECK (type IN ('EXACT','NEAR_EXACT','LONG_QUOTE','POSSIBLE_UNATTRIBUTED_OVERLAP','COMMON_PHRASE','OTHER')),
    target_start INTEGER NOT NULL,
    target_end INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_writing_reviews (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    target_type VARCHAR(80) NOT NULL CHECK (target_type IN ('RESEARCH_PROBLEM','LITERATURE_REVIEW_SECTION','METHODOLOGY','FINDING','DISCUSSION','CONCLUSION','RECOMMENDATION','REPORT_SECTION','REPORT')),
    target_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('REQUESTED','RUNNING','COMPLETED','FAILED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    reviewer_type VARCHAR(80) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE academic_writing_issues (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES academic_writing_reviews(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO','WARNING','ERROR')),
    start_offset INTEGER,
    end_offset INTEGER,
    text_snapshot TEXT,
    message TEXT NOT NULL,
    suggestion TEXT,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE research_integrity_reviews (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    report_id UUID REFERENCES research_reports(id),
    status VARCHAR(40) NOT NULL CHECK (status IN ('REQUESTED','RUNNING','COMPLETED','FAILED')),
    requested_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE research_integrity_issues (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES research_integrity_reviews(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO','WARNING','ERROR')),
    message TEXT NOT NULL,
    artifact_type VARCHAR(100),
    artifact_id UUID,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_export_jobs (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES research_reports(id),
    format VARCHAR(40) NOT NULL CHECK (format IN ('DOCX','PDF','HTML','MARKDOWN')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('QUEUED','RUNNING','COMPLETED','FAILED','EXPIRED')),
    storage_key VARCHAR(1000),
    filename VARCHAR(500) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    report_revision_number INTEGER NOT NULL,
    citation_style VARCHAR(60) NOT NULL,
    template_id UUID,
    style_configuration_snapshot TEXT,
    requested_by UUID NOT NULL REFERENCES users(id),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(100),
    error_message TEXT
);
CREATE INDEX idx_report_export_jobs_report ON report_export_jobs(report_id);

ALTER TABLE research_report_citations
    ADD COLUMN IF NOT EXISTS reference_id UUID REFERENCES reference_entries(id),
    ADD COLUMN IF NOT EXISTS project_reference_id UUID REFERENCES project_references(id);
