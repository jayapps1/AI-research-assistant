import { useState } from 'react';
import { NavLink, Link } from 'react-router-dom';
import { Menu, Moon, Sun, ArrowRight, LayoutDashboard, LogIn } from 'lucide-react';
import { useAuth } from '../../auth/AuthProvider';
import { useTheme } from '../../app/ThemeProvider';
import { paths } from '../../routes/paths';
import { PublicMobileNavigation } from './PublicMobileNavigation';
import type { PublicSiteSettingsResponse } from '../../types/publicSite';

interface PublicHeaderProps {
  settings?: PublicSiteSettingsResponse | null;
}

export function PublicHeader({ settings }: PublicHeaderProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { isAuthenticated, user } = useAuth();
  const { theme, setTheme } = useTheme();

  const siteName = settings?.siteName || 'AI Research Assistant';
  const registrationEnabled = settings?.registrationEnabled ?? true;

  return (
    <header className="public-header" id="public-main-header">
      <div className="public-container public-header-inner">
        {/* Brand */}
        <Link to={paths.home} className="public-brand" aria-label={`${siteName} home`}>
          <span className="public-brand-mark">RA</span>
          <span className="public-brand-name">{siteName}</span>
        </Link>

        {/* Desktop Navigation */}
        <nav className="public-desktop-nav" aria-label="Main Navigation">
          <NavLink
            to={paths.home}
            end
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            Home
          </NavLink>
          <NavLink
            to={paths.services}
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            Services
          </NavLink>
          <NavLink
            to={paths.pricing}
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            Pricing
          </NavLink>
          <NavLink
            to={paths.about}
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            About
          </NavLink>
          <NavLink
            to={paths.faq}
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            FAQ
          </NavLink>
          <NavLink
            to={paths.contact}
            className={({ isActive }) => `public-nav-item ${isActive ? 'active' : ''}`}
          >
            Contact
          </NavLink>
        </nav>

        {/* Header Right Actions */}
        <div className="public-header-actions">
          {/* Theme Toggle */}
          <button
            type="button"
            className="public-theme-btn"
            onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            aria-label="Toggle dark/light theme"
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          {/* Dynamic Authentication CTA */}
          {isAuthenticated ? (
            <Link
              to={paths.app}
              className="btn btn-primary public-header-cta"
              id="header-app-link"
            >
              <LayoutDashboard size={16} />
              <span>Go to Workbench</span>
              {user?.firstName && <span className="user-badge">{user.firstName}</span>}
            </Link>
          ) : (
            <div className="public-auth-buttons">
              <Link
                to={paths.login}
                className="btn btn-ghost public-login-btn"
                id="header-login-link"
              >
                <LogIn size={16} />
                <span>Sign In</span>
              </Link>
              {registrationEnabled && (
                <Link
                  to={paths.register}
                  className="btn btn-primary public-register-btn"
                  id="header-register-link"
                >
                  <span>Get Started</span>
                  <ArrowRight size={16} />
                </Link>
              )}
            </div>
          )}

          {/* Mobile Hamburger Menu Toggle */}
          <button
            type="button"
            className="public-mobile-menu-btn"
            onClick={() => setMobileMenuOpen(true)}
            aria-label="Open mobile navigation"
            aria-expanded={mobileMenuOpen}
          >
            <Menu size={22} />
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      <PublicMobileNavigation
        isOpen={mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
        siteName={siteName}
        registrationEnabled={registrationEnabled}
      />
    </header>
  );
}
