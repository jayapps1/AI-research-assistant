import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { analysisApi, billingApi, documentApi, projectApi, workspaceApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Pagination, Select } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { pageContent } from '../utils/collections';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { paths } from '../routes/paths';

const projectSchema = z.object({ title: z.string().min(3), description: z.string().optional() });

export function HomeDashboard() {
  const { selectedWorkspace: workspace, isLoading } = useWorkspace();
  const projects = useQuery({ queryKey: ['projects', workspace?.id], queryFn: () => projectApi.list(workspace!.id), enabled: Boolean(workspace?.id) });
  const usage = useQuery({ queryKey: ['billing', workspace?.id, 'usage'], queryFn: () => billingApi.usage(workspace!.id), enabled: Boolean(workspace?.id) });
  const subscription = useQuery({ queryKey: ['billing', workspace?.id, 'subscription'], queryFn: () => billingApi.subscription(workspace!.id), enabled: Boolean(workspace?.id) });
  if (isLoading) return <PageLoading label="Loading dashboard" />;
  return (
    <section className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="muted">Current workspace: {workspace?.name ?? 'No workspace selected'}</p>
        </div>
        <Button asChild><Link to={paths.projects}>Open projects</Link></Button>
      </div>
      <div className="grid cols-3">
        <Card><div className="stat"><span>Active projects</span><strong>{pageContent(projects.data).length}</strong></div></Card>
        <Card><div className="stat"><span>AI usage</span><strong>{String(usage.data?.aiRequests ?? usage.data?.requests ?? 'Backend scoped')}</strong></div></Card>
        <Card><div className="stat"><span>Current plan</span><strong>{String(subscription.data?.planCode ?? subscription.data?.accessType ?? 'Backend')}</strong></div></Card>
      </div>
      <div className="grid cols-2" style={{ marginTop: 16 }}>
        <Card>
          <h2>Recent projects</h2>
          {pageContent(projects.data).length ? pageContent(projects.data).slice(0, 5).map((project) => (
            <p key={project.id}><Link to={paths.project(project.id)}>{project.title}</Link> <Badge>{project.status ?? 'Active'}</Badge></p>
          )) : <EmptyState title="No projects yet" description="Create a project from the projects page." />}
        </Card>
        <Card>
          <h2>Operational states</h2>
          <p className="muted">Notifications, usage, storage, and entitlement cards render from backend endpoints when available.</p>
        </Card>
      </div>
    </section>
  );
}

export function WorkspacePage() {
  const query = useQuery({ queryKey: ['workspaces'], queryFn: workspaceApi.list });
  if (query.isLoading) return <PageLoading />;
  if (query.isError) return <ErrorState error={query.error} />;
  return (
    <section className="page">
      <h1 className="page-title">Workspaces</h1>
      <div className="grid cols-3">
        {query.data?.map((workspace) => (
          <Card key={workspace.id}>
            <h2>{workspace.name}</h2>
            <p><Badge tone="info">{workspace.type ?? 'Workspace'}</Badge> <Badge>{workspace.currentUserRole ?? workspace.role ?? 'Member'}</Badge></p>
            <Button asChild variant="secondary"><Link to={`/app/workspaces/${workspace.id}/projects`}>View projects</Link></Button>
          </Card>
        ))}
      </div>
    </section>
  );
}

export function ProjectsPage() {
  const { selectedWorkspace: workspace } = useWorkspace();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');
  const projects = useQuery({
    queryKey: ['projects', workspace?.id, { page, q, status }],
    queryFn: () => projectApi.list(workspace!.id, page, 10, { q: q || undefined, status: status || undefined }),
    enabled: Boolean(workspace?.id),
  });
  const client = useQueryClient();
  const form = useForm<z.infer<typeof projectSchema>>({ resolver: zodResolver(projectSchema), defaultValues: { title: '', description: '' } });
  const create = useMutation({
    mutationFn: (values: z.infer<typeof projectSchema>) => projectApi.create(workspace!.id, values),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['projects', workspace?.id] });
      client.invalidateQueries({ queryKey: ['billing', workspace?.id, 'usage'] });
      form.reset();
    },
  });
  return (
    <section className="page">
      <div className="page-header">
        <div><h1 className="page-title">Projects</h1><p className="muted">Create and navigate research projects from the selected workspace.</p></div>
      </div>
      <div className="grid cols-2">
        <Card>
          <h2>Create project</h2>
          {create.error ? <ErrorState title="Project was not created" error={create.error} /> : null}
          <form className="form" onSubmit={form.handleSubmit((values) => create.mutate(values))}>
            <Field label="Research title" error={form.formState.errors.title?.message}><Input {...form.register('title')} /></Field>
            <Field label="Description"><Textarea {...form.register('description')} /></Field>
            <Button type="submit" disabled={!workspace || create.isPending}>Create project</Button>
          </form>
        </Card>
        <Card>
          <h2>Project list</h2>
          <div className="toolbar">
            <Input aria-label="Search projects" placeholder="Search projects" value={q} onChange={(event) => setQ(event.target.value)} />
            <Select aria-label="Filter status" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">All statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="ARCHIVED">Archived</option>
            </Select>
          </div>
          {projects.isLoading ? <PageLoading /> : null}
          {projects.isError ? <ErrorState error={projects.error} onRetry={() => projects.refetch()} /> : null}
          <div className="table-wrap">
            <table>
              <thead><tr><th>Title</th><th>Status</th><th>Role</th><th>Members</th></tr></thead>
              <tbody>{pageContent(projects.data).map((project) => (
                <tr key={project.id}>
                  <td><Link to={paths.project(project.id)}>{project.title}</Link></td>
                  <td>{project.status ?? 'Active'}</td>
                  <td>{project.currentUserRole ?? project.role ?? 'Member'}</td>
                  <td>{project.memberCount ?? 'Backend'}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
          <Pagination page={projects.data?.page ?? page} totalPages={projects.data?.totalPages ?? 1} onPageChange={setPage} />
        </Card>
      </div>
    </section>
  );
}

export function ProjectDashboard() {
  const { projectId = '' } = useParams();
  const project = useQuery({ queryKey: ['project', projectId], queryFn: () => projectApi.get(projectId) });
  const members = useQuery({ queryKey: ['project-members', projectId], queryFn: () => projectApi.members(projectId) });
  const documents = useQuery({ queryKey: ['documents', projectId], queryFn: () => documentApi.list(projectId) });
  const traceability = useQuery({ queryKey: ['traceability', projectId], queryFn: () => analysisApi.traceability(projectId) });
  if (project.isLoading) return <PageLoading />;
  if (project.isError) return <ErrorState error={project.error} />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', project.data?.title ?? 'Project']} />
      <div className="page-header">
        <div><h1 className="page-title">{project.data?.title}</h1><p className="muted">{project.data?.description ?? 'Project overview and workflow status.'}</p></div>
        <Badge tone="info">{project.data?.status ?? 'Active'}</Badge>
      </div>
      <div className="grid cols-3">
        <Card><div className="stat"><span>Members</span><strong>{members.data?.length ?? '...'}</strong></div></Card>
        <Card><div className="stat"><span>Documents</span><strong>{documents.data?.totalElements ?? '...'}</strong></div></Card>
        <Card><div className="stat"><span>Traceability rows</span><strong>{Array.isArray(traceability.data) ? traceability.data.length : '...'}</strong></div></Card>
      </div>
      <ProjectSections projectId={projectId} />
    </section>
  );
}

function ProjectSections({ projectId }: { projectId: string }) {
  const sections = ['Research', 'Documents', 'AI Assistant', 'Data & Analysis', 'Collaboration', 'Report', 'References', 'Activity', 'Settings'];
  const pathFor = (section: string) => `/app/projects/${projectId}/${section.toLowerCase().replaceAll(' ', '-').replace('data-&-analysis', 'analysis').replace('ai-assistant', 'ai').replace('report', 'reports')}`;
  return <div className="grid cols-3" style={{ marginTop: 16 }}>{sections.map((section) => <Card key={section}><h2>{section}</h2><Button asChild variant="secondary"><Link to={pathFor(section)}>Open</Link></Button></Card>)}</div>;
}
