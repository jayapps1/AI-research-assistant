CREATE TABLE research_problems (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    statement TEXT NOT NULL,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_problems_project ON research_problems(project_id);

CREATE TABLE research_objectives (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('GENERAL','SPECIFIC')),
    text TEXT NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_objectives_project ON research_objectives(project_id);

CREATE TABLE research_questions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    objective_id UUID REFERENCES research_objectives(id),
    text TEXT NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_questions_project ON research_questions(project_id);

CREATE TABLE research_hypotheses (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    question_id UUID REFERENCES research_questions(id),
    text TEXT NOT NULL,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_hypotheses_project ON research_hypotheses(project_id);

CREATE TABLE literature_matrices (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE literature_reviews (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    body TEXT,
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conceptual_frameworks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_conceptual_framework_active_project ON conceptual_frameworks(project_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_conceptual_frameworks_project ON conceptual_frameworks(project_id);
CREATE INDEX idx_conceptual_frameworks_status ON conceptual_frameworks(status);

CREATE TABLE conceptual_variables (
    id UUID PRIMARY KEY,
    framework_id UUID NOT NULL REFERENCES conceptual_frameworks(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(40) NOT NULL,
    operational_definition TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(framework_id, display_order)
);
CREATE INDEX idx_conceptual_variables_framework ON conceptual_variables(framework_id);

CREATE TABLE conceptual_relationships (
    id UUID PRIMARY KEY,
    framework_id UUID NOT NULL REFERENCES conceptual_frameworks(id),
    source_variable_id UUID NOT NULL REFERENCES conceptual_variables(id),
    target_variable_id UUID NOT NULL REFERENCES conceptual_variables(id),
    type VARCHAR(40) NOT NULL,
    label VARCHAR(255),
    rationale TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (source_variable_id <> target_variable_id)
);
CREATE INDEX idx_conceptual_relationships_framework ON conceptual_relationships(framework_id);

CREATE TABLE conceptual_variable_objectives (
    variable_id UUID NOT NULL REFERENCES conceptual_variables(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id),
    PRIMARY KEY(variable_id, objective_id)
);
CREATE TABLE conceptual_variable_questions (
    variable_id UUID NOT NULL REFERENCES conceptual_variables(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES research_questions(id),
    PRIMARY KEY(variable_id, question_id)
);
CREATE TABLE conceptual_relationship_hypotheses (
    relationship_id UUID NOT NULL REFERENCES conceptual_relationships(id) ON DELETE CASCADE,
    hypothesis_id UUID NOT NULL REFERENCES research_hypotheses(id),
    PRIMARY KEY(relationship_id, hypothesis_id)
);

CREATE TABLE theoretical_frameworks (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    title VARCHAR(255) NOT NULL,
    overview TEXT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_theoretical_framework_active_project ON theoretical_frameworks(project_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_theoretical_frameworks_project ON theoretical_frameworks(project_id);

CREATE TABLE theoretical_framework_theories (
    id UUID PRIMARY KEY,
    framework_id UUID NOT NULL REFERENCES theoretical_frameworks(id),
    theory_name VARCHAR(255) NOT NULL,
    theorist VARCHAR(255),
    original_year INTEGER,
    description TEXT,
    key_constructs TEXT,
    relevance_to_study TEXT,
    limitations TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(framework_id, display_order)
);
CREATE INDEX idx_theories_framework ON theoretical_framework_theories(framework_id);

CREATE TABLE theoretical_framework_evidence (
    id UUID PRIMARY KEY,
    theory_id UUID NOT NULL REFERENCES theoretical_framework_theories(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id),
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    page_id UUID REFERENCES document_pages(id),
    chunk_id UUID REFERENCES document_chunks(id),
    evidence_text_snapshot TEXT NOT NULL,
    evidence_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_theory_evidence_theory ON theoretical_framework_evidence(theory_id);

CREATE TABLE theoretical_framework_objectives (
    theory_id UUID NOT NULL REFERENCES theoretical_framework_theories(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id),
    PRIMARY KEY(theory_id, objective_id)
);
CREATE TABLE theoretical_framework_questions (
    theory_id UUID NOT NULL REFERENCES theoretical_framework_theories(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES research_questions(id),
    PRIMARY KEY(theory_id, question_id)
);
CREATE TABLE theoretical_framework_hypotheses (
    theory_id UUID NOT NULL REFERENCES theoretical_framework_theories(id) ON DELETE CASCADE,
    hypothesis_id UUID NOT NULL REFERENCES research_hypotheses(id),
    PRIMARY KEY(theory_id, hypothesis_id)
);
CREATE TABLE theoretical_framework_variables (
    theory_id UUID NOT NULL REFERENCES theoretical_framework_theories(id) ON DELETE CASCADE,
    variable_id UUID NOT NULL REFERENCES conceptual_variables(id),
    PRIMARY KEY(theory_id, variable_id)
);

CREATE TABLE methodologies (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    research_problem_id UUID REFERENCES research_problems(id),
    title VARCHAR(255),
    approach VARCHAR(40) NOT NULL,
    design_type VARCHAR(80) NOT NULL,
    design_description TEXT,
    study_setting TEXT,
    study_period VARCHAR(255),
    rationale TEXT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','SUPERSEDED','ARCHIVED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_methodologies_active_project ON methodologies(project_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_methodologies_project ON methodologies(project_id);

CREATE TABLE methodology_objectives (
    methodology_id UUID NOT NULL REFERENCES methodologies(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id),
    PRIMARY KEY(methodology_id, objective_id)
);
CREATE TABLE methodology_questions (
    methodology_id UUID NOT NULL REFERENCES methodologies(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES research_questions(id),
    PRIMARY KEY(methodology_id, question_id)
);

CREATE TABLE study_populations (
    id UUID PRIMARY KEY,
    methodology_id UUID NOT NULL REFERENCES methodologies(id) ON DELETE CASCADE,
    target_population_description TEXT NOT NULL,
    target_population_size BIGINT CHECK (target_population_size IS NULL OR target_population_size >= 1),
    accessible_population_description TEXT,
    accessible_population_size BIGINT CHECK (accessible_population_size IS NULL OR accessible_population_size >= 1),
    inclusion_criteria TEXT,
    exclusion_criteria TEXT,
    geographic_scope TEXT,
    demographic_characteristics TEXT,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_study_populations_methodology ON study_populations(methodology_id);

CREATE TABLE sampling_plans (
    id UUID PRIMARY KEY,
    methodology_id UUID NOT NULL REFERENCES methodologies(id) ON DELETE CASCADE,
    population_id UUID REFERENCES study_populations(id),
    approach VARCHAR(40) NOT NULL,
    technique VARCHAR(40) NOT NULL,
    planned_sample_size INTEGER CHECK (planned_sample_size IS NULL OR planned_sample_size >= 1),
    rationale TEXT,
    sampling_frame TEXT,
    recruitment_strategy TEXT,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sampling_plans_methodology ON sampling_plans(methodology_id);

CREATE TABLE sample_size_calculations (
    id UUID PRIMARY KEY,
    sampling_plan_id UUID NOT NULL REFERENCES sampling_plans(id) ON DELETE CASCADE,
    method VARCHAR(50) NOT NULL,
    population_size BIGINT CHECK (population_size IS NULL OR population_size >= 1),
    confidence_level DOUBLE PRECISION CHECK (confidence_level IS NULL OR (confidence_level > 0 AND confidence_level <= 1)),
    margin_of_error DOUBLE PRECISION CHECK (margin_of_error IS NULL OR margin_of_error > 0),
    estimated_proportion DOUBLE PRECISION CHECK (estimated_proportion IS NULL OR (estimated_proportion > 0 AND estimated_proportion < 1)),
    design_effect DOUBLE PRECISION CHECK (design_effect IS NULL OR design_effect > 0),
    expected_response_rate DOUBLE PRECISION CHECK (expected_response_rate IS NULL OR (expected_response_rate > 0 AND expected_response_rate <= 1)),
    initial_sample_size INTEGER CHECK (initial_sample_size IS NULL OR initial_sample_size >= 1),
    adjusted_sample_size INTEGER NOT NULL CHECK (adjusted_sample_size >= 1),
    formula_description TEXT NOT NULL,
    assumptions TEXT NOT NULL,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sample_size_sampling_plan ON sample_size_calculations(sampling_plan_id);

CREATE TABLE data_collection_methods (
    id UUID PRIMARY KEY,
    methodology_id UUID NOT NULL REFERENCES methodologies(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    rationale TEXT,
    source_type VARCHAR(30) NOT NULL,
    administration_mode VARCHAR(255),
    setting TEXT,
    timing VARCHAR(255),
    primary_method BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(methodology_id, display_order)
);
CREATE INDEX idx_data_collection_methods_methodology ON data_collection_methods(methodology_id);

CREATE TABLE data_collection_method_objectives (
    method_id UUID NOT NULL REFERENCES data_collection_methods(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id),
    PRIMARY KEY(method_id, objective_id)
);
CREATE TABLE data_collection_method_questions (
    method_id UUID NOT NULL REFERENCES data_collection_methods(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES research_questions(id),
    PRIMARY KEY(method_id, question_id)
);
CREATE TABLE data_collection_method_populations (
    method_id UUID NOT NULL REFERENCES data_collection_methods(id) ON DELETE CASCADE,
    population_id UUID NOT NULL REFERENCES study_populations(id),
    PRIMARY KEY(method_id, population_id)
);
CREATE TABLE data_collection_method_sampling_plans (
    method_id UUID NOT NULL REFERENCES data_collection_methods(id) ON DELETE CASCADE,
    sampling_plan_id UUID NOT NULL REFERENCES sampling_plans(id),
    PRIMARY KEY(method_id, sampling_plan_id)
);
