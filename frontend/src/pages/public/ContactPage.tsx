import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Mail,
  Phone,
  MapPin,
  Send,
  CheckCircle2,
  AlertCircle,
  Clock,
  Building,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import { SeoMetadata } from '../../layouts/public/SeoMetadata';
import type {
  PublicContactSubmissionRequest,
  PublicContactSubmissionResponse,
} from '../../types/publicSite';

export function ContactPage() {
  const [formData, setFormData] = useState<PublicContactSubmissionRequest>({
    name: '',
    email: '',
    phone: '',
    organization: '',
    academicRole: 'Doctoral Researcher',
    subject: '',
    message: '',
    preferredChannel: 'EMAIL',
    honeypot: '', // hidden field for bot detection
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [submissionResult, setSubmissionResult] = useState<PublicContactSubmissionResponse | null>(null);

  const { data: settings } = useQuery({
    queryKey: ['publicSiteSettings'],
    queryFn: () => publicApi.getSiteSettings(),
    staleTime: 5 * 60 * 1000,
  });

  const mutation = useMutation({
    mutationFn: (payload: PublicContactSubmissionRequest) => publicApi.submitContact(payload),
    onSuccess: (data) => {
      setSubmissionResult(data);
      setFormData({
        name: '',
        email: '',
        phone: '',
        organization: '',
        academicRole: 'Doctoral Researcher',
        subject: '',
        message: '',
        preferredChannel: 'EMAIL',
        honeypot: '',
      });
      setFormErrors({});
    },
    onError: (err: any) => {
      if (err.validationErrors) {
        setFormErrors(err.validationErrors);
      } else {
        setFormErrors({
          form: err.message || 'Failed to submit your message. Please verify all fields and try again.',
        });
      }
    },
  });

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!formData.name.trim()) errors.name = 'Full name is required.';
    if (!formData.email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = 'Please enter a valid email address.';
    }
    if (!formData.subject.trim()) errors.subject = 'Subject is required.';
    if (!formData.message.trim()) {
      errors.message = 'Message is required.';
    } else if (formData.message.trim().length < 10) {
      errors.message = 'Message must be at least 10 characters long.';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    mutation.mutate(formData);
  };

  return (
    <div className="public-contact-page" id="contact-page-content">
      <SeoMetadata
        title="Contact & Support | AI Research Assistant"
        description="Get in touch with our academic advisory and institutional support team for campus licensing, methodology consultations, or technical assistance."
      />

      {/* Hero Header */}
      <section className="public-page-hero">
        <div className="public-container text-center">
          <span className="section-eyebrow">Direct Scholar Support</span>
          <h1 className="public-page-title">Contact Academic Inquiries & Support</h1>
          <p className="public-page-subtitle">
            Have a question about institutional licensing, research methodology, or student grants?
            Our academic advisory team reviews every inquiry with diligence.
          </p>
        </div>
      </section>

      {/* Main Form and Contact Details */}
      <section className="public-section">
        <div className="public-container">
          <div className="contact-layout-grid">
            {/* Left Column: Direct Info & Institutional Details */}
            <div className="contact-info-col">
              <div className="contact-info-card">
                <h3>Direct Communication</h3>
                <p>
                  Reach out directly for urgent queries or institutional RFP documentation.
                </p>

                <div className="contact-item-row">
                  <div className="contact-item-icon">
                    <Mail size={20} />
                  </div>
                  <div>
                    <strong>Support Email</strong>
                    <p>
                      <a href={`mailto:${settings?.supportEmail || 'support@researchassistant.ai'}`}>
                        {settings?.supportEmail || 'support@researchassistant.ai'}
                      </a>
                    </p>
                  </div>
                </div>

                {settings?.supportPhone && (
                  <div className="contact-item-row">
                    <div className="contact-item-icon">
                      <Phone size={20} />
                    </div>
                    <div>
                      <strong>Support Telephone</strong>
                      <p>
                        <a href={`tel:${settings.supportPhone}`}>{settings.supportPhone}</a>
                      </p>
                    </div>
                  </div>
                )}

                {settings?.address && (
                  <div className="contact-item-row">
                    <div className="contact-item-icon">
                      <MapPin size={20} />
                    </div>
                    <div>
                      <strong>Headquarters / Office</strong>
                      <p>{settings.address}</p>
                    </div>
                  </div>
                )}

                <div className="contact-item-row">
                  <div className="contact-item-icon">
                    <Clock size={20} />
                  </div>
                  <div>
                    <strong>Academic Advisory Hours</strong>
                    <p>Monday – Friday: 08:00 – 18:00 UTC</p>
                    <small className="text-muted">Inquiries dispatched within 24 business hours.</small>
                  </div>
                </div>
              </div>

              <div className="contact-institutional-callout">
                <Building size={24} className="text-primary mb-2" />
                <h4>Departmental & Enterprise Trials</h4>
                <p>
                  Are you a department head, dean, or doctoral program coordinator? Inquire about campus-wide
                  workshops, custom data privacy agreements, and cohort pilot access.
                </p>
              </div>
            </div>

            {/* Right Column: Contact Form or Success State */}
            <div className="contact-form-col">
              {submissionResult ? (
                <div className="contact-success-card" id="contact-success-state">
                  <div className="success-icon-wrap">
                    <CheckCircle2 size={48} className="text-success" />
                  </div>
                  <h2>Inquiry Successfully Received</h2>
                  <p className="success-message">{submissionResult.message}</p>

                  <div className="reference-code-box">
                    <span className="reference-label">Your Tracking Reference Code:</span>
                    <strong className="reference-code" id="contact-reference-code">
                      {submissionResult.referenceCode}
                    </strong>
                    <p className="reference-subtext">
                      Please retain this reference code for follow-ups with our academic advisory team.
                    </p>
                  </div>

                  <button
                    type="button"
                    className="btn btn-secondary mt-6"
                    onClick={() => setSubmissionResult(null)}
                  >
                    Submit Another Inquiry
                  </button>
                </div>
              ) : (
                <div className="contact-form-card">
                  <h3>Send a Research Message</h3>
                  <p className="form-subtitle">
                    Fill out the details below. We assign each inquiry to a specialized advisory specialist.
                  </p>

                  {formErrors.form && (
                    <div className="alert alert-error mb-4">
                      <AlertCircle size={18} />
                      <span>{formErrors.form}</span>
                    </div>
                  )}

                  <form onSubmit={handleSubmit} noValidate>
                    {/* Honeypot hidden input */}
                    <div style={{ display: 'none' }} aria-hidden="true">
                      <label htmlFor="website-hp">Do not fill this</label>
                      <input
                        id="website-hp"
                        type="text"
                        tabIndex={-1}
                        autoComplete="off"
                        value={formData.honeypot}
                        onChange={(e) => setFormData({ ...formData, honeypot: e.target.value })}
                      />
                    </div>

                    <div className="form-row two-col">
                      <div className="form-group">
                        <label htmlFor="contact-name" className="form-label">
                          Full Name <span className="text-danger">*</span>
                        </label>
                        <input
                          id="contact-name"
                          type="text"
                          className={`input ${formErrors.name ? 'input-error' : ''}`}
                          placeholder="e.g. Dr. Kwame Nkrumah"
                          value={formData.name}
                          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                          required
                        />
                        {formErrors.name && <span className="field-error">{formErrors.name}</span>}
                      </div>

                      <div className="form-group">
                        <label htmlFor="contact-email" className="form-label">
                          Academic / Work Email <span className="text-danger">*</span>
                        </label>
                        <input
                          id="contact-email"
                          type="email"
                          className={`input ${formErrors.email ? 'input-error' : ''}`}
                          placeholder="name@university.edu"
                          value={formData.email}
                          onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                          required
                        />
                        {formErrors.email && <span className="field-error">{formErrors.email}</span>}
                      </div>
                    </div>

                    <div className="form-row two-col">
                      <div className="form-group">
                        <label htmlFor="contact-org" className="form-label">
                          Institution or Organization
                        </label>
                        <input
                          id="contact-org"
                          type="text"
                          className="input"
                          placeholder="University or Research Institute"
                          value={formData.organization}
                          onChange={(e) => setFormData({ ...formData, organization: e.target.value })}
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="contact-role" className="form-label">
                          Academic Role
                        </label>
                        <select
                          id="contact-role"
                          className="input select"
                          value={formData.academicRole}
                          onChange={(e) => setFormData({ ...formData, academicRole: e.target.value })}
                        >
                          <option value="Doctoral Researcher">Doctoral Researcher (PhD / DPhil)</option>
                          <option value="Masters Scholar">Master’s Scholar (MSc / MPhil)</option>
                          <option value="Undergraduate Student">Undergraduate Researcher</option>
                          <option value="Faculty / Professor">Faculty Member / Professor</option>
                          <option value="Department Head / Dean">Department Head / Dean</option>
                          <option value="Independent Scholar">Independent Scholar / Analyst</option>
                          <option value="Other">Other</option>
                        </select>
                      </div>
                    </div>

                    <div className="form-row two-col">
                      <div className="form-group">
                        <label htmlFor="contact-phone" className="form-label">
                          Phone Number (Optional)
                        </label>
                        <input
                          id="contact-phone"
                          type="tel"
                          className="input"
                          placeholder="+233 ... or +1 ..."
                          value={formData.phone}
                          onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                        />
                      </div>

                      <div className="form-group">
                        <label htmlFor="contact-channel" className="form-label">
                          Preferred Response Channel
                        </label>
                        <select
                          id="contact-channel"
                          className="input select"
                          value={formData.preferredChannel}
                          onChange={(e) => setFormData({ ...formData, preferredChannel: e.target.value })}
                        >
                          <option value="EMAIL">Email Response</option>
                          <option value="PHONE">Phone Follow-up</option>
                        </select>
                      </div>
                    </div>

                    <div className="form-group">
                      <label htmlFor="contact-subject" className="form-label">
                        Subject <span className="text-danger">*</span>
                      </label>
                      <input
                        id="contact-subject"
                        type="text"
                        className={`input ${formErrors.subject ? 'input-error' : ''}`}
                        placeholder="e.g. Institutional License Inquiry for Department of Economics"
                        value={formData.subject}
                        onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                        required
                      />
                      {formErrors.subject && <span className="field-error">{formErrors.subject}</span>}
                    </div>

                    <div className="form-group">
                      <label htmlFor="contact-message" className="form-label">
                        Inquiry Message <span className="text-danger">*</span>
                      </label>
                      <textarea
                        id="contact-message"
                        rows={5}
                        className={`input textarea ${formErrors.message ? 'input-error' : ''}`}
                        placeholder="Please describe your research scope, timeline, or licensing questions..."
                        value={formData.message}
                        onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                        required
                      />
                      {formErrors.message && <span className="field-error">{formErrors.message}</span>}
                    </div>

                    <div className="form-actions mt-6">
                      <button
                        type="submit"
                        className="btn btn-primary btn-lg btn-block"
                        disabled={mutation.isPending}
                        id="contact-submit-btn"
                      >
                        {mutation.isPending ? (
                          <span>Sending Inquiry...</span>
                        ) : (
                          <>
                            <Send size={18} />
                            <span>Transmit Inquiry</span>
                          </>
                        )}
                      </button>
                    </div>
                  </form>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
