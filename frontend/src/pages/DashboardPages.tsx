import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  FolderGit2,
  CheckSquare,
  FileText,
  Sparkles,
  Plus,
  ArrowRight,
  Clock,
  Users,
  HardDrive,
  BarChart3,
  BookOpen,
  Settings,
  Layers,
  Database,
  Search,
} from 'lucide-react';
import { dashboardApi, projectApi } from '../api/endpoints';
import { Badge, Breadcrumbs, Button, Card, Input, Pagination, Select } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { CreateProjectModal } from '../features/projects/CreateProjectModal';
import { pageContent } from '../utils/collections';
import { paths } from '../routes/paths';

function formatBytes(bytes?: number): string {
  if (!bytes || bytes <= 0) return '0 MB';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return dateStr;
  }
}

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    const now = new Date();
    const diffSec = Math.floor((now.getTime() - d.getTime()) / 1000);
    if (diffSec < 60) return 'Just now';
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
    if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
    if (diffSec < 604800) return `${Math.floor(diffSec / 86400)}d ago`;
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  } catch {
    return dateStr;
  }
}

// =========================================================================
// 1. HOME DASHBOARD (/app)
// =========================================================================

export function HomeDashboard() {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const { selectedWorkspace } = useWorkspace();

  const userDashboard = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.userDashboard,
  });

  if (userDashboard.isLoading) return <PageLoading label="Loading your research workspace..." />;
  if (userDashboard.isError) return <ErrorState error={userDashboard.error} onRetry={() => userDashboard.refetch()} />;

  const data = userDashboard.data;
  const currentWorkspace = data?.currentWorkspace || selectedWorkspace;

  return (
    <section className="page">
      {/* Top Banner / Welcome Bar */}
      <div className="page-header" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: 4 }}>
            {data?.greeting ?? 'Welcome back'}
          </h1>
          <p className="muted" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            Workspace: <strong style={{ color: 'var(--text)' }}>{currentWorkspace?.name ?? 'Personal Workspace'}</strong>
            {currentWorkspace?.type ? <Badge tone="info">{currentWorkspace.type}</Badge> : null}
          </p>
        </div>
        <Button type="button" onClick={() => setCreateModalOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Plus size={16} /> New Research Project
        </Button>
      </div>

      {/* 4 Stat Cards Grid */}
      <div className="grid cols-4" style={{ gap: 16 }}>
        <Card className="stat-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <span className="muted" style={{ fontSize: '0.88rem', fontWeight: 500 }}>Active Projects</span>
            <span style={{ color: 'var(--primary)', opacity: 0.8 }}><FolderGit2 size={20} /></span>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.2 }}>
            {data?.activeProjectCount ?? 0}
          </div>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: 4 }}>
            In current workspace
          </p>
        </Card>

        <Card className="stat-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <span className="muted" style={{ fontSize: '0.88rem', fontWeight: 500 }}>My Open Tasks</span>
            <span style={{ color: 'var(--primary)', opacity: 0.8 }}><CheckSquare size={20} /></span>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.2 }}>
            {data?.openTaskCount ?? 0}
          </div>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: 4 }}>
            Assigned to you across projects
          </p>
        </Card>

        <Card className="stat-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <span className="muted" style={{ fontSize: '0.88rem', fontWeight: 500 }}>Documents</span>
            <span style={{ color: 'var(--primary)', opacity: 0.8 }}><FileText size={20} /></span>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.2 }}>
            {data?.documentCount ?? 0}
          </div>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: 4 }}>
            Indexed research literature & data
          </p>
        </Card>

        <Card className="stat-card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
            <span className="muted" style={{ fontSize: '0.88rem', fontWeight: 500 }}>AI Requests (Today)</span>
            <span style={{ color: 'var(--primary)', opacity: 0.8 }}><Sparkles size={20} /></span>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, lineHeight: 1.2 }}>
            {data?.aiUsage?.requestsToday ?? 0}
          </div>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: 4 }}>
            {data?.aiUsage?.isUnlimited
              ? 'Unlimited Plan'
              : `${data?.aiUsage?.requestsThisMonth ?? 0} / ${data?.aiUsage?.requestLimit ?? '∞'} this month`}
          </p>
        </Card>
      </div>

      {/* Main Content Layout: 2 Columns */}
      <div className="grid cols-2" style={{ gap: 20, marginTop: 20 }}>
        {/* Left Column: Recent Projects */}
        <Card>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>Recent Projects</h2>
            <Button asChild variant="secondary" style={{ fontSize: '0.82rem', padding: '4px 10px' }}>
              <Link to={paths.projects}>View All</Link>
            </Button>
          </div>

          {data?.recentProjects && data.recentProjects.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {data.recentProjects.map((project) => (
                <div
                  key={project.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    borderRadius: 8,
                    border: '1px solid var(--border)',
                    background: 'var(--surface-hover)',
                  }}
                >
                  <div style={{ minWidth: 0, flex: 1, paddingRight: 12 }}>
                    <Link
                      to={paths.project(project.id)}
                      style={{ fontWeight: 600, color: 'var(--text)', textDecoration: 'none', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                    >
                      {project.title}
                    </Link>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4, fontSize: '0.78rem' }} className="muted">
                      <span>Updated {formatRelativeTime(project.updatedAt)}</span>
                      {project.currentUserRole ? <span>• {project.currentUserRole}</span> : null}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Badge tone={project.status === 'ACTIVE' ? 'success' : 'info'}>
                      {project.status ?? 'Draft'}
                    </Badge>
                    <Button asChild variant="secondary" style={{ padding: '4px 8px', fontSize: '0.8rem' }}>
                      <Link to={paths.project(project.id)}><ArrowRight size={14} /></Link>
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No research projects yet"
              description="Create your first research project to begin literature synthesis, methodology design, and data analysis."
            >
              <Button type="button" onClick={() => setCreateModalOpen(true)} style={{ marginTop: 12 }}>
                <Plus size={14} /> Create Research Project
              </Button>
            </EmptyState>
          )}
        </Card>

        {/* Right Column: My Open Tasks & Activity */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {/* Tasks Card */}
          <Card>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>My Open Tasks</h2>
              <Button asChild variant="secondary" style={{ fontSize: '0.82rem', padding: '4px 10px' }}>
                <Link to={paths.tasks}>All Tasks</Link>
              </Button>
            </div>

            {data?.recentTasks && data.recentTasks.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {data.recentTasks.map((task) => (
                  <div
                    key={task.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '8px 12px',
                      borderRadius: 6,
                      border: '1px solid var(--border)',
                    }}
                  >
                    <div style={{ minWidth: 0, flex: 1, paddingRight: 8 }}>
                      <div style={{ fontWeight: 500, fontSize: '0.9rem' }}>{task.title}</div>
                      <div className="muted" style={{ fontSize: '0.78rem', marginTop: 2 }}>
                        {task.projectTitle} {task.dueDate ? `• Due ${formatDate(task.dueDate)}` : ''}
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      {task.overdue ? <Badge tone="danger">Overdue</Badge> : null}
                      <Badge tone={task.priority === 'URGENT' ? 'danger' : task.priority === 'HIGH' ? 'warning' : 'info'}>
                        {task.priority}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted" style={{ fontSize: '0.88rem', padding: '12px 0' }}>
                No pending tasks assigned to you.
              </p>
            )}
          </Card>

          {/* Activity Feed */}
          <Card>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 600, marginBottom: 12 }}>Recent Workspace Activity</h2>
            {data?.recentActivities && data.recentActivities.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {data.recentActivities.map((act) => (
                  <div key={act.id} style={{ display: 'flex', alignItems: 'flex-start', gap: 10, fontSize: '0.85rem' }}>
                    <div style={{ marginTop: 2, color: 'var(--muted)' }}><Clock size={14} /></div>
                    <div style={{ flex: 1 }}>
                      <div>
                        <strong>{act.actorName}</strong> {act.summary}
                      </div>
                      <div className="muted" style={{ fontSize: '0.75rem', marginTop: 2 }}>
                        {act.projectTitle} • {formatRelativeTime(act.occurredAt)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted" style={{ fontSize: '0.88rem', padding: '8px 0' }}>
                Activity stream will reflect collaborator uploads, analysis runs, and status milestones.
              </p>
            )}
          </Card>
        </div>
      </div>

      {/* Creation Modal */}
      <CreateProjectModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        defaultWorkspaceId={currentWorkspace?.id}
      />
    </section>
  );
}

// =========================================================================
// 2. WORKSPACE DASHBOARD (/app/workspaces)
// =========================================================================

export function WorkspacePage() {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const { workspaces, selectedWorkspaceId, setSelectedWorkspaceId } = useWorkspace();

  const activeWorkspace = workspaces.find((w) => w.id === selectedWorkspaceId) ?? workspaces[0];

  const workspaceDashboard = useQuery({
    queryKey: ['workspace-dashboard', activeWorkspace?.id],
    queryFn: () => dashboardApi.workspaceDashboard(activeWorkspace!.id),
    enabled: Boolean(activeWorkspace?.id),
  });

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Workspaces</h1>
          <p className="muted">Manage your research environments, organization tiers, and collaborator seats.</p>
        </div>
        <Button type="button" onClick={() => setCreateModalOpen(true)}>
          <Plus size={16} /> New Project in Workspace
        </Button>
      </div>

      {/* Active Workspace Overview Card */}
      {activeWorkspace ? (
        <Card style={{ marginBottom: 24, border: '1px solid var(--primary-border, var(--border))' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <h2 style={{ fontSize: '1.35rem', fontWeight: 700, margin: 0 }}>{activeWorkspace.name}</h2>
                <Badge tone="info">{activeWorkspace.type ?? 'PERSONAL'}</Badge>
                <Badge tone="success">{workspaceDashboard.data?.currentUserRole ?? 'OWNER'}</Badge>
              </div>
              <p className="muted" style={{ fontSize: '0.85rem', marginTop: 4 }}>
                Active workspace context • Subscription Tier: <strong>{workspaceDashboard.data?.planCode ?? 'FREE'}</strong>
              </p>
            </div>
          </div>

          {/* Aggregated Metrics Grid */}
          <div className="grid cols-4" style={{ gap: 12, marginTop: 12 }}>
            <div style={{ padding: '12px 14px', borderRadius: 8, background: 'var(--surface-hover)' }}>
              <div className="muted" style={{ fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                <FolderGit2 size={14} /> Total Projects
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: 4 }}>
                {workspaceDashboard.data?.projectCount ?? 0}
              </div>
            </div>

            <div style={{ padding: '12px 14px', borderRadius: 8, background: 'var(--surface-hover)' }}>
              <div className="muted" style={{ fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                <Users size={14} /> Members
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: 4 }}>
                {workspaceDashboard.data?.memberCount ?? 1}
              </div>
            </div>

            <div style={{ padding: '12px 14px', borderRadius: 8, background: 'var(--surface-hover)' }}>
              <div className="muted" style={{ fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                <FileText size={14} /> Indexed Documents
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: 4 }}>
                {workspaceDashboard.data?.documentCount ?? 0}
              </div>
            </div>

            <div style={{ padding: '12px 14px', borderRadius: 8, background: 'var(--surface-hover)' }}>
              <div className="muted" style={{ fontSize: '0.78rem', display: 'flex', alignItems: 'center', gap: 6 }}>
                <HardDrive size={14} /> Storage Used
              </div>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: 4 }}>
                {formatBytes(workspaceDashboard.data?.storageBytes)}
              </div>
            </div>
          </div>
        </Card>
      ) : null}

      {/* Workspace Switcher / Directory */}
      <h2 style={{ fontSize: '1.15rem', fontWeight: 600, marginBottom: 12 }}>Your Accessible Workspaces</h2>
      <div className="grid cols-3" style={{ gap: 16 }}>
        {workspaces.map((ws) => {
          const isSelected = ws.id === selectedWorkspaceId;
          return (
            <Card key={ws.id} style={{ borderColor: isSelected ? 'var(--primary)' : undefined }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 600 }}>{ws.name}</h3>
                  <div style={{ display: 'flex', gap: 6, marginTop: 6 }}>
                    <Badge tone="info">{ws.type ?? 'PERSONAL'}</Badge>
                    {isSelected ? <Badge tone="success">Selected</Badge> : null}
                  </div>
                </div>
              </div>

              <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
                {!isSelected ? (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setSelectedWorkspaceId(ws.id)}
                    style={{ fontSize: '0.82rem' }}
                  >
                    Select Workspace
                  </Button>
                ) : null}
                <Button asChild variant="secondary" style={{ fontSize: '0.82rem' }}>
                  <Link to={paths.projects}>View Projects</Link>
                </Button>
              </div>
            </Card>
          );
        })}
      </div>

      <CreateProjectModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        defaultWorkspaceId={activeWorkspace?.id}
      />
    </section>
  );
}

// =========================================================================
// 3. PROJECTS DIRECTORY DASHBOARD (/app/projects)
// =========================================================================

export function ProjectsPage() {
  const { selectedWorkspace: workspace } = useWorkspace();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [q, setQ] = useState('');

  const projectsQuery = useQuery({
    queryKey: ['projects', workspace?.id, { page, q, status }],
    queryFn: () =>
      workspace?.id
        ? projectApi.list(workspace.id, page, 10, { q: q || undefined, status: status || undefined })
        : projectApi.mine(page, 10, { q: q || undefined, status: status || undefined }),
    enabled: true,
  });

  const projects = pageContent(projectsQuery.data);

  return (
    <section className="page">
      <div className="page-header" style={{ alignItems: 'center' }}>
        <div>
          <h1 className="page-title">Research Projects</h1>
          <p className="muted">
            Workspace: <strong>{workspace?.name ?? 'Personal'}</strong> • Manage, design, and collaborate on your academic research.
          </p>
        </div>
        <Button type="button" onClick={() => setCreateModalOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Plus size={16} /> New Research Project
        </Button>
      </div>

      {/* Toolbar: Search + Filter */}
      <Card style={{ marginBottom: 16, padding: '12px 16px' }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ flex: 1, minWidth: 200, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Search size={16} className="muted" />
            <Input
              aria-label="Search projects"
              placeholder="Search projects by title or description..."
              value={q}
              onChange={(e) => {
                setQ(e.target.value);
                setPage(0);
              }}
              style={{ border: 'none', background: 'transparent', padding: 0 }}
            />
          </div>
          <div style={{ width: 160 }}>
            <Select
              aria-label="Filter status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="ACTIVE">Active</option>
              <option value="COMPLETED">Completed</option>
              <option value="ARCHIVED">Archived</option>
            </Select>
          </div>
        </div>
      </Card>

      {/* Project Listing */}
      {projectsQuery.isLoading ? (
        <PageLoading label="Loading projects..." />
      ) : projectsQuery.isError ? (
        <ErrorState error={projectsQuery.error} onRetry={() => projectsQuery.refetch()} />
      ) : projects.length > 0 ? (
        <Card>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title & Description</th>
                  <th>Status</th>
                  <th>Your Role</th>
                  <th>Last Updated</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {projects.map((project) => (
                  <tr key={project.id}>
                    <td style={{ maxWidth: 360 }}>
                      <Link
                        to={paths.project(project.id)}
                        style={{ fontWeight: 600, color: 'var(--text)', textDecoration: 'none' }}
                      >
                        {project.title}
                      </Link>
                      {project.description ? (
                        <p className="muted" style={{ fontSize: '0.8rem', margin: '4px 0 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {project.description}
                        </p>
                      ) : null}
                    </td>
                    <td>
                      <Badge tone={project.status === 'ACTIVE' ? 'success' : project.status === 'COMPLETED' ? 'info' : 'warning'}>
                        {project.status ?? 'DRAFT'}
                      </Badge>
                    </td>
                    <td>
                      <Badge tone="info">
                        {project.currentUserRole ?? project.role ?? 'LEAD'}
                      </Badge>
                    </td>
                    <td className="muted" style={{ fontSize: '0.85rem' }}>
                      {formatDate(project.updatedAt || project.lastActivityAt)}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Button asChild variant="secondary" style={{ fontSize: '0.82rem', padding: '4px 10px' }}>
                        <Link to={paths.project(project.id)}>
                          Open Dashboard <ArrowRight size={12} style={{ marginLeft: 4 }} />
                        </Link>
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={projectsQuery.data?.page ?? page}
            totalPages={projectsQuery.data?.totalPages ?? 1}
            onPageChange={setPage}
          />
        </Card>
      ) : (
        <EmptyState
          title={q || status ? 'No projects match your search filters' : 'No research projects in this workspace'}
          description={
            q || status
              ? 'Try clearing the search text or adjusting the status filter.'
              : 'Launch your first research project to access the 18 academic research stages, document upload, and AI grounding.'
          }
        >
          {q || status ? (
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setQ('');
                setStatus('');
                setPage(0);
              }}
              style={{ marginTop: 12 }}
            >
              Clear Filters
            </Button>
          ) : (
            <Button type="button" onClick={() => setCreateModalOpen(true)} style={{ marginTop: 12 }}>
              <Plus size={14} /> Create Research Project
            </Button>
          )}
        </EmptyState>
      )}

      <CreateProjectModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        defaultWorkspaceId={workspace?.id}
      />
    </section>
  );
}

// =========================================================================
// 4. PROJECT DASHBOARD (/app/projects/:projectId)
// =========================================================================

export function ProjectDashboard() {
  const { projectId = '' } = useParams();

  const dashboardQuery = useQuery({
    queryKey: ['project-dashboard', projectId],
    queryFn: () => dashboardApi.projectDashboard(projectId),
    enabled: Boolean(projectId),
  });

  if (dashboardQuery.isLoading) return <PageLoading label="Loading project overview..." />;
  if (dashboardQuery.isError) return <ErrorState error={dashboardQuery.error} onRetry={() => dashboardQuery.refetch()} />;

  const data = dashboardQuery.data;
  const project = data?.project;
  const progress = data?.researchProgress;
  const docs = data?.documents;
  const tasks = data?.tasks;

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', project?.title ?? 'Project Dashboard']} />

      {/* Project Header Banner */}
      <div className="page-header" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <h1 className="page-title" style={{ fontSize: '1.65rem', fontWeight: 700, margin: 0 }}>
              {project?.title}
            </h1>
            <Badge tone={project?.status === 'ACTIVE' ? 'success' : 'info'}>
              {project?.status ?? 'DRAFT'}
            </Badge>
            <Badge tone="info">
              {data?.currentUserRole ?? 'LEAD'}
            </Badge>
          </div>
          {project?.description ? (
            <p className="muted" style={{ fontSize: '0.95rem', marginTop: 6, maxWidth: 800 }}>
              {project.description}
            </p>
          ) : null}
        </div>
        <Button asChild>
          <Link to={progress?.nextStageUrl ?? paths.projectResearch(projectId)}>
            Continue Research <ArrowRight size={14} style={{ marginLeft: 4 }} />
          </Link>
        </Button>
      </div>

      {/* Progress & Summary Cards */}
      <div className="grid cols-4" style={{ gap: 16 }}>
        {/* Research Readiness Card */}
        <Card>
          <div className="muted" style={{ fontSize: '0.82rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
            <BarChart3 size={16} /> Research Progress
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: 4 }}>
            {progress?.percentComplete ?? 0}%
          </div>
          <div style={{ width: '100%', height: 6, borderRadius: 3, background: 'var(--border)', margin: '8px 0', overflow: 'hidden' }}>
            <div
              style={{
                width: `${Math.max(progress?.percentComplete ?? 0, 4)}%`,
                height: '100%',
                background: 'var(--primary)',
                borderRadius: 3,
              }}
            />
          </div>
          <p className="muted" style={{ fontSize: '0.78rem' }}>
            {progress?.completedStages ?? 0} of {progress?.totalStages ?? 18} stages complete
          </p>
        </Card>

        {/* Documents Card */}
        <Card>
          <div className="muted" style={{ fontSize: '0.82rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
            <FileText size={16} /> Documents
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: 4 }}>
            {docs?.total ?? 0}
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 6, fontSize: '0.78rem' }} className="muted">
            <span style={{ color: 'var(--success-text, #10b981)' }}>{docs?.ready ?? 0} Ready</span>
            {docs?.processing ? <span>• {docs.processing} In Progress</span> : null}
            {docs?.failed ? <span style={{ color: 'var(--danger-text, #ef4444)' }}>• {docs.failed} Failed</span> : null}
          </div>
        </Card>

        {/* Tasks Card */}
        <Card>
          <div className="muted" style={{ fontSize: '0.82rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
            <CheckSquare size={16} /> Collaboration Tasks
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: 4 }}>
            {tasks?.total ?? 0}
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 6, fontSize: '0.78rem' }} className="muted">
            <span>{tasks?.inProgress ?? 0} In Progress</span>
            <span>• {tasks?.completed ?? 0} Completed</span>
          </div>
        </Card>

        {/* Members Card */}
        <Card>
          <div className="muted" style={{ fontSize: '0.82rem', fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Users size={16} /> Project Team
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: 4 }}>
            {data?.memberCount ?? 1}
          </div>
          <p className="muted" style={{ fontSize: '0.78rem', marginTop: 6 }}>
            Authorized researchers & reviewers
          </p>
        </Card>
      </div>

      {/* Research Next Step Callout */}
      {progress?.nextIncompleteStage ? (
        <Card style={{ marginTop: 16, background: 'var(--surface-hover)', border: '1px solid var(--border)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <span className="muted" style={{ fontSize: '0.8rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Next Incomplete Stage
              </span>
              <h3 style={{ margin: '4px 0 0', fontSize: '1.1rem', fontWeight: 600 }}>
                {progress.nextIncompleteStage}
              </h3>
            </div>
            <Button asChild>
              <Link to={progress.nextStageUrl ?? paths.projectResearch(projectId)}>
                Open Stage Workbench <ArrowRight size={14} style={{ marginLeft: 4 }} />
              </Link>
            </Button>
          </div>
        </Card>
      ) : null}

      {/* 9 Project Sections Grid */}
      <h2 style={{ fontSize: '1.2rem', fontWeight: 600, marginTop: 24, marginBottom: 12 }}>
        Project Modules & Workbenches
      </h2>
      <ProjectSections projectId={projectId} />

      {/* Recent Project Activity */}
      {data?.recentActivities && data.recentActivities.length > 0 ? (
        <Card style={{ marginTop: 24 }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: 12 }}>Recent Activity</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {data.recentActivities.map((act) => (
              <div key={act.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.85rem' }}>
                <Clock size={14} className="muted" />
                <span>
                  <strong>{act.actorName}</strong> {act.summary}
                </span>
                <span className="muted" style={{ fontSize: '0.75rem', marginLeft: 'auto' }}>
                  {formatRelativeTime(act.occurredAt)}
                </span>
              </div>
            ))}
          </div>
        </Card>
      ) : null}
    </section>
  );
}

function ProjectSections({ projectId }: { projectId: string }) {
  const sections = [
    { title: 'Research Workflow', desc: '18 academic stages from problem statement to defense', icon: <BookOpen size={20} />, path: `/app/projects/${projectId}/research` },
    { title: 'Document Library', desc: 'Manage PDF literature, data instruments, and doc codes', icon: <FileText size={20} />, path: `/app/projects/${projectId}/documents` },
    { title: 'AI Assistant', desc: 'Evidence-grounded queries with verbatim citations', icon: <Sparkles size={20} />, path: `/app/projects/${projectId}/ai` },
    { title: 'Datasets & Analysis', desc: 'Import datasets, variables, and run statistical tests', icon: <Database size={20} />, path: `/app/projects/${projectId}/data` },
    { title: 'Collaboration & Tasks', desc: 'Assign research tasks, leave review notes, invite peers', icon: <Users size={20} />, path: `/app/projects/${projectId}/tasks` },
    { title: 'Dissertation & Report', desc: 'Draft chapters, assemble report, and export docx/pdf', icon: <Layers size={20} />, path: `/app/projects/${projectId}/reports` },
    { title: 'References & Citations', desc: 'APA7/Harvard reference management and integrity audit', icon: <HardDrive size={20} />, path: `/app/projects/${projectId}/references` },
    { title: 'Activity Stream', desc: 'Audit log of revisions, member actions, and analysis runs', icon: <Clock size={20} />, path: `/app/projects/${projectId}/activity` },
    { title: 'Project Settings', desc: 'Manage title, description, team roles, and archiving', icon: <Settings size={20} />, path: `/app/projects/${projectId}/members` },
  ];

  return (
    <div className="grid cols-3" style={{ gap: 16 }}>
      {sections.map((section) => (
        <Card key={section.title} style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ color: 'var(--primary)', marginBottom: 8 }}>{section.icon}</div>
            <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600 }}>{section.title}</h3>
            <p className="muted" style={{ fontSize: '0.82rem', marginTop: 4 }}>{section.desc}</p>
          </div>
          <div style={{ marginTop: 16 }}>
            <Button asChild variant="secondary" style={{ fontSize: '0.82rem', width: '100%' }}>
              <Link to={section.path}>Open Module</Link>
            </Button>
          </div>
        </Card>
      ))}
    </div>
  );
}
