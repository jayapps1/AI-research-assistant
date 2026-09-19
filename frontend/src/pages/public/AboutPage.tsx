import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  Award,
  CheckCircle,
  Lock,
  ArrowRight,
} from 'lucide-react';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import { PublicStatisticsSection } from '../../features/publicSite/PublicStatisticsSection';

export function AboutPage() {
  return (
    <div className="public-about-page" id="about-page-content">
      <SeoMetadata
        title="About & Academic Mission | AI Research Assistant"
        description="Learn about our mission to restore empirical rigor and auditable transparency to scholarly research using responsible, citation-anchored artificial intelligence."
      />

      {/* Hero Header */}
      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Academic Rigor & Integrity</span>
          <h1 className="public-page-title">Empowering Scholars with Auditable AI</h1>
          <p className="public-page-subtitle">
            We are dedicated to building research intelligence tools that uphold the highest standards of scientific
            reproducibility, academic honesty, and methodological transparency.
          </p>
        </div>
      </section>

      {/* Mission & Vision */}
      <section className="public-section">
        <div className="public-container">
          <div className="about-split-grid">
            <div className="about-text-col">
              <span className="section-eyebrow">Our Foundational Belief</span>
              <h2>AI Should Augment Human Rigor, Never Replace Scholarly Thinking</h2>
              <p>
                The explosion of generative AI has created severe risks in academia: fabricated citations, ungrounded
                hallucinations, and ghostwritten prose that undermines intellectual growth.
              </p>
              <p>
                <strong>AI Research Assistant was founded on the opposite premise:</strong> artificial intelligence
                must serve as an evidence-anchored research copilot. It speeds up the grueling clerical and procedural
                stages of scholarship — extracting data matrices, checking statistical assumptions, transcribing and
                organizing qualitative codes — while keeping the human researcher in absolute control of hypotheses,
                critical interpretations, and conclusions.
              </p>
            </div>

            <div className="about-principles-card">
              <h3>The 4 Non-Negotiable Pillars</h3>
              <ul className="principles-list">
                <li>
                  <div className="principle-bullet">
                    <CheckCircle size={18} className="text-primary" />
                  </div>
                  <div>
                    <strong>100% Citation Grounding</strong>
                    <p>Every factual assertion links to verified DOI, page number, and sentence coordinates.</p>
                  </div>
                </li>
                <li>
                  <div className="principle-bullet">
                    <CheckCircle size={18} className="text-primary" />
                  </div>
                  <div>
                    <strong>Human-in-the-Loop Governance</strong>
                    <p>No draft or code is finalized without explicit researcher review, verification, and sign-off.</p>
                  </div>
                </li>
                <li>
                  <div className="principle-bullet">
                    <CheckCircle size={18} className="text-primary" />
                  </div>
                  <div>
                    <strong>Data Privacy & Sovereignty</strong>
                    <p>Your unpublished manuscripts and private datasets are never used to train foundation models.</p>
                  </div>
                </li>
                <li>
                  <div className="principle-bullet">
                    <CheckCircle size={18} className="text-primary" />
                  </div>
                  <div>
                    <strong>Anti-Ghostwriting Architecture</strong>
                    <p>
                      We do not write turnkey dissertations. We scaffold empirical reasoning, systematic review
                      matrices, and analytical methodologies.
                    </p>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* Platform Scale & Impact */}
      <PublicStatisticsSection
        eyebrow="Platform Scale"
        title="Empowering Research Across the Globe"
        subtitle="Transparent metrics on our growing network of researchers, validated studies, and analyzed academic documents."
      />

      {/* Responsible AI & COPE Alignment */}
      <section className="public-section bg-subtle">
        <div className="public-container">
          <div className="section-header text-center">
            <span className="section-eyebrow">Publication Ethics</span>
            <h2 className="section-title">Aligned with International Scholarly Standards</h2>
            <p className="section-subtitle">
              Engineered to satisfy institutional review boards (IRBs) and major journal publishers (Elsevier, Springer Nature, Wiley, IEEE).
            </p>
          </div>

          <div className="public-cards-grid three-col">
            <div className="feature-card">
              <ShieldCheck size={28} className="text-primary mb-3" />
              <h3>COPE & ICMJE Compliance</h3>
              <p>
                Follows Committee on Publication Ethics (COPE) position statements regarding AI author attribution,
                acknowledgment declarations, and transparent methodology disclosure.
              </p>
            </div>

            <div className="feature-card">
              <Lock size={28} className="text-primary mb-3" />
              <h3>Confidentiality & GDPR</h3>
              <p>
                Strict data isolation. All participant interview transcripts and sensitive survey items are encrypted
                using AES-256 and stored in compliant regional infrastructure.
              </p>
            </div>

            <div className="feature-card">
              <Award size={28} className="text-primary mb-3" />
              <h3>Reproducible Output</h3>
              <p>
                Every qualitative coding decision and statistical test logs parameter configurations, package versions,
                and formula equations for full dissertation defense audits.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Leadership & Academic Community */}
      <section className="public-section">
        <div className="public-container text-center">
          <div className="section-header">
            <span className="section-eyebrow">Community & Fellowship</span>
            <h2 className="section-title">Built by Researchers, for Researchers</h2>
            <p className="section-subtitle">
              Our engineering and methodology advisors span disciplines including public health, empirical economics,
              educational psychology, and computational sciences.
            </p>
          </div>

          <div className="about-stats-grid">
            <div className="stat-box">
              <div className="stat-number">100%</div>
              <div className="stat-label">Citation Grounded</div>
            </div>
            <div className="stat-box">
              <div className="stat-number">23+</div>
              <div className="stat-label">Disciplines Supported</div>
            </div>
            <div className="stat-box">
              <div className="stat-number">0%</div>
              <div className="stat-label">Model Training on User Data</div>
            </div>
            <div className="stat-box">
              <div className="stat-number">24/7</div>
              <div className="stat-label">Audit Log Availability</div>
            </div>
          </div>

          <div className="mt-12">
            <Link to={paths.register} className="btn btn-primary btn-lg">
              <span>Experience Grounded Academic Research</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
