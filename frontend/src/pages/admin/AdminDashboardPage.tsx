import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Activity,
  Bot,
  CheckCircle2,
  CreditCard,
  Database,
  FileText,
  FolderKanban,
  Gift,
  Layers,
  RefreshCw,
  Server,
  Sparkles,
  Users,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Field, Input, Select, Textarea } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { displayValue, pageContent } from '../../utils/collections';

export function AdminDashboardPage() {
  const queryClient = useQueryClient();
  const [showGrantModal, setShowGrantModal] = useState(false);
  const [grantTargetId, setGrantTargetId] = useState('');
  const [grantTargetType, setGrantTargetType] = useState<'workspace' | 'user'>('workspace');
  const [grantPlan, setGrantPlan] = useState('PRO');
  const [grantType, setGrantType] = useState('DEVELOPER_ACCESS');
  const [grantReason, setGrantReason] = useState('');
  const [grantDays, setGrantDays] = useState('90');
  const [grantError, setGrantError] = useState<string | null>(null);
  const [grantSuccess, setGrantSuccess] = useState<string | null>(null);

  const dashboardQuery = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: adminApi.dashboard,
  });

  const operationsQuery = useQuery({
    queryKey: ['admin', 'operations'],
    queryFn: adminApi.operations,
  });

  const complimentaryQuery = useQuery({
    queryKey: ['admin', 'complimentary-access'],
    queryFn: adminApi.complimentaryAccess,
  });

  const grantMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => adminApi.grantComplimentaryAccess(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'complimentary-access'] });
      setShowGrantModal(false);
      setGrantTargetId('');
      setGrantReason('');
      setGrantSuccess('Complimentary access successfully granted.');
      setGrantError(null);
    },
    onError: (err: unknown) => {
      setGrantError(err instanceof Error ? err.message : 'Failed to grant complimentary access.');
    },
  });

  const handleGrantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setGrantError(null);
    if (!grantTargetId.trim()) {
      setGrantError('Please provide a valid Workspace or User UUID.');
      return;
    }

    const payload: Record<string, unknown> = {
      scope: grantTargetType === 'workspace' ? 'WORKSPACE' : 'USER',
      planCode: grantPlan,
      type: grantType,
      reason: grantReason || 'System Administrator grant',
      startsAt: new Date().toISOString(),
    };

    if (grantTargetType === 'workspace') {
      payload.workspaceId = grantTargetId.trim();
    } else {
      payload.userId = grantTargetId.trim();
    }

    const days = parseInt(grantDays, 10);
    if (!isNaN(days) && days > 0) {
      const expiry = new Date();
      expiry.setDate(expiry.getDate() + days);
      payload.expiresAt = expiry.toISOString();
    }

    grantMutation.mutate(payload);
  };

  const data = dashboardQuery.data ?? {};
  const healthData = (data.systemHealth as Record<string, string>) ?? {};

  const totalUsers = Number(data.totalUsers ?? 0);
  const activeUsers = Number(data.activeUsers ?? totalUsers);
  const workspaces = Number(data.workspaces ?? 0);
  const projects = Number(data.researchProjects ?? 0);
  const activeSubscriptions = Number(data.activeSubscriptions ?? 0);
  const aiRequestsToday = Number(data.aiRequestsToday ?? 0);
  const aiRequestsThisMonth = Number(data.aiRequestsThisMonth ?? aiRequestsToday);
  const aiTokensTotal = Number(data.aiTokensTotal ?? 0);
  const aiFailures = Number(data.aiFailures ?? 0);

  const testPayments = Number(data.testPayments ?? 0);
  const successfulPayments = Number(data.successfulPayments ?? 0);
  const pendingPayments = Number(data.pendingPayments ?? 0);
  const failedPayments = Number(data.failedPayments ?? 0);
  const documentsCount = Number(data.documentsCount ?? 0);
  const datasetsCount = Number(data.datasetsCount ?? 0);

  const totalStorageBytes = Number(data.totalStorageBytes ?? 0);
  const formattedStorage = totalStorageBytes > 1024 * 1024 * 1024
    ? `${(totalStorageBytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
    : totalStorageBytes > 1024 * 1024
    ? `${(totalStorageBytes / (1024 * 1024)).toFixed(2)} MB`
    : `${(totalStorageBytes / 1024).toFixed(1)} KB`;

  const aiBudget = (data.aiBudget as Record<string, any>) ?? {};
  const aiTotalSpendUsd = Number(data.aiTotalSpendUsd ?? aiBudget.currentSpendUsd ?? 0);
  const budgetLimitUsd = Number(aiBudget.limitUsd ?? 4.00);
  const remainingBudgetUsd = Number(aiBudget.remainingUsd ?? Math.max(0, budgetLimitUsd - aiTotalSpendUsd));
  const spendPercentage = Number(aiBudget.spendPercentage ?? (budgetLimitUsd > 0 ? (aiTotalSpendUsd / budgetLimitUsd) * 100 : 0));
  const budgetStatus = aiBudget.thresholdStatus ?? 'NORMAL';


  const grants = pageContent(complimentaryQuery.data) as Record<string, unknown>[];

  return (
    <section className="page admin-dashboard-page">
      <header className="page-header admin-header">
        <div>
          <div className="admin-title-row">
            <h1 className="page-title">System Administration</h1>
            <Badge tone="warning">PAYSTACK TEST MODE</Badge>
          </div>
          <p className="muted">
            Live platform metrics, service health, resource quotas, and administrative controls.
          </p>
        </div>

        <div className="toolbar">
          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              dashboardQuery.refetch();
              operationsQuery.refetch();
            }}
            disabled={dashboardQuery.isFetching}
          >
            <RefreshCw size={15} className={dashboardQuery.isFetching ? 'spin' : ''} /> Refresh Metrics
          </Button>
          <Button type="button" variant="primary" onClick={() => setShowGrantModal(true)}>
            <Gift size={16} /> Grant Complimentary Access
          </Button>
        </div>
      </header>

      {grantSuccess ? (
        <div className="alert success" role="status" style={{ marginBottom: '20px' }}>
          <CheckCircle2 size={18} />
          <span>{grantSuccess}</span>
        </div>
      ) : null}

      {dashboardQuery.isError ? (
        <ErrorState error={dashboardQuery.error} />
      ) : null}

      {/* ROW 1: PRIMARY PLATFORM AGGREGATES */}
      <div className="admin-metrics-grid">
        <Card className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Total Users</span>
            <Users size={18} className="text-brand" />
          </div>
          <div className="metric-value">{totalUsers.toLocaleString()}</div>
          <span className="metric-subtext">{activeUsers.toLocaleString()} active accounts</span>
        </Card>

        <Card className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Workspaces</span>
            <Layers size={18} className="text-brand" />
          </div>
          <div className="metric-value">{workspaces.toLocaleString()}</div>
          <span className="metric-subtext">Collaborative research teams</span>
        </Card>

        <Card className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Research Projects</span>
            <FolderKanban size={18} className="text-brand" />
          </div>
          <div className="metric-value">{projects.toLocaleString()}</div>
          <span className="metric-subtext">Empirical studies in progress</span>
        </Card>

        <Card className="metric-card">
          <div className="metric-header">
            <span className="metric-label">Active Subscriptions</span>
            <CreditCard size={18} className="text-brand" />
          </div>
          <div className="metric-value">{activeSubscriptions.toLocaleString()}</div>
          <span className="metric-subtext">Verified subscription tiers</span>
        </Card>

        <Card className="metric-card">
          <div className="metric-header">
            <span className="metric-label">AI Requests (This Month)</span>
            <Sparkles size={18} className="text-brand" />
          </div>
          <div className="metric-value">{aiRequestsThisMonth.toLocaleString()}</div>
          <span className="metric-subtext">{aiRequestsToday.toLocaleString()} requests today</span>
        </Card>
      </div>

      {/* ROW 2: OPERATIONS PANELS (PAYMENTS + AI USAGE + STORAGE) */}
      <div className="admin-sections-grid">
        {/* PAYMENTS & TRANSACTIONS */}
        <Card className="admin-feature-card">
          <div className="card-header-iconic">
            <CreditCard size={22} className="text-brand" />
            <div>
              <h3>Payment Status & Transactions</h3>
              <p className="muted">Simulated Paystack gateway transactions in TEST mode</p>
            </div>
          </div>

          <div className="admin-status-pill-row">
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Total Recorded</span>
              <span className="mini-stat-value">{testPayments}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Successful</span>
              <span className="mini-stat-value text-success">{successfulPayments}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Pending</span>
              <span className="mini-stat-value text-warning">{pendingPayments}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Failed</span>
              <span className="mini-stat-value text-danger">{failedPayments}</span>
            </div>
          </div>

          <div className="alert info" style={{ marginTop: '16px' }}>
            <Activity size={16} />
            <span>Paystack integration is currently in <strong>TEST MODE</strong>. Real payment charges are disabled.</span>
          </div>
        </Card>

        {/* AI ENGINE METRICS */}
        <Card className="admin-feature-card">
          <div className="card-header-iconic">
            <Bot size={22} className="text-brand" />
            <div>
              <h3>AI Engine & Orchestration</h3>
              <p className="muted">Token throughput, model requests, and system error rates</p>
            </div>
          </div>

          <div className="admin-status-pill-row">
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Total Requests</span>
              <span className="mini-stat-value">{aiRequestsThisMonth}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Tokens Processed</span>
              <span className="mini-stat-value">{aiTokensTotal.toLocaleString()}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Failures / Timeouts</span>
              <span className="mini-stat-value text-danger">{aiFailures}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Success Rate</span>
              <span className="mini-stat-value text-success">
                {aiRequestsThisMonth > 0
                  ? `${Math.round(((aiRequestsThisMonth - aiFailures) / aiRequestsThisMonth) * 100)}%`
                  : '100%'}
              </span>
            </div>
          </div>

          <div className="resource-counts-row" style={{ marginTop: '16px' }}>
            <div className="resource-badge">
              <FileText size={16} />
              <span>Documents: <strong>{documentsCount}</strong></span>
            </div>
            <div className="resource-badge">
              <FileText size={16} />
              <span>Storage Used: <strong>{formattedStorage}</strong></span>
            </div>
            <div className="resource-badge">
              <Database size={16} />
              <span>Datasets: <strong>{datasetsCount}</strong></span>
            </div>
          </div>
        </Card>

        {/* AI COST & DEV BUDGET */}
        <Card className="admin-feature-card">
          <div className="card-header-iconic">
            <Sparkles size={22} className="text-brand" />
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <h3 style={{ margin: 0 }}>AI Cost & Development Budget</h3>
                <Badge tone={budgetStatus === 'EXCEEDED' ? 'danger' : budgetStatus.startsWith('WARNING') ? 'warning' : 'success'}>
                  {budgetStatus}
                </Badge>
              </div>
              <p className="muted">Application-side spending guard to protect prepaid development funds ($4.00 ceiling)</p>
            </div>
          </div>

          <div className="admin-status-pill-row">
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Budget Limit</span>
              <span className="mini-stat-value">${budgetLimitUsd.toFixed(2)}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Total Spent</span>
              <span className="mini-stat-value" style={{ color: budgetStatus === 'EXCEEDED' ? 'var(--danger-text)' : 'inherit' }}>
                ${aiTotalSpendUsd.toFixed(4)}
              </span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Remaining</span>
              <span className="mini-stat-value text-success">${remainingBudgetUsd.toFixed(4)}</span>
            </div>
            <div className="admin-mini-stat">
              <span className="mini-stat-label">Budget Used</span>
              <span className="mini-stat-value">{spendPercentage.toFixed(1)}%</span>
            </div>
          </div>

          <div className="alert info" style={{ marginTop: '16px', fontSize: '0.84rem' }}>
            <Activity size={15} style={{ flexShrink: 0 }} />
            <div>
              <strong>Credit Valuation Rule:</strong> 1 Platform AI Credit = $0.001 USD of calculated provider usage.
              <br />
              <span className="muted" style={{ fontSize: '0.8rem' }}>
                gpt-5.6-luna: $0.20/1M input (200 cr/M), $0.02/1M cached (20 cr/M), $1.20/1M output (1,200 cr/M)
              </span>
            </div>
          </div>
        </Card>
      </div>


      {/* ROW 3: SYSTEM HEALTH STATUS & RECENT COMPLIMENTARY GRANTS */}
      <div className="admin-sections-grid">
        {/* SYSTEM HEALTH CARDS */}
        <Card className="admin-feature-card">
          <div className="card-header-iconic">
            <Server size={22} className="text-brand" />
            <div>
              <h3>System Health & Infrastructure</h3>
              <p className="muted">Status reports from underlying backend sub-services</p>
            </div>
          </div>

          <div className="health-items-grid">
            <div className="health-item">
              <span className="health-service">PostgreSQL Database</span>
              <Badge tone="success"><CheckCircle2 size={13} /> {healthData.database ?? 'Healthy'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">AI Generation Engine</span>
              <Badge tone="success"><CheckCircle2 size={13} /> {healthData.aiGeneration ?? 'Healthy'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">Vector & Embeddings</span>
              <Badge tone="success"><CheckCircle2 size={13} /> {healthData.embeddings ?? 'Healthy'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">Paystack Gateway</span>
              <Badge tone="warning"><Activity size={13} /> {healthData.paystack ?? 'TEST'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">Document Storage</span>
              <Badge tone="success"><CheckCircle2 size={13} /> {healthData.storage ?? 'Healthy'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">Email Dispatch Service</span>
              <Badge tone="success"><CheckCircle2 size={13} /> {healthData.email ?? 'Healthy'}</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">SMS (Arkesel)</span>
              <Badge tone="info">Disabled by default</Badge>
            </div>
            <div className="health-item">
              <span className="health-service">Push (Firebase)</span>
              <Badge tone="info">Disabled by default</Badge>
            </div>
          </div>
        </Card>

        {/* RECENT COMPLIMENTARY ACCESS GRANTS */}
        <Card className="admin-feature-card">
          <div className="card-header-iconic">
            <Gift size={22} className="text-brand" />
            <div>
              <h3>Complimentary Access Grants</h3>
              <p className="muted">Active administrative and developer tier grants</p>
            </div>
          </div>

          {grants.length === 0 ? (
            <p className="muted" style={{ padding: '20px 0' }}>No complimentary access grants on record.</p>
          ) : (
            <div className="admin-table-container">
              <table className="admin-data-table">
                <thead>
                  <tr>
                    <th>Target</th>
                    <th>Plan</th>
                    <th>Type</th>
                    <th>Status</th>
                    <th>Expires</th>
                  </tr>
                </thead>
                <tbody>
                  {grants.slice(0, 5).map((grant, idx) => (
                    <tr key={String(grant.id ?? idx)}>
                      <td>
                        <span style={{ fontSize: '0.82rem', fontFamily: 'monospace' }}>
                          {String(grant.workspaceId || grant.userId || grant.id).slice(0, 12)}...
                        </span>
                      </td>
                      <td><strong>{displayValue(grant.planCode)}</strong></td>
                      <td><Badge tone="info">{displayValue(grant.type)}</Badge></td>
                      <td><Badge tone="success">{displayValue(grant.status)}</Badge></td>
                      <td className="muted" style={{ fontSize: '0.85rem' }}>
                        {grant.expiresAt ? String(grant.expiresAt).slice(0, 10) : 'Never'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

      {/* GRANT COMPLIMENTARY ACCESS MODAL */}
      {showGrantModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="grant-modal-title">
          <div className="modal-dialog">
            <h3 id="grant-modal-title">Grant Complimentary Access</h3>
            <p className="muted">
              Grant elevated research entitlements to a Workspace or specific User without requiring payment.
            </p>

            {grantError ? <div className="alert danger">{grantError}</div> : null}

            <form className="form" onSubmit={handleGrantSubmit}>
              <Field label="Grant Scope">
                <Select
                  value={grantTargetType}
                  onChange={(e) => setGrantTargetType(e.target.value as 'workspace' | 'user')}
                >
                  <option value="workspace">Workspace UUID</option>
                  <option value="user">User UUID</option>
                </Select>
              </Field>

              <Field label={grantTargetType === 'workspace' ? 'Workspace ID' : 'User ID'}>
                <Input
                  value={grantTargetId}
                  onChange={(e) => setGrantTargetId(e.target.value)}
                  placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                  required
                />
              </Field>

              <Field label="Subscription Plan">
                <Select value={grantPlan} onChange={(e) => setGrantPlan(e.target.value)}>
                  <option value="STUDENT">Student Tier</option>
                  <option value="PRO">Pro Tier</option>
                  <option value="INSTITUTIONAL">Institutional Tier</option>
                </Select>
              </Field>

              <Field label="Grant Type">
                <Select value={grantType} onChange={(e) => setGrantType(e.target.value)}>
                  <option value="DEVELOPER_ACCESS">Developer Access (Explicit)</option>
                  <option value="COMPLIMENTARY">Complimentary Scholarly Access</option>
                  <option value="PROMOTIONAL">Promotional Grant</option>
                </Select>
              </Field>

              <Field label="Duration (Days, 0 for indefinite)">
                <Input
                  type="number"
                  min="0"
                  max="3650"
                  value={grantDays}
                  onChange={(e) => setGrantDays(e.target.value)}
                />
              </Field>

              <Field label="Justification / Reason">
                <Textarea
                  value={grantReason}
                  onChange={(e) => setGrantReason(e.target.value)}
                  placeholder="e.g. Core development and empirical workflow validation."
                  style={{ minHeight: '80px' }}
                />
              </Field>

              <div className="toolbar" style={{ justifyContent: 'flex-end', marginTop: '16px' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowGrantModal(false)}
                  disabled={grantMutation.isPending}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={grantMutation.isPending}>
                  {grantMutation.isPending ? 'Granting...' : 'Confirm Grant'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  );
}
