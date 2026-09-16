import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, Lock } from 'lucide-react';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';

export function PrivacyPolicyPage() {
  return (
    <div className="public-legal-page" id="privacy-page-content">
      <SeoMetadata
        title="Privacy Policy & Data Protection | AI Research Assistant"
        description="Our policy on researcher data confidentiality, non-training commitments on private datasets, and encryption standards."
      />

      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Data Sovereignty & Privacy</span>
          <h1 className="public-page-title">Privacy Policy</h1>
          <p className="public-page-subtitle">Last updated: September 2026 • Version 1.0 (Academic Edition)</p>
        </div>
      </section>

      <section className="public-section">
        <div className="public-container legal-container">
          <div className="legal-notice-box">
            <ShieldAlert size={20} className="text-warning flex-shrink-0" />
            <div>
              <strong>Institutional Review Notice:</strong> This document represents our default academic privacy policy.
              Universities with custom Master Service Agreements or Data Processing Addenda (DPAs) can request
              bespoke terms via <Link to={paths.contact}>Institutional Support</Link>.
            </div>
          </div>

          <div className="legal-body">
            <h2>1. Core Non-Negotiable Commitment: Zero Model Training on User Data</h2>
            <p>
              We recognize that academic research involves proprietary data, unpublished hypotheses, and sensitive
              human participant interviews. Under no circumstances do we train, fine-tune, or reinforce any public or
              shared artificial intelligence models using your uploaded documents, notes, survey tables, or audio transcripts.
            </p>

            <h2>2. Information We Collect</h2>
            <p>
              We collect account identifiers (name, institutional email address, password hash), workspace collaboration
              metadata, audit logs of analysis queries, and technical diagnostic telemetry.
            </p>

            <h2>3. Encryption & Data Storage</h2>
            <p>
              All researcher content is encrypted in transit via TLS 1.3 and at rest using AES-256. Database instances
              are partitioned by tenant workspaces with strict role-based access control (RBAC).
            </p>

            <h2>4. Researcher Rights & GDPR Compliance</h2>
            <p>
              Under applicable data protection laws (including GDPR and CCPA), you retain full rights to export your
              complete project workspaces, download verbatim audit logs, or permanently delete your account and associated assets.
            </p>

            <div className="mt-8">
              <Link to={paths.home} className="btn btn-secondary">
                <ArrowLeft size={16} />
                <span>Return to Home</span>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export function TermsOfServicePage() {
  return (
    <div className="public-legal-page" id="terms-page-content">
      <SeoMetadata
        title="Terms of Academic Use | AI Research Assistant"
        description="Terms and conditions governing the academic and ethical use of the AI Research Assistant workbench."
      />

      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Academic Standards & Terms</span>
          <h1 className="public-page-title">Terms of Service</h1>
          <p className="public-page-subtitle">Last updated: September 2026 • Version 1.0 (Scholarly Edition)</p>
        </div>
      </section>

      <section className="public-section">
        <div className="public-container legal-container">
          <div className="legal-notice-box">
            <Lock size={20} className="text-warning flex-shrink-0" />
            <div>
              <strong>Academic Integrity Clause:</strong> This platform is an intellectual copilot designed to assist
              with evidence matrix organization, methodology validation, and qualitative/quantitative procedures.
              It is not a ghostwriting service.
            </div>
          </div>

          <div className="legal-body">
            <h2>1. Acceptance of Terms</h2>
            <p>
              By accessing or utilizing the AI Research Assistant platform, you agree to comply with these terms, your
              home institution’s research ethics guidelines, and applicable international publication policies.
            </p>

            <h2>2. Ethical Use & Academic Honesty</h2>
            <p>
              You agree not to misrepresent AI-assisted exploratory drafts as pure human authorship without appropriate
              acknowledgment where required by journal guidelines, institutional honor codes, or thesis examination rules.
            </p>

            <h2>3. Subscription & Billing Terms</h2>
            <p>
              Paid subscription tiers provide defined quotas for document processing, statistical runs, and qualitative
              audio transcriptions. Subscriptions may be canceled at any time from your account billing console.
            </p>

            <h2>4. Limitation of Liability</h2>
            <p>
              While our system employs evidence-anchoring and citation checks, the researcher remains solely responsible
              for verifying empirical calculations, verifying ethics committee approvals, and defending findings before
              examination committees.
            </p>

            <div className="mt-8">
              <Link to={paths.home} className="btn btn-secondary">
                <ArrowLeft size={16} />
                <span>Return to Home</span>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
