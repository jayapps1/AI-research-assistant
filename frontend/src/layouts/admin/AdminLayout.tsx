import { useState, useEffect, useRef } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Activity,
  ArrowLeft,
  BarChart3,
  CreditCard,
  Cpu,
  FileText,
  FolderKanban,
  Gift,
  Globe,
  Inbox,
  Layers,
  LogOut,
  Menu,
  Moon,
  Receipt,
  Shield,
  Sparkles,
  Sun,
  User,
  Users,
  X,
} from 'lucide-react';
import { useAuth } from '../../auth/AuthProvider';
import { useTheme } from '../../app/ThemeProvider';
import { Badge, Button } from '../../components/ui';
import { Avatar } from '../../components/Avatar';
import { paths } from '../../routes/paths';

const adminNav = [
  ['Overview', paths.admin, BarChart3, true],
  ['Users', '/admin/users', Users, false],
  ['Workspaces', '/admin/workspaces', Layers, false],
  ['Projects', '/admin/projects', FolderKanban, false],
  ['Documents', '/admin/documents', FileText, false],
  ['Processing Jobs', '/admin/processing-jobs', Cpu, false],
  ['Research Templates', '/admin/research-templates', FileText, false],
  ['Report Templates', '/admin/report-templates', FileText, false],
  ['Plans', '/admin/plans', CreditCard, false],
  ['Payments', '/admin/payments', Receipt, false],
  ['Complimentary Access', '/admin/complimentary-access', Gift, false],
  ['AI Operations', '/admin/ai-operations', Sparkles, false],
  ['AI Usage / Cost', '/admin/ai-usage', Sparkles, false],
  ['Notifications', '/admin/notifications', Activity, false],
  ['Contact Messages', paths.adminContactSubmissions, Inbox, false],
  ['Public Website CMS', paths.adminPublicSite, Globe, false],
  ['Storage', '/admin/storage', Cpu, false],
  ['References', '/admin/references', FileText, false],
  ['Audit Events', '/admin/audit', FileText, false],
  ['System Health', '/admin/health', Activity, false],
  ['Settings', '/admin/settings', Shield, false],
] as const;

export function AdminLayout() {
  const [open, setOpen] = useState(false);
  const [avatarOpen, setAvatarOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const auth = useAuth();
  const theme = useTheme();
  const navigate = useNavigate();

  // Close on Escape key
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

  // Close avatar dropdown when clicking outside
  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setAvatarOpen(false);
      }
    }
    if (avatarOpen) {
      document.addEventListener('pointerdown', handlePointerDown);
    }
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [avatarOpen]);

  // Close avatar dropdown on history navigation
  useEffect(() => {
    const handlePopState = () => setAvatarOpen(false);
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const adminName = auth.user?.fullName || auth.user?.firstName || 'System Administrator';

  return (
    <div className="shell admin-shell">
      {/* Off-canvas Backdrop for Mobile/Tablet */}
      {open ? (
        <div
          className="sidebar-backdrop"
          onClick={() => setOpen(false)}
          aria-hidden="true"
        />
      ) : null}

      <header className="topbar admin-topbar">
        <div className="brand">
          <Button
            className="mobile-nav-button"
            type="button"
            variant="secondary"
            aria-label="Open admin navigation"
            onClick={() => setOpen((v) => !v)}
          >
            {open ? <X size={18} /> : <Menu size={18} />}
          </Button>

          <span className="brand-mark admin-mark">
            <Shield size={18} />
          </span>
          <div className="admin-brand-text">
            <span className="brand-title">RA System Admin</span>
            <Badge tone="warning" className="badge-sm">TEST MODE</Badge>
          </div>
        </div>

        <div className="toolbar">
          <NavLink to={paths.app} className="button secondary btn-compact">
            <ArrowLeft size={15} />
            <span className="desktop-only-text">Research Workspace</span>
          </NavLink>

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

          {/* Admin Avatar Dropdown */}
          <div className="user-menu-wrapper" ref={menuRef}>
            <button
              type="button"
              className="avatar-btn"
              aria-label="Admin account menu"
              aria-haspopup="menu"
              aria-expanded={avatarOpen}
              onClick={() => setAvatarOpen((v) => !v)}
            >
              <Avatar
                src={auth.user?.avatarUrl || (auth.user?.id ? `/api/v1/users/${auth.user.id}/avatar` : undefined)}
                name={adminName}
                email={auth.user?.email}
                size={34}
              />
            </button>

            {avatarOpen ? (
              <div className="avatar-dropdown-menu" role="menu">
                <div className="dropdown-user-info" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '10px' }}>
                  <Avatar
                    src={auth.user?.avatarUrl || (auth.user?.id ? `/api/v1/users/${auth.user.id}/avatar` : undefined)}
                    name={adminName}
                    email={auth.user?.email}
                    size={40}
                  />
                  <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <strong style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{adminName}</strong>
                    <span className="muted" style={{ fontSize: '0.8rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{auth.user?.email}</span>
                    <Badge tone="info" className="badge-sm" style={{ marginTop: '4px', alignSelf: 'flex-start' }}>SYSTEM_ADMIN</Badge>
                  </div>
                </div>
                <hr className="dropdown-divider" />
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => {
                    setAvatarOpen(false);
                    navigate(paths.profile);
                  }}
                >
                  <User size={15} /> Profile
                </button>
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => {
                    setAvatarOpen(false);
                    navigate(paths.security);
                  }}
                >
                  <Shield size={15} /> Security Settings
                </button>
                <button
                  type="button"
                  className="dropdown-item"
                  onClick={() => {
                    setAvatarOpen(false);
                    navigate(paths.app);
                  }}
                >
                  <ArrowLeft size={15} /> Go to Research App
                </button>
                <hr className="dropdown-divider" />
                <button
                  type="button"
                  className="dropdown-item text-danger"
                  onClick={() => {
                    setAvatarOpen(false);
                    auth.logout();
                  }}
                >
                  <LogOut size={15} /> Logout
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      {/* Admin Sidebar */}
      <aside
        className={`sidebar admin-sidebar ${open ? 'open' : ''}`}
        aria-label="System Administration"
      >
        <div className="nav-group">
          <div className="nav-title">Administration</div>
          {adminNav.map(([label, to, Icon, end]) => (
            <NavLink
              key={to}
              className="nav-link"
              to={to}
              end={end}
              onClick={() => setOpen(false)}
            >
              <Icon size={18} aria-hidden />
              <span>{label}</span>
            </NavLink>
          ))}
        </div>

        <div className="nav-group" style={{ marginTop: 'auto', paddingTop: '20px' }}>
          <div className="nav-title">Return</div>
          <NavLink className="nav-link" to={paths.app} onClick={() => setOpen(false)}>
            <ArrowLeft size={18} aria-hidden />
            <span>Workspace View</span>
          </NavLink>
        </div>
      </aside>

      <main className="main admin-main">
        <Outlet />
      </main>
    </div>
  );
}
