import { Link } from 'react-router-dom';
import { Mail, Phone, MapPin, ExternalLink, ShieldCheck } from 'lucide-react';
import { paths } from '../../routes/paths';
import type { PublicSiteSettingsResponse } from '../../types/publicSite';

interface PublicFooterProps {
  settings?: PublicSiteSettingsResponse | null;
}

export function PublicFooter({ settings }: PublicFooterProps) {
  const currentYear = new Date().getFullYear();
  const siteName = settings?.siteName || 'AI Research Assistant';
  const tagline = settings?.tagline || 'From Research Question to Auditable Final Report.';
  const copyrightText = settings?.copyrightText || `© ${currentYear} ${siteName}. All rights reserved.`;

  return (
    <footer className="public-footer" id="public-main-footer">
      <div className="public-container">
        <div className="public-footer-grid">
          {/* Brand Column */}
          <div className="public-footer-col brand-col">
            <div className="public-brand">
              <span className="public-brand-mark">RA</span>
              <span className="public-brand-name">{siteName}</span>
            </div>
            <p className="public-footer-tagline">{tagline}</p>
            <div className="public-academic-badge">
              <ShieldCheck size={16} className="text-primary" />
              <span>Grounded in Evidence • Human-in-the-Loop • Plagiarism Resistant</span>
            </div>
            {settings?.address && (
              <div className="public-footer-contact-item">
                <MapPin size={16} />
                <span>{settings.address}</span>
              </div>
            )}
            {settings?.supportEmail && (
              <div className="public-footer-contact-item">
                <Mail size={16} />
                <a href={`mailto:${settings.supportEmail}`} className="footer-link">
                  {settings.supportEmail}
                </a>
              </div>
            )}
            {settings?.supportPhone && (
              <div className="public-footer-contact-item">
                <Phone size={16} />
                <a href={`tel:${settings.supportPhone}`} className="footer-link">
                  {settings.supportPhone}
                </a>
              </div>
            )}
          </div>

          {/* Product & Solutions */}
          <div className="public-footer-col">
            <h3 className="public-footer-heading">Capabilities</h3>
            <ul className="public-footer-links">
              <li>
                <Link to={paths.services} className="footer-link">
                  Literature Synthesis & Matrix
                </Link>
              </li>
              <li>
                <Link to={paths.services} className="footer-link">
                  Methodology & Sampling Design
                </Link>
              </li>
              <li>
                <Link to={paths.services} className="footer-link">
                  Qualitative Thematic Coding
                </Link>
              </li>
              <li>
                <Link to={paths.services} className="footer-link">
                  Statistical Hypothesis Testing
                </Link>
              </li>
              <li>
                <Link to={paths.services} className="footer-link">
                  Viva & Defense Preparation
                </Link>
              </li>
            </ul>
          </div>

          {/* Navigation & Company */}
          <div className="public-footer-col">
            <h3 className="public-footer-heading">Navigation</h3>
            <ul className="public-footer-links">
              <li>
                <Link to={paths.home} className="footer-link">
                  Home
                </Link>
              </li>
              <li>
                <Link to={paths.about} className="footer-link">
                  About & Academic Mission
                </Link>
              </li>
              <li>
                <Link to={paths.pricing} className="footer-link">
                  Plans & Pricing
                </Link>
              </li>
              <li>
                <Link to={paths.faq} className="footer-link">
                  Frequently Asked Questions
                </Link>
              </li>
              <li>
                <Link to={paths.contact} className="footer-link">
                  Contact & Institutional Inquiries
                </Link>
              </li>
            </ul>
          </div>

          {/* Legal & Compliance */}
          <div className="public-footer-col">
            <h3 className="public-footer-heading">Ethics & Legal</h3>
            <ul className="public-footer-links">
              <li>
                <Link to={paths.privacy} className="footer-link">
                  Privacy Policy & Data Security
                </Link>
              </li>
              <li>
                <Link to={paths.terms} className="footer-link">
                  Terms of Academic Use
                </Link>
              </li>
              <li>
                <Link to={paths.about} className="footer-link">
                  Responsible AI Guidelines
                </Link>
              </li>
              <li>
                <Link to={paths.faq} className="footer-link">
                  Auditability & Verification
                </Link>
              </li>
            </ul>

            {/* Social Links */}
            {(settings?.twitterUrl || settings?.linkedinUrl || settings?.githubUrl) && (
              <div className="public-social-links">
                {settings.twitterUrl && (
                  <a
                    href={settings.twitterUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="social-btn"
                    aria-label="X / Twitter"
                  >
                    <span>X</span>
                    <ExternalLink size={12} />
                  </a>
                )}
                {settings.linkedinUrl && (
                  <a
                    href={settings.linkedinUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="social-btn"
                    aria-label="LinkedIn"
                  >
                    <span>LinkedIn</span>
                    <ExternalLink size={12} />
                  </a>
                )}
                {settings.githubUrl && (
                  <a
                    href={settings.githubUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="social-btn"
                    aria-label="GitHub"
                  >
                    <span>GitHub</span>
                    <ExternalLink size={12} />
                  </a>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="public-footer-bottom">
          <p className="public-copyright">{copyrightText}</p>
          <div className="public-footer-bottom-links">
            <Link to={paths.privacy} className="footer-link-subtle">
              Privacy
            </Link>
            <span className="dot-sep">•</span>
            <Link to={paths.terms} className="footer-link-subtle">
              Terms
            </Link>
            <span className="dot-sep">•</span>
            <Link to={paths.contact} className="footer-link-subtle">
              Support
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
