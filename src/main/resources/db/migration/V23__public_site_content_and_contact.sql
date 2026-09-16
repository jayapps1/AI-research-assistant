-- ============================================================
-- AI RESEARCH ASSISTANT
-- V23 - PUBLIC SITE CONTENT, SERVICES, FAQS, AND CONTACT MANAGEMENT
-- ============================================================

-- ------------------------------------------------------------
-- 1. PUBLIC SITE SETTINGS
-- ------------------------------------------------------------
CREATE TABLE public_site_settings (
    id UUID PRIMARY KEY,
    site_name VARCHAR(120) NOT NULL,
    tagline VARCHAR(255),
    support_email VARCHAR(254),
    support_phone VARCHAR(50),
    contact_address VARCHAR(500),
    facebook_url VARCHAR(500),
    instagram_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    youtube_url VARCHAR(500),
    x_url VARCHAR(500),
    logo_storage_key VARCHAR(255),
    favicon_storage_key VARCHAR(255),
    default_meta_title VARCHAR(255),
    default_meta_description TEXT,
    copyright_text VARCHAR(255),
    registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    public_pricing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- 2. PUBLIC PAGES
-- ------------------------------------------------------------
CREATE TABLE public_pages (
    id UUID PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500),
    meta_title VARCHAR(255),
    meta_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    show_in_navigation BOOLEAN NOT NULL DEFAULT TRUE,
    navigation_order INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMP WITH TIME ZONE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_public_pages_slug UNIQUE (slug),
    CONSTRAINT chk_public_pages_type CHECK (type IN ('HOME','SERVICES','PRICING','ABOUT','FAQ','CONTACT','CUSTOM')),
    CONSTRAINT chk_public_pages_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT chk_public_pages_nav_order CHECK (navigation_order >= 0)
);
CREATE INDEX idx_public_pages_slug ON public_pages(slug);
CREATE INDEX idx_public_pages_status ON public_pages(status);
CREATE INDEX idx_public_pages_nav ON public_pages(show_in_navigation, navigation_order);

-- ------------------------------------------------------------
-- 3. PUBLIC PAGE SECTIONS
-- ------------------------------------------------------------
CREATE TABLE public_page_sections (
    id UUID PRIMARY KEY,
    page_id UUID NOT NULL REFERENCES public_pages(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    eyebrow VARCHAR(200),
    heading VARCHAR(300),
    subheading VARCHAR(500),
    body TEXT,
    image_storage_key VARCHAR(255),
    primary_cta_label VARCHAR(100),
    primary_cta_url VARCHAR(500),
    secondary_cta_label VARCHAR(100),
    secondary_cta_url VARCHAR(500),
    configuration_json TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_public_page_sections_page_key UNIQUE (page_id, section_key),
    CONSTRAINT chk_public_page_sections_type CHECK (type IN (
        'HERO','FEATURES','SERVICE_SUMMARY','HOW_IT_WORKS','BENEFITS',
        'STATISTICS','CTA','TEXT','IMAGE_TEXT','PRICING_PREVIEW',
        'FAQ_PREVIEW','CONTACT_PREVIEW','CUSTOM'
    )),
    CONSTRAINT chk_public_page_sections_order CHECK (display_order >= 0)
);
CREATE INDEX idx_public_page_sections_page_order ON public_page_sections(page_id, display_order);

-- ------------------------------------------------------------
-- 4. SERVICE OFFERINGS
-- ------------------------------------------------------------
CREATE TABLE service_offerings (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    short_description VARCHAR(500) NOT NULL,
    description TEXT,
    icon_key VARCHAR(100),
    feature_list_json TEXT,
    cta_label VARCHAR(100),
    cta_url VARCHAR(500),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_service_offerings_code UNIQUE (code),
    CONSTRAINT chk_service_offerings_status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT chk_service_offerings_order CHECK (display_order >= 0)
);
CREATE INDEX idx_service_offerings_status_order ON service_offerings(status, display_order);

-- ------------------------------------------------------------
-- 5. FAQ ITEMS
-- ------------------------------------------------------------
CREATE TABLE faq_items (
    id UUID PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_faq_items_category CHECK (category IN (
        'GENERAL','AI','RESEARCH','DOCUMENTS','COLLABORATION',
        'PRICING','PAYMENTS','SECURITY','PRIVACY','ACCOUNT','OTHER'
    )),
    CONSTRAINT chk_faq_items_order CHECK (display_order >= 0)
);
CREATE INDEX idx_faq_items_pub_order ON faq_items(published, display_order);
CREATE INDEX idx_faq_items_category ON faq_items(category);

-- ------------------------------------------------------------
-- 6. CONTACT SUBMISSIONS
-- ------------------------------------------------------------
CREATE SEQUENCE contact_submission_ref_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE contact_submissions (
    id UUID PRIMARY KEY,
    reference_code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(50),
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(5000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    source VARCHAR(30) NOT NULL DEFAULT 'PUBLIC_WEBSITE',
    ip_hash VARCHAR(128),
    user_agent_summary VARCHAR(255),
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_read_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_contact_submissions_ref UNIQUE (reference_code),
    CONSTRAINT chk_contact_submissions_status CHECK (status IN ('NEW','READ','IN_PROGRESS','RESPONDED','CLOSED','SPAM')),
    CONSTRAINT chk_contact_submissions_source CHECK (source IN ('PUBLIC_WEBSITE','AUTHENTICATED_USER','OTHER'))
);
CREATE INDEX idx_contact_submissions_status_time ON contact_submissions(status, submitted_at);
CREATE INDEX idx_contact_submissions_ref ON contact_submissions(reference_code);
CREATE INDEX idx_contact_submissions_email ON contact_submissions(email);

-- ------------------------------------------------------------
-- 7. CONTACT RESPONSES
-- ------------------------------------------------------------
CREATE TABLE contact_responses (
    id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES contact_submissions(id) ON DELETE CASCADE,
    responded_by UUID NOT NULL REFERENCES users(id),
    response_message TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
    provider_message_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_contact_responses_channel CHECK (channel IN ('EMAIL','SMS','INTERNAL_NOTE')),
    CONSTRAINT chk_contact_responses_status CHECK (status IN ('DRAFT','SENT','FAILED'))
);
CREATE INDEX idx_contact_responses_submission ON contact_responses(submission_id);
