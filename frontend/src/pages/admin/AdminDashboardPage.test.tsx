import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminApi } from '../../api/endpoints';
import { AdminDashboardPage } from './AdminDashboardPage';

function renderAdminDashboard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <QueryClientProvider client={queryClient}>
      <AdminDashboardPage />
    </QueryClientProvider>,
  );
}

describe('AdminDashboardPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders backend aggregate metrics and system health indicators', async () => {
    vi.spyOn(adminApi, 'dashboard').mockResolvedValue({
      totalUsers: 42,
      activeUsers: 42,
      workspaces: 15,
      researchProjects: 88,
      activeSubscriptions: 12,
      aiRequestsToday: 134,
      aiRequestsThisMonth: 2890,
      aiTokensTotal: 4500000,
      aiFailures: 0,
      documentsCount: 310,
      datasetsCount: 45,
      testPayments: 8,
      successfulPayments: 24,
      failedPayments: 1,
      pendingPayments: 2,
      systemHealth: {
        database: 'Healthy',
        aiGeneration: 'Healthy',
        paystack: 'TEST',
      },
    });

    vi.spyOn(adminApi, 'operations').mockResolvedValue({
      healthStatus: 'HEALTHY',
      activeJobs: 2,
    });

    vi.spyOn(adminApi, 'complimentaryAccess').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });

    renderAdminDashboard();

    // Verify key headline metric cards
    await waitFor(() => {
      expect(screen.getAllByText('42')[0]).toBeInTheDocument();
      expect(screen.getByText('15')).toBeInTheDocument();
      expect(screen.getByText('88')).toBeInTheDocument();
      expect(screen.getByText(/134 requests today/i)).toBeInTheDocument();
    });

    // Verify Paystack TEST MODE badge
    expect(screen.getByText('PAYSTACK TEST MODE')).toBeInTheDocument();

    // Verify System Health indicators
    expect(screen.getByText(/database/i)).toBeInTheDocument();
    expect(screen.getAllByText(/paystack/i).length).toBeGreaterThan(0);
  });

  it('opens grant complimentary access modal and submits form', async () => {
    vi.spyOn(adminApi, 'dashboard').mockResolvedValue({
      totalUsers: 5,
      activeUsers: 5,
      workspaces: 2,
      researchProjects: 4,
      activeSubscriptions: 1,
      aiRequestsToday: 10,
      aiRequestsThisMonth: 100,
      aiTokensTotal: 50000,
      aiFailures: 0,
      documentsCount: 12,
      datasetsCount: 2,
      testPayments: 0,
      successfulPayments: 1,
      failedPayments: 0,
      pendingPayments: 0,
      systemHealth: {
        database: 'Healthy',
        paystack: 'TEST',
      },
    });

    vi.spyOn(adminApi, 'operations').mockResolvedValue({});
    vi.spyOn(adminApi, 'complimentaryAccess').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });
    const grantSpy = vi.spyOn(adminApi, 'grantComplimentaryAccess').mockResolvedValue({
      id: 'grant-123',
    });

    renderAdminDashboard();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /grant complimentary access/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /grant complimentary access/i }));

    // Verify modal appeared
    expect(screen.getByRole('heading', { name: /grant complimentary access/i })).toBeInTheDocument();

    // Fill form
    const targetInput = screen.getByPlaceholderText(/123e4567-e89b-12d3-a456-426614174000/i);
    fireEvent.change(targetInput, { target: { value: 'ws-test-456' } });

    const reasonInput = screen.getByPlaceholderText(/core development and empirical workflow validation/i);
    fireEvent.change(reasonInput, { target: { value: 'Academic partnership grant' } });

    // Click confirm
    const submitBtn = screen.getByRole('button', { name: /confirm grant/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(grantSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          workspaceId: 'ws-test-456',
          planCode: 'PRO',
          reason: 'Academic partnership grant',
        }),
      );
    });
  });
});
