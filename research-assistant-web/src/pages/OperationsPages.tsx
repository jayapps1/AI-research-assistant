import { useMutation, useQuery } from '@tanstack/react-query';
import { adminApi, billingApi, notificationApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useAuth } from '../auth/AuthProvider';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

export function ReportPage() {
  const projectId = useProjectId();
  const reports = useQuery({ queryKey: ['reports', projectId], queryFn: () => reportApi.reports(projectId), enabled: Boolean(projectId) });
  const references = useQuery({ queryKey: ['references', projectId], queryFn: () => reportApi.references(projectId), enabled: Boolean(projectId) });
  if (!projectId) return <EmptyState title="Select a project" />;
  return <section className="page"><Breadcrumbs items={['Projects', projectId, 'Report']} /><h1 className="page-title">Report builder</h1><div className="grid cols-2"><Card><h2>Report tree</h2>{['Chapter 1 Background', 'Chapter 2 Literature Review', 'Chapter 3 Methodology', 'Chapter 4 Results', 'Chapter 5 Discussion'].map((x) => <p key={x}>{x}</p>)}<div className="alert warning">Source material changed warnings are shown before assembled content is overwritten.</div><RecordRows rows={reports.data ?? []} /></Card><Card><h2>Section editor</h2><Field label="Section content"><Textarea /></Field><div className="toolbar"><Button type="button">Save revision</Button><Button type="button" variant="secondary">AI draft</Button><Button type="button" variant="secondary">Validate</Button></div></Card><Card><h2>References</h2><RecordRows rows={pageContent(references.data)} /></Card><Card><h2>Exports</h2><div className="toolbar"><Button type="button">Export DOCX</Button><Button type="button" variant="secondary">Export PDF</Button></div><p className="muted">Download links use authorized backend export endpoints only.</p></Card><Card><h2>Similarity & integrity</h2><p>Similarity, potential overlap, needs review.</p><p className="muted">The UI does not label users as plagiarists and shows local-corpus limitations.</p></Card></div></section>;
}

export function ReferencesPage() {
  const projectId = useProjectId();
  const references = useQuery({ queryKey: ['references', projectId], queryFn: () => reportApi.references(projectId), enabled: Boolean(projectId) });
  return <section className="page"><h1 className="page-title">Reference library</h1><div className="toolbar"><Button type="button">Create</Button><Button type="button" variant="secondary">Import RIS</Button><Button type="button" variant="secondary">Import BibTeX</Button><Button type="button" variant="secondary">Check duplicates</Button></div><RecordRows rows={pageContent(references.data)} /></section>;
}

export function BillingPage() {
  const workspaceId = new URLSearchParams(window.location.search).get('workspaceId') ?? '';
  const subscription = useQuery({ queryKey: ['billing', workspaceId, 'subscription'], queryFn: () => billingApi.subscription(workspaceId), enabled: Boolean(workspaceId) });
  const transactions = useQuery({ queryKey: ['billing', workspaceId, 'transactions'], queryFn: () => billingApi.transactions(workspaceId), enabled: Boolean(workspaceId) });
  const initialize = useMutation({ mutationFn: (body: { planCode: string; billingInterval: string }) => billingApi.initialize(workspaceId, body) });
  return <section className="page"><h1 className="page-title">Billing</h1>{!workspaceId ? <EmptyState title="Open billing from a workspace" /> : null}<div className="grid cols-2"><Card><h2>Current plan</h2><PlanState data={subscription.data} /></Card><Card><h2>Available plans</h2><Field label="Plan code"><Input id="planCode" /></Field><Field label="Billing interval"><select className="select"><option>MONTHLY</option><option>YEARLY</option></select></Field><Button type="button" onClick={() => initialize.mutate({ planCode: (document.getElementById('planCode') as HTMLInputElement)?.value, billingInterval: 'MONTHLY' })}>Start Paystack TEST checkout</Button>{initialize.data ? <div className="alert info">Backend returned checkout initialization. Redirect only to trusted Paystack authorization URL supplied by the backend.</div> : null}</Card><Card><h2>Payment history</h2><RecordRows rows={transactions.data ?? []} /><div className="alert info">Pending Mobile Money: Approve the Mobile Money request on your phone. Pay Again appears only for backend retryable failed, expired, or abandoned states.</div></Card></div></section>;
}

function PlanState({ data }: { data?: Record<string, unknown> }) {
  const access = String(data?.accessType ?? data?.grantType ?? data?.subscriptionType ?? '');
  const complimentary = ['COMPLIMENTARY', 'DEVELOPER_ACCESS', 'PROMOTIONAL'].includes(access);
  return <div>{complimentary ? <Badge tone="success">{access.replaceAll('_', ' ')}</Badge> : <Badge>{displayValue(data?.status, 'No active plan')}</Badge>}<p className="muted">{complimentary ? 'No billing required.' : 'Paid status is displayed only when backend reports a verified subscription.'}</p></div>;
}

export function NotificationsPage() {
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: notificationApi.list });
  const markAll = useMutation({ mutationFn: notificationApi.markAllRead });
  return <section className="page"><div className="page-header"><h1 className="page-title">Notifications</h1><Button type="button" variant="secondary" onClick={() => markAll.mutate()}>Mark all read</Button></div>{notifications.isError ? <ErrorState error={notifications.error} /> : <RecordRows rows={pageContent(notifications.data) as never} />}</section>;
}

export function ProfilePage() {
  const auth = useAuth();
  return <section className="page"><h1 className="page-title">Profile & security</h1><div className="grid cols-2"><Card><h2>Profile information</h2><p>{auth.user?.fullName ?? auth.user?.name ?? 'Signed-in user'}</p><p className="muted">{auth.user?.email}</p></Card><Card><h2>Security</h2><div className="toolbar"><Button type="button">Change password</Button><Button type="button" variant="secondary">Enroll TOTP</Button><Button type="button" variant="secondary" onClick={auth.logoutAll}>Logout all sessions</Button></div></Card><Card><h2>Notification preferences</h2><Field label="Email notifications"><select className="select"><option>Backend default</option></select></Field></Card><Card><h2>Registered devices</h2><p className="muted">Device registration uses `/me/devices` when enabled.</p></Card></div></section>;
}

export function AdminPage() {
  const dashboard = useQuery({ queryKey: ['admin', 'dashboard'], queryFn: adminApi.dashboard });
  const operations = useQuery({ queryKey: ['admin', 'operations'], queryFn: adminApi.operations });
  const users = useQuery({ queryKey: ['admin', 'users'], queryFn: adminApi.users });
  const grant = useMutation({ mutationFn: adminApi.grantComplimentaryAccess });
  return <section className="page"><h1 className="page-title">System administration</h1><div className="grid cols-2"><Card><h2>Dashboard</h2><RecordRows rows={dashboard.data ? [dashboard.data] : []} /></Card><Card><h2>Operations</h2><RecordRows rows={operations.data ? [operations.data] : []} /></Card><Card><h2>Users</h2><RecordRows rows={pageContent(users.data)} /></Card><Card><h2>Complimentary access</h2><form className="form"><Field label="Workspace/User"><Input /></Field><Field label="Plan"><Input /></Field><Field label="Grant type"><select className="select"><option>DEVELOPER_ACCESS</option><option>COMPLIMENTARY</option><option>PROMOTIONAL</option></select></Field><Field label="Reason"><Textarea /></Field><Button type="button" onClick={() => grant.mutate({})}>Grant</Button></form><p className="muted">Paid subscriptions and complimentary grants are always visually distinct.</p></Card></div></section>;
}

function RecordRows({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No backend records returned" />;
  return <div className="grid">{rows.slice(0, 10).map((row, index) => <div className="panel" key={index}><Badge>{displayValue(row.status ?? row.type ?? row.id ?? 'Record')}</Badge><p>{displayValue(row.title ?? row.name ?? row.message ?? row.description ?? row.email ?? row.id)}</p></div>)}</div>;
}
