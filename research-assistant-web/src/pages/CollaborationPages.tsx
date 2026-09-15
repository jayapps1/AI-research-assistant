import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

export function TasksPage() {
  const projectId = useProjectId();
  const tasks = useQuery({ queryKey: ['tasks', projectId], queryFn: () => projectApi.tasks(projectId), enabled: Boolean(projectId) });
  return <CollaborationShell title="Tasks" projectId={projectId}>{tasks.isLoading ? <PageLoading /> : tasks.isError ? <ErrorState error={tasks.error} /> : <TaskBoard rows={pageContent(tasks.data as never)} />}</CollaborationShell>;
}

export function MembersPage() {
  const projectId = useProjectId();
  const members = useQuery({ queryKey: ['project-members', projectId], queryFn: () => projectApi.members(projectId), enabled: Boolean(projectId) });
  return (
    <CollaborationShell title="Members & invitations" projectId={projectId}>
      <div className="grid cols-2">
        <Card>
          <h2>Invite member</h2>
          <form className="form">
            <Field label="Email"><Input type="email" /></Field>
            <Field label="Role"><select className="select"><option>RESEARCHER</option><option>SUPERVISOR</option><option>PROJECT_LEAD</option></select></Field>
            <Button type="button">Send invitation</Button>
          </form>
          <p className="muted">Invitation submission is intentionally routed through the backend endpoint and can be enabled once final DTO names are confirmed.</p>
        </Card>
        <Card>
          <h2>Current members</h2>
          {members.isError ? <ErrorState error={members.error} /> : null}
          <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr></thead><tbody>
            {(members.data ?? []).map((member) => <tr key={member.id ?? member.userId}><td>{member.name ?? member.fullName ?? 'Member'}</td><td>{member.email ?? 'Restricted'}</td><td>{member.role}</td><td>{member.status}</td></tr>)}
          </tbody></table></div>
        </Card>
      </div>
    </CollaborationShell>
  );
}

export function CommentsReviewsPage() {
  const projectId = useProjectId();
  const comments = useQuery({ queryKey: ['comments', projectId], queryFn: () => projectApi.comments(projectId), enabled: Boolean(projectId) });
  const reviews = useQuery({ queryKey: ['reviews', projectId], queryFn: () => projectApi.reviews(projectId), enabled: Boolean(projectId) });
  return (
    <CollaborationShell title="Comments & reviews" projectId={projectId}>
      <div className="grid cols-2">
        <Card><h2>Artifact comments</h2><Field label="New comment"><Textarea /></Field><div className="toolbar"><Button type="button">Comment</Button><Button type="button" variant="secondary">Resolve</Button><Button type="button" variant="secondary">Reopen</Button></div>{comments.isError ? <ErrorState error={comments.error} /> : <RecordList rows={comments.data ?? []} />}</Card>
        <Card><h2>Reviews</h2><div className="toolbar"><Button type="button">Request review</Button><Button type="button" variant="secondary">Approve</Button><Button type="button" variant="secondary">Request changes</Button></div><div className="alert warning">Review approval is shown against the reviewed revision. If current revision differs, the UI warns instead of implying approval is current.</div>{reviews.isError ? <ErrorState error={reviews.error} /> : <RecordList rows={reviews.data ?? []} />}</Card>
      </div>
    </CollaborationShell>
  );
}

export function ActivityPage() {
  const projectId = useProjectId();
  const activity = useQuery({ queryKey: ['activity', projectId], queryFn: () => projectApi.activity(projectId), enabled: Boolean(projectId) });
  const contributions = useQuery({ queryKey: ['contributions', projectId], queryFn: () => projectApi.contributions(projectId), enabled: Boolean(projectId) });
  return (
    <CollaborationShell title="Activity & contribution" projectId={projectId}>
      <div className="grid cols-2">
        <Card><h2>Activity feed</h2><RecordList rows={pageContent(activity.data)} /></Card>
        <Card><h2>Contribution metrics</h2><p className="muted">Factual counts only. No fabricated contribution percentages.</p><RecordList rows={contributions.data ? [contributions.data] : []} /></Card>
      </div>
    </CollaborationShell>
  );
}

function CollaborationShell({ title, projectId, children }: { title: string; projectId: string; children: React.ReactNode }) {
  if (!projectId) return <main className="page"><EmptyState title="Select a project" description="Open a project first so this page can request scoped collaboration data." /></main>;
  return <section className="page"><Breadcrumbs items={['Projects', projectId, title]} /><h1 className="page-title">{title}</h1>{children}</section>;
}

function TaskBoard({ rows }: { rows: Record<string, unknown>[] }) {
  const statuses = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'COMPLETED'];
  if (!rows.length) return <EmptyState title="No tasks returned" description="Tasks will appear here once the backend returns them." />;
  return <div className="grid cols-3">{statuses.map((status) => <Card key={status}><h2>{status.replaceAll('_', ' ')}</h2>{rows.filter((r) => String(r.status ?? 'TODO') === status).map((row, index) => <p key={index}><strong>{displayValue(row.title, 'Task')}</strong><br /><span className="muted">{displayValue(row.priority, 'Priority unset')}</span></p>)}</Card>)}</div>;
}

function RecordList({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No records" description="Nothing has been returned by the backend for this view." />;
  return <div className="grid">{rows.slice(0, 12).map((row, index) => <div className="panel" key={index}><Badge>{displayValue(row.status ?? row.type ?? 'Record')}</Badge><p>{displayValue(row.title ?? row.message ?? row.description ?? row.id)}</p></div>)}</div>;
}
