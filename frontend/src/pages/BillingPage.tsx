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
import { Badge, Button, Card, Field, Input } from '../components/ui';
import { paths } from '../routes/paths';
import type { PublicPricingTierResponse } from '../types/publicSite';
import { displayValue } from '../utils/collections';

export function BillingPage() {
  const navigate = useNavigate();
  const { selectedWorkspaceId: workspaceId, selectedWorkspace } = useWorkspace();
  const [billingInterval, setBillingInterval] = useState<'MONTHLY' | 'YEARLY'>('MONTHLY');
  const [retryIntentId, setRetryIntentId] = useState('');

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

  const transactionsQuery = useQuery({
    queryKey: ['billing', workspaceId, 'transactions'],
    queryFn: () => billingApi.transactions(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const plansQuery = useQuery({
    queryKey: ['public', 'pricing'],
    queryFn: publicApi.getPricing,
  });

  const initializeMutation = useMutation({
    mutationFn: (body: { planCode: string; billingInterval: string }) =>
      billingApi.initialize(workspaceId, body),
  });

  const retryMutation = useMutation({
    mutationFn: (paymentIntentId: string) => billingApi.retry(paymentIntentId),
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
    initializeMutation.mutate({
      planCode: plan.code,
      billingInterval,
    });
  };

  const transactions = (transactionsQuery.data as Record<string, unknown>[]) ?? [];

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
            }}
            disabled={subscriptionQuery.isFetching}
          >
            <RefreshCw size={15} className={subscriptionQuery.isFetching ? 'spin' : ''} /> Refresh Status
          </Button>
        </div>
      </header>

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
            <div className="plan-name-display">{currentPlanCode} TIER</div>
            {isComplimentary ? (
              <Badge tone="success">
                <Gift size={13} /> {accessType.replaceAll('_', ' ')}
              </Badge>
            ) : (
              <Badge tone="info">{displayValue(subscriptionData?.status, 'ACTIVE')}</Badge>
            )}
          </div>

          <div className="plan-meta-details">
            <div className="plan-meta-row">
              <span className="muted">Billing status:</span>
              <strong>{isComplimentary ? 'No billing required (Granted)' : 'Active subscription'}</strong>
            </div>
            {subscriptionData?.periodEnd ? (
              <div className="plan-meta-row">
                <span className="muted">Current period ends:</span>
                <strong>{String(subscriptionData.periodEnd).slice(0, 10)}</strong>
              </div>
            ) : null}
            <div className="plan-meta-row">
              <span className="muted">Environment:</span>
              <Badge tone="warning" className="badge-sm">Paystack Test Gateway</Badge>
            </div>
          </div>
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

          {/* SEGMENTED BILLING INTERVAL SELECTOR */}
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
              Yearly <span className="discount-pill">Save 20%</span>
            </button>
          </div>
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
              const price = billingInterval === 'YEARLY' ? plan.annualPrice : plan.monthlyPrice;
              const periodSuffix = billingInterval === 'YEARLY' ? '/ year' : '/ month';

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
                    <div className="plan-card-price">
                      <span className="price-currency">{plan.currency}</span>
                      <span className="price-amount">{price}</span>
                      <span className="price-interval muted">{periodSuffix}</span>
                    </div>
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
                    ) : (
                      <Button
                        type="button"
                        variant={plan.highlighted ? 'primary' : 'secondary'}
                        className="w-full"
                        onClick={() => handleChoosePlan(plan)}
                        disabled={initializeMutation.isPending}
                      >
                        {initializeMutation.isPending ? 'Processing...' : `Choose ${plan.name}`}
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

      {/* PAYMENT HISTORY */}
      <Card className="billing-history-card">
        <div className="card-header-iconic">
          <Activity size={24} className="text-brand" />
          <div>
            <h3>Payment History & Mobile Money Verification</h3>
            <p className="muted">Transactions processed through Paystack in TEST mode</p>
          </div>
        </div>

        {transactions.length === 0 ? (
          <p className="muted" style={{ padding: '16px 0' }}>
            No transaction records found for this workspace.
          </p>
        ) : (
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
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx, idx) => (
                  <tr key={String(tx.id ?? idx)}>
                    <td>
                      <code style={{ fontSize: '0.82rem' }}>
                        {String(tx.internalReference ?? tx.reference ?? tx.id).slice(0, 16)}...
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
                          tx.status === 'SUCCESS'
                            ? 'success'
                            : tx.status === 'PENDING'
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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

        <div className="retry-intent-box" style={{ marginTop: '14px' }}>
          <Field label="Retry payment intent ID (if required)">
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <Input
                value={retryIntentId}
                onChange={(e) => setRetryIntentId(e.target.value)}
                placeholder="UUID of unfulfilled payment intent"
                style={{ maxWidth: '380px' }}
              />
              <Button
                type="button"
                variant="secondary"
                disabled={!retryIntentId || retryMutation.isPending}
                onClick={() => retryMutation.mutate(retryIntentId)}
              >
                {retryMutation.isPending ? 'Retrying...' : 'Pay Again'}
              </Button>
            </div>
          </Field>
          {retryMutation.data ? <CheckoutResultBanner data={retryMutation.data} /> : null}
        </div>
      </Card>
    </section>
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
  const used = metric?.used ?? 0;
  const limit = metric?.limit;
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
              <strong>{used.toLocaleString()}</strong> / {limit.toLocaleString()} {unit}
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
  const isTrusted = /^https:\/\/(checkout|standard)\.paystack\.com\//.test(url);

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
