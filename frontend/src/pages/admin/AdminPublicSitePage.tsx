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
  BarChart3,
  Calculator,
  Pencil,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import type {
  UpdateSiteSettingsRequest,
  CreateOrUpdateServiceRequest,
  CreateOrUpdateFaqRequest,
  FaqCategory,
  AdminPublicStatistic,
  CreatePublicStatisticRequest,
  UpdatePublicStatisticRequest,
  PublicStatisticValueSource,
  PublicSystemMetric,
} from '../../types/publicSite';

export function AdminPublicSitePage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'settings' | 'pages' | 'services' | 'faqs' | 'statistics'>('settings');
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

  // 5. Statistics Query & Mutations
  const { data: statistics, isLoading: statsLoading } = useQuery({
    queryKey: ['adminStatistics'],
    queryFn: () => publicApi.getAdminStatistics(),
  });

  const [statModalOpen, setStatModalOpen] = useState(false);
  const [editingStatId, setEditingStatId] = useState<string | null>(null);
  const [metricPreviewResult, setMetricPreviewResult] = useState<string | null>(null);
  const [previewingMetric, setPreviewingMetric] = useState(false);

  const initialStatForm = {
    code: '',
    label: '',
    description: '',
    valueSource: 'SYSTEM_DERIVED' as PublicStatisticValueSource,
    manualValue: '',
    systemMetric: 'TOTAL_RESEARCH_PROJECTS' as PublicSystemMetric,
    prefix: '',
    suffix: '+',
    iconKey: 'folder-kanban',
    enabled: true,
    featured: false,
    displayOrder: 0,
  };

  const [statForm, setStatForm] = useState(initialStatForm);

  const createStatMutation = useMutation({
    mutationFn: (payload: CreatePublicStatisticRequest) => publicApi.createAdminStatistic(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminStatistics'] });
      queryClient.invalidateQueries({ queryKey: ['publicStatistics'] });
      setStatModalOpen(false);
      setEditingStatId(null);
      setMetricPreviewResult(null);
      setStatusMessage({ type: 'success', text: 'Statistic created successfully.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to create statistic.' });
    },
  });

  const updateStatMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdatePublicStatisticRequest }) =>
      publicApi.updateAdminStatistic(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminStatistics'] });
      queryClient.invalidateQueries({ queryKey: ['publicStatistics'] });
      setStatModalOpen(false);
      setEditingStatId(null);
      setMetricPreviewResult(null);
      setStatusMessage({ type: 'success', text: 'Statistic updated successfully.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to update statistic.' });
    },
  });

  const deleteStatMutation = useMutation({
    mutationFn: (id: string) => publicApi.deleteAdminStatistic(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminStatistics'] });
      queryClient.invalidateQueries({ queryKey: ['publicStatistics'] });
      setStatusMessage({ type: 'success', text: 'Statistic deleted successfully.' });
      setTimeout(() => setStatusMessage(null), 4000);
    },
    onError: (err: Error) => {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to delete statistic.' });
    },
  });

  const handlePreviewMetric = async (metric: PublicSystemMetric) => {
    try {
      setPreviewingMetric(true);
      setMetricPreviewResult(null);
      const res = await publicApi.previewMetric(metric);
      setMetricPreviewResult(`Live DB count: ${res.rawValue} (formatted: "${res.formattedValue}")`);
    } catch {
      setMetricPreviewResult('Failed to preview live metric.');
    } finally {
      setPreviewingMetric(false);
    }
  };

  const openCreateStatModal = () => {
    setEditingStatId(null);
    setStatForm({
      ...initialStatForm,
      displayOrder: statistics?.length ?? 0,
    });
    setMetricPreviewResult(null);
    setStatModalOpen(true);
  };

  const openEditStatModal = (item: AdminPublicStatistic) => {
    setEditingStatId(item.id);
    setStatForm({
      code: item.code,
      label: item.label,
      description: item.description || '',
      valueSource: item.valueSource,
      manualValue: item.manualValue || '',
      systemMetric: item.systemMetric || 'TOTAL_RESEARCH_PROJECTS',
      prefix: item.prefix || '',
      suffix: item.suffix || '',
      iconKey: item.iconKey || '',
      enabled: item.enabled,
      featured: item.featured,
      displayOrder: item.displayOrder,
    });
    setMetricPreviewResult(null);
    setStatModalOpen(true);
  };

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
        <button
          type="button"
          className={`tab-btn px-4 py-2 font-medium ${activeTab === 'statistics' ? 'border-b-2 border-primary text-primary' : 'text-muted'}`}
          onClick={() => setActiveTab('statistics')}
        >
          <BarChart3 size={16} className="inline mr-2" />
          Statistics
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

      {/* TAB 5: PUBLIC STATISTICS */}
      {activeTab === 'statistics' && (
        <div className="tab-statistics-content">
          <div className="flex justify-between items-center mb-4">
            <div>
              <h2 className="text-xl font-bold">Public Statistics & Counters</h2>
              <p className="text-muted text-sm">
                Display empirical platform counters and metrics on the public homepage and about page.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-primary flex items-center gap-2"
              onClick={openCreateStatModal}
            >
              <Plus size={16} />
              <span>Add Statistic</span>
            </button>
          </div>

          {statsLoading ? (
            <p>Loading statistics...</p>
          ) : !statistics || statistics.length === 0 ? (
            <div className="card p-8 bg-card border rounded-lg text-center">
              <p className="text-muted mb-4">No statistics configured yet.</p>
              <button
                type="button"
                className="btn btn-primary"
                onClick={openCreateStatModal}
              >
                Add Your First Statistic
              </button>
            </div>
          ) : (
            <div className="card bg-card border rounded-lg overflow-hidden">
              <table className="w-full text-left border-collapse" style={{ width: '100%', textAlign: 'left' }}>
                <thead>
                  <tr className="border-b bg-muted/20" style={{ borderBottom: '1px solid var(--line)', background: 'var(--surface-2)' }}>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Order</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Code</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Label</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Source</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Resolved Display</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Featured</th>
                    <th className="p-3" style={{ padding: '10px 12px' }}>Status</th>
                    <th className="p-3 text-right" style={{ padding: '10px 12px', textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {statistics.map((stat) => (
                    <tr key={stat.id} className="border-b hover:bg-muted/10" style={{ borderBottom: '1px solid var(--line)' }}>
                      <td className="p-3 font-mono text-xs" style={{ padding: '10px 12px' }}>{stat.displayOrder}</td>
                      <td className="p-3" style={{ padding: '10px 12px' }}>
                        <span className="badge font-mono text-xs" style={{ background: 'var(--surface-2)', padding: '2px 6px', borderRadius: '4px' }}>
                          {stat.code}
                        </span>
                      </td>
                      <td className="p-3 font-medium" style={{ padding: '10px 12px' }}>{stat.label}</td>
                      <td className="p-3" style={{ padding: '10px 12px' }}>
                        <span className={`badge ${stat.valueSource === 'SYSTEM_DERIVED' ? 'badge-info' : 'badge-secondary'}`} style={{ fontSize: '0.75rem' }}>
                          {stat.valueSource === 'SYSTEM_DERIVED' ? `System (${stat.systemMetric})` : 'Manual'}
                        </span>
                      </td>
                      <td className="p-3 font-bold text-primary" style={{ padding: '10px 12px' }}>
                        {stat.prefix || ''}{stat.resolvedValue}{stat.suffix || ''}
                      </td>
                      <td className="p-3" style={{ padding: '10px 12px' }}>
                        {stat.featured ? (
                          <span className="badge badge-primary text-xs" style={{ background: 'var(--brand)', color: '#fff', padding: '2px 6px', borderRadius: '4px' }}>
                            Featured
                          </span>
                        ) : (
                          <span className="text-muted text-xs">—</span>
                        )}
                      </td>
                      <td className="p-3" style={{ padding: '10px 12px' }}>
                        <span className={`badge ${stat.enabled ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.75rem' }}>
                          {stat.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td className="p-3 text-right" style={{ padding: '10px 12px', textAlign: 'right' }}>
                        <div className="flex justify-end gap-2" style={{ display: 'inline-flex', gap: '8px' }}>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => openEditStatModal(stat)}
                            title="Edit statistic"
                            style={{ padding: '4px 8px' }}
                          >
                            <Pencil size={14} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              if (window.confirm(`Delete statistic "${stat.label}"?`)) {
                                deleteStatMutation.mutate(stat.id);
                              }
                            }}
                            title="Delete statistic"
                            style={{ padding: '4px 8px' }}
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Modal for Create / Edit Statistic */}
          {statModalOpen && (
            <div className="modal-backdrop fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
              <div className="modal-content bg-card border rounded-lg max-w-xl w-full p-6 shadow-xl max-h-[90vh] overflow-y-auto" style={{ background: 'var(--surface, #ffffff)', border: '1px solid var(--line)', borderRadius: '12px', maxWidth: '560px', width: '100%', padding: '24px', maxHeight: '90vh', overflowY: 'auto' }}>
                <h3 className="text-xl font-bold mb-4" style={{ marginTop: 0 }}>
                  {editingStatId ? 'Edit Public Statistic' : 'Create Public Statistic'}
                </h3>

                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (editingStatId) {
                      updateStatMutation.mutate({
                        id: editingStatId,
                        payload: {
                          label: statForm.label,
                          description: statForm.description,
                          valueSource: statForm.valueSource,
                          manualValue: statForm.valueSource === 'MANUAL' ? statForm.manualValue : undefined,
                          systemMetric: statForm.valueSource === 'SYSTEM_DERIVED' ? statForm.systemMetric : undefined,
                          prefix: statForm.prefix,
                          suffix: statForm.suffix,
                          iconKey: statForm.iconKey,
                          enabled: statForm.enabled,
                          featured: statForm.featured,
                          displayOrder: Number(statForm.displayOrder),
                        },
                      });
                    } else {
                      createStatMutation.mutate({
                        code: statForm.code,
                        label: statForm.label,
                        description: statForm.description,
                        valueSource: statForm.valueSource,
                        manualValue: statForm.valueSource === 'MANUAL' ? statForm.manualValue : undefined,
                        systemMetric: statForm.valueSource === 'SYSTEM_DERIVED' ? statForm.systemMetric : undefined,
                        prefix: statForm.prefix,
                        suffix: statForm.suffix,
                        iconKey: statForm.iconKey,
                        enabled: statForm.enabled,
                        featured: statForm.featured,
                        displayOrder: Number(statForm.displayOrder),
                      });
                    }
                  }}
                >
                  {!editingStatId && (
                    <div className="form-group mb-3">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Code</label>
                      <input
                        type="text"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.code}
                        onChange={(e) => setStatForm({ ...statForm, code: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') })}
                        placeholder="e.g. total-projects"
                        pattern="^[a-z0-9-]+$"
                        title="Lowercase alphanumeric with hyphens"
                        required
                      />
                      <span className="text-muted text-xs" style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>Unique key (lowercase alphanumeric with hyphens)</span>
                    </div>
                  )}

                  <div className="form-group mb-3">
                    <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Label</label>
                    <input
                      type="text"
                      className="input w-full"
                      style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                      value={statForm.label}
                      onChange={(e) => setStatForm({ ...statForm, label: e.target.value })}
                      placeholder="e.g. Active Research Projects"
                      required
                    />
                  </div>

                  <div className="form-group mb-3">
                    <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Description</label>
                    <input
                      type="text"
                      className="input w-full"
                      style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                      value={statForm.description}
                      onChange={(e) => setStatForm({ ...statForm, description: e.target.value })}
                      placeholder="Optional internal description"
                    />
                  </div>

                  <div className="form-group mb-3">
                    <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Value Source</label>
                    <select
                      className="input w-full"
                      style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                      value={statForm.valueSource}
                      onChange={(e) => {
                        const newSource = e.target.value as PublicStatisticValueSource;
                        setStatForm({ ...statForm, valueSource: newSource });
                        setMetricPreviewResult(null);
                      }}
                    >
                      <option value="SYSTEM_DERIVED">System Derived (Real database counts)</option>
                      <option value="MANUAL">Manual Value</option>
                    </select>
                  </div>

                  {statForm.valueSource === 'SYSTEM_DERIVED' ? (
                    <div className="form-group mb-3 p-3 bg-muted/10 rounded" style={{ background: 'var(--surface-2)', padding: '12px', borderRadius: '8px' }}>
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>System Metric</label>
                      <select
                        className="input w-full mb-2"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box', marginBottom: '8px' }}
                        value={statForm.systemMetric}
                        onChange={(e) => {
                          const metric = e.target.value as PublicSystemMetric;
                          setStatForm({ ...statForm, systemMetric: metric });
                          setMetricPreviewResult(null);
                        }}
                      >
                        <option value="TOTAL_RESEARCH_PROJECTS">TOTAL_RESEARCH_PROJECTS (Active & Draft projects)</option>
                        <option value="TOTAL_ACTIVE_USERS">TOTAL_ACTIVE_USERS (Active user accounts)</option>
                        <option value="TOTAL_DOCUMENTS_PROCESSED">TOTAL_DOCUMENTS_PROCESSED (Uploaded documents)</option>
                        <option value="TOTAL_WORKSPACES">TOTAL_WORKSPACES (Active workspaces)</option>
                        <option value="TOTAL_COMPLETED_REPORT_EXPORTS">TOTAL_COMPLETED_REPORT_EXPORTS (Completed report exports)</option>
                      </select>

                      <div className="flex items-center gap-2" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm flex items-center gap-1"
                          onClick={() => handlePreviewMetric(statForm.systemMetric)}
                          disabled={previewingMetric}
                          style={{ padding: '6px 12px', fontSize: '0.85rem' }}
                        >
                          <Calculator size={14} />
                          <span>{previewingMetric ? 'Calculating...' : 'Preview Live DB Count'}</span>
                        </button>
                        {metricPreviewResult && (
                          <span className="text-xs font-semibold text-primary" style={{ fontSize: '0.8rem', color: 'var(--brand)' }}>
                            {metricPreviewResult}
                          </span>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="form-group mb-3">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Manual Value</label>
                      <input
                        type="text"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.manualValue}
                        onChange={(e) => setStatForm({ ...statForm, manualValue: e.target.value })}
                        placeholder="e.g. 99.9% or 5,000"
                        required
                      />
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-3 mb-3" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                    <div className="form-group">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Prefix</label>
                      <input
                        type="text"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.prefix}
                        onChange={(e) => setStatForm({ ...statForm, prefix: e.target.value })}
                        placeholder="e.g. > or $"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Suffix</label>
                      <input
                        type="text"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.suffix}
                        onChange={(e) => setStatForm({ ...statForm, suffix: e.target.value })}
                        placeholder="e.g. + or %"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3 mb-3" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                    <div className="form-group">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Icon Key</label>
                      <input
                        type="text"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.iconKey}
                        onChange={(e) => setStatForm({ ...statForm, iconKey: e.target.value })}
                        placeholder="e.g. folder-kanban, users, file-text"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label font-medium" style={{ display: 'block', marginBottom: '4px' }}>Display Order</label>
                      <input
                        type="number"
                        className="input w-full"
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        value={statForm.displayOrder}
                        onChange={(e) => setStatForm({ ...statForm, displayOrder: parseInt(e.target.value, 10) || 0 })}
                      />
                    </div>
                  </div>

                  <div className="flex gap-4 mb-4" style={{ display: 'flex', gap: '16px', margin: '12px 0' }}>
                    <label className="flex items-center gap-2 cursor-pointer" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <input
                        type="checkbox"
                        checked={statForm.enabled}
                        onChange={(e) => setStatForm({ ...statForm, enabled: e.target.checked })}
                      />
                      <span>Enabled</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <input
                        type="checkbox"
                        checked={statForm.featured}
                        onChange={(e) => setStatForm({ ...statForm, featured: e.target.checked })}
                      />
                      <span>Featured (Highlighted)</span>
                    </label>
                  </div>

                  <div className="flex justify-end gap-2 mt-4" style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setStatModalOpen(false)}
                      style={{ padding: '8px 16px' }}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={createStatMutation.isPending || updateStatMutation.isPending}
                      style={{ padding: '8px 16px' }}
                    >
                      {editingStatId ? 'Save Changes' : 'Create Statistic'}
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
