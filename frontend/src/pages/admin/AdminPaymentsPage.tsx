import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Receipt,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Clock,
  ChevronDown,
  ChevronRight,
  ExternalLink,
  ShieldAlert,
  Building,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Pagination } from '../../components/ui';
import { ErrorState } from '../../components/states';
import type { AdminPaymentItem, PageResponse } from '../../types/api';

export function AdminPaymentsPage() {
  const [page, setPage] = useState(0);
  const [expandedIntents, setExpandedIntents] = useState<Record<string, boolean>>({});

  const { data, isLoading, error, refetch } = useQuery<PageResponse<AdminPaymentItem>>({
    queryKey: ['admin', 'payments', page],
    queryFn: () => adminApi.payments(page, 15),
  });

  const toggleExpand = (id: string) => {
    setExpandedIntents((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCEEDED':
      case 'SUCCESS':
        return <Badge tone="success"><CheckCircle2 className="w-3 h-3 mr-1 inline" />Success</Badge>;
      case 'PROCESSING':
      case 'PENDING':
        return <Badge tone="warning"><Clock className="w-3 h-3 mr-1 inline" />Pending</Badge>;
      case 'FAILED':
        return <Badge tone="danger"><XCircle className="w-3 h-3 mr-1 inline" />Failed</Badge>;
      case 'CANCELLED':
        return <Badge tone="info">Cancelled</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const items = data?.content || [];
  const totalPages = data?.totalPages || 1;

  return (
    <div className="space-y-6" style={{ padding: '1.5rem' }}>
      {/* Top Banner: PAYSTACK TEST MODE */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.75rem 1.25rem',
          backgroundColor: '#3b2505',
          border: '1px solid #78350f',
          borderRadius: '8px',
          color: '#fef3c7',
          marginBottom: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <ShieldAlert className="w-5 h-5 text-amber-400" style={{ width: '20px', height: '20px', color: '#fbbf24' }} />
          <div>
            <span style={{ fontWeight: 700, letterSpacing: '0.05em' }}>PAYSTACK TEST ENVIRONMENT</span>
            <p style={{ margin: 0, fontSize: '0.8rem', opacity: 0.9 }}>
              All transactions displayed below are evaluated via Paystack Sandbox / Test API. No real currency charges are incurred.
            </p>
          </div>
        </div>
        <Badge tone="warning" style={{ fontWeight: 700, padding: '4px 8px' }}>
          TEST MODE
        </Badge>
      </div>

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Receipt className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, margin: 0 }}>Payment Transactions</h1>
          </div>
          <p className="text-sm muted" style={{ margin: '0.25rem 0 0' }}>
            Audit payment intents, customer attempts, Paystack verification timestamps, and failure diagnostics.
          </p>
        </div>
        <Button variant="secondary" onClick={() => refetch()}>
          <RefreshCw className="w-4 h-4 mr-1" style={{ width: '16px', height: '16px', verticalAlign: 'middle' }} />
          Refresh
        </Button>
      </div>

      {/* Loading & Error States */}
      {isLoading && <p className="muted">Loading payment transactions...</p>}
      {error && <ErrorState error={error} onRetry={() => { refetch(); }} />}

      {/* Transactions Table */}
      {!isLoading && !error && (
        <Card>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border, #333)', opacity: 0.8, fontSize: '0.85rem' }}>
                  <th style={{ padding: '0.75rem 1rem', width: '40px' }}></th>
                  <th style={{ padding: '0.75rem 1rem' }}>Customer / Workspace</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Plan & Interval</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Amount</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Env</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Intent Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Attempts</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Latest Attempt</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Created</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Verified</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={10} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                      No payment intents found in the database.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => {
                    const isExpanded = !!expandedIntents[item.id];
                    return (
                      <>
                        <tr
                          key={item.id}
                          style={{
                            borderBottom: '1px solid var(--border, #222)',
                            cursor: 'pointer',
                            backgroundColor: isExpanded ? 'var(--card-subtle, #181818)' : 'transparent',
                          }}
                          onClick={() => toggleExpand(item.id)}
                        >
                          <td style={{ padding: '0.75rem 0.5rem', textAlign: 'center' }}>
                            {isExpanded ? (
                              <ChevronDown className="w-4 h-4 text-muted" style={{ width: '16px', height: '16px' }} />
                            ) : (
                              <ChevronRight className="w-4 h-4 text-muted" style={{ width: '16px', height: '16px' }} />
                            )}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div style={{ fontWeight: 600 }}>{item.userName || item.userEmail}</div>
                            <div className="text-xs muted" style={{ fontSize: '0.75rem' }}>
                              <Building className="w-3 h-3 inline mr-1" style={{ width: '12px', height: '12px' }} />
                              {item.workspaceName}
                            </div>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <div>
                              <code style={{ background: 'var(--card-subtle, #1a1a1a)', padding: '2px 6px', borderRadius: '4px', fontWeight: 600 }}>
                                {item.planCode}
                              </code>
                            </div>
                            <span className="text-xs muted" style={{ fontSize: '0.75rem' }}>
                              {item.planName} ({item.billingInterval})
                            </span>
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                            {item.currency} {Number(item.amount).toFixed(2)}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <Badge tone="warning" style={{ fontSize: '0.75rem' }}>{item.environment}</Badge>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            {getStatusBadge(item.intentStatus)}
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            <span style={{ fontWeight: 600 }}>{item.attemptCount}</span>
                          </td>
                          <td style={{ padding: '0.75rem 1rem' }}>
                            {getStatusBadge(item.latestAttemptStatus)}
                            {item.latestFailureCode && (
                              <div className="text-xs text-danger" style={{ color: '#ef4444', fontSize: '0.7rem' }}>
                                {item.latestFailureCode}
                              </div>
                            )}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontSize: '0.8rem' }} className="muted">
                            {new Date(item.createdAt).toLocaleString()}
                          </td>
                          <td style={{ padding: '0.75rem 1rem', fontSize: '0.8rem' }} className="muted">
                            {item.latestVerifiedAt ? new Date(item.latestVerifiedAt).toLocaleString() : '—'}
                          </td>
                        </tr>

                        {/* Expanded Attempts Detail Row */}
                        {isExpanded && (
                          <tr key={`${item.id}-detail`} style={{ backgroundColor: 'var(--card-subtle, #121212)' }}>
                            <td colSpan={10} style={{ padding: '1rem 2rem', borderBottom: '1px solid var(--border, #333)' }}>
                              <div style={{ marginBottom: '0.5rem', fontWeight: 600, fontSize: '0.85rem' }}>
                                Payment Attempts History for Intent #{item.id.substring(0, 8)}:
                              </div>
                              {item.attempts && item.attempts.length > 0 ? (
                                <table style={{ width: '100%', fontSize: '0.8rem', borderCollapse: 'collapse' }}>
                                  <thead>
                                    <tr style={{ opacity: 0.6, borderBottom: '1px solid #333' }}>
                                      <th style={{ padding: '4px 8px' }}>#</th>
                                      <th style={{ padding: '4px 8px' }}>Internal Ref</th>
                                      <th style={{ padding: '4px 8px' }}>Provider Ref</th>
                                      <th style={{ padding: '4px 8px' }}>Status</th>
                                      <th style={{ padding: '4px 8px' }}>Provider Status</th>
                                      <th style={{ padding: '4px 8px' }}>Failure Diagnostic</th>
                                      <th style={{ padding: '4px 8px' }}>Created</th>
                                      <th style={{ padding: '4px 8px' }}>Verified</th>
                                      <th style={{ padding: '4px 8px' }}>Checkout Link</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {item.attempts.map((att) => (
                                      <tr key={att.id} style={{ borderBottom: '1px solid #222' }}>
                                        <td style={{ padding: '6px 8px', fontWeight: 600 }}>#{att.attemptNumber}</td>
                                        <td style={{ padding: '6px 8px' }}>
                                          <code>{att.internalReference}</code>
                                        </td>
                                        <td style={{ padding: '6px 8px' }}>
                                          {att.providerReference ? <code>{att.providerReference}</code> : '—'}
                                        </td>
                                        <td style={{ padding: '6px 8px' }}>{getStatusBadge(att.status)}</td>
                                        <td style={{ padding: '6px 8px' }}>{att.providerStatus || '—'}</td>
                                        <td style={{ padding: '6px 8px', color: att.failureCode ? '#f87171' : 'inherit' }}>
                                          {att.failureCode ? `${att.failureCode}: ${att.failureMessageSafe || ''}` : 'None'}
                                        </td>
                                        <td style={{ padding: '6px 8px' }}>{new Date(att.createdAt).toLocaleTimeString()}</td>
                                        <td style={{ padding: '6px 8px' }}>
                                          {att.providerVerifiedAt ? new Date(att.providerVerifiedAt).toLocaleTimeString() : '—'}
                                        </td>
                                        <td style={{ padding: '6px 8px' }}>
                                          {att.authorizationUrl ? (
                                            <a
                                              href={att.authorizationUrl}
                                              target="_blank"
                                              rel="noopener noreferrer"
                                              style={{ display: 'flex', alignItems: 'center', gap: '2px', color: '#60a5fa' }}
                                            >
                                              Open <ExternalLink className="w-3 h-3" style={{ width: '12px', height: '12px' }} />
                                            </a>
                                          ) : (
                                            '—'
                                          )}
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              ) : (
                                <p className="muted" style={{ margin: 0, fontSize: '0.8rem' }}>No individual attempts recorded yet.</p>
                              )}
                            </td>
                          </tr>
                        )}
                      </>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{ padding: '1rem', borderTop: '1px solid var(--border, #333)', display: 'flex', justifyContent: 'center' }}>
              <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
