import { useState, useEffect } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Bell,
  BookOpen,
  BriefcaseBusiness,
  ChartNoAxesColumn,
  CreditCard,
  FileText,
  FolderKanban,
  Globe,
  Home,
  Inbox,
  Library,
  LogOut,
  Menu,
  MessageSquare,
  Moon,
  Settings,
  Shield,
  Sun,
  User,
  Users,
  X,
} from 'lucide-react';
import { useTheme } from '../app/ThemeProvider';
import { useAuth } from '../auth/AuthProvider';
import { Badge, Button, Select } from '../components/ui';
import { NotificationBell } from '../features/notifications/NotificationBell';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { paths } from '../routes/paths';

const workspaceNav = [
  ['Dashboard', paths.dashboard, Home, true],
  ['Projects', paths.projects, FolderKanban, false],
  ['My Tasks', paths.tasks, BriefcaseBusiness, false],
] as const;

const researchNav = [
  ['Documents', paths.documents, FileText],
  ['Research', paths.research, BookOpen],
  ['AI Assistant', paths.ai, MessageSquare],
  ['Analysis', paths.analysis, ChartNoAxesColumn],
  ['Reports', paths.reports, Library],
  ['References', paths.references, BookOpen],
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
  const [avatarOpen, setAvatarOpen] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const workspace = useWorkspace();
  const projectId =
    location.pathname.match(/\/projects\/([^/]+)/)?.[1] ??
    new URLSearchParams(location.search).get('projectId') ??
    '';

  // Close drawer on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        setAvatarOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const userName =
    auth.user?.fullName ||
    auth.user?.name ||
    (auth.user?.firstName ? `${auth.user.firstName} ${auth.user.lastName ?? ''}`.trim() : '') ||
    'Researcher';

  return (
    <div className="shell">
      {/* Off-canvas backdrop for mobile drawer */}
      {open ? (
        <div
          className="sidebar-backdrop"
          onClick={() => setOpen(false)}
          aria-hidden="true"
        />
      ) : null}

      <header className="topbar">
        <div className="brand">
          <Button
            className="mobile-nav-button"
            type="button"
            variant="secondary"
            aria-label="Open navigation"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X size={18} /> : <Menu size={18} />}
          </Button>
          <span className="brand-mark">RA</span>
          <span className="brand-title">AI Research Assistant</span>
        </div>

        <div className="toolbar">
          <Select
            className="select topbar-select"
            aria-label="Select workspace"
            value={workspace.selectedWorkspaceId}
            onChange={(event) => workspace.setSelectedWorkspaceId(event.target.value)}
          >
            {workspace.workspaces.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name} · {item.currentUserRole ?? item.role ?? 'Member'}
              </option>
            ))}
          </Select>

          <NotificationBell />

          <Button
            variant="secondary"
            type="button"
            className="theme-toggle-btn"
            aria-label="Switch theme"
            title="Switch theme"
            onClick={() =>
              theme.setTheme(
                theme.theme === 'dark' ? 'light' : theme.theme === 'light' ? 'system' : 'dark',
              )
            }
          >
            {theme.theme === 'dark' ? <Moon size={16} /> : <Sun size={16} />}
            <span className="desktop-only-text">{theme.theme}</span>
          </Button>

          {/* User Avatar Menu */}
          <div className="user-menu-wrapper">
            <button
              type="button"
              className="avatar-btn"
              aria-label="Account menu"
              aria-expanded={avatarOpen}
              onClick={() => setAvatarOpen((v) => !v)}
            >
              <span className="avatar-circle">{(userName[0] || 'U').toUpperCase()}</span>
            </button>

            {avatarOpen ? (
              <div className="avatar-dropdown-menu" role="menu">
                <div className="dropdown-user-info">
                  <strong>{userName}</strong>
                  <span className="muted">{auth.user?.email}</span>
                  {auth.hasCapability('admin') ? (
                    <Badge tone="info" className="badge-sm">
                      SYSTEM_ADMIN
                    </Badge>
                  ) : null}
                </div>
                <hr className="dropdown-divider" />
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => navigate(paths.profile)}
                >
                  <User size={15} /> Profile
                </button>
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => navigate(paths.security)}
                >
                  <Shield size={15} /> Security Settings
                </button>
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => navigate(paths.billing)}
                >
                  <CreditCard size={15} /> Billing & Plans
                </button>
                {auth.hasCapability('admin') ? (
                  <>
                    <hr className="dropdown-divider" />
                    <button
                      type="button"
                      className="dropdown-item text-brand"
                      onClick={() => navigate(paths.admin)}
                    >
                      <Shield size={15} /> System Admin Console
                    </button>
                  </>
                ) : null}
                <hr className="dropdown-divider" />
                <button
                  type="button"
                  className="dropdown-item text-danger"
                  onClick={auth.logout}
                >
                  <LogOut size={15} /> Logout
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Primary">
        {/* WORKSPACE GROUP */}
        <div className="nav-group">
          <div className="nav-title">Workspace</div>
          <NavLink className="nav-link" to={paths.workspaces} onClick={() => setOpen(false)}>
            <Users size={18} aria-hidden />
            <span>{workspace.selectedWorkspace?.name ?? 'Switch workspace'}</span>
          </NavLink>
          {workspaceNav.map(([label, to, Icon, end]) => (
            <NavLink
              key={to}
              className="nav-link"
              to={to}
              onClick={() => setOpen(false)}
              end={end}
            >
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>

        {/* RESEARCH GROUP */}
        <div className="nav-group">
          <div className="nav-title">Research</div>
          {researchNav.map(([label, to, Icon]) => (
            <NavLink key={to} className="nav-link" to={to} onClick={() => setOpen(false)}>
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>

        {/* COMMUNICATION GROUP */}
        <div className="nav-group">
          <div className="nav-title">Communication</div>
          <NavLink className="nav-link" to={paths.notifications} onClick={() => setOpen(false)}>
            <Bell size={18} aria-hidden />
            <span>Notifications</span>
          </NavLink>
        </div>

        {/* ACCOUNT GROUP */}
        <div className="nav-group">
          <div className="nav-title">Account</div>
          <NavLink className="nav-link" to={paths.billing} onClick={() => setOpen(false)}>
            <CreditCard size={18} />
            <span>Billing</span>
          </NavLink>
          <NavLink className="nav-link" to={paths.settings} onClick={() => setOpen(false)}>
            <Settings size={18} />
            <span>Settings</span>
          </NavLink>
        </div>

        {/* SYSTEM ADMINISTRATION GROUP */}
        {auth.hasCapability('admin') ? (
          <div className="nav-group admin-nav-group">
            <div className="nav-title">System Administration</div>
            <NavLink className="nav-link" to={paths.admin} onClick={() => setOpen(false)}>
              <Shield size={18} />
              <span>System Admin</span>
            </NavLink>
            <NavLink
              className="nav-link"
              to={paths.adminPublicSite}
              onClick={() => setOpen(false)}
            >
              <Globe size={18} />
              <span>Public Site CMS</span>
            </NavLink>
            <NavLink
              className="nav-link"
              to={paths.adminContactSubmissions}
              onClick={() => setOpen(false)}
            >
              <Inbox size={18} />
              <span>Contact Inbox</span>
            </NavLink>
          </div>
        ) : null}
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
        return (
          <NavLink key={label} className="button secondary" to={to}>
            {label}
          </NavLink>
        );
      })}
    </nav>
  );
}
