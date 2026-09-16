import { NavLink } from 'react-router-dom';
import { X, Moon, Sun, ArrowRight, LogIn, LayoutDashboard } from 'lucide-react';
import { useAuth } from '../../auth/AuthProvider';
import { useTheme } from '../../app/ThemeProvider';
import { paths } from '../../routes/paths';

interface PublicMobileNavigationProps {
  isOpen: boolean;
  onClose: () => void;
  siteName?: string;
  registrationEnabled?: boolean;
}

export function PublicMobileNavigation({
  isOpen,
  onClose,
  siteName = 'AI Research Assistant',
  registrationEnabled = true,
}: PublicMobileNavigationProps) {
  const { isAuthenticated, user } = useAuth();
  const { theme, setTheme } = useTheme();

  if (!isOpen) return null;

  return (
    <div className="public-mobile-nav-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-label="Mobile Navigation">
      <div
        className="public-mobile-nav-panel"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="public-mobile-nav-header">
          <div className="brand">
            <span className="brand-mark">RA</span>
            <span className="brand-title">{siteName}</span>
          </div>
          <button
            type="button"
            className="public-nav-close-btn"
            onClick={onClose}
            aria-label="Close navigation"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="public-mobile-nav-links">
          <NavLink
            to={paths.home}
            end
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            Home
          </NavLink>
          <NavLink
            to={paths.services}
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            Services & Capabilities
          </NavLink>
          <NavLink
            to={paths.pricing}
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            Plans & Pricing
          </NavLink>
          <NavLink
            to={paths.about}
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            About & Methodology
          </NavLink>
          <NavLink
            to={paths.faq}
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            FAQ
          </NavLink>
          <NavLink
            to={paths.contact}
            className={({ isActive }) => `public-mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={onClose}
          >
            Contact Us
          </NavLink>
        </nav>

        <div className="public-mobile-nav-footer">
          <div className="public-mobile-nav-actions">
            <button
              type="button"
              className="btn btn-secondary public-theme-toggle-btn"
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
              <span>{theme === 'dark' ? 'Light Theme' : 'Dark Theme'}</span>
            </button>

            {isAuthenticated ? (
              <NavLink
                to={paths.app}
                className="btn btn-primary public-mobile-cta"
                onClick={onClose}
              >
                <LayoutDashboard size={18} />
                <span>Go to Workbench ({user?.firstName || 'Dashboard'})</span>
              </NavLink>
            ) : (
              <>
                <NavLink
                  to={paths.login}
                  className="btn btn-secondary public-mobile-cta"
                  onClick={onClose}
                >
                  <LogIn size={18} />
                  <span>Sign In</span>
                </NavLink>
                {registrationEnabled && (
                  <NavLink
                    to={paths.register}
                    className="btn btn-primary public-mobile-cta"
                    onClick={onClose}
                  >
                    <span>Get Started</span>
                    <ArrowRight size={18} />
                  </NavLink>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
