import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Bell, BookOpen, BriefcaseBusiness, ChartNoAxesColumn, CreditCard, FileText, FolderKanban, Home, Library, Menu, MessageSquare, Settings, Shield, UserRound, Users } from 'lucide-react';
import { useAuth } from '../auth/AuthProvider';
import { Button } from '../components/ui';
import { NotificationBell } from '../features/notifications/NotificationBell';

const primaryNav = [
  ['Dashboard', '/', Home],
  ['Projects', '/projects', FolderKanban],
  ['Tasks', '/tasks', BriefcaseBusiness],
  ['Documents', '/documents', FileText],
  ['Research', '/research', BookOpen],
  ['AI Assistant', '/ai', MessageSquare],
  ['Analysis', '/analysis', ChartNoAxesColumn],
  ['Reports', '/reports', Library],
  ['References', '/references', Library],
  ['Notifications', '/notifications', Bell],
] as const;

export function AppShell() {
  const [open, setOpen] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();

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
          <NotificationBell />
          <Button variant="secondary" type="button" onClick={() => navigate('/profile')}>
            <UserRound size={16} /> Profile
          </Button>
          <Button variant="secondary" type="button" onClick={auth.logout}>Logout</Button>
        </div>
      </header>
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Primary">
        <div className="nav-group">
          <div className="nav-title">Workspace</div>
          <WorkspaceMiniSwitcher />
        </div>
        <div className="nav-group">
          <div className="nav-title">Research</div>
          {primaryNav.map(([label, to, Icon]) => (
            <NavLink key={to} className="nav-link" to={to} onClick={() => setOpen(false)}>
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>
        <div className="nav-group">
          <div className="nav-title">Account</div>
          <NavLink className="nav-link" to="/billing"><CreditCard size={18} /><span>Billing</span></NavLink>
          <NavLink className="nav-link" to="/settings"><Settings size={18} /><span>Settings</span></NavLink>
          {auth.hasCapability('admin') ? <NavLink className="nav-link" to="/admin"><Shield size={18} /><span>System Admin</span></NavLink> : null}
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}

function WorkspaceMiniSwitcher() {
  return (
    <NavLink className="nav-link" to="/workspaces">
      <Users size={18} aria-hidden />
      <span>Switch workspace</span>
      <span className="workspace-meta muted">role scoped</span>
    </NavLink>
  );
}
