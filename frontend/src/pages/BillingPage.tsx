import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Activity,
  ArrowRight,
  Building2,
  Check,
  CreditCard,
  ExternalLink,
  Gift,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { billingApi } from '../api/endpoints';
import { publicApi } from '../api/public';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { Badge, Button, Card, Pagination } from '../components/ui';
import { paths } from '../routes/paths';
import type { PaymentTransaction, PaymentTransactionPage } from '../types/api';
import type { PublicPricingTierResponse } from '../types/publicSite';
import { displayValue } from '../utils/collections';

export function BillingPage() {
  const navigate = useNavigate();
  const { selectedWorkspaceId: workspaceId, selectedWorkspace } = useWorkspace();
  const [billingInterval, setBillingInterval] = useState<'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [transactionPage, setTransactionPage] = useState(0);
  const [nowMs] = useState(() => Date.now());

  const subscriptionQuery = useQuery({
    queryKey: ['billing', workspaceId, 'subscription'],
    queryFn: () => billingApi.subscription(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const usageQuery = useQuery({
    queryKey: ['billing', workspaceId, 'usage'],
    queryFn: () => billingApi.usage(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const transactionsQuery = useQuery<PaymentTransactionPage>({
    queryKey: ['billing', workspaceId, 'transactions', transactionPage],
    queryFn: () => billingApi.transactions(workspaceId, transactionPage, 10),
    enabled: Boolean(workspaceId),
  });

  const plansQuery = useQuery({
    queryKey: ['public', 'pricing'],
    queryFn: publicApi.getPricing,
  });

  const initializeMutation = useMutation({
    mutationFn: (body: { planCode: string; billingInterval: string }) =>
      billingApi.initialize(workspaceId, body),
    onSuccess: (data) => {
      if (data?.authorizationUrl) {
        window.location.assign(data.authorizationUrl);
      }
    },
  });

  const retryMutation = useMutation({
    mutationFn: (paymentIntentId: string) => billingApi.retry(paymentIntentId),
    onSuccess: (data) => {
      if (data?.authorizationUrl) {
        window.location.assign(data.authorizationUrl);
      }
    },
  });

  const aiCreditsQuery = useQuery({
    queryKey: ['billing', workspaceId, 'ai-credits'],
    queryFn: () => billingApi.aiCredits(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const aiCreditPacksQuery = useQuery({
    queryKey: ['billing', workspaceId, 'ai-credit-packs'],
    queryFn: () => billingApi.aiCreditPacks(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const [ledgerPage, setLedgerPage] = useState(0);
  const aiCreditLedgerQuery = useQuery({
    queryKey: ['billing', workspaceId, 'ai-credit-ledger', ledgerPage],
    queryFn: () => billingApi.aiCreditLedger(workspaceId, ledgerPage, 10),
    enabled: Boolean(workspaceId),
  });

  const buyCreditPackMutation = useMutation({
    mutationFn: (packId: string) => billingApi.buyAiCreditPack(workspaceId, packId),
    onSuccess: (data) => {
      if (data?.authorizationUrl && /^https:\/\/(checkout|standard)\.paystack\.(com|co)\//.test(data.authorizationUrl)) {
        window.location.href = data.authorizationUrl;
      }
    },
  });

  // 1. WORKSPACE REQUIREMENT: CLEAN EMPTY STATE WHEN NO WORKSPACE IS SELECTED
  if (!workspaceId) {
    return (
      <section className="page billing-page">
        <header className="page-header">
          <div>
            <h1 className="page-title">Workspace Billing & Plans</h1>
            <p className="muted">Manage your subscription, resource quotas, and transaction history.</p>
          </div>
        </header>

        <Card className="empty-workspace-billing-card">
          <div className="empty-billing-icon-box">
            <Building2 size={44} className="text-brand" />
          </div>
          <h2>Select a workspace to manage billing</h2>
          <p className="muted" style={{ maxWidth: '480px', margin: '8px auto 20px', lineHeight: 1.6 }}>
            Subscriptions, empirical tool quotas, and Paystack payment records are scoped to individual workspaces.
            Please select or switch to an active workspace to view or upgrade your plan.
          </p>
          <Button type="button" variant="primary" onClick={() => navigate(paths.workspaces)}>
            Select Workspace
          </Button>
        </Card>
      </section>
    );
  }

  const subscriptionData = subscriptionQuery.data as Record<string, unknown> | undefined;
  const currentPlanCode = String(subscriptionData?.planCode ?? subscriptionData?.code ?? 'FREE').toUpperCase();
  const accessType = String(subscriptionData?.accessType ?? subscriptionData?.grantType ?? '');
  const isComplimentary = ['COMPLIMENTARY', 'DEVELOPER_ACCESS', 'PROMOTIONAL'].includes(accessType);

  const usageData = usageQuery.data as Record<string, unknown> | undefined;

  const handleChoosePlan = (plan: PublicPricingTierResponse) => {
    if (plan.code.toUpperCase() === 'FREE' || Number(plan.monthlyPrice) === 0) {
      return; // FREE must never invoke Paystack
    }
    initializeMutation.mutate({
      planCode: plan.code,
      billingInterval,
    });
  };

  const transactionsPage = transactionsQuery.data;
  const transactions: PaymentTransaction[] = Array.isArray(transactionsPage?.content)
    ? transactionsPage.content
    : Array.isArray(transactionsPage)
      ? (transactionsPage as unknown as PaymentTransaction[])
      : [];
  const totalPages = transactionsPage?.totalPages ?? 1;

  const periodEndStr = subscriptionData?.periodEnd ? String(subscriptionData.periodEnd) : null;
  const periodEndDate = periodEndStr ? new Date(periodEndStr) : null;
  const isApproachingExpiry = Boolean(
    currentPlanCode !== 'FREE' &&
    periodEndDate &&
    (periodEndDate.getTime() - nowMs) <= 7 * 24 * 60 * 60 * 1000
  );
  const isExpired = Boolean(
    currentPlanCode !== 'FREE' &&
    periodEndDate &&
    periodEndDate.getTime() <= nowMs
  );

  const hasYearlyPricing = (plansQuery.data ?? []).some(
    (p) => p.annualPrice != null && Number(p.annualPrice) > 0,
  );

  const handleRenewNow = () => {
    initializeMutation.mutate({
      planCode: currentPlanCode,
      billingInterval: (subscriptionData?.billingInterval as string) || 'MONTHLY',
    });
  };

  return (
    <section className="page billing-page">
      <header className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
            <h1 className="page-title">Workspace Billing & Plans</h1>
            <Badge tone="warning">PAYSTACK TEST MODE</Badge>
          </div>
          <p className="muted">
            Managing subscription for workspace: <strong>{selectedWorkspace?.name ?? workspaceId}</strong>
          </p>
        </div>

        <div className="toolbar">
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              subscriptionQuery.refetch();
              usageQuery.refetch();
              transactionsQuery.refetch();
              plansQuery.refetch();
            }}
            disabled={subscriptionQuery.isFetching}
          >
            <RefreshCw size={15} className={subscriptionQuery.isFetching ? 'spin' : ''} /> Refresh Status
          </Button>
        </div>
      </header>

      {/* RENEWAL NOTICE BANNER FOR PAID PLANS */}
      {(isApproachingExpiry || isExpired) && (
        <div
          className={`alert ${isExpired ? 'danger' : 'warning'}`}
          style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Sparkles size={20} />
            <div>
              <strong>{isExpired ? 'Subscription Expired:' : 'Subscription Renewal Notice:'}</strong>{' '}
              Your {currentPlanCode} plan {isExpired ? 'expired on' : 'expires on'} {periodEndStr ? periodEndStr.slice(0, 10) : 'soon'}.
              {isExpired ? ' Your workspace has fallen back to the Free plan limits.' : ' Renew now to avoid interruption to your academic research features.'}
            </div>
          </div>
          <Button
            type="button"
            variant="primary"
            onClick={handleRenewNow}
            disabled={initializeMutation.isPending}
          >
            {initializeMutation.isPending ? 'Processing...' : 'Renew Now'}
          </Button>
        </div>
      )}

      {/* PAYSTACK TEST CHECKOUT RESULT ALERT */}
      {initializeMutation.data ? (
        <CheckoutResultBanner data={initializeMutation.data} />
      ) : null}

      {/* CURRENT ACCESS & USAGE QUOTAS */}
      <div className="responsive-grid-cards" style={{ marginBottom: '28px' }}>
        {/* CARD 1: CURRENT PLAN DETAILS */}
        <Card className="billing-summary-card">
          <div className="card-header-iconic">
            <CreditCard size={24} className="text-brand" />
            <div>
              <h3>Current Access</h3>
              <p className="muted">Active subscription and entitlement scope</p>
            </div>
          </div>

          <div className="current-plan-badge-box">
            <div className="plan-name-display">{currentPlanCode}</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text)' }}>
              {currentPlanCode === 'FREE'
                ? 'GHS 0'
                : subscriptionData?.price != null
                  ? `${subscriptionData.currency ?? 'GHS'} ${subscriptionData.price} / ${(subscriptionData.billingInterval as string)?.toLowerCase() === 'yearly' ? 'year' : 'month'}`
                  : ''}
            </div>
            {isComplimentary ? (
              <Badge tone="success">
                <Gift size={13} /> {accessType.replaceAll('_', ' ')}
              </Badge>
            ) : (
              <Badge tone={isExpired ? 'danger' : 'info'}>
                {isExpired ? 'EXPIRED' : displayValue(subscriptionData?.status, 'ACTIVE')}
              </Badge>
            )}
          </div>

          <div className="plan-meta-details">
            <div className="plan-meta-row">
              <span className="muted">Access source:</span>
              <strong>{isComplimentary ? 'Complimentary' : currentPlanCode === 'FREE' ? 'Free Default' : 'Paid Subscription'}</strong>
            </div>
            <div className="plan-meta-row">
              <span className="muted">Billing status:</span>
              <strong>{isComplimentary ? 'No billing required (Granted)' : currentPlanCode === 'FREE' ? 'Active free plan (No charges)' : isExpired ? 'Expired (Fallback to Free)' : 'Active subscription'}</strong>
            </div>
            {subscriptionData?.periodStart && currentPlanCode !== 'FREE' ? (
              <div className="plan-meta-row">
                <span className="muted">Started:</span>
                <strong>{String(subscriptionData.periodStart).slice(0, 10)}</strong>
              </div>
            ) : null}
            {subscriptionData?.periodEnd && currentPlanCode !== 'FREE' ? (
              <div className="plan-meta-row">
                <span className="muted">{isExpired ? 'Expired:' : 'Renews/Expires:'}</span>
                <strong>{String(subscriptionData.periodEnd).slice(0, 10)}</strong>
              </div>
            ) : null}
            <div className="plan-meta-row">
              <span className="muted">Environment:</span>
              <Badge tone="warning" className="badge-sm">Paystack Test Gateway</Badge>
            </div>
          </div>

          {currentPlanCode !== 'FREE' && (isApproachingExpiry || isExpired) ? (
            <div style={{ marginTop: '16px' }}>
              <Button
                type="button"
                variant="primary"
                className="w-full"
                onClick={handleRenewNow}
                disabled={initializeMutation.isPending}
              >
                {initializeMutation.isPending ? 'Processing...' : 'Renew Now'}
              </Button>
            </div>
          ) : null}
        </Card>

        {/* CARD 2: RESOURCE QUOTAS WITH PROGRESS BARS */}
        <Card className="billing-summary-card">
          <div className="card-header-iconic">
            <Sparkles size={24} className="text-brand" />
            <div>
              <h3>Resource Quotas & Usage</h3>
              <p className="muted">Current billing period allowances and reset schedule</p>
            </div>
          </div>

          <div className="usage-metrics-list">
            <UsageProgressItem
              label="AI Requests"
              metric={usageData?.aiRequests as { used?: number; limit?: number | null } | undefined}
              unit="requests"
            />
            <UsageProgressItem
              label="AI Tokens"
              metric={usageData?.aiTokens as { used?: number; limit?: number | null } | undefined}
              unit="tokens"
            />
            <UsageProgressItem
              label="Research Projects"
              metric={usageData?.projects as { used?: number; limit?: number | null } | undefined}
              unit="projects"
            />
            <UsageProgressItem
              label="Document Storage"
              metric={usageData?.storage as { used?: number; limit?: number | null } | undefined}
              unit="MB"
            />
          </div>
        </Card>
      </div>

      {/* AVAILABLE PLANS SECTION */}
      <div className="billing-plans-section">
        <div className="plans-section-header">
          <div>
            <h2>Available Plans</h2>
            <p className="muted">Choose the empirical research tier that fits your academic project.</p>
          </div>

          {/* DYNAMIC BILLING INTERVAL SELECTOR: show Monthly only if no yearly prices exist */}
          {hasYearlyPricing ? (
            <div className="interval-toggle-container" role="radiogroup" aria-label="Billing interval">
              <button
                type="button"
                className={`interval-toggle-btn ${billingInterval === 'MONTHLY' ? 'is-active' : ''}`}
                onClick={() => setBillingInterval('MONTHLY')}
              >
                Monthly
              </button>
              <button
                type="button"
                className={`interval-toggle-btn ${billingInterval === 'YEARLY' ? 'is-active' : ''}`}
                onClick={() => setBillingInterval('YEARLY')}
              >
                Yearly
              </button>
            </div>
          ) : (
            <div className="interval-toggle-container">
              <span className="badge info" style={{ padding: '6px 12px', fontSize: '0.85rem' }}>
                Monthly Billing
              </span>
            </div>
          )}
        </div>

        {/* PLAN CARDS GRID */}
        {plansQuery.isLoading ? (
          <p className="muted">Loading subscription plans from backend...</p>
        ) : plansQuery.isError ? (
          <div className="alert danger">Unable to fetch available subscription plans.</div>
        ) : (
          <div className="billing-plan-cards-grid">
            {(plansQuery.data ?? []).map((plan) => {
              const isCurrent = currentPlanCode === plan.code.toUpperCase();
              const isFree = plan.code.toUpperCase() === 'FREE' || Number(plan.monthlyPrice) === 0;
              const hasYearlyForThisPlan = plan.annualPrice != null && Number(plan.annualPrice) > 0;
              const isYearlyUnavailable = billingInterval === 'YEARLY' && !isFree && !hasYearlyForThisPlan;

              return (
                <div
                  key={plan.id}
                  className={`plan-pricing-card ${plan.highlighted ? 'is-highlighted' : ''} ${isCurrent ? 'is-current' : ''}`}
                >
                  {plan.highlightBadge ? (
                    <div className="plan-badge-ribbon">{plan.highlightBadge}</div>
                  ) : null}

                  <div className="plan-card-top">
                    <h3 className="plan-card-title">{plan.name}</h3>
                    <p className="plan-card-description muted">{plan.description}</p>
                    <PlanBreakdownDisplay
                      planCode={plan.code}
                      interval={billingInterval}
                      fallbackCurrency={plan.currency}
                      fallbackBasePrice={isFree ? 0 : billingInterval === 'YEARLY' ? (plan.annualPrice ?? 0) : plan.monthlyPrice}
                      isFree={isFree}
                    />
                  </div>

                  <hr className="plan-card-divider" />

                  <ul className="plan-features-list">
                    {plan.features.map((feature, idx) => (
                      <li key={idx}>
                        <Check size={16} className="text-success feature-check" />
                        <span>{feature}</span>
                      </li>
                    ))}
                  </ul>

                  <div className="plan-card-action">
                    {isCurrent ? (
                      <Button type="button" variant="secondary" disabled className="w-full">
                        Current Plan
                      </Button>
                    ) : isFree ? (
                      <Button type="button" variant="secondary" disabled className="w-full">
                        Included Base Plan
                      </Button>
                    ) : isYearlyUnavailable ? (
                      <Button type="button" variant="secondary" disabled className="w-full">
                        Yearly Not Available
                      </Button>
                    ) : (
                      <Button
                        type="button"
                        variant={plan.highlighted ? 'primary' : 'secondary'}
                        className="w-full"
                        onClick={() => handleChoosePlan(plan)}
                        disabled={initializeMutation.isPending}
                      >
                        {initializeMutation.isPending ? 'Processing...' : `Upgrade to ${plan.name}`}
                        <ArrowRight size={15} />
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* AI CREDITS & TOP-UP SECTION */}
      <Card className="billing-credits-card" style={{ marginBottom: '24px' }}>
        <div className="card-header-iconic" style={{ marginBottom: '16px' }}>
          <Sparkles size={24} className="text-brand" />
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
              <div>
                <h3 style={{ margin: 0 }}>AI Credits & On-Demand Top-Up</h3>
                <p className="muted" style={{ margin: '4px 0 0' }}>
                  Empirical credit wallet for AI research generation, grounded document retrieval, and analysis.
                </p>
              </div>
              <Badge tone="info">NO 30% SURCHARGE ON CREDIT PACKS</Badge>
            </div>
          </div>
        </div>

        {/* WALLET BALANCES GRID */}
        {aiCreditsQuery.isLoading ? (
          <p className="muted">Loading credit balances...</p>
        ) : (
          <div className="grid cols-4" style={{ gap: '14px', marginBottom: '20px' }}>
            <div style={{ padding: '14px', borderRadius: '8px', background: 'var(--surface-subtle, rgba(255,255,255,0.03))', border: '1px solid var(--border-color, rgba(255,255,255,0.08))' }}>
              <span className="muted" style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Total Available</span>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '4px 0' }}>
                {Number(aiCreditsQuery.data?.totalAvailable ?? 0).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Ready for generation tasks</span>
            </div>

            <div style={{ padding: '14px', borderRadius: '8px', background: 'var(--surface-subtle, rgba(255,255,255,0.03))', border: '1px solid var(--border-color, rgba(255,255,255,0.08))' }}>
              <span className="muted" style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Plan Monthly Allowance</span>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '4px 0' }}>
                {aiCreditsQuery.data?.included?.remaining != null
                  ? Number(aiCreditsQuery.data.included.remaining).toLocaleString()
                  : (aiCreditsQuery.data?.included?.limitMode === 'UNLIMITED' ? 'Unlimited' : '0')}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                {aiCreditsQuery.data?.included?.limit ? `Limit: ${Number(aiCreditsQuery.data.included.limit).toLocaleString()} / mo` : 'Reset monthly with plan'}
              </span>
            </div>

            <div style={{ padding: '14px', borderRadius: '8px', background: 'var(--surface-subtle, rgba(255,255,255,0.03))', border: '1px solid var(--border-color, rgba(255,255,255,0.08))' }}>
              <span className="muted" style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Purchased Packs</span>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '4px 0' }}>
                {Number(aiCreditsQuery.data?.purchased?.remaining ?? 0).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Never expire while active</span>
            </div>

            <div style={{ padding: '14px', borderRadius: '8px', background: 'var(--surface-subtle, rgba(255,255,255,0.03))', border: '1px solid var(--border-color, rgba(255,255,255,0.08))' }}>
              <span className="muted" style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Promotional / Admin</span>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, margin: '4px 0' }}>
                {Number(aiCreditsQuery.data?.promotional?.remaining ?? 0).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 2 })}
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Complimentary grants</span>
            </div>
          </div>
        )}

        <div className="alert info" style={{ marginBottom: '20px', fontSize: '0.875rem' }}>
          <strong>Order of consumption:</strong> When running AI generations, the system automatically uses your <strong>(1) Included monthly allowance</strong> first, then <strong>(2) Promotional credits</strong>, and finally <strong>(3) Purchased credits</strong>. Pre-reservations prevent concurrent exhaustion and only exact token credits are deducted.
        </div>

        {/* TOP-UP PACKS */}
        <h4 style={{ margin: '0 0 12px', fontSize: '1.05rem' }}>Buy AI Credit Top-Up Packs</h4>
        {buyCreditPackMutation.isError && (
          <div className="alert danger" style={{ marginBottom: '12px' }}>
            {(buyCreditPackMutation.error as any)?.response?.data?.message || 'Failed to initialize pack purchase.'}
          </div>
        )}
        {aiCreditPacksQuery.isLoading ? (
          <p className="muted">Loading available packs...</p>
        ) : (aiCreditPacksQuery.data ?? []).length === 0 ? (
          <p className="muted">No credit packs available for purchase.</p>
        ) : (
          <div className="grid cols-3" style={{ gap: '16px', marginBottom: '24px' }}>
            {(aiCreditPacksQuery.data ?? []).map((pack) => (
              <div
                key={pack.id}
                style={{
                  padding: '16px',
                  borderRadius: '10px',
                  border: '1px solid var(--border-color, rgba(255,255,255,0.1))',
                  background: 'var(--surface-card, rgba(255,255,255,0.02))',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                    <h4 style={{ margin: 0, fontSize: '1.1rem' }}>{pack.name}</h4>
                    <Badge tone="success">{pack.currency} {Number(pack.priceAmount || pack.price).toFixed(2)}</Badge>
                  </div>
                  <p className="muted" style={{ fontSize: '0.85rem', margin: '0 0 12px' }}>
                    {pack.description || `${Number(pack.creditAmount || pack.credits).toLocaleString()} AI credits`}
                  </p>
                  <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--brand-color, #4f46e5)', marginBottom: '14px' }}>
                    +{Number(pack.creditAmount || pack.credits).toLocaleString()} <span style={{ fontSize: '0.85rem', fontWeight: 400, color: 'var(--text-muted)' }}>credits</span>
                  </div>
                </div>

                <Button
                  type="button"
                  variant="primary"
                  className="w-full"
                  disabled={buyCreditPackMutation.isPending}
                  onClick={() => buyCreditPackMutation.mutate(pack.id)}
                >
                  {buyCreditPackMutation.isPending ? 'Redirecting to Paystack...' : `Buy for ${pack.currency} ${Number(pack.priceAmount || pack.price).toFixed(2)}`}
                  <ArrowRight size={14} />
                </Button>
              </div>
            ))}
          </div>
        )}

        {/* LEDGER AUDIT TABLE */}
        <h4 style={{ margin: '0 0 12px', fontSize: '1.05rem' }}>AI Credit Ledger & Usage History</h4>
        {aiCreditLedgerQuery.isLoading ? (
          <p className="muted">Loading credit transactions...</p>
        ) : (aiCreditLedgerQuery.data?.content ?? []).length === 0 ? (
          <p className="muted">No credit transactions recorded yet.</p>
        ) : (
          <>
            <div className="admin-table-container" style={{ marginTop: '8px' }}>
              <table className="admin-data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Bucket</th>
                    <th>Amount</th>
                    <th>Balance Before</th>
                    <th>Balance After</th>
                    <th>Source / Request</th>
                  </tr>
                </thead>
                <tbody>
                  {(aiCreditLedgerQuery.data?.content ?? []).map((entry) => (
                    <tr key={entry.id}>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        {entry.createdAt ? new Date(entry.createdAt).toLocaleString() : '—'}
                      </td>
                      <td>
                        <Badge
                          tone={
                            entry.type.includes('GRANT') || entry.type.includes('PURCHASE')
                              ? 'success'
                              : entry.type.includes('RELEASE')
                                ? 'info'
                                : 'warning'
                          }
                        >
                          {entry.type}
                        </Badge>
                      </td>
                      <td>
                        <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>{entry.bucket}</span>
                      </td>
                      <td style={{ fontWeight: 600 }}>
                        {Number(entry.creditAmount) > 0 ? `+${Number(entry.creditAmount).toFixed(2)}` : Number(entry.creditAmount).toFixed(2)}
                      </td>
                      <td>{Number(entry.balanceBefore).toFixed(2)}</td>
                      <td>{Number(entry.balanceAfter).toFixed(2)}</td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {entry.sourceType}{entry.aiRequestId ? ` · Req ${entry.aiRequestId.slice(0, 8)}...` : ''}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {(aiCreditLedgerQuery.data?.totalPages ?? 1) > 1 && (
              <div style={{ marginTop: '14px' }}>
                <Pagination
                  page={ledgerPage}
                  totalPages={aiCreditLedgerQuery.data?.totalPages ?? 1}
                  onPageChange={setLedgerPage}
                />
              </div>
            )}
          </>
        )}
      </Card>

      {/* PAYMENT HISTORY */}
      <Card className="billing-history-card">
        <div className="card-header-iconic">
          <Activity size={24} className="text-brand" />
          <div>
            <h3>Payment History & Mobile Money Verification</h3>
            <p className="muted">Transactions processed through Paystack in TEST mode</p>
          </div>
        </div>

        {transactionsQuery.isLoading ? (
          <p className="muted" style={{ padding: '16px 0' }}>
            Loading payment transactions...
          </p>
        ) : transactionsQuery.isError ? (
          <div className="alert danger" style={{ marginTop: '12px' }}>
            Unable to load transaction history. Please refresh or try again later.
          </div>
        ) : transactions.length === 0 ? (
          <p className="muted" style={{ padding: '16px 0' }}>
            No payment transactions yet.
          </p>
        ) : (
          <>
            <div className="admin-table-container" style={{ marginTop: '12px' }}>
              <table className="admin-data-table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Plan</th>
                    <th>Interval</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx, idx) => (
                    <tr key={String(tx.id ?? idx)}>
                      <td>
                        <code style={{ fontSize: '0.82rem' }}>
                          {(() => {
                            const ref = String(tx.reference ?? tx.internalReference ?? tx.id);
                            return ref.length > 24 ? `${ref.slice(0, 20)}...` : ref;
                          })()}
                        </code>
                      </td>
                      <td><strong>{displayValue(tx.planCode)}</strong></td>
                      <td>{displayValue(tx.billingInterval)}</td>
                      <td>
                        {displayValue(tx.currency)} {displayValue(tx.amount)}
                      </td>
                      <td>
                        <Badge
                          tone={
                            tx.status === 'SUCCESS' || tx.status === 'SUCCEEDED'
                              ? 'success'
                              : tx.status === 'PENDING' || tx.status === 'INITIALIZED'
                                ? 'warning'
                                : 'danger'
                          }
                        >
                          {displayValue(tx.status)}
                        </Badge>
                      </td>
                      <td className="muted" style={{ fontSize: '0.85rem' }}>
                        {tx.createdAt ? String(tx.createdAt).slice(0, 10) : 'Recent'}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {['FAILED', 'PENDING', 'INITIALIZED'].includes(String(tx.status)) && tx.paymentIntentId ? (
                          <Button
                            type="button"
                            variant="primary"
                            className="btn-compact"
                            style={{ padding: '3px 8px', fontSize: '0.75rem' }}
                            disabled={retryMutation.isPending}
                            onClick={() => retryMutation.mutate(String(tx.paymentIntentId))}
                          >
                            <RefreshCw size={12} className={retryMutation.isPending ? 'spin' : ''} /> Pay Again
                          </Button>
                        ) : (
                          <span className="muted" style={{ fontSize: '0.75rem' }}>—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {totalPages > 1 ? (
              <div style={{ marginTop: '16px', display: 'flex', justifyContent: 'flex-end' }}>
                <Pagination
                  page={transactionPage}
                  totalPages={totalPages}
                  onPageChange={setTransactionPage}
                />
              </div>
            ) : null}
          </>
        )}

        <div className="alert info" style={{ marginTop: '20px' }}>
          <Activity size={16} />
          <div>
            <strong>Pending Mobile Money Transactions:</strong>
            <p style={{ margin: '4px 0 0', fontSize: '0.88rem' }}>
              If waiting for mobile authorization approval, use "Refresh Status" once you complete the prompt on your handset.
            </p>
          </div>
        </div>

        {retryMutation.data ? <CheckoutResultBanner data={retryMutation.data} /> : null}
      </Card>
    </section>
  );
}

function PlanBreakdownDisplay({
  planCode,
  interval,
  fallbackCurrency,
  fallbackBasePrice,
  isFree,
}: {
  planCode: string;
  interval: string;
  fallbackCurrency: string;
  fallbackBasePrice: number | string;
  isFree: boolean;
}) {
  const breakdownQuery = useQuery({
    queryKey: ['billing', 'breakdown', planCode, interval],
    queryFn: () => billingApi.priceBreakdown(planCode, interval),
    enabled: !isFree,
    staleTime: 60_000,
  });

  if (isFree) {
    return (
      <div className="plan-card-price">
        <span className="price-currency">{fallbackCurrency}</span>
        <span className="price-amount">0.00</span>
        <span className="price-interval muted">/ month</span>
      </div>
    );
  }

  const breakdown = breakdownQuery.data;
  const curr = breakdown?.currency ?? fallbackCurrency;
  const base = breakdown ? Number(breakdown.baseAmount).toFixed(2) : Number(fallbackBasePrice || 0).toFixed(2);
  const proc = breakdown ? Number(breakdown.processingAmount).toFixed(2) : (Number(base) * 0.02).toFixed(2);
  const ai = breakdown ? Number(breakdown.aiGenerationAmount).toFixed(2) : (Number(base) * 0.30).toFixed(2);
  const total = breakdown ? Number(breakdown.totalAmount).toFixed(2) : (Number(base) * 1.32).toFixed(2);

  return (
    <div className="plan-breakdown-box" style={{ marginTop: '8px', fontSize: '0.82rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
        <span className="muted">Base subscription:</span>
        <span>{curr} {base}</span>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
        <span className="muted">Processing:</span>
        <span>{curr} {proc}</span>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
        <span className="muted">AI generation:</span>
        <span>{curr} {ai}</span>
      </div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          borderTop: '1px solid var(--border, #334155)',
          paddingTop: '6px',
          marginTop: '4px',
          fontWeight: 700,
          fontSize: '0.92rem',
        }}
      >
        <span>Total {interval === 'YEARLY' ? '/ year' : '/ month'}:</span>
        <span style={{ color: 'var(--brand, #38bdf8)' }}>{curr} {total}</span>
      </div>
    </div>
  );
}

function UsageProgressItem({
  label,
  metric,
  unit,
}: {
  label: string;
  metric?: { used?: number; limit?: number | null };
  unit: string;
}) {
  const rawUsed = metric?.used ?? 0;
  const rawLimit = metric?.limit;
  const isStorage = unit === 'MB' || unit === 'BYTES';
  const used = isStorage && rawUsed > 1024 * 1024 ? Math.round(rawUsed / (1024 * 1024)) : rawUsed;
  const limit = isStorage && rawLimit && rawLimit > 1024 * 1024 ? Math.round(rawLimit / (1024 * 1024)) : rawLimit;
  const displayUnit = isStorage ? 'MB' : unit;
  const isUnlimited = limit === null || limit === undefined;
  const percent = isUnlimited ? 0 : Math.min(100, Math.round((used / Math.max(1, limit)) * 100));

  return (
    <div className="usage-progress-item">
      <div className="usage-labels-row">
        <span className="usage-name">{label}</span>
        <span className="usage-numbers">
          {isUnlimited ? (
            <span className="badge-sm badge-info">Unlimited</span>
          ) : (
            <>
              <strong>{used.toLocaleString()}</strong> / {limit.toLocaleString()} {displayUnit}
            </>
          )}
        </span>
      </div>

      {!isUnlimited ? (
        <div className="usage-bar-track">
          <div
            className={`usage-bar-fill ${percent >= 90 ? 'danger' : percent >= 70 ? 'warning' : 'primary'}`}
            style={{ width: `${percent}%` }}
          />
        </div>
      ) : null}
    </div>
  );
}

function CheckoutResultBanner({ data }: { data: { authorizationUrl?: string } }) {
  const url = String(data.authorizationUrl ?? '');
  const isTrusted = /^https:\/\/(checkout|standard)\.paystack\.(com|co)\//.test(url);

  return (
    <div className="alert info checkout-banner" style={{ marginBottom: '24px' }}>
      <CreditCard size={20} />
      <div style={{ flex: 1 }}>
        <strong>Paystack TEST Checkout Ready</strong>
        <p style={{ margin: '4px 0 0', fontSize: '0.88rem' }}>
          Backend initialized a payment attempt. Open the test payment interface to complete the flow.
        </p>
      </div>
      {isTrusted ? (
        <a
          href={url}
          target="_blank"
          rel="noopener noreferrer"
          className="button primary btn-compact"
        >
          Open Paystack Checkout <ExternalLink size={14} />
        </a>
      ) : (
        <Badge tone="danger">Host verification failed</Badge>
      )}
    </div>
  );
}
