import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Globe,
  Settings,
  FileText,
  Layers,
  HelpCircle,
  Plus,
  Save,
  Trash2,
  CheckCircle,
  AlertCircle,
  ExternalLink,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import type {
  UpdateSiteSettingsRequest,
  CreateOrUpdateServiceRequest,
  CreateOrUpdateFaqRequest,
  FaqCategory,
} from '../../types/publicSite';

export function AdminPublicSitePage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'settings' | 'pages' | 'services' | 'faqs'>('settings');
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // 1. Site Settings Query
  const { data: settings, isLoading: settingsLoading } = useQuery({
    queryKey: ['adminSiteSettings'],
    queryFn: () => publicApi.getAdminSiteSettings(),
  });

  const [settingsForm, setSettingsForm] = useState<UpdateSiteSettingsRequest | null>(null);

  // Initialize local settings form once data arrives
  if (settings && !settingsForm) {
    setSettingsForm({
      siteName: settings.siteName,
      tagline: settings.tagline || '',
      supportEmail: settings.supportEmail || '',
      supportPhone: settings.supportPhone || '',
      address: settings.address || '',
      logoUrl: settings.logoUrl || '',
      faviconUrl: settings.faviconUrl || '',
      twitterUrl: settings.twitterUrl || '',
      linkedinUrl: settings.linkedinUrl || '',
      githubUrl: settings.githubUrl || '',
      privacyPolicyUrl: settings.privacyPolicyUrl || '',
      termsOfServiceUrl: settings.termsOfServiceUrl || '',
      defaultMetaTitle: settings.defaultMetaTitle || '',
      defaultMetaDescription: settings.defaultMetaDescription || '',
      copyrightText: settings.copyrightText || '',
      registrationEnabled: settings.registrationEnabled,
      publicPricingEnabled: settings.publicPricingEnabled,
    });
  }

  const updateSettingsMutation = useMutation({
    mutationFn: (payload: UpdateSiteSettingsRequest) => publicApi.updateAdminSiteSettings(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminSiteSettings'] });
      queryClient.invalidateQueries({ queryKey: ['publicSiteSettings'] });
      setStatusMessage({ type: 'success', text: 'Site settings successfully updated and cached evicted.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
    onError: (err: any) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to update site settings.' });
    },
  });

  // 2. Services Query
  const { data: services, isLoading: servicesLoading } = useQuery({
    queryKey: ['adminServices'],
    queryFn: () => publicApi.getAdminServices(),
  });

  const [newServiceModalOpen, setNewServiceModalOpen] = useState(false);
  const [serviceFormData, setServiceFormData] = useState<CreateOrUpdateServiceRequest>({
    slug: '',
    name: '',
    tagline: '',
    description: '',
    academicBenefit: '',
    category: 'Literature Review',
    iconName: 'BookOpen',
    featured: true,
    displayOrder: 1,
  });

  const createServiceMutation = useMutation({
    mutationFn: (payload: CreateOrUpdateServiceRequest) => publicApi.createAdminService(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminServices'] });
      queryClient.invalidateQueries({ queryKey: ['publicServices'] });
      setNewServiceModalOpen(false);
      setServiceFormData({
        slug: '',
        name: '',
        tagline: '',
        description: '',
        academicBenefit: '',
        category: 'Literature Review',
        iconName: 'BookOpen',
        featured: true,
        displayOrder: 1,
      });
      setStatusMessage({ type: 'success', text: 'Service offering created successfully.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
  });

  const deleteServiceMutation = useMutation({
    mutationFn: (serviceId: string) => publicApi.deleteAdminService(serviceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminServices'] });
      queryClient.invalidateQueries({ queryKey: ['publicServices'] });
      setStatusMessage({ type: 'success', text: 'Service offering deleted.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
  });

  // 3. FAQs Query
  const { data: faqs, isLoading: faqsLoading } = useQuery({
    queryKey: ['adminFaqs'],
    queryFn: () => publicApi.getAdminFaqs(),
  });

  const [newFaqModalOpen, setNewFaqModalOpen] = useState(false);
  const [faqFormData, setFaqFormData] = useState<CreateOrUpdateFaqRequest>({
    category: 'GENERAL',
    question: '',
    answerMarkdown: '',
    displayOrder: 1,
    enabled: true,
  });

  const createFaqMutation = useMutation({
    mutationFn: (payload: CreateOrUpdateFaqRequest) => publicApi.createAdminFaq(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminFaqs'] });
      queryClient.invalidateQueries({ queryKey: ['publicFaqs'] });
      setNewFaqModalOpen(false);
      setFaqFormData({
        category: 'GENERAL',
        question: '',
        answerMarkdown: '',
        displayOrder: 1,
        enabled: true,
      });
      setStatusMessage({ type: 'success', text: 'FAQ item created successfully.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
  });

  const deleteFaqMutation = useMutation({
    mutationFn: (faqId: string) => publicApi.deleteAdminFaq(faqId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminFaqs'] });
      queryClient.invalidateQueries({ queryKey: ['publicFaqs'] });
      setStatusMessage({ type: 'success', text: 'FAQ item deleted.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
  });

  // 4. Pages Query
  const { data: pages, isLoading: pagesLoading } = useQuery({
    queryKey: ['adminPages'],
    queryFn: () => publicApi.getAdminPages(),
  });

  return (
    <div className="admin-public-site-page p-6" id="admin-public-site-root">
      <div className="admin-header flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Globe size={24} className="text-primary" />
            <span>Public Website Management</span>
          </h1>
          <p className="text-muted">
            Configure site branding, public page sections, service catalog, and FAQ repository.
          </p>
        </div>

        <a
          href="/"
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-secondary flex items-center gap-2"
        >
          <span>Preview Public Site</span>
          <ExternalLink size={16} />
        </a>
      </div>

      {statusMessage && (
        <div className={`alert alert-${statusMessage.type} mb-6 flex items-center gap-2`}>
          {statusMessage.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span>{statusMessage.text}</span>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="admin-tabs flex gap-2 border-b border-border mb-6">
        <button
          type="button"
          className={`tab-btn px-4 py-2 font-medium ${activeTab === 'settings' ? 'border-b-2 border-primary text-primary' : 'text-muted'}`}
          onClick={() => setActiveTab('settings')}
        >
          <Settings size={16} className="inline mr-2" />
          Site Settings
        </button>
        <button
          type="button"
          className={`tab-btn px-4 py-2 font-medium ${activeTab === 'pages' ? 'border-b-2 border-primary text-primary' : 'text-muted'}`}
          onClick={() => setActiveTab('pages')}
        >
          <FileText size={16} className="inline mr-2" />
          Pages & Navigation
        </button>
        <button
          type="button"
          className={`tab-btn px-4 py-2 font-medium ${activeTab === 'services' ? 'border-b-2 border-primary text-primary' : 'text-muted'}`}
          onClick={() => setActiveTab('services')}
        >
          <Layers size={16} className="inline mr-2" />
          Services Catalog
        </button>
        <button
          type="button"
          className={`tab-btn px-4 py-2 font-medium ${activeTab === 'faqs' ? 'border-b-2 border-primary text-primary' : 'text-muted'}`}
          onClick={() => setActiveTab('faqs')}
        >
          <HelpCircle size={16} className="inline mr-2" />
          FAQs
        </button>
      </div>

      {/* TAB 1: SITE SETTINGS */}
      {activeTab === 'settings' && (
        <div className="card p-6 bg-card border rounded-lg max-w-4xl">
          {settingsLoading || !settingsForm ? (
            <p>Loading site settings...</p>
          ) : (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                updateSettingsMutation.mutate(settingsForm);
              }}
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div className="form-group">
                  <label className="form-label font-medium">Site Name</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.siteName}
                    onChange={(e) => setSettingsForm({ ...settingsForm, siteName: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">Tagline</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.tagline}
                    onChange={(e) => setSettingsForm({ ...settingsForm, tagline: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">Support Email</label>
                  <input
                    type="email"
                    className="input w-full"
                    value={settingsForm.supportEmail}
                    onChange={(e) => setSettingsForm({ ...settingsForm, supportEmail: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">Support Phone</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.supportPhone}
                    onChange={(e) => setSettingsForm({ ...settingsForm, supportPhone: e.target.value })}
                  />
                </div>

                <div className="form-group col-span-2">
                  <label className="form-label font-medium">Physical / Campus Address</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.address}
                    onChange={(e) => setSettingsForm({ ...settingsForm, address: e.target.value })}
                  />
                </div>

                <div className="form-group col-span-2">
                  <label className="form-label font-medium">Copyright Text</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.copyrightText}
                    onChange={(e) => setSettingsForm({ ...settingsForm, copyrightText: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">Default Meta Title</label>
                  <input
                    type="text"
                    className="input w-full"
                    value={settingsForm.defaultMetaTitle}
                    onChange={(e) => setSettingsForm({ ...settingsForm, defaultMetaTitle: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">LinkedIn Profile URL</label>
                  <input
                    type="url"
                    className="input w-full"
                    value={settingsForm.linkedinUrl}
                    onChange={(e) => setSettingsForm({ ...settingsForm, linkedinUrl: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">X / Twitter URL</label>
                  <input
                    type="url"
                    className="input w-full"
                    value={settingsForm.twitterUrl}
                    onChange={(e) => setSettingsForm({ ...settingsForm, twitterUrl: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label font-medium">GitHub Repository URL</label>
                  <input
                    type="url"
                    className="input w-full"
                    value={settingsForm.githubUrl}
                    onChange={(e) => setSettingsForm({ ...settingsForm, githubUrl: e.target.value })}
                  />
                </div>
              </div>

              <div className="border-t pt-4 my-4 flex gap-6">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={settingsForm.registrationEnabled}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, registrationEnabled: e.target.checked })
                    }
                  />
                  <span>Allow Visitor Self-Registration</span>
                </label>

                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={settingsForm.publicPricingEnabled}
                    onChange={(e) =>
                      setSettingsForm({ ...settingsForm, publicPricingEnabled: e.target.checked })
                    }
                  />
                  <span>Display Pricing Page to Public</span>
                </label>
              </div>

              <button
                type="submit"
                className="btn btn-primary mt-4 flex items-center gap-2"
                disabled={updateSettingsMutation.isPending}
              >
                <Save size={16} />
                <span>{updateSettingsMutation.isPending ? 'Saving Settings...' : 'Save Site Settings'}</span>
              </button>
            </form>
          )}
        </div>
      )}

      {/* TAB 2: PAGES */}
      {activeTab === 'pages' && (
        <div className="card p-6 bg-card border rounded-lg">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-lg">Published & Custom Pages</h3>
          </div>

          {pagesLoading ? (
            <p>Loading pages...</p>
          ) : (
            <div className="table-responsive">
              <table className="table w-full">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Title</th>
                    <th>Slug</th>
                    <th>Status</th>
                    <th>Nav Visible</th>
                    <th>Order</th>
                  </tr>
                </thead>
                <tbody>
                  {pages?.map((p) => (
                    <tr key={p.id}>
                      <td><span className="badge badge-subtle">{p.type}</span></td>
                      <td className="font-medium">{p.title}</td>
                      <td className="text-muted">/{p.slug}</td>
                      <td>
                        <span className={`badge ${p.status === 'PUBLISHED' ? 'badge-success' : 'badge-secondary'}`}>
                          {p.status}
                        </span>
                      </td>
                      <td>{p.showInNavigation ? 'Yes' : 'No'}</td>
                      <td>{p.navigationOrder}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: SERVICES */}
      {activeTab === 'services' && (
        <div className="card p-6 bg-card border rounded-lg">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-lg">Service Offerings</h3>
            <button
              type="button"
              className="btn btn-primary btn-sm flex items-center gap-2"
              onClick={() => setNewServiceModalOpen(true)}
            >
              <Plus size={16} />
              <span>Add Service Offering</span>
            </button>
          </div>

          {servicesLoading ? (
            <p>Loading service offerings...</p>
          ) : (
            <div className="table-responsive">
              <table className="table w-full">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Academic Benefit</th>
                    <th>Featured</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {services?.map((s) => (
                    <tr key={s.id}>
                      <td className="font-medium">{s.name}</td>
                      <td><span className="badge badge-subtle">{s.category || 'General'}</span></td>
                      <td className="max-w-xs truncate">{s.description}</td>
                      <td className="max-w-xs truncate">{s.academicBenefit || '-'}</td>
                      <td>{s.featured ? 'Yes' : 'No'}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm text-danger"
                          onClick={() => {
                            if (confirm(`Delete service offering '${s.name}'?`)) {
                              deleteServiceMutation.mutate(s.id);
                            }
                          }}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* New Service Modal */}
          {newServiceModalOpen && (
            <div className="modal-backdrop">
              <div className="modal-card p-6 bg-card border rounded-lg max-w-lg">
                <h3 className="text-lg font-bold mb-4">New Service Offering</h3>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    createServiceMutation.mutate(serviceFormData);
                  }}
                >
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Name</label>
                    <input
                      type="text"
                      className="input w-full"
                      value={serviceFormData.name}
                      onChange={(e) => {
                        const name = e.target.value;
                        const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
                        setServiceFormData({ ...serviceFormData, name, slug });
                      }}
                      required
                    />
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Slug</label>
                    <input
                      type="text"
                      className="input w-full"
                      value={serviceFormData.slug}
                      onChange={(e) => setServiceFormData({ ...serviceFormData, slug: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Category</label>
                    <input
                      type="text"
                      className="input w-full"
                      value={serviceFormData.category}
                      onChange={(e) => setServiceFormData({ ...serviceFormData, category: e.target.value })}
                    />
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Description</label>
                    <textarea
                      className="input textarea w-full"
                      rows={3}
                      value={serviceFormData.description}
                      onChange={(e) => setServiceFormData({ ...serviceFormData, description: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Academic Benefit</label>
                    <textarea
                      className="input textarea w-full"
                      rows={2}
                      value={serviceFormData.academicBenefit}
                      onChange={(e) => setServiceFormData({ ...serviceFormData, academicBenefit: e.target.value })}
                    />
                  </div>
                  <div className="flex justify-end gap-2 mt-4">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setNewServiceModalOpen(false)}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="btn btn-primary" disabled={createServiceMutation.isPending}>
                      Create Service
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 4: FAQS */}
      {activeTab === 'faqs' && (
        <div className="card p-6 bg-card border rounded-lg">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-lg">Frequently Asked Questions</h3>
            <button
              type="button"
              className="btn btn-primary btn-sm flex items-center gap-2"
              onClick={() => setNewFaqModalOpen(true)}
            >
              <Plus size={16} />
              <span>Add FAQ Item</span>
            </button>
          </div>

          {faqsLoading ? (
            <p>Loading FAQs...</p>
          ) : (
            <div className="table-responsive">
              <table className="table w-full">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Question</th>
                    <th>Answer Preview</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {faqs?.map((f) => (
                    <tr key={f.id}>
                      <td><span className="badge badge-subtle">{f.category}</span></td>
                      <td className="font-medium max-w-sm">{f.question}</td>
                      <td className="max-w-md truncate text-muted">{f.answerMarkdown}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-ghost btn-sm text-danger"
                          onClick={() => {
                            if (confirm(`Delete FAQ: "${f.question}"?`)) {
                              deleteFaqMutation.mutate(f.id);
                            }
                          }}
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* New FAQ Modal */}
          {newFaqModalOpen && (
            <div className="modal-backdrop">
              <div className="modal-card p-6 bg-card border rounded-lg max-w-lg">
                <h3 className="text-lg font-bold mb-4">New FAQ Item</h3>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    createFaqMutation.mutate(faqFormData);
                  }}
                >
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Category</label>
                    <select
                      className="input select w-full"
                      value={faqFormData.category}
                      onChange={(e) => setFaqFormData({ ...faqFormData, category: e.target.value as FaqCategory })}
                    >
                      <option value="GENERAL">General</option>
                      <option value="ACADEMIC_INTEGRITY">Academic Integrity</option>
                      <option value="METHODOLOGY_AND_DATA">Methodology & Data</option>
                      <option value="SECURITY_AND_COMPLIANCE">Security & Compliance</option>
                      <option value="BILLING_AND_LICENSING">Billing & Licensing</option>
                    </select>
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Question</label>
                    <input
                      type="text"
                      className="input w-full"
                      value={faqFormData.question}
                      onChange={(e) => setFaqFormData({ ...faqFormData, question: e.target.value })}
                      required
                    />
                  </div>
                  <div className="form-group mb-3">
                    <label className="form-label font-medium">Answer (Markdown)</label>
                    <textarea
                      className="input textarea w-full"
                      rows={5}
                      value={faqFormData.answerMarkdown}
                      onChange={(e) => setFaqFormData({ ...faqFormData, answerMarkdown: e.target.value })}
                      required
                    />
                  </div>
                  <div className="flex justify-end gap-2 mt-4">
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setNewFaqModalOpen(false)}
                    >
                      Cancel
                    </button>
                    <button type="submit" className="btn btn-primary" disabled={createFaqMutation.isPending}>
                      Create FAQ
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
