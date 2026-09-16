import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '../api/endpoints';
import { clearTokens } from '../api/tokens';
import { renderApp } from '../test/test-utils';
import { LoginPage, TotpPage } from './AuthPages';

describe('LoginPage', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders the brand panel before the login card for desktop layout', () => {
    renderApp(<LoginPage />);

    const layout = screen.getByRole('main');
    expect(layout.firstElementChild).toHaveClass('auth-context');
    expect(layout.lastElementChild).toHaveClass('auth-panel');
    expect(screen.getByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
  });

  it('defaults to the password tab and validates required fields', async () => {
    renderApp(<LoginPage />);

    expect(screen.getByRole('tab', { name: /password/i })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText(/^email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));
    expect(await screen.findByText(/invalid email/i)).toBeInTheDocument();
  });

  it('shows the authenticator code tab and sends TOTP-only login', async () => {
    const login = vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: { id: 'user-1', email: 'researcher@example.com', systemRoles: [] },
    });
    renderApp(<LoginPage />);

    await userEvent.click(screen.getByRole('tab', { name: /authenticator code/i }));
    await userEvent.type(screen.getByLabelText(/^email/i), 'researcher@example.com');
    await userEvent.type(screen.getByLabelText(/authenticator code/i), '123456');
    await userEvent.click(screen.getByRole('button', { name: /sign in with authenticator/i }));

    await waitFor(() => expect(login).toHaveBeenCalledWith({
      email: 'researcher@example.com',
      totpCode: '123456',
      authenticationMethod: 'TOTP',
    }));
  });

  it('toggles password visibility accessibly', async () => {
    renderApp(<LoginPage />);

    const password = screen.getByLabelText(/^password/i);
    expect(password).toHaveAttribute('type', 'password');

    await userEvent.click(screen.getByRole('button', { name: /show password/i }));
    expect(password).toHaveAttribute('type', 'text');

    await userEvent.click(screen.getByRole('button', { name: /hide password/i }));
    expect(password).toHaveAttribute('type', 'password');
  });

  it('submits password login with mocked backend', async () => {
    const login = vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: { id: 'user-1', email: 'researcher@example.com', systemRoles: [] },
    });
    renderApp(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/^email/i), 'researcher@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'correct-password');
    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));

    await waitFor(() => expect(login).toHaveBeenCalledWith({
      email: 'researcher@example.com',
      password: 'correct-password',
      totpCode: undefined,
      authenticationMethod: 'PASSWORD',
    }));
  });

  it('moves PASSWORD_AND_TOTP challenge to a second step', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      status: 'TOTP_REQUIRED',
      challengeId: 'challenge-1',
      authenticationMethod: 'PASSWORD_AND_TOTP',
      expiresIn: 300,
    });
    const complete = vi.spyOn(authApi, 'completeTotpLoginChallenge').mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: { id: 'user-1', email: 'admin@example.com', systemRoles: ['SYSTEM_ADMIN'] },
    });
    renderApp(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/^email/i), 'admin@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'correct-password');
    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));

    expect(await screen.findByText(/password has been accepted/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/^password/i)).not.toBeInTheDocument();

    await userEvent.type(screen.getByLabelText(/authenticator code/i), '654321');
    await userEvent.click(screen.getByRole('button', { name: /verify & sign in/i }));

    await waitFor(() => expect(complete).toHaveBeenCalledWith({
      challengeId: 'challenge-1',
      totpCode: '654321',
    }));
  });

  it('displays TOTP and rate limit errors without stack traces', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue(new Error('Too many requests. Try again shortly.'));
    renderApp(<LoginPage />);

    await userEvent.click(screen.getByRole('tab', { name: /authenticator code/i }));
    await userEvent.type(screen.getByLabelText(/^email/i), 'researcher@example.com');
    await userEvent.type(screen.getByLabelText(/authenticator code/i), '123456');
    await userEvent.click(screen.getByRole('button', { name: /sign in with authenticator/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests. Try again shortly.');
    expect(screen.getByRole('alert')).not.toHaveTextContent(/exception|stack/i);
  });

  it('displays account inactive and account suspended errors safely', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue(new Error('Account is inactive.'));
    renderApp(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/^email/i), 'inactive@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'password123');
    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Account is inactive.');
    expect(screen.getByRole('alert')).not.toHaveTextContent(/stack|exception/i);
  });

  it('shows recovery link on PASSWORD_AND_TOTP challenge screen', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      status: 'TOTP_REQUIRED',
      challengeId: 'challenge-rec-1',
      authenticationMethod: 'PASSWORD_AND_TOTP',
      expiresIn: 300,
    });
    renderApp(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/^email/i), 'admin@example.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'correct-password');
    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));

    expect(await screen.findByRole('link', { name: /use a recovery code/i })).toBeInTheDocument();
  });

  it('logs in SYSTEM_ADMIN user and exposes admin capabilities', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'admin-access-token',
      refreshToken: 'admin-refresh-token',
      user: { id: 'admin-user-id', email: 'nanagyachie@gmail.com', systemRoles: ['SYSTEM_ADMIN'] },
    });
    renderApp(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/^email/i), 'nanagyachie@gmail.com');
    await userEvent.type(screen.getByLabelText(/^password/i), 'admin-password');
    await userEvent.click(screen.getByRole('button', { name: /^sign in$/i }));

    await waitFor(() => {
      const stored = sessionStorage.getItem('raa.user');
      expect(stored).not.toBeNull();
      const parsed = JSON.parse(stored!);
      expect(parsed.systemRoles).toContain('SYSTEM_ADMIN');
    });
  });
});

describe('TotpPage', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders the dedicated password plus TOTP verification fallback form', () => {
    renderApp(<TotpPage />);
    expect(screen.getByLabelText(/6-digit code/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify and continue/i })).toBeInTheDocument();
  });
});
