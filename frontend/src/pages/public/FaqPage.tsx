import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  ChevronDown,
  Search,
  HelpCircle,
  Mail,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import type { FaqCategory, PublicFaqResponse } from '../../types/publicSite';

export function FaqPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [openItems, setOpenItems] = useState<Record<string, boolean>>({});

  const { data: faqs, isLoading } = useQuery({
    queryKey: ['publicFaqs'],
    queryFn: () => publicApi.getFaqs(),
    staleTime: 5 * 60 * 1000,
  });

  const categories: { label: string; value: string }[] = [
    { label: 'All Questions', value: 'ALL' },
    { label: 'Academic Integrity', value: 'ACADEMIC_INTEGRITY' },
    { label: 'Methodology & Data', value: 'METHODOLOGY_AND_DATA' },
    { label: 'Security & Compliance', value: 'SECURITY_AND_COMPLIANCE' },
    { label: 'Billing & Licensing', value: 'BILLING_AND_LICENSING' },
    { label: 'General', value: 'GENERAL' },
  ];

  const filteredFaqs = useMemo(() => {
    if (!faqs) return [];
    return faqs.filter((faq: PublicFaqResponse) => {
      const matchesCategory =
        selectedCategory === 'ALL' || faq.category === selectedCategory;
      const matchesSearch =
        !searchQuery ||
        faq.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
        faq.answerMarkdown.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [faqs, selectedCategory, searchQuery]);

  const toggleItem = (id: string) => {
    setOpenItems((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const getCategoryBadge = (cat: FaqCategory) => {
    switch (cat) {
      case 'ACADEMIC_INTEGRITY':
        return 'Academic Integrity';
      case 'METHODOLOGY_AND_DATA':
        return 'Methodology & Data';
      case 'SECURITY_AND_COMPLIANCE':
        return 'Security & Compliance';
      case 'BILLING_AND_LICENSING':
        return 'Billing & Licensing';
      default:
        return 'General';
    }
  };

  return (
    <div className="public-faq-page" id="faq-page-content">
      <SeoMetadata
        title="Frequently Asked Questions | AI Research Assistant"
        description="Detailed answers to common questions about our grounded AI architecture, academic plagiarism policies, data privacy, and student discounts."
      />

      {/* Header */}
      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Clarity & Verification</span>
          <h1 className="public-page-title">Frequently Asked Questions</h1>
          <p className="public-page-subtitle">
            Transparent information regarding academic integrity, institutional privacy, model training policies,
            and research methodology.
          </p>
        </div>
      </section>

      {/* Filter and Search Bar */}
      <section className="public-section-sm bg-subtle">
        <div className="public-container">
          <div className="faq-filter-toolbar">
            <div className="faq-search-box">
              <Search size={18} className="search-icon" />
              <input
                type="text"
                placeholder="Search questions by keyword, e.g. 'plagiarism', 'GDPR', 'defense'..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="input faq-search-input"
                aria-label="Search FAQ"
              />
            </div>

            <div className="faq-category-chips">
              {categories.map((c) => (
                <button
                  key={c.value}
                  type="button"
                  className={`category-chip ${selectedCategory === c.value ? 'active' : ''}`}
                  onClick={() => setSelectedCategory(c.value)}
                >
                  {c.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Accordion FAQ list */}
      <section className="public-section">
        <div className="public-container">
          {isLoading ? (
            <div className="loading-state text-center p-12">
              <p>Loading questions & answers...</p>
            </div>
          ) : filteredFaqs.length > 0 ? (
            <div className="faq-accordion-container">
              {filteredFaqs.map((item: PublicFaqResponse) => {
                const isOpen = !!openItems[item.id];
                return (
                  <div key={item.id} className={`faq-card ${isOpen ? 'open' : ''}`}>
                    <button
                      type="button"
                      className="faq-card-header"
                      onClick={() => toggleItem(item.id)}
                      aria-expanded={isOpen}
                    >
                      <div className="faq-header-content">
                        <span className="badge badge-subtle mb-1">
                          {getCategoryBadge(item.category)}
                        </span>
                        <h3 className="faq-card-question">{item.question}</h3>
                      </div>
                      <ChevronDown size={20} className={`faq-card-icon ${isOpen ? 'rotate' : ''}`} />
                    </button>

                    {isOpen && (
                      <div className="faq-card-body">
                        <div className="faq-markdown-text">
                          {item.answerMarkdown.split('\n\n').map((para, i) => (
                            <p key={i}>{para}</p>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="empty-search-state text-center p-12">
              <HelpCircle size={36} className="text-muted mx-auto mb-4" />
              <h3>No questions matched your search</h3>
              <p>Try searching for a different term or view all categories.</p>
              <button
                type="button"
                className="btn btn-secondary mt-4"
                onClick={() => {
                  setSelectedCategory('ALL');
                  setSearchQuery('');
                }}
              >
                Reset FAQ Filter
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Still Have Questions? */}
      <section className="public-cta-banner">
        <div className="public-container text-center">
          <h2>Still Have Unanswered Questions?</h2>
          <p>
            Our academic advisory team is available to discuss ethical clearances, departmental licenses,
            and research design requirements.
          </p>
          <div className="cta-banner-actions">
            <Link to={paths.contact} className="btn btn-primary btn-lg">
              <Mail size={18} />
              <span>Contact Academic Advisory Team</span>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
