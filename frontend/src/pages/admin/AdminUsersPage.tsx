import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Users,
  Search,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Shield,
  Lock,
  Smartphone,
  KeyRound,
  UserCheck,
  UserX,
} from 'lucide-react';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Input, Pagination } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { displayValue, pageContent } from '../../utils/collections';

interface AdminUserRecord {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  status: 'ACTIVE' | 'SUSPENDED' | string;
  authenticationMethod?: 'PASSWORD' | 'TOTP' | 'PASSWORD_AND_TOTP' | 'PASSWORD_OR_TOTP' | string;
  emailVerified?: boolean;
  createdAt?: string;
}

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ['admin', 'users', page],
    queryFn: () => adminApi.users(page, 20),
  });

  const suspendMutation = useMutation({
    mutationFn: (userId: string) => adminApi.suspendUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      setActionSuccess('User has been suspended successfully.');
      setActionError(null);
    },
    onError: (err: unknown) => {
      setActionError(err instanceof Error ? err.message : 'Failed to suspend user.');
    },
  });

  const reactivateMutation = useMutation({
    mutationFn: (userId: string) => adminApi.reactivateUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      setActionSuccess('User has been reactivated successfully.');
      setActionError(null);
    },
    onError: (err: unknown) => {
      setActionError(err instanceof Error ? err.message : 'Failed to reactivate user.');
    },
  });

  const rawUsers = (pageContent(usersQuery.data) as unknown as AdminUserRecord[]) || [];
  const filteredUsers = search.trim()
    ? rawUsers.filter((u) => {
        const query = search.toLowerCase();
        return (
          u.email?.toLowerCase().includes(query) ||
          u.firstName?.toLowerCase().includes(query) ||
          u.lastName?.toLowerCase().includes(query)
        );
      })
    : rawUsers;

  const getAuthMethodBadge = (method?: string) => {
    switch (method) {
      case 'PASSWORD_OR_TOTP':
        return <Badge tone="info"><KeyRound size={12} className="mr-1 inline" /> Password or TOTP</Badge>;
      case 'PASSWORD_AND_TOTP':
        return <Badge tone="warning"><Shield size={12} className="mr-1 inline" /> Password + TOTP (MFA)</Badge>;
      case 'TOTP':
        return <Badge tone="info"><Smartphone size={12} className="mr-1 inline" /> Authenticator Only</Badge>;
      case 'PASSWORD':
      default:
        return <Badge><Lock size={12} className="mr-1 inline" /> Password Only</Badge>;
    }
  };

  return (
    <section className="page admin-users-page" style={{ padding: '1.5rem' }}>
      <header className="page-header admin-header" style={{ marginBottom: '1.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Users className="w-6 h-6 text-primary" style={{ width: '24px', height: '24px' }} />
            <h1 className="page-title" style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>User Management</h1>
          </div>
          <p className="muted" style={{ margin: '0.25rem 0 0' }}>
            Inspect system accounts, authentication methods, verification status, and manage account access.
          </p>
        </div>

        <div className="toolbar" style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <Button
            type="button"
            variant="secondary"
            onClick={() => usersQuery.refetch()}
            disabled={usersQuery.isFetching}
          >
            <RefreshCw size={15} className={usersQuery.isFetching ? 'spin' : ''} /> Refresh
          </Button>
        </div>
      </header>

      {actionSuccess ? (
        <div className="alert success" role="status" style={{ marginBottom: '1rem' }}>
          <CheckCircle2 size={18} />
          <span>{actionSuccess}</span>
        </div>
      ) : null}

      {actionError ? (
        <div className="alert danger" role="alert" style={{ marginBottom: '1rem' }}>
          <XCircle size={18} />
          <span>{actionError}</span>
        </div>
      ) : null}

      {usersQuery.isError ? <ErrorState error={usersQuery.error} /> : null}

      <Card style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', flex: '1', minWidth: '240px' }}>
            <Search size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', opacity: 0.5 }} />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by email or name..."
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
                <th style={{ padding: '0.75rem 1rem' }}>User / Email</th>
                <th style={{ padding: '0.75rem 1rem' }}>Name</th>
                <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                <th style={{ padding: '0.75rem 1rem' }}>Authentication Method</th>
                <th style={{ padding: '0.75rem 1rem' }}>Verified</th>
                <th style={{ padding: '0.75rem 1rem' }}>Created</th>
                <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: '2rem', textAlign: 'center' }} className="muted">
                    {usersQuery.isLoading ? 'Loading users...' : 'No users found matching query.'}
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id} style={{ borderBottom: '1px solid var(--border, #222)' }}>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <strong style={{ display: 'block' }}>{user.email}</strong>
                      <span className="muted" style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{user.id}</span>
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      {user.firstName || user.lastName ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <Badge tone={user.status === 'ACTIVE' ? 'success' : 'danger'}>
                        {user.status === 'ACTIVE' ? <CheckCircle2 size={12} className="mr-1 inline" /> : <XCircle size={12} className="mr-1 inline" />}
                        {displayValue(user.status)}
                      </Badge>
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      {getAuthMethodBadge(user.authenticationMethod)}
                    </td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      {user.emailVerified ? (
                        <Badge tone="success"><CheckCircle2 size={12} className="mr-1 inline" /> Verified</Badge>
                      ) : (
                        <Badge tone="warning">Pending</Badge>
                      )}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', fontSize: '0.85rem' }} className="muted">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                      {user.status === 'ACTIVE' ? (
                        <Button
                          variant="secondary"
                          className="btn-compact"
                          onClick={() => {
                            if (window.confirm(`Suspend access for ${user.email}?`)) {
                              suspendMutation.mutate(user.id);
                            }
                          }}
                          disabled={suspendMutation.isPending}
                        >
                          <UserX size={14} className="mr-1 inline" /> Suspend
                        </Button>
                      ) : (
                        <Button
                          variant="primary"
                          className="btn-compact"
                          onClick={() => {
                            if (window.confirm(`Reactivate access for ${user.email}?`)) {
                              reactivateMutation.mutate(user.id);
                            }
                          }}
                          disabled={reactivateMutation.isPending}
                        >
                          <UserCheck size={14} className="mr-1 inline" /> Reactivate
                        </Button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {usersQuery.data?.totalPages && usersQuery.data.totalPages > 1 ? (
          <div style={{ padding: '1rem', borderTop: '1px solid var(--border, #222)' }}>
            <Pagination
              page={page}
              totalPages={usersQuery.data.totalPages}
              onPageChange={setPage}
            />
          </div>
        ) : null}
      </Card>
    </section>
  );
}
