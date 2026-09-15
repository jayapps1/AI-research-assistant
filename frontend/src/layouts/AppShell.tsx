import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Bell, BookOpen, BriefcaseBusiness, ChartNoAxesColumn, CreditCard, FileText, FolderKanban, Home, Library, Menu, MessageSquare, Moon, Settings, Shield, Sun, UserRound, Users } from 'lucide-react';
import { useTheme } from '../app/ThemeProvider';
import { useAuth } from '../auth/AuthProvider';
import { Button, Select } from '../components/ui';
import { NotificationBell } from '../features/notifications/NotificationBell';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { paths } from '../routes/paths';

const primaryNav = [
  ['Dashboard', paths.dashboard, Home],
  ['Projects', paths.projects, FolderKanban],
  ['My Tasks', paths.tasks, BriefcaseBusiness],
  ['Documents', paths.documents, FileText],
  ['Research', paths.research, BookOpen],
  ['AI Assistant', paths.ai, MessageSquare],
  ['Analysis', paths.analysis, ChartNoAxesColumn],
  ['Reports', paths.reports, Library],
  ['Notifications', paths.notifications, Bell],
] as const;

const projectSections = [
  ['Overview', ''],
  ['Research', 'research'],
  ['Documents', 'documents'],
  ['Literature', 'research#Literature Review'],
  ['Methodology', 'research#Methodology'],
  ['Instruments', 'research#Instruments'],
  ['Data', 'data'],
  ['Analysis', 'analysis'],
  ['AI Assistant', 'ai'],
  ['Findings', 'findings'],
  ['Report', 'report'],
  ['References', 'references'],
  ['Collaboration', 'collaboration'],
  ['Activity', 'activity'],
  ['Settings', 'settings'],
] as const;

export function AppShell() {
  const [open, setOpen] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const workspace = useWorkspace();
  const projectId = location.pathname.match(/\/projects\/([^/]+)/)?.[1] ?? new URLSearchParams(location.search).get('projectId') ?? '';

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <Button className="mobile-nav-button" type="button" variant="secondary" aria-label="Open navigation" onClick={() => setOpen((v) => !v)}>
            <Menu size={18} />
          </Button>
          <span className="brand-mark">RA</span>
          <span>AI Research Assistant</span>
        </div>
        <div className="toolbar">
          <Select
            className="select topbar-select"
            aria-label="Select workspace"
            value={workspace.selectedWorkspaceId}
            onChange={(event) => workspace.setSelectedWorkspaceId(event.target.value)}
          >
            {workspace.workspaces.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.currentUserRole ?? item.role ?? 'Member'}</option>)}
          </Select>
          <NotificationBell />
          <Button variant="secondary" type="button" aria-label="Toggle theme" onClick={() => theme.setTheme(theme.theme === 'dark' ? 'light' : theme.theme === 'light' ? 'system' : 'dark')}>
            {theme.theme === 'dark' ? <Moon size={16} /> : <Sun size={16} />} {theme.theme}
          </Button>
          <Button variant="secondary" type="button" onClick={() => navigate(paths.profile)}>
            <UserRound size={16} /> Profile
          </Button>
          <Button variant="secondary" type="button" onClick={auth.logout}>Logout</Button>
        </div>
      </header>
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Primary">
        <div className="nav-group">
          <div className="nav-title">Workspace</div>
          <NavLink className="nav-link" to={paths.workspaces} onClick={() => setOpen(false)}>
            <Users size={18} aria-hidden />
            <span>{workspace.selectedWorkspace?.name ?? 'Switch workspace'}</span>
          </NavLink>
        </div>
        <div className="nav-group">
          <div className="nav-title">Research</div>
          {primaryNav.map(([label, to, Icon]) => (
            <NavLink key={to} className="nav-link" to={to} onClick={() => setOpen(false)} end={to === paths.dashboard}>
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>
        <div className="nav-group">
          <div className="nav-title">Account</div>
          <NavLink className="nav-link" to={paths.billing}><CreditCard size={18} /><span>Billing</span></NavLink>
          <NavLink className="nav-link" to={paths.settings}><Settings size={18} /><span>Settings</span></NavLink>
          {auth.hasCapability('admin') ? <NavLink className="nav-link" to={paths.admin}><Shield size={18} /><span>System Admin</span></NavLink> : null}
        </div>
      </aside>
      <main className="main">
        {projectId ? <ProjectNav projectId={projectId} /> : null}
        <Outlet />
      </main>
    </div>
  );
}

function ProjectNav({ projectId }: { projectId: string }) {
  return (
    <nav className="project-nav" aria-label="Project">
      {projectSections.map(([label, suffix]) => {
        const to = suffix ? `/app/projects/${projectId}/${suffix}` : `/app/projects/${projectId}`;
        return <NavLink key={label} className="button secondary" to={to}>{label}</NavLink>;
      })}
    </nav>
  );
}
