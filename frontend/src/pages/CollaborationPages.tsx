import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { projectApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Select } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

export function TasksPage() {
  const projectId = useProjectId();
  const client = useQueryClient();
  const form = useForm<{ title: string; description: string; priority: string; dueDate: string }>({ defaultValues: { title: '', description: '', priority: 'MEDIUM', dueDate: '' } });
  const tasks = useQuery({ queryKey: ['tasks', projectId], queryFn: () => projectApi.tasks(projectId), enabled: Boolean(projectId) });
  const create = useMutation({
    mutationFn: (values: { title: string; description: string; priority: string; dueDate: string }) =>
      projectApi.createTask(projectId, { ...values, dueDate: values.dueDate || undefined }),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['tasks', projectId] });
      form.reset();
    },
  });
  return (
    <CollaborationShell title="Tasks" projectId={projectId}>
      <div className="grid cols-2">
        <Card>
          <h2>Create task</h2>
          {create.error ? <ErrorState title="Task was not created" error={create.error} /> : null}
          <form className="form" onSubmit={form.handleSubmit((values) => create.mutate(values))}>
            <Field label="Title"><Input {...form.register('title', { required: true })} /></Field>
            <Field label="Description"><Textarea {...form.register('description')} /></Field>
            <div className="grid cols-2">
              <Field label="Priority"><Select {...form.register('priority')}><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>URGENT</option></Select></Field>
              <Field label="Due date"><Input type="date" {...form.register('dueDate')} /></Field>
            </div>
            <Button type="submit" disabled={create.isPending}>Create task</Button>
          </form>
        </Card>
        <Card>
          <h2>My and project tasks</h2>
          {tasks.isLoading ? <PageLoading /> : tasks.isError ? <ErrorState error={tasks.error} /> : <TaskBoard rows={pageContent(tasks.data as never)} />}
        </Card>
      </div>
    </CollaborationShell>
  );
}

export function MembersPage() {
  const projectId = useProjectId();
  const client = useQueryClient();
  const form = useForm<{ email: string; role: string }>({ defaultValues: { email: '', role: 'EDITOR' } });
  const members = useQuery({ queryKey: ['project-members', projectId], queryFn: () => projectApi.members(projectId), enabled: Boolean(projectId) });
  const invitations = useQuery({ queryKey: ['project-invitations', projectId], queryFn: () => projectApi.invitations(projectId), enabled: Boolean(projectId) });
  const invite = useMutation({
    mutationFn: (values: { email: string; role: string }) => projectApi.invite(projectId, values),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['project-invitations', projectId] });
      client.invalidateQueries({ queryKey: ['project-members', projectId] });
      form.reset({ email: '', role: 'EDITOR' });
    },
  });
  return (
    <CollaborationShell title="Members & invitations" projectId={projectId}>
      <div className="grid cols-2">
        <Card>
          <h2>Invite member</h2>
          {invite.error ? <ErrorState title="Invitation failed" error={invite.error} /> : null}
          <form className="form" onSubmit={form.handleSubmit((values) => invite.mutate(values))}>
            <Field label="Email"><Input type="email" {...form.register('email', { required: true })} /></Field>
            <Field label="Role"><Select {...form.register('role')}><option>LEAD</option><option>EDITOR</option><option>REVIEWER</option><option>SUPERVISOR</option><option>VIEWER</option></Select></Field>
            <Button type="submit" disabled={invite.isPending}>Send invitation</Button>
          </form>
          <h3>Pending invitations</h3>
          <RecordList rows={invitations.data ?? []} />
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
  const client = useQueryClient();
  const commentForm = useForm<{ artifactType: string; artifactId: string; content: string }>({ defaultValues: { artifactType: 'RESEARCH_PROBLEM', artifactId: '', content: '' } });
  const reviewForm = useForm<{ artifactType: string; artifactId: string; artifactRevision: number; reviewerMemberId: string; summary: string }>({ defaultValues: { artifactType: 'RESEARCH_PROBLEM', artifactId: '', artifactRevision: 1, reviewerMemberId: '', summary: '' } });
  const comments = useQuery({ queryKey: ['comments', projectId], queryFn: () => projectApi.comments(projectId), enabled: Boolean(projectId) });
  const reviews = useQuery({ queryKey: ['reviews', projectId], queryFn: () => projectApi.reviews(projectId), enabled: Boolean(projectId) });
  const createComment = useMutation({ mutationFn: (values: Record<string, unknown>) => projectApi.createComment(projectId, values), onSuccess: () => client.invalidateQueries({ queryKey: ['comments', projectId] }) });
  const createReview = useMutation({ mutationFn: (values: Record<string, unknown>) => projectApi.createReview(projectId, values), onSuccess: () => client.invalidateQueries({ queryKey: ['reviews', projectId] }) });
  return (
    <CollaborationShell title="Comments & reviews" projectId={projectId}>
      <div className="grid cols-2">
        <Card>
          <h2>Artifact comments</h2>
          <form className="form" onSubmit={commentForm.handleSubmit((values) => createComment.mutate(values))}>
            <div className="grid cols-2">
              <Field label="Artifact type"><Input {...commentForm.register('artifactType', { required: true })} /></Field>
              <Field label="Artifact ID"><Input {...commentForm.register('artifactId', { required: true })} /></Field>
            </div>
            <Field label="New comment"><Textarea {...commentForm.register('content', { required: true })} /></Field>
            <Button type="submit" disabled={createComment.isPending}>Comment</Button>
          </form>
          {comments.isError ? <ErrorState error={comments.error} /> : <RecordList rows={comments.data ?? []} />}
        </Card>
        <Card>
          <h2>Reviews</h2>
          <form className="form" onSubmit={reviewForm.handleSubmit((values) => createReview.mutate(values))}>
            <div className="grid cols-2">
              <Field label="Artifact type"><Input {...reviewForm.register('artifactType', { required: true })} /></Field>
              <Field label="Artifact ID"><Input {...reviewForm.register('artifactId', { required: true })} /></Field>
              <Field label="Revision"><Input type="number" min={1} {...reviewForm.register('artifactRevision', { valueAsNumber: true })} /></Field>
              <Field label="Reviewer member ID"><Input {...reviewForm.register('reviewerMemberId', { required: true })} /></Field>
            </div>
            <Field label="Summary"><Textarea {...reviewForm.register('summary')} /></Field>
            <Button type="submit" disabled={createReview.isPending}>Request review</Button>
          </form>
          <div className="alert warning">Review approval is shown against the reviewed revision. If current revision differs, the UI warns instead of implying approval is current.</div>
          {reviews.isError ? <ErrorState error={reviews.error} /> : <RecordList rows={reviews.data ?? []} />}
        </Card>
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
  const statuses = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'COMPLETED', 'CANCELLED'];
  if (!rows.length) return <EmptyState title="No tasks returned" description="Tasks will appear here once the backend returns them." />;
  return <div className="grid cols-3">{statuses.map((status) => <Card key={status}><h2>{status.replaceAll('_', ' ')}</h2>{rows.filter((r) => String(r.status ?? 'TODO') === status).map((row, index) => <p key={index}><strong>{displayValue(row.title, 'Task')}</strong><br /><span className="muted">{displayValue(row.priority, 'Priority unset')}</span></p>)}</Card>)}</div>;
}

function RecordList({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No records" description="Nothing has been returned by the backend for this view." />;
  return <div className="grid">{rows.slice(0, 12).map((row, index) => <div className="panel" key={index}><Badge>{displayValue(row.status ?? row.type ?? 'Record')}</Badge><p>{displayValue(row.title ?? row.message ?? row.description ?? row.id)}</p></div>)}</div>;
}
