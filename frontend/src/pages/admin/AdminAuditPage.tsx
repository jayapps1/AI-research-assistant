import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  FileText,
  Search,
  RefreshCw,
  Clock,
  Shield,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Input, Pagination } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { displayValue, pageContent } from '../../utils/collections';

interface AdminAuditEventRecord {
  id: string;
  eventType: string;
  principalType?: string;
  principalId?: string;
  entityType?: string;
  entityId?: string;
  details?: string;
  occurredAt: string;
}

export function AdminAuditPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const auditQuery = useQuery({
    queryKey: ['admin', 'audit-events', page],
    queryFn: () => adminApi.auditEvents(page, 25),
  });

  const rawEvents = (pageContent(auditQuery.data) as unknown as AdminAuditEventRecord[]) || [];
  const filteredEvents = search.trim()
    ? rawEvents.filter((e) => {
        const query = search.toLowerCase();
        return (
          e.eventType?.toLowerCase().includes(query) ||
          e.details?.toLowerCase().includes(query) ||
          e.entityType?.toLowerCase().includes(query)
        );
      })
    : rawEvents;

  return (
    <section className="page admin-audit-page" style={{ padding: '1.5rem' }}>
      <header className="page-header admin-header" style={{ marginBottom: '1.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <FileText className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
            <h1 className="page-title" style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>Security Audit Log</h1>
          </div>
          <p className="muted" style={{ margin: '0.25rem 0 0' }}>
            Immutable chronological record of administrative actions, authentication events, and security state changes.
          </p>
        </div>

        <div className="toolbar">
          <Button
            type="button"
            variant="secondary"
            onClick={() => auditQuery.refetch()}
            disabled={auditQuery.isFetching}
          >
            <RefreshCw size={15} className={auditQuery.isFetching ? 'spin' : ''} /> Refresh
          </Button>
        </div>
      </header>

      {auditQuery.isError ? <ErrorState error={auditQuery.error} /> : null}

      <Card style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: '1', minWidth: '240px' }}>
            <Search size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', opacity: 0.5 }} />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by event type or details..."
              style={{ paddingLeft: '32px' }}
            />
          </div>
        </div>
      </Card>

      <Card style={{ padding: 0, overflow: 'hidden' }}>
        <div className="admin-table-container" style={{ overflowX: 'auto' }}>
          <table className="admin-data-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border, #333)', textAlign: 'left' }}>
                <th style={{ padding: '0.75rem 1rem' }}>Timestamp</th>
                <th style={{ padding: '0.75rem 1rem' }}>Event Type</th>
                <th style={{ padding: '0.75rem 1rem' }}>Principal</th>
                <th style={{ padding: '0.75rem 1rem' }}>Entity</th>
                <th style={{ padding: '0.75rem 1rem' }}>Details</th>
              </tr>
            </thead>
            <tbody>
              {filteredEvents.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                    {auditQuery.isLoading ? 'Loading audit events...' : 'No audit events recorded.'}
                  </td>
                </tr>
              ) : (
                filteredEvents.map((evt) => (
                  <tr key={evt.id} style={{ borderBottom: '1px solid var(--border, #222)' }}>
                    <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap', fontSize: '0.85rem' }} className="muted">
                      <Clock size={12} className="mr-1 inline" />
                      {evt.occurredAt ? new Date(evt.occurredAt).toLocaleString() : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <Badge tone="info"><Shield size={12} className="mr-1 inline" /> {displayValue(evt.eventType)}</Badge>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.82rem', fontFamily: 'monospace' }}>
                      {evt.principalId ? evt.principalId.slice(0, 12) + '...' : (evt.principalType ?? 'SYSTEM')}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.85rem' }}>
                      {evt.entityType ? (
                        <span>
                          <strong>{evt.entityType}</strong>
                          {evt.entityId ? <code style={{ marginLeft: '4px', fontSize: '0.75rem' }}>{evt.entityId.slice(0, 8)}</code> : null}
                        </span>
                      ) : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.85rem', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={evt.details}>
                      {evt.details ?? '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {auditQuery.data?.totalPages && auditQuery.data.totalPages > 1 ? (
          <div style={{ padding: '1rem', borderTop: '1px solid var(--border, #222)' }}>
            <Pagination
              page={page}
              totalPages={auditQuery.data.totalPages}
              onPageChange={setPage}
            />
          </div>
        ) : null}
      </Card>
    </section>
  );
}
