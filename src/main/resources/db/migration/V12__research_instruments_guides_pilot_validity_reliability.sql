CREATE TABLE research_instruments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    methodology_id UUID NOT NULL REFERENCES methodologies(id),
    data_collection_method_id UUID NOT NULL REFERENCES data_collection_methods(id),
    type VARCHAR(50) NOT NULL CHECK (type IN ('QUESTIONNAIRE','INTERVIEW_GUIDE','FOCUS_GROUP_GUIDE','OBSERVATION_CHECKLIST')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    status VARCHAR(40) NOT NULL CHECK (status IN ('DRAFT','READY_FOR_REVIEW','VALIDATED','PILOTED','ACTIVE','SUPERSEDED','ARCHIVED')),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    revision_number INTEGER NOT NULL CHECK (revision_number >= 1),
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_research_instruments_project ON research_instruments(project_id);
CREATE INDEX idx_research_instruments_methodology ON research_instruments(methodology_id);
CREATE INDEX idx_research_instruments_method ON research_instruments(data_collection_method_id);

CREATE TABLE research_instrument_objectives (
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    objective_id UUID NOT NULL REFERENCES research_objectives(id),
    PRIMARY KEY(instrument_id, objective_id)
);
CREATE TABLE research_instrument_questions (
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES research_questions(id),
    PRIMARY KEY(instrument_id, question_id)
);
CREATE TABLE research_instrument_hypotheses (
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    hypothesis_id UUID NOT NULL REFERENCES research_hypotheses(id),
    PRIMARY KEY(instrument_id, hypothesis_id)
);

CREATE TABLE questionnaires (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL UNIQUE REFERENCES research_instruments(id) ON DELETE CASCADE,
    introduction TEXT,
    anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_completion_minutes INTEGER CHECK (estimated_completion_minutes IS NULL OR estimated_completion_minutes > 0),
    target_respondent_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questionnaire_sections (
    id UUID PRIMARY KEY,
    questionnaire_id UUID NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(questionnaire_id, display_order)
);
CREATE INDEX idx_questionnaire_sections_questionnaire ON questionnaire_sections(questionnaire_id);

CREATE TABLE response_scales (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES research_projects(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL,
    minimum_value INTEGER,
    maximum_value INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (minimum_value IS NULL OR maximum_value IS NULL OR minimum_value <= maximum_value)
);
CREATE INDEX idx_response_scales_project ON response_scales(project_id);

CREATE TABLE response_scale_options (
    id UUID PRIMARY KEY,
    scale_id UUID NOT NULL REFERENCES response_scales(id) ON DELETE CASCADE,
    numeric_value INTEGER NOT NULL,
    label VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    UNIQUE(scale_id, numeric_value),
    UNIQUE(scale_id, display_order)
);
CREATE INDEX idx_response_scale_options_scale ON response_scale_options(scale_id);

CREATE TABLE questionnaire_items (
    id UUID PRIMARY KEY,
    section_id UUID NOT NULL REFERENCES questionnaire_sections(id) ON DELETE CASCADE,
    item_code VARCHAR(40) NOT NULL,
    prompt TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    help_text TEXT,
    validation_rules_json TEXT,
    response_scale_id UUID REFERENCES response_scales(id),
    reverse_scored BOOLEAN NOT NULL DEFAULT FALSE,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(section_id, item_code),
    UNIQUE(section_id, display_order)
);
CREATE INDEX idx_questionnaire_items_section ON questionnaire_items(section_id);

CREATE TABLE questionnaire_options (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES questionnaire_items(id) ON DELETE CASCADE,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 1),
    numeric_score DOUBLE PRECISION,
    UNIQUE(item_id, value),
    UNIQUE(item_id, display_order)
);
CREATE INDEX idx_questionnaire_options_item ON questionnaire_options(item_id);

CREATE TABLE questionnaire_item_objectives (item_id UUID NOT NULL REFERENCES questionnaire_items(id) ON DELETE CASCADE, objective_id UUID NOT NULL REFERENCES research_objectives(id), PRIMARY KEY(item_id, objective_id));
CREATE TABLE questionnaire_item_questions (item_id UUID NOT NULL REFERENCES questionnaire_items(id) ON DELETE CASCADE, question_id UUID NOT NULL REFERENCES research_questions(id), PRIMARY KEY(item_id, question_id));
CREATE TABLE questionnaire_item_hypotheses (item_id UUID NOT NULL REFERENCES questionnaire_items(id) ON DELETE CASCADE, hypothesis_id UUID NOT NULL REFERENCES research_hypotheses(id), PRIMARY KEY(item_id, hypothesis_id));
CREATE TABLE questionnaire_item_conceptual_variables (item_id UUID NOT NULL REFERENCES questionnaire_items(id) ON DELETE CASCADE, variable_id UUID NOT NULL REFERENCES conceptual_variables(id), PRIMARY KEY(item_id, variable_id));

CREATE TABLE interview_guides (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL UNIQUE REFERENCES research_instruments(id) ON DELETE CASCADE,
    opening_script TEXT,
    closing_script TEXT,
    estimated_duration_minutes INTEGER CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0),
    interview_type VARCHAR(40) NOT NULL,
    interviewer_instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE interview_guide_sections (id UUID PRIMARY KEY, guide_id UUID NOT NULL REFERENCES interview_guides(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, description TEXT, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(guide_id, display_order));
CREATE INDEX idx_interview_sections_guide ON interview_guide_sections(guide_id);
CREATE TABLE interview_questions (id UUID PRIMARY KEY, section_id UUID NOT NULL REFERENCES interview_guide_sections(id) ON DELETE CASCADE, question_code VARCHAR(40) NOT NULL, question_text TEXT NOT NULL, display_order INTEGER NOT NULL CHECK(display_order >= 1), required BOOLEAN NOT NULL DEFAULT FALSE, origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')), created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(section_id, question_code), UNIQUE(section_id, display_order));
CREATE INDEX idx_interview_questions_section ON interview_questions(section_id);
CREATE TABLE interview_probes (id UUID PRIMARY KEY, question_id UUID NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE, probe_text TEXT NOT NULL, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(question_id, display_order));
CREATE INDEX idx_interview_probes_question ON interview_probes(question_id);
CREATE TABLE interview_question_objectives (question_id UUID NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE, objective_id UUID NOT NULL REFERENCES research_objectives(id), PRIMARY KEY(question_id, objective_id));
CREATE TABLE interview_question_research_questions (question_id UUID NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE, research_question_id UUID NOT NULL REFERENCES research_questions(id), PRIMARY KEY(question_id, research_question_id));
CREATE TABLE interview_question_hypotheses (question_id UUID NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE, hypothesis_id UUID NOT NULL REFERENCES research_hypotheses(id), PRIMARY KEY(question_id, hypothesis_id));
CREATE TABLE interview_question_conceptual_variables (question_id UUID NOT NULL REFERENCES interview_questions(id) ON DELETE CASCADE, variable_id UUID NOT NULL REFERENCES conceptual_variables(id), PRIMARY KEY(question_id, variable_id));

CREATE TABLE focus_group_guides (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL UNIQUE REFERENCES research_instruments(id) ON DELETE CASCADE,
    facilitator_instructions TEXT,
    opening_script TEXT,
    closing_script TEXT,
    planned_duration_minutes INTEGER CHECK (planned_duration_minutes IS NULL OR planned_duration_minutes > 0),
    recommended_min_participants INTEGER CHECK (recommended_min_participants IS NULL OR recommended_min_participants > 0),
    recommended_max_participants INTEGER CHECK (recommended_max_participants IS NULL OR recommended_max_participants > 0),
    group_composition_guidance TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (recommended_min_participants IS NULL OR recommended_max_participants IS NULL OR recommended_min_participants <= recommended_max_participants)
);
CREATE TABLE focus_group_sections (id UUID PRIMARY KEY, guide_id UUID NOT NULL REFERENCES focus_group_guides(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, description TEXT, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(guide_id, display_order));
CREATE INDEX idx_focus_sections_guide ON focus_group_sections(guide_id);
CREATE TABLE focus_group_questions (id UUID PRIMARY KEY, section_id UUID NOT NULL REFERENCES focus_group_sections(id) ON DELETE CASCADE, question_code VARCHAR(40) NOT NULL, question_text TEXT NOT NULL, type VARCHAR(40) NOT NULL, display_order INTEGER NOT NULL CHECK(display_order >= 1), moderator_notes TEXT, origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')), UNIQUE(section_id, question_code), UNIQUE(section_id, display_order));
CREATE INDEX idx_focus_questions_section ON focus_group_questions(section_id);
CREATE TABLE focus_group_probes (id UUID PRIMARY KEY, question_id UUID NOT NULL REFERENCES focus_group_questions(id) ON DELETE CASCADE, probe_text TEXT NOT NULL, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(question_id, display_order));
CREATE TABLE focus_group_question_objectives (question_id UUID NOT NULL REFERENCES focus_group_questions(id) ON DELETE CASCADE, objective_id UUID NOT NULL REFERENCES research_objectives(id), PRIMARY KEY(question_id, objective_id));
CREATE TABLE focus_group_question_research_questions (question_id UUID NOT NULL REFERENCES focus_group_questions(id) ON DELETE CASCADE, research_question_id UUID NOT NULL REFERENCES research_questions(id), PRIMARY KEY(question_id, research_question_id));

CREATE TABLE observation_checklists (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL UNIQUE REFERENCES research_instruments(id) ON DELETE CASCADE,
    observation_type VARCHAR(40) NOT NULL,
    observer_instructions TEXT,
    observation_setting TEXT,
    estimated_duration_minutes INTEGER CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE observation_checklist_sections (id UUID PRIMARY KEY, checklist_id UUID NOT NULL REFERENCES observation_checklists(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, description TEXT, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(checklist_id, display_order));
CREATE INDEX idx_observation_sections_checklist ON observation_checklist_sections(checklist_id);
CREATE TABLE observation_items (id UUID PRIMARY KEY, section_id UUID NOT NULL REFERENCES observation_checklist_sections(id) ON DELETE CASCADE, item_code VARCHAR(40) NOT NULL, description TEXT NOT NULL, type VARCHAR(40) NOT NULL, required BOOLEAN NOT NULL DEFAULT FALSE, display_order INTEGER NOT NULL CHECK(display_order >= 1), origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')), UNIQUE(section_id, item_code), UNIQUE(section_id, display_order));
CREATE INDEX idx_observation_items_section ON observation_items(section_id);
CREATE TABLE observation_options (id UUID PRIMARY KEY, item_id UUID NOT NULL REFERENCES observation_items(id) ON DELETE CASCADE, label VARCHAR(255) NOT NULL, numeric_value DOUBLE PRECISION, display_order INTEGER NOT NULL CHECK(display_order >= 1), UNIQUE(item_id, display_order));
CREATE TABLE observation_item_objectives (item_id UUID NOT NULL REFERENCES observation_items(id) ON DELETE CASCADE, objective_id UUID NOT NULL REFERENCES research_objectives(id), PRIMARY KEY(item_id, objective_id));
CREATE TABLE observation_item_questions (item_id UUID NOT NULL REFERENCES observation_items(id) ON DELETE CASCADE, question_id UUID NOT NULL REFERENCES research_questions(id), PRIMARY KEY(item_id, question_id));
CREATE TABLE observation_item_conceptual_variables (item_id UUID NOT NULL REFERENCES observation_items(id) ON DELETE CASCADE, variable_id UUID NOT NULL REFERENCES conceptual_variables(id), PRIMARY KEY(item_id, variable_id));

CREATE TABLE instrument_pilot_studies (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    planned_participant_count INTEGER CHECK (planned_participant_count IS NULL OR planned_participant_count > 0),
    actual_participant_count INTEGER CHECK (actual_participant_count IS NULL OR actual_participant_count >= 0),
    participant_description TEXT,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    procedure TEXT,
    observations TEXT,
    issues_identified TEXT,
    modifications_recommended TEXT,
    modifications_implemented TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instrument_validity_assessments (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','INVALIDATED','SUPERSEDED')),
    method_description TEXT NOT NULL,
    findings TEXT,
    interpretation TEXT,
    score DOUBLE PRECISION,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instrument_reliability_assessments (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    method VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','INVALIDATED','SUPERSEDED')),
    coefficient DOUBLE PRECISION,
    item_count INTEGER CHECK (item_count IS NULL OR item_count >= 1),
    respondent_count INTEGER CHECK (respondent_count IS NULL OR respondent_count >= 1),
    assumptions TEXT,
    interpretation TEXT,
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('USER','AI_ASSISTED','AI_GENERATED','IMPORTED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expert_reviews (
    id UUID PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES research_instruments(id) ON DELETE CASCADE,
    reviewer_code VARCHAR(100) NOT NULL,
    reviewer_role_or_expertise VARCHAR(255),
    review_date DATE,
    overall_comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expert_review_item_ratings (
    id UUID PRIMARY KEY,
    expert_review_id UUID NOT NULL REFERENCES expert_reviews(id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL CHECK (target_type IN ('QUESTIONNAIRE_ITEM','INTERVIEW_QUESTION','FOCUS_GROUP_QUESTION','OBSERVATION_ITEM')),
    target_id UUID NOT NULL,
    criterion VARCHAR(40) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_expert_ratings_review ON expert_review_item_ratings(expert_review_id);
