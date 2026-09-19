import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CreditCard,
  Plus,
  Edit2,
  Sliders,
  RefreshCw,
  Sparkles,
  Shield,
  HardDrive,
  Users,
  FolderKanban,
  FileDown,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Field, Input, Select, Textarea, LoadingButton } from '../../components/ui';
import { ErrorState } from '../../components/states';
import type {
  AdminPlanEntitlement,
  AdminSubscriptionPlan,
  AiCreditPack,
  CreateAiCreditPackRequest,
  CreateSubscriptionPlanRequest,
  LimitMode,
  UpdateAiCreditPackRequest,
  UpdateSubscriptionPlanRequest,
} from '../../types/api';

const KNOWN_FEATURES = [
  { key: 'PROJECTS', label: 'Maximum Projects', defaultUnit: 'COUNT', icon: FolderKanban, isStorage: false },
  { key: 'MEMBERS_PER_WORKSPACE', label: 'Team Members per Workspace', defaultUnit: 'COUNT', icon: Users, isStorage: false },
  { key: 'STORAGE', label: 'Storage Quota', defaultUnit: 'BYTES', icon: HardDrive, isStorage: true },
  { key: 'AI_TOKENS_PER_MONTH', label: 'AI Tokens per Month', defaultUnit: 'COUNT', icon: Sparkles, isStorage: false },
  { key: 'AI_GENERATION_CREDITS_MONTHLY', label: 'AI Credits per Month (Allowance)', defaultUnit: 'CREDITS', icon: Sparkles, isStorage: false },
  { key: 'EXPORTS_PER_MONTH', label: 'Document Exports per Month', defaultUnit: 'COUNT', icon: FileDown, isStorage: false },
  { key: 'ADVANCED_AI_MODELS', label: 'Advanced AI Models Access', defaultUnit: 'BOOLEAN', icon: Sparkles, isStorage: false },
  { key: 'PRIORITY_SUPPORT', label: 'Priority Support', defaultUnit: 'BOOLEAN', icon: Shield, isStorage: false },
];

export function AdminPlansPage() {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState<AdminSubscriptionPlan | null>(null);
  const [entitlementPlan, setEntitlementPlan] = useState<AdminSubscriptionPlan | null>(null);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Form states for create
  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newCurrency, setNewCurrency] = useState('GHS');
  const [newPrice, setNewPrice] = useState('50.00');
  const [newYearlyPrice, setNewYearlyPrice] = useState('');
  const [newInterval, setNewInterval] = useState<'NONE' | 'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [newPublic, setNewPublic] = useState(true);
  const [newFeatured, setNewFeatured] = useState(false);
  const [newDisplayOrder, setNewDisplayOrder] = useState('2');

  // Form states for edit
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editCurrency, setEditCurrency] = useState('GHS');
  const [editPrice, setEditPrice] = useState('0');
  const [editYearlyPrice, setEditYearlyPrice] = useState('');
  const [editInterval, setEditInterval] = useState<'NONE' | 'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [editPublic, setEditPublic] = useState(true);
  const [editFeatured, setEditFeatured] = useState(false);
  const [editDisplayOrder, setEditDisplayOrder] = useState('0');

  // Entitlements state
  const [entitlements, setEntitlements] = useState<AdminPlanEntitlement[]>([]);

  // Tab state
  const [activeTab, setActiveTab] = useState<'plans' | 'credit-packs' | 'grant-credits'>('plans');

  // Credit pack modals & form states
  const [showCreatePackModal, setShowCreatePackModal] = useState(false);
  const [editingPack, setEditingPack] = useState<AiCreditPack | null>(null);

  const [packCode, setPackCode] = useState('');
  const [packName, setPackName] = useState('');
  const [packDescription, setPackDescription] = useState('');
  const [packCredits, setPackCredits] = useState('100');
  const [packPrice, setPackPrice] = useState('50.00');
  const [packCurrency, setPackCurrency] = useState('GHS');
  const [packActive, setPackActive] = useState(true);
  const [packDisplayOrder, setPackDisplayOrder] = useState('0');

  const [editPackName, setEditPackName] = useState('');
  const [editPackDescription, setEditPackDescription] = useState('');
  const [editPackCredits, setEditPackCredits] = useState('');
  const [editPackPrice, setEditPackPrice] = useState('');
  const [editPackCurrency, setEditPackCurrency] = useState('GHS');
  const [editPackActive, setEditPackActive] = useState(true);
  const [editPackDisplayOrder, setEditPackDisplayOrder] = useState('0');

  // Admin Grant Form state
  const [grantWorkspaceId, setGrantWorkspaceId] = useState('');
  const [grantAmount, setGrantAmount] = useState('50');
  const [grantBucket, setGrantBucket] = useState<'PURCHASED' | 'PROMOTIONAL'>('PROMOTIONAL');
  const [grantReason, setGrantReason] = useState('');

  // Fetch plans
  const { data: plans, isLoading, error, refetch } = useQuery<AdminSubscriptionPlan[]>({
    queryKey: ['admin', 'subscription-plans'],
    queryFn: () => adminApi.plans(),
  });

  // Fetch AI credit packs
  const { data: creditPacks, isLoading: isPacksLoading, error: packsError, refetch: refetchPacks } = useQuery<AiCreditPack[]>({
    queryKey: ['admin', 'ai-credit-packs'],
    queryFn: () => adminApi.aiCreditPacks(),
  });

  // Create plan mutation
  const createMutation = useMutation({
    mutationFn: (body: CreateSubscriptionPlanRequest) => adminApi.createPlan(body),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-plans'] });
      setShowCreateModal(false);
      resetCreateForm();
      setStatusMessage({ type: 'success', text: `Plan "${created.name}" (${created.code}) created successfully.` });
    },
    onError: (err: unknown) => {
      setStatusMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to create plan.' });
    },
  });

  // Create AI credit pack mutation
  const createPackMutation = useMutation({
    mutationFn: (body: CreateAiCreditPackRequest) => adminApi.createAiCreditPack(body),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'ai-credit-packs'] });
      setShowCreatePackModal(false);
      resetPackCreateForm();
      setStatusMessage({ type: 'success', text: `AI Credit Pack "${created.name}" (${created.code}) created successfully.` });
    },
    onError: (err: any) => {
      setStatusMessage({ type: 'error', text: err?.response?.data?.message || err?.message || 'Failed to create credit pack.' });
    },
  });

  // Update AI credit pack mutation
  const updatePackMutation = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateAiCreditPackRequest }) => adminApi.updateAiCreditPack(id, body),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'ai-credit-packs'] });
      setEditingPack(null);
      setStatusMessage({ type: 'success', text: `AI Credit Pack "${updated.name}" updated successfully.` });
    },
    onError: (err: any) => {
      setStatusMessage({ type: 'error', text: err?.response?.data?.message || err?.message || 'Failed to update credit pack.' });
    },
  });

  // Admin grant credits mutation
  const grantCreditsMutation = useMutation({
    mutationFn: ({ workspaceId, body }: { workspaceId: string; body: { creditAmount: number; bucket: 'PURCHASED' | 'PROMOTIONAL'; reason: string } }) =>
      adminApi.grantAiCredits(workspaceId, body),
    onSuccess: (data) => {
      setGrantReason('');
      setStatusMessage({
        type: 'success',
        text: `Successfully granted credits to workspace! Total available: ${Number(data.totalAvailable).toFixed(2)} credits.`,
      });
    },
    onError: (err: any) => {
      setStatusMessage({ type: 'error', text: err?.response?.data?.message || err?.message || 'Failed to grant credits.' });
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateSubscriptionPlanRequest }) => adminApi.updatePlan(id, body),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-plans'] });
      setEditingPlan(null);
      setStatusMessage({ type: 'success', text: `Plan "${updated.name}" updated successfully.` });
    },
    onError: (err: unknown) => {
      setStatusMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to update plan.' });
    },
  });

  // Status toggle mutation
  const toggleStatusMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'activate' | 'deactivate' }) => {
      if (action === 'activate') return adminApi.activatePlan(id);
      return adminApi.deactivatePlan(id);
    },
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-plans'] });
      setStatusMessage({ type: 'success', text: `Plan status updated to ${updated.status}.` });
    },
    onError: (err: unknown) => {
      setStatusMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to toggle plan status.' });
    },
  });

  // Fetch entitlements for a plan
  const loadEntitlements = async (plan: AdminSubscriptionPlan) => {
    try {
      const data = await adminApi.getPlanEntitlements(plan.id);
      // Ensure all KNOWN_FEATURES are present in the list
      const merged = KNOWN_FEATURES.map((kf) => {
        const existing = data.find((e) => e.feature === kf.key);
        if (existing) return existing;
        return {
          feature: kf.key,
          enabled: true,
          limitMode: 'LIMITED' as LimitMode,
          limitValue: kf.key === 'PROJECTS' ? 5 : kf.key === 'MEMBERS_PER_WORKSPACE' ? 10 : 1000,
          limitUnit: kf.defaultUnit,
          formattedValue: '',
        };
      });
      setEntitlements(merged);
      setEntitlementPlan(plan);
    } catch (err: unknown) {
      setStatusMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to load entitlements.' });
    }
  };

  // Save entitlements mutation
  const saveEntitlementsMutation = useMutation({
    mutationFn: ({ id, items }: { id: string; items: AdminPlanEntitlement[] }) =>
      adminApi.updatePlanEntitlements(id, items),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'subscription-plans'] });
      setEntitlementPlan(null);
      setStatusMessage({ type: 'success', text: 'Entitlements updated successfully.' });
    },
    onError: (err: unknown) => {
      setStatusMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to update entitlements.' });
    },
  });

  const resetCreateForm = () => {
    setNewCode('');
    setNewName('');
    setNewDescription('');
    setNewCurrency('GHS');
    setNewPrice('50.00');
    setNewYearlyPrice('');
    setNewInterval('MONTHLY');
    setNewPublic(true);
    setNewFeatured(false);
    setNewDisplayOrder('2');
  };

  const resetPackCreateForm = () => {
    setPackCode('');
    setPackName('');
    setPackDescription('');
    setPackCredits('100');
    setPackPrice('50.00');
    setPackCurrency('GHS');
    setPackActive(true);
    setPackDisplayOrder('0');
  };

  const handleOpenPackEdit = (pack: AiCreditPack) => {
    setEditingPack(pack);
    setEditPackName(pack.name);
    setEditPackDescription(pack.description || '');
    setEditPackCredits((pack.creditAmount || pack.credits || 0).toString());
    setEditPackPrice((pack.priceAmount || pack.price || 0).toString());
    setEditPackCurrency(pack.currency || 'GHS');
    setEditPackActive(pack.active);
    setEditPackDisplayOrder((pack.displayOrder || 0).toString());
  };

  const handleCreatePackSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!packCode.trim() || !packName.trim()) {
      setStatusMessage({ type: 'error', text: 'Pack Code and Name are required.' });
      return;
    }
    const creditsNum = parseFloat(packCredits);
    if (isNaN(creditsNum) || creditsNum <= 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid credit amount greater than 0.' });
      return;
    }
    const priceNum = parseFloat(packPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid price.' });
      return;
    }
    createPackMutation.mutate({
      code: packCode.trim().toUpperCase(),
      name: packName.trim(),
      description: packDescription.trim() || undefined,
      creditAmount: creditsNum,
      priceAmount: priceNum,
      currency: packCurrency.trim().toUpperCase(),
      active: packActive,
      displayOrder: parseInt(packDisplayOrder, 10) || 0,
    });
  };

  const handleEditPackSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPack) return;
    const creditsNum = parseFloat(editPackCredits);
    if (isNaN(creditsNum) || creditsNum <= 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid credit amount greater than 0.' });
      return;
    }
    const priceNum = parseFloat(editPackPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid price.' });
      return;
    }
    updatePackMutation.mutate({
      id: editingPack.id,
      body: {
        name: editPackName.trim(),
        description: editPackDescription.trim() || undefined,
        creditAmount: creditsNum,
        priceAmount: priceNum,
        currency: editPackCurrency.trim().toUpperCase(),
        active: editPackActive,
        displayOrder: parseInt(editPackDisplayOrder, 10) || 0,
      },
    });
  };

  const handleOpenEdit = (plan: AdminSubscriptionPlan) => {
    setEditingPlan(plan);
    setEditName(plan.name);
    setEditDescription(plan.description || '');
    setEditCurrency(plan.currency);
    setEditPrice(plan.price.toString());
    setEditYearlyPrice(plan.yearlyPrice != null ? plan.yearlyPrice.toString() : '');
    setEditInterval(plan.billingInterval);
    setEditPublic(plan.publiclyAvailable);
    setEditFeatured(plan.featured);
    setEditDisplayOrder(plan.displayOrder.toString());
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCode.trim() || !newName.trim()) {
      setStatusMessage({ type: 'error', text: 'Code and Name are required.' });
      return;
    }
    const priceNum = parseFloat(newPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid price.' });
      return;
    }
    const yearlyPriceNum = newYearlyPrice.trim() ? parseFloat(newYearlyPrice) : undefined;
    if (yearlyPriceNum !== undefined && (isNaN(yearlyPriceNum) || yearlyPriceNum < 0)) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid yearly price.' });
      return;
    }
    createMutation.mutate({
      code: newCode.trim().toUpperCase(),
      name: newName.trim(),
      description: newDescription.trim() || undefined,
      currency: newCurrency.trim().toUpperCase(),
      price: priceNum,
      yearlyPrice: yearlyPriceNum,
      billingInterval: newInterval,
      publiclyAvailable: newPublic,
      featured: newFeatured,
      displayOrder: parseInt(newDisplayOrder, 10) || 0,
      status: 'ACTIVE',
    });
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPlan) return;
    const priceNum = parseFloat(editPrice);
    if (isNaN(priceNum) || priceNum < 0) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid price.' });
      return;
    }
    const yearlyPriceNum = editYearlyPrice.trim() ? parseFloat(editYearlyPrice) : null;
    if (yearlyPriceNum !== null && (isNaN(yearlyPriceNum) || yearlyPriceNum < 0)) {
      setStatusMessage({ type: 'error', text: 'Please enter a valid yearly price.' });
      return;
    }
    updateMutation.mutate({
      id: editingPlan.id,
      body: {
        name: editName.trim(),
        description: editDescription.trim() || undefined,
        currency: editCurrency.trim().toUpperCase(),
        price: priceNum,
        yearlyPrice: yearlyPriceNum,
        billingInterval: editInterval,
        publiclyAvailable: editPublic,
        featured: editFeatured,
        displayOrder: parseInt(editDisplayOrder, 10) || 0,
      },
    });
  };

  return (
    <div className="space-y-6" style={{ padding: '1.5rem' }}>
      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border, #333)', paddingBottom: '12px', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button
          type="button"
          className={`button ${activeTab === 'plans' ? 'primary' : 'secondary'}`}
          onClick={() => setActiveTab('plans')}
        >
          Subscription Plans ({plans?.length ?? 0})
        </button>
        <button
          type="button"
          className={`button ${activeTab === 'credit-packs' ? 'primary' : 'secondary'}`}
          onClick={() => setActiveTab('credit-packs')}
        >
          AI Credit Packs ({creditPacks?.length ?? 0})
        </button>
        <button
          type="button"
          className={`button ${activeTab === 'grant-credits' ? 'primary' : 'secondary'}`}
          onClick={() => setActiveTab('grant-credits')}
        >
          Grant Workspace Credits
        </button>
      </div>

      {/* Status feedback */}
      {statusMessage && (
        <div
          style={{
            padding: '0.75rem 1rem',
            borderRadius: '6px',
            marginBottom: '1rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: statusMessage.type === 'success' ? '#064e3b' : '#7f1d1d',
            color: '#fff',
          }}
        >
          <span>{statusMessage.text}</span>
          <button
            onClick={() => setStatusMessage(null)}
            style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer' }}
          >
            ×
          </button>
        </div>
      )}

      {/* SUBSCRIPTION PLANS TAB */}
      {activeTab === 'plans' && (
        <>
          {/* Top Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <CreditCard className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, margin: 0 }}>Subscription Plans</h1>
              </div>
              <p className="text-sm muted" style={{ margin: '0.25rem 0 0' }}>
                Manage pricing tiers, billing cycles, public visibility, and feature entitlements.
              </p>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <Button variant="secondary" onClick={() => refetch()}>
                <RefreshCw className="w-4 h-4 mr-1" style={{ width: '16px', height: '16px', verticalAlign: 'middle' }} />
                Refresh
              </Button>
              <Button variant="primary" onClick={() => { resetCreateForm(); setShowCreateModal(true); }}>
                <Plus className="w-4 h-4 mr-1" style={{ width: '16px', height: '16px', verticalAlign: 'middle' }} />
                Create Plan
              </Button>
            </div>
          </div>

      {/* Loading & Error States */}
      {isLoading && <p className="muted">Loading plans...</p>}
      {error && <ErrorState error={error} onRetry={() => { refetch(); }} />}

      {/* Plans Table */}
      {!isLoading && !error && plans && (
        <Card>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border, #333)', opacity: 0.8, fontSize: '0.85rem' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Code</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Name</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Price & Interval</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Visibility</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Subscribers</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Order</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {plans.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                      No subscription plans found. Click "Create Plan" to add the first plan (e.g., FREE or PRO).
                    </td>
                  </tr>
                ) : (
                  plans.map((p) => (
                    <tr key={p.id} style={{ borderBottom: '1px solid var(--border, #222)' }}>
                      <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                        <code style={{ background: 'var(--card-subtle, #1a1a1a)', padding: '2px 6px', borderRadius: '4px' }}>
                          {p.code}
                        </code>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>{p.name}</div>
                        {p.description && <div className="text-xs muted" style={{ fontSize: '0.75rem', opacity: 0.7 }}>{p.description}</div>}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>
                          <span style={{ fontWeight: 600 }}>
                            {p.currency} {Number(p.price).toFixed(2)}
                          </span>
                          <span className="text-xs muted" style={{ fontSize: '0.75rem', marginLeft: '4px' }}>
                            / {p.billingInterval.toLowerCase()}
                          </span>
                        </div>
                        {p.yearlyPrice != null && Number(p.yearlyPrice) > 0 ? (
                          <div style={{ fontSize: '0.75rem', color: 'var(--brand, #4f46e5)', marginTop: '2px' }}>
                            Yearly: {p.currency} {Number(p.yearlyPrice).toFixed(2)} / yr
                          </div>
                        ) : p.billingInterval !== 'NONE' ? (
                          <div className="text-xs muted" style={{ fontSize: '0.75rem', opacity: 0.6, marginTop: '2px' }}>
                            Yearly: Not configured
                          </div>
                        ) : null}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <Badge tone={p.status === 'ACTIVE' ? 'success' : p.status === 'INACTIVE' ? 'warning' : 'danger'}>
                          {p.status}
                        </Badge>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                          {p.publiclyAvailable ? (
                            <Badge tone="info">Public</Badge>
                          ) : (
                            <Badge tone="warning">Private</Badge>
                          )}
                          {p.featured && <Badge tone="success">Featured</Badge>}
                        </div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span style={{ fontWeight: 600 }}>{p.workspacesSubscribed}</span>{' '}
                        <span className="text-xs muted" style={{ fontSize: '0.75rem' }}>workspaces</span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>{p.displayOrder}</td>
                      <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                          <Button
                            variant="secondary"
                            onClick={() => loadEntitlements(p)}
                            title="Manage Entitlements"
                            style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                          >
                            <Sliders className="w-3.5 h-3.5 mr-1" style={{ width: '14px', height: '14px', verticalAlign: 'middle' }} />
                            Entitlements
                          </Button>
                          <Button
                            variant="secondary"
                            onClick={() => handleOpenEdit(p)}
                            title="Edit Plan"
                            style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                          >
                            <Edit2 className="w-3.5 h-3.5 mr-1" style={{ width: '14px', height: '14px', verticalAlign: 'middle' }} />
                            Edit
                          </Button>
                          {p.status === 'ACTIVE' ? (
                            <Button
                              variant="danger"
                              onClick={() => {
                                if (confirm(`Are you sure you want to deactivate plan ${p.name}? New subscriptions will not be allowed.`)) {
                                  toggleStatusMutation.mutate({ id: p.id, action: 'deactivate' });
                                }
                              }}
                              disabled={toggleStatusMutation.isPending}
                              style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                            >
                              Deactivate
                            </Button>
                          ) : (
                            <Button
                              variant="primary"
                              onClick={() => toggleStatusMutation.mutate({ id: p.id, action: 'activate' })}
                              disabled={toggleStatusMutation.isPending}
                              style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                            >
                              Activate
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}
        </>
      )}

      {/* AI CREDIT PACKS TAB */}
      {activeTab === 'credit-packs' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Sparkles className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, margin: 0 }}>AI Credit Top-Up Packs</h1>
              </div>
              <p className="text-sm muted" style={{ margin: '0.25rem 0 0' }}>
                Configure on-demand credit packs purchased through Paystack. Separate product — no recurring commitment, no 30% surcharge.
              </p>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <Button variant="secondary" onClick={() => refetchPacks()}>
                <RefreshCw className="w-4 h-4 mr-1" style={{ width: '16px', height: '16px', verticalAlign: 'middle' }} />
                Refresh
              </Button>
              <Button variant="primary" onClick={() => { resetPackCreateForm(); setShowCreatePackModal(true); }}>
                <Plus className="w-4 h-4 mr-1" style={{ width: '16px', height: '16px', verticalAlign: 'middle' }} />
                Create Credit Pack
              </Button>
            </div>
          </div>

          {isPacksLoading && <p className="muted">Loading AI credit packs...</p>}
          {packsError && <ErrorState error={packsError} onRetry={() => refetchPacks()} />}

          {!isPacksLoading && !packsError && creditPacks && (
            <Card>
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border, #333)', opacity: 0.8, fontSize: '0.85rem' }}>
                      <th style={{ padding: '0.75rem 1rem' }}>Code</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Name</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Credits</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Price</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                      <th style={{ padding: '0.75rem 1rem' }}>Order</th>
                      <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {creditPacks.length === 0 ? (
                      <tr>
                        <td colSpan={7} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                          No credit packs found. Click "Create Credit Pack" to create the first pack (e.g., STARTER_100, PRO_500).
                        </td>
                      </tr>
                    ) : (
                      creditPacks.map((pack) => (
                        <tr key={pack.id} style={{ borderBottom: '1px solid var(--border, #222)' }}>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                            <code style={{ background: 'var(--card-subtle, #1a1a1a)', padding: '2px 6px', borderRadius: '4px' }}>
                              {pack.code}
                            </code>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div style={{ fontWeight: 600 }}>{pack.name}</div>
                            {pack.description && (
                              <div className="text-xs muted" style={{ fontSize: '0.75rem', opacity: 0.7 }}>
                                {pack.description}
                              </div>
                            )}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: 600, color: 'var(--brand-color, #4f46e5)' }}>
                            +{Number(pack.creditAmount || pack.credits).toLocaleString()} credits
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                            {pack.currency} {Number(pack.priceAmount || pack.price).toFixed(2)}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <Badge tone={pack.active ? 'success' : 'warning'}>
                              {pack.active ? 'Active' : 'Inactive'}
                            </Badge>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>{pack.displayOrder}</td>
                          <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                              <Button
                                variant="secondary"
                                onClick={() => handleOpenPackEdit(pack)}
                                style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                              >
                                <Edit2 className="w-3 h-3 mr-1" style={{ width: '12px', height: '12px', verticalAlign: 'middle' }} />
                                Edit
                              </Button>
                              <Button
                                variant={pack.active ? 'danger' : 'secondary'}
                                onClick={() => updatePackMutation.mutate({ id: pack.id, body: { active: !pack.active } })}
                                disabled={updatePackMutation.isPending}
                                style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                              >
                                {pack.active ? 'Deactivate' : 'Activate'}
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </>
      )}

      {/* GRANT CREDITS TAB */}
      {activeTab === 'grant-credits' && (
        <Card style={{ maxWidth: '600px', margin: '0 auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
            <Sparkles className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
            <h2 style={{ fontSize: '1.3rem', fontWeight: 700, margin: 0 }}>Grant Credits to Workspace</h2>
          </div>
          <p className="muted" style={{ fontSize: '0.875rem', marginBottom: '1.25rem' }}>
            Directly credit a workspace's AI balance for testing, customer support, or promotional grants. Every grant creates an immutable ledger entry with the specified reason.
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!grantWorkspaceId.trim()) {
                setStatusMessage({ type: 'error', text: 'Workspace ID is required.' });
                return;
              }
              const amt = parseFloat(grantAmount);
              if (isNaN(amt) || amt <= 0) {
                setStatusMessage({ type: 'error', text: 'Please enter a valid credit amount.' });
                return;
              }
              if (!grantReason.trim()) {
                setStatusMessage({ type: 'error', text: 'Audit reason is mandatory for credit grants.' });
                return;
              }
              grantCreditsMutation.mutate({
                workspaceId: grantWorkspaceId.trim(),
                body: {
                  creditAmount: amt,
                  bucket: grantBucket,
                  reason: grantReason.trim(),
                },
              });
            }}
            style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}
          >
            <Field label="Target Workspace ID *">
              <Input
                placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                value={grantWorkspaceId}
                onChange={(e) => setGrantWorkspaceId(e.target.value)}
                required
              />
            </Field>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <Field label="Credit Bucket *">
                <Select
                  value={grantBucket}
                  onChange={(e) => setGrantBucket(e.target.value as 'PURCHASED' | 'PROMOTIONAL')}
                >
                  <option value="PROMOTIONAL">PROMOTIONAL (Complimentary)</option>
                  <option value="PURCHASED">PURCHASED (Manual Top-Up)</option>
                </Select>
              </Field>

              <Field label="Credit Amount *">
                <Input
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="50.00"
                  value={grantAmount}
                  onChange={(e) => setGrantAmount(e.target.value)}
                  required
                />
              </Field>
            </div>

            <Field label="Audit Reason (Mandatory) *">
              <Textarea
                placeholder="Describe why these credits are being granted (e.g. Test allocation, Beta tester credit, Service credit)..."
                value={grantReason}
                onChange={(e) => setGrantReason(e.target.value)}
                rows={3}
                required
              />
            </Field>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <LoadingButton
                type="submit"
                variant="primary"
                loading={grantCreditsMutation.isPending}
              >
                Grant Credits
              </LoadingButton>
            </div>
          </form>
        </Card>
      )}

      {/* CREATE AI CREDIT PACK MODAL */}
      {showCreatePackModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <Card style={{ width: '100%', maxWidth: '520px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>Create AI Credit Pack</h2>
              <button
                onClick={() => setShowCreatePackModal(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted, #888)', cursor: 'pointer', fontSize: '1.25rem' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleCreatePackSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.75rem' }}>
                <Field label="Pack Code *">
                  <Input
                    placeholder="STARTER_100"
                    value={packCode}
                    onChange={(e) => setPackCode(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Pack Name *">
                  <Input
                    placeholder="Starter Pack"
                    value={packName}
                    onChange={(e) => setPackName(e.target.value)}
                    required
                  />
                </Field>
              </div>

              <Field label="Description">
                <Input
                  placeholder="100 AI credits for research queries"
                  value={packDescription}
                  onChange={(e) => setPackDescription(e.target.value)}
                />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                <Field label="Credits *">
                  <Input
                    type="number"
                    step="1"
                    min="1"
                    value={packCredits}
                    onChange={(e) => setPackCredits(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Price (GHS) *">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    value={packPrice}
                    onChange={(e) => setPackPrice(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Currency">
                  <Input
                    value={packCurrency}
                    onChange={(e) => setPackCurrency(e.target.value)}
                  />
                </Field>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <Field label="Display Order">
                  <Input
                    type="number"
                    value={packDisplayOrder}
                    onChange={(e) => setPackDisplayOrder(e.target.value)}
                  />
                </Field>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1.5rem' }}>
                  <input
                    type="checkbox"
                    id="createPackActive"
                    checked={packActive}
                    onChange={(e) => setPackActive(e.target.checked)}
                  />
                  <label htmlFor="createPackActive" style={{ cursor: 'pointer' }}>Active for Purchase</label>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <Button type="button" variant="secondary" onClick={() => setShowCreatePackModal(false)}>
                  Cancel
                </Button>
                <LoadingButton type="submit" variant="primary" loading={createPackMutation.isPending}>
                  Create Pack
                </LoadingButton>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* EDIT AI CREDIT PACK MODAL */}
      {editingPack && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <Card style={{ width: '100%', maxWidth: '520px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>Edit AI Credit Pack: {editingPack.code}</h2>
              <button
                onClick={() => setEditingPack(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted, #888)', cursor: 'pointer', fontSize: '1.25rem' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleEditPackSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <Field label="Pack Name *">
                <Input
                  value={editPackName}
                  onChange={(e) => setEditPackName(e.target.value)}
                  required
                />
              </Field>

              <Field label="Description">
                <Input
                  value={editPackDescription}
                  onChange={(e) => setEditPackDescription(e.target.value)}
                />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                <Field label="Credits *">
                  <Input
                    type="number"
                    step="1"
                    min="1"
                    value={editPackCredits}
                    onChange={(e) => setEditPackCredits(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Price (GHS) *">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editPackPrice}
                    onChange={(e) => setEditPackPrice(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Currency">
                  <Input
                    value={editPackCurrency}
                    onChange={(e) => setEditPackCurrency(e.target.value)}
                  />
                </Field>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <Field label="Display Order">
                  <Input
                    type="number"
                    value={editPackDisplayOrder}
                    onChange={(e) => setEditPackDisplayOrder(e.target.value)}
                  />
                </Field>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1.5rem' }}>
                  <input
                    type="checkbox"
                    id="editPackActive"
                    checked={editPackActive}
                    onChange={(e) => setEditPackActive(e.target.checked)}
                  />
                  <label htmlFor="editPackActive" style={{ cursor: 'pointer' }}>Active for Purchase</label>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <Button type="button" variant="secondary" onClick={() => setEditingPack(null)}>
                  Cancel
                </Button>
                <LoadingButton type="submit" variant="primary" loading={updatePackMutation.isPending}>
                  Save Changes
                </LoadingButton>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* CREATE PLAN MODAL */}
      {showCreateModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <Card style={{ width: '100%', maxWidth: '550px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>Create Subscription Plan</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted, #888)', cursor: 'pointer', fontSize: '1.25rem' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleCreateSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.75rem' }}>
                <Field label="Plan Code *">
                  <Input
                    placeholder="PRO"
                    value={newCode}
                    onChange={(e) => setNewCode(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Plan Name *">
                  <Input
                    placeholder="Pro Researcher"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    required
                  />
                </Field>
              </div>

              <Field label="Description">
                <Textarea
                  placeholder="Advanced tools for serious academic & industry researchers..."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  rows={2}
                />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '0.75rem' }}>
                <Field label="Currency">
                  <Input
                    placeholder="GHS"
                    value={newCurrency}
                    onChange={(e) => setNewCurrency(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Price (Monthly)">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="20.00"
                    value={newPrice}
                    onChange={(e) => setNewPrice(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Yearly Price (Optional)">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="e.g. 200.00"
                    value={newYearlyPrice}
                    onChange={(e) => setNewYearlyPrice(e.target.value)}
                  />
                </Field>
                <Field label="Default Interval">
                  <Select
                    value={newInterval}
                    onChange={(e) => setNewInterval(e.target.value as any)}
                  >
                    <option value="NONE">NONE (One-off / Free)</option>
                    <option value="MONTHLY">MONTHLY</option>
                    <option value="YEARLY">YEARLY</option>
                  </Select>
                </Field>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem', alignItems: 'center' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={newPublic}
                    onChange={(e) => setNewPublic(e.target.checked)}
                  />
                  <span>Publicly Available</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={newFeatured}
                    onChange={(e) => setNewFeatured(e.target.checked)}
                  />
                  <span>Featured (Badge)</span>
                </label>
                <Field label="Display Order">
                  <Input
                    type="number"
                    value={newDisplayOrder}
                    onChange={(e) => setNewDisplayOrder(e.target.value)}
                  />
                </Field>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                <Button type="button" variant="secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <LoadingButton type="submit" loading={createMutation.isPending} variant="primary">
                  Create Plan
                </LoadingButton>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* EDIT PLAN MODAL */}
      {editingPlan && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <Card style={{ width: '100%', maxWidth: '550px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>Edit Plan: {editingPlan.code}</h2>
                <span className="text-xs muted">Plan code is permanent. Other attributes can be updated below.</span>
              </div>
              <button
                onClick={() => setEditingPlan(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted, #888)', cursor: 'pointer', fontSize: '1.25rem' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <Field label="Plan Name *">
                <Input
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  required
                />
              </Field>

              <Field label="Description">
                <Textarea
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  rows={2}
                />
              </Field>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '0.75rem' }}>
                <Field label="Currency">
                  <Input
                    value={editCurrency}
                    onChange={(e) => setEditCurrency(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Price (Monthly)">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    value={editPrice}
                    onChange={(e) => setEditPrice(e.target.value)}
                    required
                  />
                </Field>
                <Field label="Yearly Price (Optional)">
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="e.g. 200.00"
                    value={editYearlyPrice}
                    onChange={(e) => setEditYearlyPrice(e.target.value)}
                  />
                </Field>
                <Field label="Default Interval">
                  <Select
                    value={editInterval}
                    onChange={(e) => setEditInterval(e.target.value as any)}
                  >
                    <option value="NONE">NONE (One-off / Free)</option>
                    <option value="MONTHLY">MONTHLY</option>
                    <option value="YEARLY">YEARLY</option>
                  </Select>
                </Field>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem', alignItems: 'center' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={editPublic}
                    onChange={(e) => setEditPublic(e.target.checked)}
                  />
                  <span>Publicly Available</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={editFeatured}
                    onChange={(e) => setEditFeatured(e.target.checked)}
                  />
                  <span>Featured</span>
                </label>
                <Field label="Display Order">
                  <Input
                    type="number"
                    value={editDisplayOrder}
                    onChange={(e) => setEditDisplayOrder(e.target.value)}
                  />
                </Field>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                <Button type="button" variant="secondary" onClick={() => setEditingPlan(null)}>
                  Cancel
                </Button>
                <LoadingButton type="submit" loading={updateMutation.isPending} variant="primary">
                  Save Changes
                </LoadingButton>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* ENTITLEMENTS MODAL */}
      {entitlementPlan && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
        >
          <Card style={{ width: '100%', maxWidth: '750px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>
                  Entitlements for {entitlementPlan.name} ({entitlementPlan.code})
                </h2>
                <span className="text-xs muted">
                  Configure feature limits, quotas, and capability flags. Changes take effect on next token refresh.
                </span>
              </div>
              <button
                onClick={() => setEntitlementPlan(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted, #888)', cursor: 'pointer', fontSize: '1.25rem' }}
              >
                ×
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {entitlements.map((ent, idx) => {
                const kf = KNOWN_FEATURES.find((k) => k.key === ent.feature);
                const Icon = kf?.icon || Sparkles;
                const isStorage = kf?.isStorage;

                // Handle storage GB vs bytes representation
                const displayLimit = isStorage && ent.limitValue != null
                  ? (ent.limitValue / (1024 * 1024 * 1024)).toFixed(1)
                  : ent.limitValue != null ? ent.limitValue.toString() : '';

                return (
                  <div
                    key={ent.feature}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '2fr 1.5fr 1.5fr',
                      gap: '0.75rem',
                      alignItems: 'center',
                      padding: '0.75rem',
                      backgroundColor: 'var(--card-subtle, #141414)',
                      borderRadius: '6px',
                      border: '1px solid var(--border, #2a2a2a)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Icon className="w-5 h-5 text-primary" style={{ width: '20px', height: '20px' }} />
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{kf?.label || ent.feature}</div>
                        <code style={{ fontSize: '0.75rem', opacity: 0.6 }}>{ent.feature}</code>
                      </div>
                    </div>

                    <div>
                      <Select
                        value={ent.limitMode}
                        onChange={(e) => {
                          const nextMode = e.target.value as LimitMode;
                          const next = [...entitlements];
                          next[idx] = {
                            ...next[idx],
                            limitMode: nextMode,
                            enabled: nextMode !== 'DISABLED',
                            limitValue: nextMode === 'LIMITED' ? (next[idx].limitValue || 5) : null,
                          };
                          setEntitlements(next);
                        }}
                      >
                        <option value="LIMITED">LIMITED (Specific Quota)</option>
                        <option value="UNLIMITED">UNLIMITED (No Cap)</option>
                        <option value="DISABLED">DISABLED (Not Included)</option>
                      </Select>
                    </div>

                    <div>
                      {ent.limitMode === 'LIMITED' ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <Input
                            type="number"
                            min="0"
                            step={isStorage ? '0.5' : '1'}
                            value={displayLimit}
                            onChange={(e) => {
                              const val = parseFloat(e.target.value);
                              const next = [...entitlements];
                              if (isNaN(val)) {
                                next[idx] = { ...next[idx], limitValue: null };
                              } else {
                                next[idx] = {
                                  ...next[idx],
                                  limitValue: isStorage ? Math.round(val * 1024 * 1024 * 1024) : Math.round(val),
                                };
                              }
                              setEntitlements(next);
                            }}
                            placeholder={isStorage ? 'GB' : 'Count'}
                            style={{ width: '100%' }}
                          />
                          <span className="text-xs muted" style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>
                            {isStorage ? 'GB' : ent.limitUnit}
                          </span>
                        </div>
                      ) : (
                        <span className="text-xs muted" style={{ fontStyle: 'italic' }}>
                          {ent.limitMode === 'UNLIMITED' ? 'Unlimited access' : 'Feature disabled'}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.25rem' }}>
              <Button type="button" variant="secondary" onClick={() => setEntitlementPlan(null)}>
                Cancel
              </Button>
              <LoadingButton
                type="button"
                loading={saveEntitlementsMutation.isPending}
                variant="primary"
                onClick={() => {
                  if (entitlementPlan) {
                    saveEntitlementsMutation.mutate({ id: entitlementPlan.id, items: entitlements });
                  }
                }}
              >
                Save Entitlements
              </LoadingButton>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
