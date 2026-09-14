-- ============================================================
-- AI RESEARCH ASSISTANT
-- V10 - RAG CONVERSATIONS, QUERY EVIDENCE AND GROUNDED ANSWERS
-- ============================================================

CREATE TABLE rag_conversations
(
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    created_by UUID NOT NULL,
    title VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_rag_conversations_project
        FOREIGN KEY (project_id)
        REFERENCES research_projects(id),

    CONSTRAINT fk_rag_conversations_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_rag_conversations_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_rag_conversations_project_id
    ON rag_conversations(project_id);
CREATE INDEX idx_rag_conversations_created_by
    ON rag_conversations(created_by);
CREATE INDEX idx_rag_conversations_project_created_by
    ON rag_conversations(project_id, created_by);
CREATE INDEX idx_rag_conversations_status
    ON rag_conversations(status);

CREATE TABLE rag_queries
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    created_by UUID NOT NULL,
    question TEXT NOT NULL,
    scope_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_evidence_limit INTEGER,
    retrieval_mode VARCHAR(40),
    retrieval_duration_ms BIGINT,
    failure_code VARCHAR(100),
    failure_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_rag_queries_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES rag_conversations(id),

    CONSTRAINT fk_rag_queries_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_rag_queries_scope_type
        CHECK (scope_type IN ('PROJECT_ALL_DOCUMENTS', 'SELECTED_DOCUMENTS')),

    CONSTRAINT chk_rag_queries_status
        CHECK (
            status IN (
                'RECEIVED',
                'RETRIEVING',
                'EVIDENCE_READY',
                'GENERATING',
                'COMPLETED',
                'INSUFFICIENT_EVIDENCE',
                'FAILED'
            )
        )
);

CREATE INDEX idx_rag_queries_conversation_id
    ON rag_queries(conversation_id);
CREATE INDEX idx_rag_queries_created_by
    ON rag_queries(created_by);
CREATE INDEX idx_rag_queries_status
    ON rag_queries(status);
CREATE INDEX idx_rag_queries_created_at
    ON rag_queries(created_at);

CREATE TABLE rag_query_documents
(
    query_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_version_id UUID NOT NULL,

    CONSTRAINT pk_rag_query_documents
        PRIMARY KEY (query_id, document_id, document_version_id),

    CONSTRAINT fk_rag_query_documents_query
        FOREIGN KEY (query_id)
        REFERENCES rag_queries(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rag_query_documents_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_rag_query_documents_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id)
);

CREATE INDEX idx_rag_query_documents_document_id
    ON rag_query_documents(document_id);
CREATE INDEX idx_rag_query_documents_version_id
    ON rag_query_documents(document_version_id);

CREATE TABLE rag_query_evidence
(
    id UUID PRIMARY KEY,
    query_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_code VARCHAR(32) NOT NULL,
    document_title VARCHAR(500) NOT NULL,
    document_version_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    chunk_number INTEGER NOT NULL,
    evidence_ordinal INTEGER NOT NULL,
    text_snapshot TEXT NOT NULL,
    lexical_score DOUBLE PRECISION,
    semantic_score DOUBLE PRECISION,
    fused_score DOUBLE PRECISION,
    rerank_score DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rag_query_evidence_query
        FOREIGN KEY (query_id)
        REFERENCES rag_queries(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rag_query_evidence_chunk
        FOREIGN KEY (chunk_id)
        REFERENCES document_chunks(id),

    CONSTRAINT fk_rag_query_evidence_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT fk_rag_query_evidence_version
        FOREIGN KEY (document_version_id)
        REFERENCES document_versions(id),

    CONSTRAINT uk_rag_query_evidence_ordinal
        UNIQUE (query_id, evidence_ordinal),

    CONSTRAINT uk_rag_query_evidence_chunk
        UNIQUE (query_id, chunk_id)
);

CREATE INDEX idx_rag_query_evidence_query_id
    ON rag_query_evidence(query_id);
CREATE INDEX idx_rag_query_evidence_document_id
    ON rag_query_evidence(document_id);

CREATE TABLE grounded_answers
(
    id UUID PRIMARY KEY,
    query_id UUID NOT NULL UNIQUE,
    answer_text TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider VARCHAR(100),
    model VARCHAR(200),
    input_tokens INTEGER,
    output_tokens INTEGER,
    generation_duration_ms BIGINT,
    finish_reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_grounded_answers_query
        FOREIGN KEY (query_id)
        REFERENCES rag_queries(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_grounded_answers_status
        CHECK (
            status IN (
                'GENERATED',
                'VERIFIED',
                'REJECTED_UNGROUNDED',
                'INSUFFICIENT_EVIDENCE'
            )
        )
);

CREATE TABLE answer_citations
(
    id UUID PRIMARY KEY,
    answer_id UUID NOT NULL,
    query_evidence_id UUID NOT NULL,
    citation_ordinal INTEGER NOT NULL,
    claim_text TEXT,
    supporting_text_snapshot TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_answer_citations_answer
        FOREIGN KEY (answer_id)
        REFERENCES grounded_answers(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_answer_citations_query_evidence
        FOREIGN KEY (query_evidence_id)
        REFERENCES rag_query_evidence(id),

    CONSTRAINT uk_answer_citations_ordinal
        UNIQUE (answer_id, citation_ordinal)
);

CREATE INDEX idx_answer_citations_answer_id
    ON answer_citations(answer_id);
CREATE INDEX idx_answer_citations_query_evidence_id
    ON answer_citations(query_evidence_id);
