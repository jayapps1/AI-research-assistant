import { useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/endpoints';
import { Badge, Button, Card, Pagination } from '../../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../../components/states';
import { displayValue, pageContent } from '../../utils/collections';

const routeConfig: Record<string, { title: string; type: 'page' | 'list' | 'object'; query: (page: number) => Promise<unknown> }> = {
  '/admin/projects': { title: 'Projects', type: 'page', query: (page) => adminApi.projects(page, 20) },
  '/admin/documents': { title: 'Documents', type: 'page', query: (page) => adminApi.documents(page, 20) },
  '/admin/jobs': { title: 'Processing Jobs', type: 'page', query: (page) => adminApi.processingJobs(page, 20) },
  '/admin/processing-jobs': { title: 'Processing Jobs', type: 'page', query: (page) => adminApi.processingJobs(page, 20) },
  '/admin/research-templates': { title: 'Research Templates', type: 'list', query: () => adminApi.researchTemplates() },
  '/admin/report-templates': { title: 'Report Templates', type: 'list', query: () => adminApi.reportTemplates() },
  '/admin/ai-operations': { title: 'AI Operations', type: 'page', query: (page) => adminApi.aiOperations(page, 20) },
  '/admin/ai-usage': { title: 'AI Usage / Cost', type: 'object', query: () => adminApi.aiUsage() },
  '/admin/storage': { title: 'Storage', type: 'object', query: () => adminApi.storage() },
  '/admin/references': { title: 'Citation Styles', type: 'list', query: () => adminApi.referenceStyles() },
  '/admin/health': { title: 'System Health', type: 'object', query: () => adminApi.systemHealth() },
  '/admin/settings': { title: 'Settings', type: 'object', query: () => adminApi.settings() },
  '/admin/notifications': { title: 'Notifications', type: 'object', query: () => adminApi.operations() },
};

export function AdminRecordsPage() {
  const location = useLocation();
  const [page, setPage] = useState(0);
  const config = routeConfig[location.pathname] ?? routeConfig['/admin/projects'];
  const recordsQuery = useQuery({
    queryKey: ['admin-records', location.pathname, page],
    queryFn: () => config.query(page),
  });

  const rows = useMemo(() => {
    if (!recordsQuery.data) return [];
    if (config.type === 'page') return pageContent(recordsQuery.data as any) as Record<string, unknown>[];
    if (Array.isArray(recordsQuery.data)) return recordsQuery.data as Record<string, unknown>[];
    return [recordsQuery.data as Record<string, unknown>];
  }, [recordsQuery.data, config.type]);

  if (recordsQuery.isLoading) return <PageLoading label={`Loading ${config.title.toLowerCase()}...`} />;
  if (recordsQuery.isError) return <ErrorState title={`${config.title} failed to load`} error={recordsQuery.error} onRetry={() => recordsQuery.refetch()} />;

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">{config.title}</h1>
          <p className="muted">Real backend records. Sensitive credentials, token hashes, API keys, and raw prompts are not exposed.</p>
        </div>
        <Button type="button" variant="secondary" onClick={() => recordsQuery.refetch()}>Refresh</Button>
      </div>
      {!rows.length ? (
        <EmptyState title="No backend records returned" />
      ) : (
        <Card>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>{headers(rows).map((header) => <th key={header}>{header}</th>)}</tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={String(row.id ?? index)}>
                    {headers(rows).map((header) => <td key={header}>{renderCell(row[header])}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {config.type === 'page' ? (
            <Pagination page={(recordsQuery.data as any)?.page ?? page} totalPages={(recordsQuery.data as any)?.totalPages ?? 1} onPageChange={setPage} />
          ) : null}
        </Card>
      )}
    </section>
  );
}

function headers(rows: Record<string, unknown>[]) {
  return Array.from(new Set(rows.flatMap((row) => Object.keys(row)))).slice(0, 12);
}

function renderCell(value: unknown) {
  if (typeof value === 'boolean') return <Badge tone={value ? 'success' : 'warning'}>{String(value)}</Badge>;
  if (Array.isArray(value)) return displayValue(value.map((item) => (typeof item === 'object' ? JSON.stringify(item) : String(item))).join(', '));
  if (value && typeof value === 'object') return <code>{JSON.stringify(value)}</code>;
  return displayValue(value);
}
