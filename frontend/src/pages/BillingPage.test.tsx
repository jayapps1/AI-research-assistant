import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { billingApi, workspaceApi } from '../api/endpoints';
import { publicApi } from '../api/public';
import { clearTokens, setTokens } from '../api/tokens';
import { WorkspaceProvider } from '../features/workspaces/WorkspaceProvider';
import { BillingPage } from './BillingPage';

function renderBillingPage(workspace: { id: string; name: string } | null) {
  clearTokens();
  sessionStorage.clear();
  setTokens({ accessToken: 'test-access-token', refreshToken: 'test-refresh-token' });

  if (workspace) {
    sessionStorage.setItem('raa.workspaceId', workspace.id);
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([workspace as never]);
  } else {
    vi.spyOn(workspaceApi, 'list').mockResolvedValue([]);
  }

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <WorkspaceProvider>
          <BillingPage />
        </WorkspaceProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('BillingPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders clean empty state when no workspace is selected', async () => {
    renderBillingPage(null);

    expect(await screen.findByRole('heading', { name: /select a workspace to manage billing/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /select workspace/i })).toBeInTheDocument();
  });

  it('loads dynamic pricing tiers from public API and displays usage quotas', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([
      {
        id: 'tier-pro',
        code: 'PRO_RESEARCHER',
        name: 'Pro Researcher',
        highlightBadge: 'Most Popular',
        description: 'Advanced academic synthesis',
        monthlyPrice: 29,
        annualPrice: 290,
        currency: '$',
        highlighted: true,
        features: ['500 AI queries/month', 'Full exports'],
        displayOrder: 1,
      },
    ]);

    vi.spyOn(billingApi, 'subscription').mockResolvedValue({
      planCode: 'FREE',
      status: 'ACTIVE',
      billingInterval: 'MONTHLY',
    });

    vi.spyOn(billingApi, 'usage').mockResolvedValue({
      aiRequests: { used: 48, limit: 500 },
      aiTokens: { used: 125000, limit: 1000000 },
      projects: { used: 3, limit: 10 },
      storage: { used: 120, limit: 1000 },
    });

    vi.spyOn(billingApi, 'transactions').mockResolvedValue([]);

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    // Verify workspace name in subtitle
    expect(await screen.findByText(/neuroscience lab/i)).toBeInTheDocument();

    // Verify usage metrics and plan cards
    await waitFor(() => {
      expect(screen.getByText('48')).toBeInTheDocument();
      expect(screen.getByText(/\/ 500 requests/i)).toBeInTheDocument();
      expect(screen.getByText('Pro Researcher')).toBeInTheDocument();
      expect(screen.getByText('29')).toBeInTheDocument();
    });

    // Verify Paystack TEST MODE badge
    expect(screen.getAllByText(/test mode/i).length).toBeGreaterThan(0);

    // Toggle to yearly billing
    const yearlyBtn = screen.getByRole('button', { name: /yearly/i });
    fireEvent.click(yearlyBtn);

    // Should update price to yearly
    expect(screen.getByText('290')).toBeInTheDocument();
  });
});
