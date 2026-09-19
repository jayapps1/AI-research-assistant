import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  ShieldCheck,
  Sparkles,
  BarChart3,
  FileCheck,
  Layers,
  Database,
  Search,
  GraduationCap,
  Scale,
  BrainCircuit,
  Lock,
  ChevronDown,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import { useAuth } from '../../auth/AuthProvider';
import { PublicStatisticsSection } from '../../features/publicSite/PublicStatisticsSection';

export function HomePage() {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState<'qualitative' | 'quantitative' | 'literature'>('literature');
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'annual'>('annual');
  const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(null);

  // Fetch dynamic content from backend
  const { data: pageData } = useQuery({
    queryKey: ['publicPage', 'home'],
    queryFn: () => publicApi.getPageBySlug('home').catch(() => null),
    staleTime: 5 * 60 * 1000,
  });

  const { data: pricingData } = useQuery({
    queryKey: ['publicPricing'],
    queryFn: () => publicApi.getPricing().catch(() => []),
    staleTime: 5 * 60 * 1000,
  });

  const { data: faqData } = useQuery({
    queryKey: ['publicFaqs'],
    queryFn: () => publicApi.getFaqs().catch(() => []),
    staleTime: 5 * 60 * 1000,
  });

  const workflowSteps = [
    {
      step: '01',
      title: 'Problem Framing',
      desc: 'Formulate defensible research questions, operationalize constructs, and build conceptual frameworks with audit trails.',
      icon: Search,
    },
    {
      step: '02',
      title: 'Literature Matrix',
      desc: 'Synthesize empirical papers, identify thematic gaps, evaluate methodological quality, and build APA-compliant matrices.',
      icon: BookOpen,
    },
    {
      step: '03',
      title: 'Methodology Design',
      desc: 'Formulate sampling strategies, assess validity threats, design survey instruments, and prepare ethical review protocols.',
      icon: Scale,
    },
    {
      step: '04',
      title: 'Data Collection & Prep',
      desc: 'Import multimodal research records, screen outliers, establish codebooks, and clean survey responses with audit logs.',
      icon: Database,
    },
    {
      step: '05',
      title: 'Analysis Workbench',
      desc: 'Run qualitative thematic coding or quantitative tests with step-by-step mathematical justifications.',
      icon: BarChart3,
    },
    {
      step: '06',
      title: 'Report & Defense',
      desc: 'Compile defensible academic reports with transparent citations and simulate committee viva examinations.',
      icon: GraduationCap,
    },
  ];

  return (
    <div className="public-home-page" id="home-page-content">
      <SeoMetadata
        title={pageData?.metaTitle || 'AI Research Assistant | Grounded Academic Research Platform'}
        description={
          pageData?.metaDescription ||
          'A rigorous academic AI platform for literature reviews, research methodology, qualitative coding, statistical analysis, and auditable dissertation reporting.'
        }
      />

      {/* 1. HERO SECTION */}
      <section className="public-hero-section" id="hero-section">
        <div className="public-container">
          <div className="public-hero-badge">
            <ShieldCheck size={16} className="text-primary" />
            <span>Built Specifically for Higher Education & Rigorous Inquiry</span>
          </div>

          <h1 className="public-hero-title">
            From Research Question to <br />
            <span className="gradient-text">Auditable, Defensible Findings.</span>
          </h1>

          <p className="public-hero-subtitle">
            An evidence-first workbench for academic researchers, postgraduate scholars, and faculty.
            Synthesize literature, design robust empirical methodologies, code qualitative interviews, and generate
            reproducible quantitative insights without compromising academic integrity.
          </p>

          <div className="public-hero-actions">
            {isAuthenticated ? (
              <Link to={paths.app} className="btn btn-primary btn-lg" id="hero-launch-app">
                <span>Enter Research Workbench</span>
                <ArrowRight size={18} />
              </Link>
            ) : (
              <>
                <Link to={paths.register} className="btn btn-primary btn-lg" id="hero-get-started">
                  <span>Start Free Research</span>
                  <ArrowRight size={18} />
                </Link>
                <Link to={paths.services} className="btn btn-secondary btn-lg" id="hero-explore-services">
                  <span>Explore Capabilities</span>
                </Link>
              </>
            )}
          </div>

          <div className="public-hero-trust-row">
            <div className="trust-item">
              <CheckCircle2 size={16} className="text-success" />
              <span>Evidence-Anchored Citations</span>
            </div>
            <div className="trust-item">
              <CheckCircle2 size={16} className="text-success" />
              <span>Plagiarism-Resistant Synthesis</span>
            </div>
            <div className="trust-item">
              <CheckCircle2 size={16} className="text-success" />
              <span>Institutional Grade Privacy</span>
            </div>
            <div className="trust-item">
              <CheckCircle2 size={16} className="text-success" />
              <span>Zero Model Hallucinations Claim</span>
            </div>
          </div>
        </div>
      </section>

      {/* VERIFIED PLATFORM STATISTICS */}
      <PublicStatisticsSection />

      {/* 2. VALUE PILLARS (3 CORE PRINCIPLES) */}
      <section className="public-section bg-subtle" id="pillars-section">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Academic Rigor First</span>
            <h2 className="section-title">Designed for Empirical Defense, Not Casual Summaries</h2>
            <p className="section-subtitle">
              Unlike commercial conversational bots, every output in the AI Research Assistant is mathematically,
              methodologically, and textually tied to uploaded sources.
            </p>
          </div>

          <div className="public-cards-grid three-col">
            <div className="feature-card">
              <div className="card-icon">
                <BrainCircuit size={28} />
              </div>
              <h3 className="card-title">Grounded Evidence Model</h3>
              <p className="card-text">
                Every literature synthesis links directly to verified PDF DOI, page, and sentence coordinates.
                Never worry about fictitious references or fabricated citations.
              </p>
            </div>

            <div className="feature-card">
              <div className="card-icon">
                <FileCheck size={28} />
              </div>
              <h3 className="card-title">Transparent Methodological Proofs</h3>
              <p className="card-text">
                Generates step-by-step sampling logic, construct validity evaluations, and statistical test justifications
                tailored to your theoretical framework.
              </p>
            </div>

            <div className="feature-card">
              <div className="card-icon">
                <Lock size={28} />
              </div>
              <h3 className="card-title">Researcher Data Sovereignty</h3>
              <p className="card-text">
                Your qualitative interview transcripts, raw survey datasets, and unpublished manuscripts are never used
                to train foundation models. Enterprise AES-256 encryption at rest.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 3. RESEARCH LIFECYCLE WORKFLOW */}
      <section className="public-section" id="workflow-section">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">End-to-End Governance</span>
            <h2 className="section-title">The Complete Research Journey in One Platform</h2>
            <p className="section-subtitle">
              Structured modules that mirror the doctoral and postdoctoral research lifecycle.
            </p>
          </div>

          <div className="public-workflow-grid">
            {workflowSteps.map((s) => {
              const Icon = s.icon;
              return (
                <div key={s.step} className="workflow-step-card">
                  <div className="workflow-step-num">{s.step}</div>
                  <div className="workflow-step-icon">
                    <Icon size={24} />
                  </div>
                  <h3 className="workflow-step-title">{s.title}</h3>
                  <p className="workflow-step-desc">{s.desc}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* 4. WORKBENCH SHOWCASE WITH TABS */}
      <section className="public-section bg-subtle" id="workbench-showcase">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Deep Workbenches</span>
            <h2 className="section-title">Specialized Tools for Every Academic Discipline</h2>
            <p className="section-subtitle">
              Toggle between analysis environments built with doctoral-level workflows in mind.
            </p>
          </div>

          <div className="workbench-tabs-nav">
            <button
              type="button"
              className={`workbench-tab-btn ${activeTab === 'literature' ? 'active' : ''}`}
              onClick={() => setActiveTab('literature')}
            >
              <BookOpen size={18} />
              <span>Literature Matrix</span>
            </button>
            <button
              type="button"
              className={`workbench-tab-btn ${activeTab === 'qualitative' ? 'active' : ''}`}
              onClick={() => setActiveTab('qualitative')}
            >
              <Layers size={18} />
              <span>Qualitative Thematic Coding</span>
            </button>
            <button
              type="button"
              className={`workbench-tab-btn ${activeTab === 'quantitative' ? 'active' : ''}`}
              onClick={() => setActiveTab('quantitative')}
            >
              <BarChart3 size={18} />
              <span>Quantitative & Statistical</span>
            </button>
          </div>

          <div className="workbench-tab-content">
            {activeTab === 'literature' && (
              <div className="workbench-panel">
                <div className="workbench-panel-text">
                  <h3>Exhaustive Systematic Reviews in Days, Not Months</h3>
                  <p>
                    Synthesize hundreds of empirical publications into structured comparative matrices. Automatically
                    extract sample sizes, methodologies, key findings, theoretical lenses, and critical limitations.
                  </p>
                  <ul className="workbench-features-list">
                    <li>Cross-paper comparative synthesis by research question</li>
                    <li>Automated identification of conflicting findings and literature gaps</li>
                    <li>BibTeX, Mendeley, and Zotero round-trip interoperability</li>
                    <li>Export to APA 7th, Harvard, and Chicago citation styles</li>
                  </ul>
                  <Link to={paths.services} className="btn btn-secondary">
                    <span>Learn More About Literature Synthesis</span>
                    <ArrowRight size={16} />
                  </Link>
                </div>
                <div className="workbench-panel-preview">
                  <div className="code-mockup-box">
                    <div className="mockup-header">Literature Matrix Preview</div>
                    <div className="mockup-row">
                      <span className="badge badge-outline">Author / Year</span>
                      <span className="badge badge-outline">Methodology</span>
                      <span className="badge badge-outline">Construct Validity</span>
                    </div>
                    <div className="mockup-body">
                      <div className="mockup-item">
                        <strong>Mensah et al. (2024)</strong>
                        <p>N=420 Mixed Methods • Structural Equation Modeling • High Convergent Validity</p>
                      </div>
                      <div className="mockup-item">
                        <strong>Adeyemi & Zhao (2023)</strong>
                        <p>N=32 Semi-Structured Interviews • Inductive Thematic Analysis • Cohen’s Kappa 0.88</p>
                      </div>
                      <div className="mockup-item">
                        <strong>Smith & Tanaka (2025)</strong>
                        <p>Quasi-Experimental Longitudinal • Repeated Measures ANOVA • p &lt; .001</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'qualitative' && (
              <div className="workbench-panel">
                <div className="workbench-panel-text">
                  <h3>Rigorous Thematic Coding with Verbatim Grounding</h3>
                  <p>
                    Import audio transcripts, focus group notes, and open-ended survey fields. Build inductive or
                    deductive codebooks with clear inclusion/exclusion definitions and full excerpt provenance.
                  </p>
                  <ul className="workbench-features-list">
                    <li>Interactive codebook manager with hierarchical categories</li>
                    <li>Inter-coder reliability calculations (Cohen’s Kappa, Fleiss’ Kappa)</li>
                    <li>Verbatim excerpt highlights linked to audio/text offsets</li>
                    <li>Defensible saturation tracking across interview batches</li>
                  </ul>
                  <Link to={paths.services} className="btn btn-secondary">
                    <span>Explore Qualitative Coding</span>
                    <ArrowRight size={16} />
                  </Link>
                </div>
                <div className="workbench-panel-preview">
                  <div className="code-mockup-box">
                    <div className="mockup-header">Qualitative Codebook & Excerpts</div>
                    <div className="mockup-item highlight">
                      <span className="badge badge-primary">Theme 1: Institutional Inertia</span>
                      <p>“The administrative clearance took nearly eight months before we could even deploy instruments...”</p>
                      <small>Participant #4 • Interview Transcript Page 12 • Line 45</small>
                    </div>
                    <div className="mockup-item">
                      <span className="badge badge-secondary">Theme 2: Methodological Adaptation</span>
                      <p>“We transitioned from paper surveys to offline mobile forms due to unstable connectivity...”</p>
                      <small>Participant #7 • Fieldwork Notes</small>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'quantitative' && (
              <div className="workbench-panel">
                <div className="workbench-panel-text">
                  <h3>Auditable Statistical Analysis with APA 7 Output</h3>
                  <p>
                    Clean, screen, and test your survey and experimental data. Run normality checks, reliability
                    coefficients (Cronbach’s alpha), correlations, regressions, and factorial analyses with verified mathematical steps.
                  </p>
                  <ul className="workbench-features-list">
                    <li>Exploratory & Confirmatory Factor Analysis helpers</li>
                    <li>Automatic verification of statistical assumptions (Homoscedasticity, Normality)</li>
                    <li>One-click publication-grade APA 7 formatted tables</li>
                    <li>Comprehensive mathematical documentation for committee review</li>
                  </ul>
                  <Link to={paths.services} className="btn btn-secondary">
                    <span>Explore Quantitative Analysis</span>
                    <ArrowRight size={16} />
                  </Link>
                </div>
                <div className="workbench-panel-preview">
                  <div className="code-mockup-box">
                    <div className="mockup-header">APA Formatted Output Example</div>
                    <div className="mockup-table">
                      <div className="table-row head">
                        <span>Variable</span>
                        <span>M</span>
                        <span>SD</span>
                        <span>t(148)</span>
                        <span>p</span>
                      </div>
                      <div className="table-row">
                        <span>Research Self-Efficacy</span>
                        <span>4.21</span>
                        <span>0.68</span>
                        <span>4.82</span>
                        <span>&lt; .001</span>
                      </div>
                      <div className="table-row">
                        <span>Data Literacy</span>
                        <span>3.89</span>
                        <span>0.74</span>
                        <span>3.45</span>
                        <span>.001</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 5. DEFENSE PREPARATION & VIVA SIMULATOR */}
      <section className="public-section" id="defense-prep">
        <div className="public-container">
          <div className="public-viva-card">
            <div className="viva-text">
              <span className="section-eyebrow">Viva Voce Readiness</span>
              <h2>Simulate Committee Interrogations Before You Defend</h2>
              <p>
                Our Defense Simulator interrogates your draft thesis against harsh reviewer standards. Discover
                methodological weak points, sample size vulnerabilities, and unstated epistemological assumptions before
                facing the examination board.
              </p>
              <div className="viva-perks">
                <div className="viva-perk">
                  <CheckCircle2 size={18} className="text-primary" />
                  <span>Generates adversarial questions tailored to your chosen methodology</span>
                </div>
                <div className="viva-perk">
                  <CheckCircle2 size={18} className="text-primary" />
                  <span>Provides counter-arguments with citation proofs to defend your design choices</span>
                </div>
                <div className="viva-perk">
                  <CheckCircle2 size={18} className="text-primary" />
                  <span>Assesses internal validity, construct validity, and transferability claims</span>
                </div>
              </div>
              <Link to={paths.register} className="btn btn-primary">
                <span>Start Defense Simulator</span>
                <ArrowRight size={16} />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 6. PRICING PREVIEW */}
      <section className="public-section bg-subtle" id="pricing-preview">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Transparent Academic Pricing</span>
            <h2 className="section-title">Invest in Your Scholarly Output</h2>
            <p className="section-subtitle">
              Fair, student-friendly plans with transparent quotas and no lock-in contracts.
            </p>

            <div className="pricing-billing-toggle">
              <button
                type="button"
                className={`toggle-btn ${billingCycle === 'monthly' ? 'active' : ''}`}
                onClick={() => setBillingCycle('monthly')}
              >
                Monthly
              </button>
              <button
                type="button"
                className={`toggle-btn ${billingCycle === 'annual' ? 'active' : ''}`}
                onClick={() => setBillingCycle('annual')}
              >
                Annual <span className="discount-tag">Save up to 25%</span>
              </button>
            </div>
          </div>

          <div className="public-pricing-grid">
            {pricingData && pricingData.length > 0 ? (
              pricingData.map((tier) => {
                const price = billingCycle === 'annual' ? tier.annualPrice / 12 : tier.monthlyPrice;
                return (
                  <div
                    key={tier.id}
                    className={`public-pricing-card ${tier.highlighted ? 'highlighted' : ''}`}
                  >
                    {tier.highlightBadge && (
                      <div className="pricing-card-badge">{tier.highlightBadge}</div>
                    )}
                    <h3 className="pricing-card-title">{tier.name}</h3>
                    <p className="pricing-card-desc">{tier.description}</p>
                    <div className="pricing-card-price">
                      <span className="currency">{tier.currency === 'USD' ? '$' : tier.currency}</span>
                      <span className="amount">{price.toFixed(0)}</span>
                      <span className="interval">/month</span>
                    </div>
                    {billingCycle === 'annual' && tier.annualPrice > 0 && (
                      <div className="annual-billed-note">
                        Billed annually (${tier.annualPrice}/yr)
                      </div>
                    )}

                    <ul className="pricing-features-list">
                      {tier.features.map((feat, i) => (
                        <li key={i}>
                          <CheckCircle2 size={16} className="text-primary" />
                          <span>{feat}</span>
                        </li>
                      ))}
                    </ul>

                    <div className="pricing-card-action">
                      <Link
                        to={paths.register}
                        className={`btn ${tier.highlighted ? 'btn-primary' : 'btn-secondary'} btn-block`}
                      >
                        {tier.ctaLabel || 'Get Started'}
                      </Link>
                    </div>
                  </div>
                );
              })
            ) : (
              // Fallback cards while loading
              <div className="loading-card text-center p-8">
                <p>Loading live academic subscription tiers...</p>
              </div>
            )}
          </div>

          <div className="text-center mt-8">
            <Link to={paths.pricing} className="btn btn-ghost">
              <span>Compare Full Feature & Quota Matrix</span>
              <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>

      {/* 7. FREQUENTLY ASKED QUESTIONS */}
      <section className="public-section" id="faq-preview">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Have Questions?</span>
            <h2 className="section-title">Academic Integrity & Technology FAQ</h2>
            <p className="section-subtitle">
              Clear answers regarding plagiarism policies, institutional compliance, and data sovereignty.
            </p>
          </div>

          <div className="public-faq-list">
            {(faqData && faqData.length > 0 ? faqData.slice(0, 5) : []).map((item, index) => {
              const isOpen = openFaqIndex === index;
              return (
                <div key={item.id} className={`faq-accordion-item ${isOpen ? 'open' : ''}`}>
                  <button
                    type="button"
                    className="faq-accordion-trigger"
                    onClick={() => setOpenFaqIndex(isOpen ? null : index)}
                    aria-expanded={isOpen}
                  >
                    <span className="faq-question">{item.question}</span>
                    <ChevronDown size={18} className={`faq-icon ${isOpen ? 'rotate' : ''}`} />
                  </button>
                  {isOpen && (
                    <div className="faq-accordion-body">
                      <p>{item.answerMarkdown}</p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="text-center mt-8">
            <Link to={paths.faq} className="btn btn-secondary">
              <span>View All Questions</span>
              <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>

      {/* 8. CALL TO ACTION BANNER */}
      <section className="public-cta-banner" id="bottom-cta">
        <div className="public-container text-center">
          <Sparkles size={36} className="cta-sparkle-icon" />
          <h2 className="cta-banner-title">Elevate Your Research to International Standards</h2>
          <p className="cta-banner-subtitle">
            Join thousands of researchers defending higher quality dissertations and empirical papers.
          </p>
          <div className="cta-banner-actions">
            <Link to={paths.register} className="btn btn-primary btn-lg">
              <span>Get Started Free</span>
              <ArrowRight size={18} />
            </Link>
            <Link to={paths.contact} className="btn btn-secondary btn-lg">
              <span>Request Institutional Demo</span>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
