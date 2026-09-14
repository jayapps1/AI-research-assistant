CREATE TABLE analysis_runs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    objective_id UUID REFERENCES research_objectives(id),
    question_id UUID REFERENCES research_questions(id),
    hypothesis_id UUID REFERENCES research_hypotheses(id),
    dataset_id UUID REFERENCES research_datasets(id),
    qualitative_source_type VARCHAR(40) CHECK (qualitative_source_type IS NULL OR qualitative_source_type IN ('DOCUMENT','LITERATURE_REVIEW','LITERATURE_MATRIX','FIELD_NOTES','TRANSCRIPT','OTHER')),
    qualitative_source_reference UUID,
    title VARCHAR(255) NOT NULL,
    analysis_type VARCHAR(50) NOT NULL CHECK (analysis_type IN ('DATASET_PROFILE','DESCRIPTIVE_SUMMARY','QUALITATIVE_CODING','DOCUMENT_SYNTHESIS','MIXED_METHODS_SYNTHESIS','CUSTOM')),
    status VARCHAR(40) NOT NULL CHECK (status IN ('PLANNED','RUNNING','COMPLETED','FAILED','CANCELLED')),
    method_description TEXT,
    parameters_json TEXT,
    initiated_by UUID NOT NULL REFERENCES users(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (completed_at IS NULL OR completed_at >= started_at),
    CHECK (dataset_id IS NOT NULL OR qualitative_source_reference IS NOT NULL)
);
CREATE INDEX idx_analysis_runs_project ON analysis_runs(project_id);
CREATE INDEX idx_analysis_runs_status ON analysis_runs(status);
CREATE INDEX idx_analysis_runs_objective ON analysis_runs(objective_id);
CREATE INDEX idx_analysis_runs_question ON analysis_runs(question_id);
CREATE INDEX idx_analysis_runs_hypothesis ON analysis_runs(hypothesis_id);
CREATE INDEX idx_analysis_runs_dataset ON analysis_runs(dataset_id);

CREATE TABLE analysis_results (
    id UUID PRIMARY KEY,
    analysis_run_id UUID NOT NULL REFERENCES analysis_runs(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    summary TEXT NOT NULL,
    result_payload_json TEXT,
    limitations TEXT,
    generated_by VARCHAR(30) NOT NULL CHECK (generated_by IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_analysis_results_run ON analysis_results(analysis_run_id);
CREATE INDEX idx_analysis_results_project ON analysis_results(project_id);

CREATE TABLE research_findings (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    analysis_result_id UUID NOT NULL REFERENCES analysis_results(id) ON DELETE CASCADE,
    objective_id UUID REFERENCES research_objectives(id),
    question_id UUID REFERENCES research_questions(id),
    hypothesis_id UUID REFERENCES research_hypotheses(id),
    title VARCHAR(255) NOT NULL,
    statement TEXT NOT NULL,
    evidence_summary TEXT,
    strength VARCHAR(30) NOT NULL CHECK (strength IN ('LOW','MODERATE','HIGH','INCONCLUSIVE')),
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_findings_project ON research_findings(project_id);
CREATE INDEX idx_research_findings_result ON research_findings(analysis_result_id);
CREATE INDEX idx_research_findings_question ON research_findings(question_id);

CREATE TABLE finding_discussions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    finding_id UUID NOT NULL REFERENCES research_findings(id) ON DELETE CASCADE,
    interpretation TEXT NOT NULL,
    relation_to_literature TEXT,
    implications TEXT,
    limitations TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_finding_discussions_project ON finding_discussions(project_id);
CREATE INDEX idx_finding_discussions_finding ON finding_discussions(finding_id);

CREATE TABLE research_conclusions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    discussion_id UUID REFERENCES finding_discussions(id) ON DELETE SET NULL,
    objective_id UUID REFERENCES research_objectives(id),
    question_id UUID REFERENCES research_questions(id),
    title VARCHAR(255) NOT NULL,
    statement TEXT NOT NULL,
    scope_note TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_conclusions_project ON research_conclusions(project_id);
CREATE INDEX idx_research_conclusions_discussion ON research_conclusions(discussion_id);

CREATE TABLE research_recommendations (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    conclusion_id UUID REFERENCES research_conclusions(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    recommendation TEXT NOT NULL,
    audience VARCHAR(120),
    priority VARCHAR(30) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    rationale TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_recommendations_project ON research_recommendations(project_id);
CREATE INDEX idx_research_recommendations_conclusion ON research_recommendations(conclusion_id);

CREATE TABLE final_reports (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','UNDER_REVIEW','APPROVED','ARCHIVED')),
    abstract_text TEXT,
    body TEXT,
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_final_reports_project ON final_reports(project_id);
CREATE INDEX idx_final_reports_status ON final_reports(status);

CREATE TABLE final_report_findings (
    report_id UUID NOT NULL REFERENCES final_reports(id) ON DELETE CASCADE,
    finding_id UUID NOT NULL REFERENCES research_findings(id),
    PRIMARY KEY(report_id, finding_id)
);
CREATE TABLE final_report_conclusions (
    report_id UUID NOT NULL REFERENCES final_reports(id) ON DELETE CASCADE,
    conclusion_id UUID NOT NULL REFERENCES research_conclusions(id),
    PRIMARY KEY(report_id, conclusion_id)
);
CREATE TABLE final_report_recommendations (
    report_id UUID NOT NULL REFERENCES final_reports(id) ON DELETE CASCADE,
    recommendation_id UUID NOT NULL REFERENCES research_recommendations(id),
    PRIMARY KEY(report_id, recommendation_id)
);
