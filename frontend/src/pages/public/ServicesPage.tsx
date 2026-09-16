import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  BookOpen,
  Scale,
  Layers,
  BarChart3,
  GraduationCap,
  Sparkles,
  ArrowRight,
  Search,
  Filter,
  CheckCircle,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import { paths } from '../../routes/paths';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import type { PublicServiceOfferingResponse } from '../../types/publicSite';

export function ServicesPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const { data: services, isLoading } = useQuery({
    queryKey: ['publicServices'],
    queryFn: () => publicApi.getServices(),
    staleTime: 5 * 60 * 1000,
  });

  const categories = useMemo(() => {
    if (!services) return ['ALL'];
    const cats = new Set(services.map((s) => s.category || 'General'));
    return ['ALL', ...Array.from(cats)];
  }, [services]);

  const filteredServices = useMemo(() => {
    if (!services) return [];
    return services.filter((s) => {
      const matchesCategory =
        selectedCategory === 'ALL' || (s.category || 'General').toUpperCase() === selectedCategory.toUpperCase();
      const matchesSearch =
        !searchQuery ||
        s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (s.academicBenefit && s.academicBenefit.toLowerCase().includes(searchQuery.toLowerCase()));
      return matchesCategory && matchesSearch;
    });
  }, [services, selectedCategory, searchQuery]);

  const getIcon = (iconName?: string) => {
    switch (iconName?.toLowerCase()) {
      case 'bookopen':
      case 'literature':
        return BookOpen;
      case 'scale':
      case 'methodology':
        return Scale;
      case 'layers':
      case 'qualitative':
        return Layers;
      case 'barchart':
      case 'quantitative':
      case 'statistics':
        return BarChart3;
      case 'graduationcap':
      case 'defense':
        return GraduationCap;
      default:
        return Sparkles;
    }
  };

  return (
    <div className="public-services-page" id="services-page-content">
      <SeoMetadata
        title="Services & Capabilities | AI Research Assistant"
        description="Explore specialized academic capabilities for systematic literature reviews, empirical methodology formulation, qualitative coding, and APA 7 statistical analysis."
      />

      {/* Hero Header */}
      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Comprehensive Academic Suite</span>
          <h1 className="public-page-title">Research Capabilities & Methodology Services</h1>
          <p className="public-page-subtitle">
            Every module is engineered strictly around peer-reviewed standards, evidence-anchored citations,
            and auditable mathematical procedures.
          </p>
        </div>
      </section>

      {/* Filter and Search Bar */}
      <section className="public-section-sm bg-subtle">
        <div className="public-container">
          <div className="services-filter-toolbar">
            <div className="services-search-box">
              <Search size={18} className="search-icon" />
              <input
                type="text"
                placeholder="Search capabilities, e.g. 'thematic coding', 'ANOVA', 'sampling'..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="input services-search-input"
                aria-label="Search research services"
              />
            </div>

            <div className="services-category-chips">
              {categories.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`category-chip ${selectedCategory === cat ? 'active' : ''}`}
                  onClick={() => setSelectedCategory(cat)}
                >
                  {cat === 'ALL' ? 'All Services' : cat}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Services Grid */}
      <section className="public-section">
        <div className="public-container">
          {isLoading ? (
            <div className="loading-state text-center p-12">
              <p>Loading research capabilities...</p>
            </div>
          ) : filteredServices.length > 0 ? (
            <div className="public-cards-grid three-col">
              {filteredServices.map((service: PublicServiceOfferingResponse) => {
                const IconComponent = getIcon(service.iconName);
                return (
                  <div key={service.id} className="service-card">
                    <div className="service-card-header">
                      <div className="service-icon-wrap">
                        <IconComponent size={24} />
                      </div>
                      {service.category && (
                        <span className="badge badge-subtle">{service.category}</span>
                      )}
                    </div>

                    <h3 className="service-card-title">{service.name}</h3>
                    {service.tagline && <p className="service-card-tagline">{service.tagline}</p>}
                    <p className="service-card-desc">{service.description}</p>

                    {service.academicBenefit && (
                      <div className="service-academic-benefit">
                        <div className="benefit-header">
                          <CheckCircle size={14} className="text-success" />
                          <span>Academic Benefit</span>
                        </div>
                        <p>{service.academicBenefit}</p>
                      </div>
                    )}

                    <div className="service-card-footer">
                      <Link to={paths.register} className="btn btn-secondary btn-sm btn-block">
                        <span>Get Started with {service.name.split(' ')[0]}</span>
                        <ArrowRight size={14} />
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="empty-search-state text-center p-12">
              <Filter size={36} className="text-muted mx-auto mb-4" />
              <h3>No capabilities match your criteria</h3>
              <p>Try clearing your search query or selecting "All Services".</p>
              <button
                type="button"
                className="btn btn-secondary mt-4"
                onClick={() => {
                  setSelectedCategory('ALL');
                  setSearchQuery('');
                }}
              >
                Reset Filters
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Custom Consulting & Departmental Integration Banner */}
      <section className="public-cta-banner">
        <div className="public-container text-center">
          <h2>Need Departmental or Institutional Deployment?</h2>
          <p>
            We integrate with campus Single Sign-On (SAML/OAuth2), institutional repository systems, and offer
            faculty supervision dashboards.
          </p>
          <div className="cta-banner-actions">
            <Link to={paths.contact} className="btn btn-primary btn-lg">
              <span>Schedule Institutional Consultation</span>
              <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
