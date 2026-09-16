import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  CheckCircle2,
  ArrowRight,
  ShieldCheck,
  Building,
  GraduationCap,
  CreditCard,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import { useAuth } from '../../auth/AuthProvider';

export function PricingPage() {
  const { isAuthenticated } = useAuth();
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'annual'>('annual');

  const { data: tiers, isLoading, isError } = useQuery({
    queryKey: ['publicPricing'],
    queryFn: () => publicApi.getPricing(),
    staleTime: 5 * 60 * 1000,
  });

  return (
    <div className="public-pricing-page" id="pricing-page-content">
      <SeoMetadata
        title="Plans & Pricing | AI Research Assistant"
        description="Transparent academic pricing plans for students, doctoral candidates, and university faculties. Backed by institutional-grade data privacy."
      />

      {/* Header */}
      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Fair & Transparent Academic Pricing</span>
          <h1 className="public-page-title">Predictable Plans for Every Stage of Scholarly Inquiry</h1>
          <p className="public-page-subtitle">
            No surprise usage fees, no lock-in contracts, and zero monetization of your research data.
            All plans include evidence-anchored citations and auditable report exports.
          </p>

          {/* Monthly / Annual Toggle */}
          <div className="pricing-billing-toggle">
            <button
              type="button"
              className={`toggle-btn ${billingCycle === 'monthly' ? 'active' : ''}`}
              onClick={() => setBillingCycle('monthly')}
            >
              Monthly Billing
            </button>
            <button
              type="button"
              className={`toggle-btn ${billingCycle === 'annual' ? 'active' : ''}`}
              onClick={() => setBillingCycle('annual')}
            >
              Annual Billing <span className="discount-tag">Save up to 25%</span>
            </button>
          </div>
        </div>
      </section>

      {/* Pricing Cards Grid */}
      <section className="public-section">
        <div className="public-container">
          {isLoading ? (
            <div className="loading-state text-center p-12">
              <p>Loading live subscription plans...</p>
            </div>
          ) : isError || !tiers || tiers.length === 0 ? (
            <div className="empty-state text-center p-12">
              <p>Unable to load pricing plans right now. Please try again later or contact support.</p>
              <Link to={paths.contact} className="btn btn-secondary mt-4">
                Contact Support
              </Link>
            </div>
          ) : (
            <div className="public-pricing-grid">
              {tiers.map((tier) => {
                const price =
                  billingCycle === 'annual'
                    ? tier.annualPrice > 0
                      ? tier.annualPrice / 12
                      : 0
                    : tier.monthlyPrice;

                return (
                  <div
                    key={tier.id}
                    className={`public-pricing-card ${tier.highlighted ? 'highlighted' : ''}`}
                    id={`pricing-card-${tier.code.toLowerCase()}`}
                  >
                    {tier.highlightBadge && (
                      <div className="pricing-card-badge">{tier.highlightBadge}</div>
                    )}

                    <div className="pricing-card-top">
                      <h3 className="pricing-card-title">{tier.name}</h3>
                      <p className="pricing-card-desc">{tier.description}</p>
                    </div>

                    <div className="pricing-card-price">
                      <span className="currency">{tier.currency === 'USD' ? '$' : tier.currency}</span>
                      <span className="amount">{price.toFixed(0)}</span>
                      <span className="interval">/month</span>
                    </div>

                    {billingCycle === 'annual' && tier.annualPrice > 0 ? (
                      <div className="annual-billed-note">
                        Billed annually as ${tier.annualPrice}/year
                      </div>
                    ) : (
                      <div className="annual-billed-note">&nbsp;</div>
                    )}

                    <div className="pricing-card-divider" />

                    <div className="pricing-features-section">
                      <h4 className="features-subheading">What’s included:</h4>
                      <ul className="pricing-features-list">
                        {tier.features.map((feature, idx) => (
                          <li key={idx}>
                            <CheckCircle2 size={16} className="text-primary flex-shrink-0" />
                            <span>{feature}</span>
                          </li>
                        ))}
                      </ul>
                    </div>

                    {tier.academicDiscountsAvailable && (
                      <div className="academic-discount-pill">
                        <GraduationCap size={14} className="text-secondary" />
                        <span>{tier.academicDiscountsAvailable}</span>
                      </div>
                    )}

                    <div className="pricing-card-action mt-auto">
                      {isAuthenticated ? (
                        <Link to={paths.billing} className="btn btn-primary btn-block">
                          <span>Manage Subscription</span>
                          <ArrowRight size={16} />
                        </Link>
                      ) : (
                        <Link
                          to={`${paths.register}?plan=${encodeURIComponent(tier.code)}`}
                          className={`btn ${tier.highlighted ? 'btn-primary' : 'btn-secondary'} btn-block`}
                        >
                          <span>{tier.ctaLabel || 'Get Started'}</span>
                          <ArrowRight size={16} />
                        </Link>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Institutional / Departmental Callout */}
          <div className="institutional-pricing-card mt-12">
            <div className="inst-content">
              <div className="inst-icon">
                <Building size={32} />
              </div>
              <div className="inst-text">
                <h3>Institutional, Faculty & Postgraduate Cohorts</h3>
                <p>
                  Deploy across university faculties with central invoicing, single sign-on (SSO/SAML),
                  custom retention policies, supervisor oversight dashboards, and dedicated onboarding workshops.
                </p>
              </div>
            </div>
            <div className="inst-action">
              <Link to={paths.contact} className="btn btn-primary">
                <span>Inquire for Institutional License</span>
                <ArrowRight size={16} />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Trust & Guarantee Section */}
      <section className="public-section bg-subtle">
        <div className="public-container">
          <div className="public-cards-grid three-col">
            <div className="feature-card">
              <ShieldCheck size={28} className="text-primary mb-3" />
              <h3>Academic Integrity Guarantee</h3>
              <p>
                All literature syntheses provide verified sentence-level references to primary sources.
                No unanchored hallucinations or fabricated citations.
              </p>
            </div>
            <div className="feature-card">
              <CreditCard size={28} className="text-primary mb-3" />
              <h3>Cancel Anytime</h3>
              <p>
                Upgrade, downgrade, or cancel your subscription at any time with a single click in your
                billing portal. No cancellation penalties.
              </p>
            </div>
            <div className="feature-card">
              <GraduationCap size={28} className="text-primary mb-3" />
              <h3>Student & Scholar Assistance</h3>
              <p>
                We offer verified discounts to researchers in emerging economies and postgraduates with demonstrated
                need. Reach out via our contact page.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
