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

    vi.spyOn(billingApi, 'transactions').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    // Verify workspace name in subtitle
    expect(await screen.findByText(/neuroscience lab/i)).toBeInTheDocument();

    // Verify usage metrics and plan cards
    await waitFor(() => {
      expect(screen.getByText('48')).toBeInTheDocument();
      expect(screen.getByText(/\/ 500 requests/i)).toBeInTheDocument();
      expect(screen.getByText('Pro Researcher')).toBeInTheDocument();
      expect(screen.getByText(/29/)).toBeInTheDocument();
    });

    // Verify Paystack TEST MODE badge
    expect(screen.getAllByText(/test mode/i).length).toBeGreaterThan(0);

    // Toggle to yearly billing
    const yearlyBtn = screen.getByRole('button', { name: /yearly/i });
    fireEvent.click(yearlyBtn);

    // Should update price to yearly
    expect(screen.getByText(/290/)).toBeInTheDocument();
  });

  it('renders "No payment transactions yet." when backend returns content: []', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([]);
    vi.spyOn(billingApi, 'subscription').mockResolvedValue({ planCode: 'FREE', status: 'ACTIVE' });
    vi.spyOn(billingApi, 'usage').mockResolvedValue({});
    vi.spyOn(billingApi, 'transactions').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    expect(await screen.findByText('No payment transactions yet.')).toBeInTheDocument();
    expect(screen.queryByText(/transactions\.map is not a function/i)).not.toBeInTheDocument();
  });

  it('renders real transaction records from backend PageResponse without throwing', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([]);
    vi.spyOn(billingApi, 'subscription').mockResolvedValue({ planCode: 'FREE', status: 'ACTIVE' });
    vi.spyOn(billingApi, 'usage').mockResolvedValue({});
    vi.spyOn(billingApi, 'transactions').mockResolvedValue({
      content: [
        {
          id: 'tx-001',
          workspaceId: 'ws-101',
          reference: 'pstk_test_ref_001',
          environment: 'TEST',
          status: 'SUCCESS',
          amount: 50,
          currency: 'GHS',
          planCode: 'PRO',
          billingInterval: 'MONTHLY',
          createdAt: '2026-09-19T10:00:00Z',
        },
        {
          id: 'tx-002',
          workspaceId: 'ws-101',
          reference: 'pstk_test_ref_002',
          environment: 'TEST',
          status: 'FAILED',
          amount: 50,
          currency: 'GHS',
          planCode: 'PRO',
          billingInterval: 'MONTHLY',
          createdAt: '2026-09-19T10:30:00Z',
          paymentIntentId: 'intent-002',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 2,
      totalPages: 1,
    });

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    expect(await screen.findByText('pstk_test_ref_001')).toBeInTheDocument();
    expect(screen.getByText('pstk_test_ref_002')).toBeInTheDocument();
    expect(screen.getByText('SUCCESS')).toBeInTheDocument();
    expect(screen.getByText('FAILED')).toBeInTheDocument();
    expect(screen.getAllByText('PRO').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/GHS 50/i).length).toBeGreaterThan(0);

    // Verify retry action button is rendered for failed transaction
    expect(screen.getByRole('button', { name: /pay again/i })).toBeInTheDocument();
  });

  it('renders pagination controls when totalPages > 1', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([]);
    vi.spyOn(billingApi, 'subscription').mockResolvedValue({ planCode: 'FREE', status: 'ACTIVE' });
    vi.spyOn(billingApi, 'usage').mockResolvedValue({});
    vi.spyOn(billingApi, 'transactions').mockResolvedValue({
      content: [
        {
          id: 'tx-001',
          workspaceId: 'ws-101',
          reference: 'pstk_page_1',
          environment: 'TEST',
          status: 'SUCCESS',
          amount: 50,
          currency: 'GHS',
          planCode: 'PRO',
          billingInterval: 'MONTHLY',
          createdAt: '2026-09-19T10:00:00Z',
        },
      ],
      page: 0,
      size: 1,
      totalElements: 25,
      totalPages: 3,
    });

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    expect(await screen.findByText(/page 1 of 3/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
  });

  it('renders loading and error states safely for transaction history', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValue([]);
    vi.spyOn(billingApi, 'subscription').mockResolvedValue({ planCode: 'FREE', status: 'ACTIVE' });
    vi.spyOn(billingApi, 'usage').mockResolvedValue({});
    vi.spyOn(billingApi, 'transactions').mockRejectedValue(new Error('Network error'));

    renderBillingPage({ id: 'ws-101', name: 'Neuroscience Lab' });

    expect(await screen.findByText(/unable to load transaction history/i)).toBeInTheDocument();
    expect(screen.queryByText(/transactions\.map is not a function/i)).not.toBeInTheDocument();
  });
});
