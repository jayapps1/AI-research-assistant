import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Layers,
  Search,
  RefreshCw,
  CheckCircle2,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Input, Pagination } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { displayValue, pageContent } from '../../utils/collections';

interface AdminWorkspaceRecord {
  id: string;
  name: string;
  type?: string;
  status: string;
  ownerId?: string;
  ownerEmail?: string;
  createdAt?: string;
}

export function AdminWorkspacesPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const workspacesQuery = useQuery({
    queryKey: ['admin', 'workspaces', page],
    queryFn: () => adminApi.workspaces(page, 20),
  });

  const rawWorkspaces = (pageContent(workspacesQuery.data) as unknown as AdminWorkspaceRecord[]) || [];
  const filteredWorkspaces = search.trim()
    ? rawWorkspaces.filter((w) => {
        const query = search.toLowerCase();
        return (
          w.name?.toLowerCase().includes(query) ||
          w.ownerEmail?.toLowerCase().includes(query) ||
          w.id?.toLowerCase().includes(query)
        );
      })
    : rawWorkspaces;

  return (
    <section className="page admin-workspaces-page" style={{ padding: '1.5rem' }}>
      <header className="page-header admin-header" style={{ marginBottom: '1.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Layers className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
            <h1 className="page-title" style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>Workspace Management</h1>
          </div>
          <p className="muted" style={{ margin: '0.25rem 0 0' }}>
            Inspect research workspaces across the platform, ownership records, and operational status.
          </p>
        </div>

        <div className="toolbar">
          <Button
            type="button"
            variant="secondary"
            onClick={() => workspacesQuery.refetch()}
            disabled={workspacesQuery.isFetching}
          >
            <RefreshCw size={15} className={workspacesQuery.isFetching ? 'spin' : ''} /> Refresh
          </Button>
        </div>
      </header>

      {workspacesQuery.isError ? <ErrorState error={workspacesQuery.error} /> : null}

      <Card style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: '1', minWidth: '240px' }}>
            <Search size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', opacity: 0.5 }} />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by workspace name, owner email, or ID..."
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
                <th style={{ padding: '0.75rem 1rem' }}>Workspace Name</th>
                <th style={{ padding: '0.75rem 1rem' }}>Type</th>
                <th style={{ padding: '0.75rem 1rem' }}>Owner</th>
                <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                <th style={{ padding: '0.75rem 1rem' }}>Created</th>
              </tr>
            </thead>
            <tbody>
              {filteredWorkspaces.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                    {workspacesQuery.isLoading ? 'Loading workspaces...' : 'No workspaces found matching query.'}
                  </td>
                </tr>
              ) : (
                filteredWorkspaces.map((workspace) => (
                  <tr key={workspace.id} style={{ borderBottom: '1px solid var(--border, #222)' }}>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <strong style={{ display: 'block' }}>{workspace.name}</strong>
                      <span className="muted" style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{workspace.id}</span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <Badge tone="info">{displayValue(workspace.type || 'PERSONAL')}</Badge>
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      {workspace.ownerEmail ? (
                        <span>{workspace.ownerEmail}</span>
                      ) : (
                        <span className="muted" style={{ fontSize: '0.8rem', fontFamily: 'monospace' }}>{workspace.ownerId ?? '—'}</span>
                      )}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <Badge tone={workspace.status === 'ACTIVE' ? 'success' : 'warning'}>
                        <CheckCircle2 size={12} className="mr-1 inline" /> {displayValue(workspace.status)}
                      </Badge>
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.85rem' }} className="muted">
                      {workspace.createdAt ? new Date(workspace.createdAt).toLocaleDateString() : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {workspacesQuery.data?.totalPages && workspacesQuery.data.totalPages > 1 ? (
          <div style={{ padding: '1rem', borderTop: '1px solid var(--border, #222)' }}>
            <Pagination
              page={page}
              totalPages={workspacesQuery.data.totalPages}
              onPageChange={setPage}
            />
          </div>
        ) : null}
      </Card>
    </section>
  );
}
