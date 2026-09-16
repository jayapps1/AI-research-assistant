import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi, workspaceApi } from '../api/endpoints';
import { clearTokens, setTokens } from '../api/tokens';
import { ThemeProvider } from '../app/ThemeProvider';
import { AuthProvider } from '../auth/AuthProvider';
import { WorkspaceProvider } from '../features/workspaces/WorkspaceProvider';
import { AppShell } from './AppShell';

function renderShell(user: Record<string, unknown>) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  vi.spyOn(workspaceApi, 'list').mockResolvedValue([]);
  vi.spyOn(authApi, 'logout').mockResolvedValue({});
  setTokens({ accessToken: 'access', refreshToken: 'refresh' });
  sessionStorage.setItem('raa.user', JSON.stringify(user));
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <WorkspaceProvider>
              <AppShell />
            </WorkspaceProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AppShell authorization navigation', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows admin navigation for SYSTEM_ADMIN users', async () => {
    renderShell({ id: 'admin-1', email: 'admin@example.com', systemRoles: ['SYSTEM_ADMIN'] });

    expect(await screen.findByRole('link', { name: /system admin/i })).toBeInTheDocument();
  });

  it('hides admin navigation for normal users', () => {
    renderShell({ id: 'user-1', email: 'user@example.com', systemRoles: [] });

    expect(screen.queryByRole('link', { name: /system admin/i })).not.toBeInTheDocument();
  });
});
