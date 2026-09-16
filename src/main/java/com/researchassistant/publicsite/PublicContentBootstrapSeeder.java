package com.researchassistant.publicsite;

import com.researchassistant.publicsite.entity.*;
import com.researchassistant.publicsite.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Idempotently seeds initial public content on first boot if missing.
 * Existing administrator modifications are never overwritten on subsequent application restarts.
 */
@Component
@Order(100)
public class PublicContentBootstrapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicContentBootstrapSeeder.class);

    private final PublicSiteSettingsRepository settingsRepository;
    private final PublicPageRepository pageRepository;
    private final PublicPageSectionRepository sectionRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final FaqItemRepository faqRepository;

    public PublicContentBootstrapSeeder(PublicSiteSettingsRepository settingsRepository,
                                        PublicPageRepository pageRepository,
                                        PublicPageSectionRepository sectionRepository,
                                        ServiceOfferingRepository serviceRepository,
                                        FaqItemRepository faqRepository) {
        this.settingsRepository = settingsRepository;
        this.pageRepository = pageRepository;
        this.sectionRepository = sectionRepository;
        this.serviceRepository = serviceRepository;
        this.faqRepository = faqRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapSettingsIfMissing();
        bootstrapServicesIfMissing();
        bootstrapFaqsIfMissing();
        bootstrapPagesIfMissing();
    }

    private void bootstrapSettingsIfMissing() {
        if (settingsRepository.findTopByOrderByCreatedAtAsc().isEmpty()) {
            PublicSiteSettings s = new PublicSiteSettings();
            s.setSiteName("AI Research Assistant");
            s.setTagline("From Research Question to Final Report.");
            s.setSupportEmail("support@researchassistant.ai");
            s.setSupportPhone("+233 30 000 0000");
            s.setContactAddress("Accra, Ghana");
            s.setLinkedinUrl("https://linkedin.com/company/ai-research-assistant");
            s.setXUrl("https://x.com/airesearchassist");
            s.setDefaultMetaTitle("AI Research Assistant — Grounded Academic Research Platform");
            s.setDefaultMetaDescription("A responsible, source-grounded research assistant supporting literature review, methodology, data analysis, and traceable report generation.");
            s.setCopyrightText("© 2026 AI Research Assistant. All rights reserved.");
            s.setRegistrationEnabled(true);
            s.setPublicPricingEnabled(true);
            settingsRepository.save(s);
            log.info("Default public site settings seeded.");
        }
    }

    private void bootstrapServicesIfMissing() {
        if (serviceRepository.count() > 0) {
            return;
        }

        List<ServiceOffering> initialServices = List.of(
                createService("AI_RESEARCH_ASSISTANT", "AI Research Assistant",
                        "Source-grounded assistance for literature exploration, research questions, and critical synthesis.",
                        "Directly queries your uploaded research collection with contextual retrieval and verifiable citation attribution.",
                        "Sparkles", "[\"Source-grounded synthesis\",\"Question formulation support\",\"Zero hallucinated references policy\",\"Traceable document excerpts\"]",
                        "Explore Assistant", "/services#AI_RESEARCH_ASSISTANT", true, 1),

                createService("DOCUMENT_ANALYSIS_RAG", "Document Analysis & RAG",
                        "High-accuracy retrieval-augmented generation grounded strictly in your project documents.",
                        "Processes PDFs and research papers, indexing text into verifiable chunks that ground all AI answers.",
                        "FileText", "[\"PDF and DOCX text extraction\",\"Grounded semantic chunking\",\"Direct snippet attribution\",\"Strict evidence boundaries\"]",
                        "Learn More", "/services#DOCUMENT_ANALYSIS_RAG", true, 2),

                createService("LITERATURE_REVIEW", "Literature Review Assistance",
                        "Synthesize thematic patterns, theoretical frameworks, and research gaps across your corpus.",
                        "Identifies consensus, divergence, and gaps in published scholarship to strengthen your literature chapter.",
                        "BookOpen", "[\"Matrix-based literature synthesis\",\"Thematic grouping & gap analysis\",\"Cross-study comparisons\",\"Academic neutrality guidelines\"]",
                        "Learn More", "/services#LITERATURE_REVIEW", true, 3),

                createService("RESEARCH_METHODOLOGY", "Research Methodology Support",
                        "Formulate robust qualitative, quantitative, or mixed-methods research designs.",
                        "Guides sampling design, instrument justification, and methodological alignment with your research questions.",
                        "Compass", "[\"Alignment with research questions\",\"Sampling frame validation\",\"Mixed-methods justification\",\"Triangulation workflows\"]",
                        "Learn More", "/services#RESEARCH_METHODOLOGY", false, 4),

                createService("RESEARCH_INSTRUMENTS", "Research Instruments & Pilots",
                        "Draft, refine, and validate survey questionnaires and semi-structured interview protocols.",
                        "Aligns each survey item or interview question to specific research objectives and constructs.",
                        "Sliders", "[\"Construct-to-item mapping\",\"Survey validity reviews\",\"Interview protocol design\",\"Pilot study tracking\"]",
                        "Learn More", "/services#RESEARCH_INSTRUMENTS", false, 5),

                createService("QUANTITATIVE_ANALYSIS", "Quantitative Data Analysis",
                        "Clean tabular datasets, calculate descriptive statistics, and perform inferential testing.",
                        "Supports CSV data imports, variable coding, regression models, ANOVA, and hypothesis verification.",
                        "BarChart3", "[\"CSV dataset ingestion & cleaning\",\"Descriptive & inferential tests\",\"Statistical assumption checking\",\"APA formatted summary tables\"]",
                        "Learn More", "/services#QUANTITATIVE_ANALYSIS", false, 6),

                createService("QUALITATIVE_ANALYSIS", "Qualitative / Thematic Analysis",
                        "Conduct rigorous inductive and deductive qualitative coding across interviews and focus groups.",
                        "Develop coding frameworks, code interview transcripts, extract verbatim quotes, and discover core themes.",
                        "Layers", "[\"Inductive & deductive codebooks\",\"Verbatim quote retrieval\",\"Thematic mapping\",\"Inter-coder agreement tracking\"]",
                        "Learn More", "/services#QUALITATIVE_ANALYSIS", false, 7),

                createService("CITATION_MANAGEMENT", "Citation & Reference Management",
                        "Format, cross-check, and standardize citations across APA, IEEE, Harvard, Chicago, and MLA styles.",
                        "Detects missing bibliographic entries and ensures every in-text citation matches an approved source.",
                        "Bookmark", "[\"Multi-style formatting (APA 7th, IEEE, Harvard)\",\"In-text citation cross-referencing\",\"Missing bibliography alerts\",\"DOI & metadata verification\"]",
                        "Learn More", "/services#CITATION_MANAGEMENT", true, 8),

                createService("RESEARCH_INTEGRITY", "Research Integrity Review",
                        "Verify citation fidelity, factual claims, and adherence to responsible academic conduct standards.",
                        "Audits research claims against source documents to identify unverified assertions or misattributed findings.",
                        "ShieldCheck", "[\"Citation provenance auditing\",\"Assertion grounding checks\",\"Academic integrity reports\",\"Source transparency logs\"]",
                        "Learn More", "/services#RESEARCH_INTEGRITY", true, 9),

                createService("REPORT_GENERATION", "Research Report Generation",
                        "Assemble findings, methodology, literature, and tables into coherent academic report drafts.",
                        "Generates structured chapter drafts with embedded references and analytical summaries ready for supervisor review.",
                        "FileSpreadsheet", "[\"Structured chapter drafting\",\"Automated section synthesis\",\"Tables & figures inclusion\",\"Iterative collaborative revisions\"]",
                        "Learn More", "/services#REPORT_GENERATION", false, 10),

                createService("GROUP_COLLABORATION", "Collaboration for Group Projects",
                        "Work seamlessly with research team members, co-authors, and academic supervisors.",
                        "Role-based access control, task assignments, inline review comments, and real-time activity timelines.",
                        "Users", "[\"Role-based project access\",\"Supervisory review workflows\",\"Task tracking & milestones\",\"Audit trail of contributions\"]",
                        "Learn More", "/services#GROUP_COLLABORATION", false, 11),

                createService("DOCX_PDF_EXPORT", "DOCX / PDF Export",
                        "Export publication-ready documents formatted to university guidelines and journal standards.",
                        "Compiles full reports into Microsoft Word (.docx) and portable document format (.pdf) with clean typography.",
                        "Download", "[\"Microsoft Word (.docx) export\",\"Standard PDF compilation\",\"Table of contents generation\",\"Customizable formatting styles\"]",
                        "Learn More", "/services#DOCX_PDF_EXPORT", false, 12)
        );

        serviceRepository.saveAll(initialServices);
        log.info("Seeded 12 initial service offerings.");
    }

    private ServiceOffering createService(String code, String name, String shortDesc, String fullDesc,
                                          String iconKey, String featureJson, String ctaLabel, String ctaUrl,
                                          boolean featured, int order) {
        ServiceOffering s = new ServiceOffering();
        s.setCode(code);
        s.setName(name);
        s.setShortDescription(shortDesc);
        s.setDescription(fullDesc);
        s.setIconKey(iconKey);
        s.setFeatureListJson(featureJson);
        s.setCtaLabel(ctaLabel);
        s.setCtaUrl(ctaUrl);
        s.setFeatured(featured);
        s.setStatus(ServiceOfferingStatus.ACTIVE);
        s.setDisplayOrder(order);
        return s;
    }

    private void bootstrapFaqsIfMissing() {
        if (faqRepository.count() > 0) {
            return;
        }

        List<FaqItem> initialFaqs = List.of(
                createFaq("What is the AI Research Assistant?",
                        "The AI Research Assistant is an academic software platform that helps students, researchers, and project teams plan, organize, analyze, and report empirical research while upholding rigorous standards of academic integrity.",
                        FaqCategory.GENERAL, true, 1),

                createFaq("Does the AI invent or hallucinate references?",
                        "No. Unlike general-purpose chatbots, our assistant utilizes strict Retrieval-Augmented Generation (RAG) confined to your project's verified document repository. Every claim and citation is mapped directly to actual page and paragraph excerpts from your sources.",
                        FaqCategory.AI, true, 2),

                createFaq("Can I upload PDF documents?",
                        "Yes. You can upload academic papers, reports, and books in PDF and DOCX formats. The system automatically extracts text, performs OCR when necessary, structures content into searchable chunks, and maintains exact page references.",
                        FaqCategory.DOCUMENTS, true, 3),

                createFaq("Does it support quantitative analysis?",
                        "Yes. You can import structured datasets (CSV format) into the quantitative workbench to run descriptive statistics, frequency tables, correlation matrices, and inferential regression analyses.",
                        FaqCategory.RESEARCH, false, 4),

                createFaq("Does it support qualitative research?",
                        "Yes. The qualitative workbench supports both inductive and deductive thematic analysis. You can import interview transcripts, assign codes, extract verbatim quotes, and map emergent themes.",
                        FaqCategory.RESEARCH, false, 5),

                createFaq("Can I use it for a group project?",
                        "Yes. You can invite team members and assign specific project roles (Lead Researcher, Collaborator, Reviewer). Team members can collaborate on source collections, review comments, and compile report chapters.",
                        FaqCategory.COLLABORATION, true, 6),

                createFaq("Can supervisors collaborate?",
                        "Yes. Supervisors can be invited to projects with dedicated Reviewer access to inspect source evidence, monitor task progress, and leave inline academic guidance.",
                        FaqCategory.COLLABORATION, false, 7),

                createFaq("Which citation styles are supported?",
                        "The platform natively supports standard citation styles including APA 7th Edition, IEEE, Harvard, MLA, and Chicago format for both in-text citations and reference lists.",
                        FaqCategory.DOCUMENTS, false, 8),

                createFaq("Can I export my report to Word/PDF?",
                        "Yes. You can export complete project reports or individual chapters directly into formatted Microsoft Word (.docx) and PDF files complete with citations, tables, and references.",
                        FaqCategory.DOCUMENTS, false, 9),

                createFaq("How does the free plan work?",
                        "Our Free plan provides full access to core research tools, up to 5 projects, essential AI generation quotas, and DOCX exports so students can begin their research without financial commitment.",
                        FaqCategory.PRICING, true, 10),

                createFaq("How are payments processed?",
                        "Paid subscription tiers are securely processed through Paystack, supporting Mobile Money (MTN, Telecel, AT) and major debit/credit cards (Visa, Mastercard).",
                        FaqCategory.PAYMENTS, false, 11),

                createFaq("Can a failed Mobile Money payment be retried?",
                        "Yes. If a Mobile Money transaction times out or fails on your mobile phone, the billing dashboard provides an instant retry option without creating duplicate charges.",
                        FaqCategory.PAYMENTS, false, 12),

                createFaq("Is my research private and confidential?",
                        "Absolutely. Your uploaded documents, transcripts, and analysis datasets belong exclusively to your workspace. We do not use your private research data to train public AI models.",
                        FaqCategory.PRIVACY, true, 13),

                createFaq("What happens if the AI provider is temporarily unavailable?",
                        "Your documents, datasets, task boards, notes, and report drafts remain completely accessible locally. The system gracefully signals AI status and queues requests until service is restored.",
                        FaqCategory.SECURITY, false, 14),

                createFaq("Does the AI write my thesis automatically?",
                        "No. The platform is an academic assistant designed to augment your analytical capabilities, not replace your scholarship. It assists with source synthesis, structure, and analysis while you retain full intellectual authorship.",
                        FaqCategory.GENERAL, true, 15)
        );

        faqRepository.saveAll(initialFaqs);
        log.info("Seeded 15 initial FAQ items.");
    }

    private FaqItem createFaq(String question, String answer, FaqCategory category, boolean featured, int order) {
        FaqItem f = new FaqItem();
        f.setQuestion(question);
        f.setAnswer(answer);
        f.setCategory(category);
        f.setFeatured(featured);
        f.setPublished(true);
        f.setDisplayOrder(order);
        return f;
    }

    private void bootstrapPagesIfMissing() {
        if (pageRepository.count() > 0) {
            return;
        }

        // 1. HOME
        PublicPage home = createPage(PublicPageType.HOME, "home", "Home",
                "From Research Question to Final Report",
                "AI Research Assistant — Grounded Academic Research Platform",
                "Organize sources, design rigorous methodology, analyze empirical data, collaborate with your team, and generate traceable research reports.",
                0);
        home = pageRepository.save(home);

        createSection(home, PublicSectionType.HERO, "hero",
                "Responsible AI for Academic Research",
                "From Research Question to Final Report.",
                "Organize sources, design rigorous methodology, analyze data, collaborate with your team, and produce fully traceable research reports.",
                "Get Started", "/auth/register", "Explore Services", "/services", 1);

        createSection(home, PublicSectionType.FEATURES, "core_capabilities",
                "Built for Academic Rigor",
                "Comprehensive Research Capabilities",
                "Every tool is built around the empirical research lifecycle, ensuring methodological integrity and verifiable claims.",
                "View All Services", "/services", null, null, 2);

        createSection(home, PublicSectionType.HOW_IT_WORKS, "how_it_works",
                "A Structured Research Journey",
                "How the Platform Works",
                "Follow an academically proven workflow that keeps your evidence, analysis, and writing unified.",
                "Start Your Project", "/auth/register", null, null, 3);

        createSection(home, PublicSectionType.BENEFITS, "ai_trust",
                "Evidence-First Guarantee",
                "AI Grounded in Your Research Documents",
                "Zero hallucinated references. Every synthesis query retrieves authorized excerpts with exact page and paragraph provenance.",
                "See How AI Works", "/about", null, null, 4);

        createSection(home, PublicSectionType.STATISTICS, "stats",
                "Empowering Researchers",
                "Designed for Impact",
                "Trusted by undergraduate, postgraduate, and institutional research teams across disciplines.",
                null, null, null, null, 5);

        createSection(home, PublicSectionType.CTA, "final_cta",
                "Ready to Begin?",
                "Accelerate Your Research with Complete Academic Integrity",
                "Join thousands of scholars and students crafting rigorous, well-documented research projects.",
                "Get Started Free", "/auth/register", "Browse Pricing", "/pricing", 6);

        // 2. SERVICES
        PublicPage services = createPage(PublicPageType.SERVICES, "services", "Services",
                "Specialized Academic Research Capabilities",
                "Research Services & Modules — AI Research Assistant",
                "Explore our complete suite of research tools spanning literature review, methodology, data analysis, integrity verification, and report compilation.",
                1);
        services = pageRepository.save(services);

        createSection(services, PublicSectionType.HERO, "services_hero",
                "Our Capabilities",
                "End-to-End Academic Research Support",
                "Explore tools designed to support every phase of empirical research without cutting academic corners.",
                "Get Started", "/auth/register", "View Pricing", "/pricing", 1);

        // 3. PRICING
        PublicPage pricing = createPage(PublicPageType.PRICING, "pricing", "Pricing",
                "Transparent Plans for Students & Scholars",
                "Subscription Plans & Pricing — AI Research Assistant",
                "Choose the right plan for your academic journey. From free student starter access to advanced collaborative research tiers.",
                2);
        pricing = pageRepository.save(pricing);

        createSection(pricing, PublicSectionType.HERO, "pricing_hero",
                "Simple & Transparent",
                "Plans Tailored to Your Academic Needs",
                "Start free, upgrade as your research scales. Safe and instant Mobile Money & Card payment options.",
                "Get Started Free", "/auth/register", "Read FAQ", "/faq", 1);

        // 4. ABOUT
        PublicPage about = createPage(PublicPageType.ABOUT, "about", "About Us",
                "Empowering Rigorous Scholarship",
                "About AI Research Assistant — Mission & Academic Principles",
                "Learn about our mission, responsible AI principles, and dedication to academic integrity.",
                3);
        about = pageRepository.save(about);

        createSection(about, PublicSectionType.HERO, "about_hero",
                "Our Mission",
                "Empowering Rigorous, Traceable Scholarship",
                "We believe artificial intelligence in academia should elevate critical thinking, not replace intellectual effort.",
                "Join the Platform", "/auth/register", "Contact Team", "/contact", 1);

        createSection(about, PublicSectionType.TEXT, "responsible_ai",
                "Responsible AI Commitment",
                "Why the Platform Exists",
                "Traditional chatbots hallucinate references and encourage passive ghostwriting. We designed AI Research Assistant as an evidence-first workbench: documents must be uploaded, claims must be traceable, and methodology must be justified.",
                null, null, null, null, 2);

        createSection(about, PublicSectionType.TEXT, "integrity_principles",
                "Academic Integrity",
                "Core Design Principles",
                "1. Document Provenance: Claims link to real text excerpts.\n2. Methodological Rigor: Statistical and thematic tools adhere to formal research standards.\n3. Data Confidentiality: Your scholarship remains private to your project.",
                null, null, null, null, 3);

        // 5. FAQ
        PublicPage faq = createPage(PublicPageType.FAQ, "faq", "FAQ",
                "Frequently Asked Questions",
                "Frequently Asked Questions — AI Research Assistant",
                "Find answers to common questions about research capabilities, AI grounding, citation styles, privacy, and billing.",
                4);
        faq = pageRepository.save(faq);

        createSection(faq, PublicSectionType.HERO, "faq_hero",
                "Help & Answers",
                "Frequently Asked Questions",
                "Everything you need to know about our research tools, AI ground rules, collaboration, and pricing.",
                "Have More Questions?", "/contact", null, null, 1);

        // 6. CONTACT
        PublicPage contact = createPage(PublicPageType.CONTACT, "contact", "Contact Us",
                "Get in Touch with Our Academic Support Team",
                "Contact Us — AI Research Assistant",
                "Have questions about institutional licensing, academic collaborations, or platform features? Reach out to our support team.",
                5);
        contact = pageRepository.save(contact);

        createSection(contact, PublicSectionType.HERO, "contact_hero",
                "Support & Inquiries",
                "Get in Touch",
                "Our team is ready to answer questions regarding your research projects, feedback, or institutional partnerships.",
                null, null, null, null, 1);

        log.info("Seeded 6 initial public pages with structured content sections.");
    }

    private PublicPage createPage(PublicPageType type, String slug, String title, String subtitle,
                                  String metaTitle, String metaDescription, int navOrder) {
        PublicPage p = new PublicPage();
        p.setType(type);
        p.setSlug(slug);
        p.setTitle(title);
        p.setSubtitle(subtitle);
        p.setMetaTitle(metaTitle);
        p.setMetaDescription(metaDescription);
        p.setStatus(PublicPageStatus.PUBLISHED);
        p.setShowInNavigation(true);
        p.setNavigationOrder(navOrder);
        p.setPublishedAt(OffsetDateTime.now());
        return p;
    }

    private void createSection(PublicPage page, PublicSectionType type, String key, String eyebrow,
                               String heading, String subheading, String primaryCta, String primaryUrl,
                               String secCta, String secUrl, int order) {
        PublicPageSection s = new PublicPageSection();
        s.setPage(page);
        s.setType(type);
        s.setSectionKey(key);
        s.setEyebrow(eyebrow);
        s.setHeading(heading);
        s.setSubheading(subheading);
        s.setPrimaryCtaLabel(primaryCta);
        s.setPrimaryCtaUrl(primaryUrl);
        s.setSecondaryCtaLabel(secCta);
        s.setSecondaryCtaUrl(secUrl);
        s.setEnabled(true);
        s.setDisplayOrder(order);
        sectionRepository.save(s);
    }
}
