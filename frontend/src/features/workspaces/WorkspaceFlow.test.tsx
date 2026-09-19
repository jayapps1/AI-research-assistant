import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { billingApi, projectApi, workspaceApi } from '../../api/endpoints';
import { publicApi } from '../../api/public';
import { clearTokens, setTokens } from '../../api/tokens';
import { ThemeProvider } from '../../app/ThemeProvider';
import { AuthProvider } from '../../auth/AuthProvider';
import { WorkspaceProvider } from './WorkspaceProvider';
import { AppShell } from '../../layouts/AppShell';
import { BillingPage } from '../../pages/BillingPage';
import { CreateProjectModal } from '../projects/CreateProjectModal';

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
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

describe('Workspace & FREE Plan Flow Suite', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
    vi.restoreAllMocks();
    setTokens({ accessToken: 'test-token', refreshToken: 'refresh-token' });
    sessionStorage.setItem(
      'raa.user',
      JSON.stringify({ id: 'u-1', email: 'researcher@example.com', firstName: 'Ada', systemRoles: [] }),
    );
  });

  it('removes strange top full-width select and renders compact WorkspaceSwitcher', async () => {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([
      {
        id: 'ws-1',
        name: "Ada's Workspace",
        type: 'PERSONAL',
        status: 'ACTIVE',
        role: 'OWNER',
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]);

    renderApp();

    // Verify raw <select class="topbar-select"> does NOT exist
    expect(document.querySelector('select.topbar-select')).toBeNull();

    // Verify compact WorkspaceSwitcher trigger exists
    const trigger = await screen.findByRole('button', { name: /switch workspace/i }, { timeout: 5000 });
    expect(trigger).toBeInTheDocument();
    expect(await screen.findByText("Ada's Workspace", { selector: '.workspace-switcher-name' }, { timeout: 5000 })).toBeInTheDocument();
  });

  it('renders zero workspace state with Create Workspace action', async () => {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([]);
    vi.spyOn(workspaceApi, 'ensurePersonal').mockResolvedValue(null as never);

    renderApp();

    expect(await screen.findByText(/no workspace/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create workspace/i })).toBeInTheDocument();
  });

  it('creates a workspace via real backend API and selects it', async () => {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([
      {
        id: 'ws-1',
        name: 'Personal Workspace',
        type: 'PERSONAL',
        status: 'ACTIVE',
        role: 'OWNER',
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]);

    const createSpy = vi.spyOn(workspaceApi, 'create').mockResolvedValue({
      id: 'ws-2',
      name: 'Bioinformatics Team',
      type: 'ORGANIZATION',
      status: 'ACTIVE',
      role: 'OWNER',
      createdAt: '2026-01-01T00:00:00Z',
    });

    renderApp();

    // Open switcher dropdown
    const trigger = await screen.findByRole('button', { name: /switch workspace/i });
    fireEvent.click(trigger);

    // Click Create Workspace in menu
    const createBtn = screen.getByRole('button', { name: /create workspace/i });
    fireEvent.click(createBtn);

    // Fill in modal
    const nameInput = screen.getByPlaceholderText(/e\.g\., Clinical Trials/i);
    fireEvent.change(nameInput, { target: { value: 'Bioinformatics Team' } });

    const submitBtn = screen.getByRole('button', { name: /^create workspace$/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        name: 'Bioinformatics Team',
        type: 'ORGANIZATION',
      });
    });
  });

  it('renders FREE plan as current access with GHS 0 and no Paystack checkout', async () => {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([
      {
        id: 'ws-free',
        name: "Ada's Workspace",
        type: 'PERSONAL',
        status: 'ACTIVE',
        role: 'OWNER',
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]);

    vi.spyOn(billingApi, 'subscription').mockResolvedValue({
      id: 'sub-1',
      planCode: 'FREE',
      planName: 'Free',
      price: 0,
      currency: 'GHS',
      status: 'ACTIVE',
      accessSource: 'FREE_DEFAULT',
      isComplimentary: false,
      periodStart: '2026-01-01T00:00:00Z',
      periodEnd: '2026-02-01T00:00:00Z',
      autoRenew: false,
    });

    vi.spyOn(billingApi, 'usage').mockResolvedValue({
      aiRequests: { used: 5, limit: 100 },
      aiTokens: { used: 10000, limit: 500000 },
      projects: { used: 1, limit: 5 },
      storage: { used: 50 * 1024 * 1024, limit: 5368709120 },
    });

    vi.spyOn(billingApi, 'transactions').mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });

    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([
      {
        id: 'plan-free',
        code: 'FREE',
        name: 'Free',
        description: 'Development free plan',
        monthlyPrice: 0,
        annualPrice: 0,
        currency: 'GHS',
        highlighted: false,
        features: ['Up to 5 Research Projects'],
        displayOrder: 1,
      },
      {
        id: 'plan-pro',
        code: 'PRO',
        name: 'Pro',
        description: 'Professional tier',
        monthlyPrice: 150,
        annualPrice: 1500,
        currency: 'GHS',
        highlighted: true,
        features: ['Unlimited Projects'],
        displayOrder: 2,
      },
    ]);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    sessionStorage.setItem('raa.workspaceId', 'ws-free');

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <WorkspaceProvider>
            <BillingPage />
          </WorkspaceProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Verify FREE is rendered as active access
    expect(await screen.findByText('FREE')).toBeInTheDocument();
    expect(screen.getByText('GHS 0')).toBeInTheDocument();
    expect(screen.getByText('Free Default')).toBeInTheDocument();

    // Verify usage displays real numbers from backend
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
      expect(screen.getByText(/\/ 5 projects/i)).toBeInTheDocument();
      expect(screen.getByText('Pro')).toBeInTheDocument();
    });

    // Verify FREE plan card displays "Current Plan" disabled (no checkout button)
    const currentPlanButtons = screen.getAllByRole('button', { name: /current plan/i });
    expect(currentPlanButtons[0]).toBeDisabled();

    // Verify paid plan has Upgrade button
    expect(screen.getByRole('button', { name: /upgrade to pro/i })).toBeInTheDocument();
  });

  it('displays quota exceeded feedback with View Plans action when project creation limit is reached', async () => {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([
      {
        id: 'ws-free',
        name: "Ada's Workspace",
        type: 'PERSONAL',
        status: 'ACTIVE',
        role: 'OWNER',
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]);

    const quotaError = {
      status: 429,
      code: 'QUOTA_EXCEEDED',
      message: 'Plan quota exceeded',
    };
    vi.spyOn(projectApi, 'create').mockRejectedValue(quotaError);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    sessionStorage.setItem('raa.workspaceId', 'ws-free');

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <WorkspaceProvider>
            <CreateProjectModal open={true} onClose={vi.fn()} defaultWorkspaceId="ws-free" />
          </WorkspaceProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const titleInput = screen.getByPlaceholderText(/e\.g\., Deep Learning/i);
    fireEvent.change(titleInput, { target: { value: 'Exceeding Project' } });

    const submitBtn = screen.getByRole('button', { name: /create project & launch/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/you've reached the project limit on the free plan/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /view plans/i })).toBeInTheDocument();
    });
  });
});
