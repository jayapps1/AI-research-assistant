import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminApi } from '../../api/endpoints';
import { AdminPlansPage } from './AdminPlansPage';
import type { AdminSubscriptionPlan } from '../../types/api';

const mockPlans: AdminSubscriptionPlan[] = [
  {
    id: 'plan-free-id',
    code: 'FREE',
    name: 'Free Starter',
    description: 'Basic free research quota',
    status: 'ACTIVE',
    billingInterval: 'NONE',
    price: 0,
    currency: 'GHS',
    publiclyAvailable: true,
    featured: false,
    displayOrder: 1,
    workspacesSubscribed: 42,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'plan-pro-id',
    code: 'PRO',
    name: 'Pro Researcher',
    description: 'Unlimited synthesis & empirical tooling',
    status: 'ACTIVE',
    billingInterval: 'MONTHLY',
    price: 50.0,
    currency: 'GHS',
    publiclyAvailable: true,
    featured: true,
    displayOrder: 2,
    workspacesSubscribed: 15,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

function renderAdminPlans() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminPlansPage />
    </QueryClientProvider>,
  );
}

describe('AdminPlansPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders subscription plans with codes, pricing, badges, and subscriber counts', async () => {
    vi.spyOn(adminApi, 'plans').mockResolvedValue(mockPlans);

    renderAdminPlans();

    expect(await screen.findByText('FREE')).toBeInTheDocument();
    expect(screen.getByText('Free Starter')).toBeInTheDocument();
    expect(screen.getByText('PRO')).toBeInTheDocument();
    expect(screen.getByText('Pro Researcher')).toBeInTheDocument();
    expect(screen.getByText('GHS 50.00')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('15')).toBeInTheDocument();
  });

  it('opens Create Plan modal and submits new plan to backend', async () => {
    vi.spyOn(adminApi, 'plans').mockResolvedValue(mockPlans);
    const createSpy = vi.spyOn(adminApi, 'createPlan').mockResolvedValue({
      id: 'plan-enterprise-id',
      code: 'ENTERPRISE',
      name: 'Enterprise Lab',
      description: 'Dedicated lab tier',
      status: 'ACTIVE',
      billingInterval: 'YEARLY',
      price: 500,
      currency: 'USD',
      publiclyAvailable: false,
      featured: false,
      displayOrder: 3,
      workspacesSubscribed: 0,
      createdAt: '2026-09-16T12:00:00Z',
      updatedAt: '2026-09-16T12:00:00Z',
    });

    renderAdminPlans();

    const createBtn = await screen.findByRole('button', { name: /create plan/i });
    fireEvent.click(createBtn);

    // Modal opens
    expect(screen.getByRole('heading', { name: /create subscription plan/i })).toBeInTheDocument();

    const codeInput = screen.getByPlaceholderText('PRO');
    const nameInput = screen.getByPlaceholderText('Pro Researcher');
    fireEvent.change(codeInput, { target: { value: 'ENTERPRISE' } });
    fireEvent.change(nameInput, { target: { value: 'Enterprise Lab' } });

    const submitBtns = screen.getAllByRole('button', { name: /create plan/i });
    fireEvent.click(submitBtns[submitBtns.length - 1]);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          code: 'ENTERPRISE',
          name: 'Enterprise Lab',
        }),
      );
    });
  });

  it('opens Entitlements modal and saves configured limits', async () => {
    vi.spyOn(adminApi, 'plans').mockResolvedValue(mockPlans);
    vi.spyOn(adminApi, 'getPlanEntitlements').mockResolvedValue([
      {
        feature: 'PROJECTS',
        enabled: true,
        limitMode: 'LIMITED',
        limitValue: 5,
        limitUnit: 'COUNT',
      },
      {
        feature: 'STORAGE',
        enabled: true,
        limitMode: 'LIMITED',
        limitValue: 5368709120, // 5 GB
        limitUnit: 'BYTES',
      },
    ]);
    const updateEntitlementsSpy = vi.spyOn(adminApi, 'updatePlanEntitlements').mockResolvedValue([]);

    renderAdminPlans();

    const entitlementBtns = await screen.findAllByRole('button', { name: /entitlements/i });
    fireEvent.click(entitlementBtns[1]); // Click for PRO plan

    expect(await screen.findByText(/entitlements for pro researcher/i)).toBeInTheDocument();

    const saveBtn = screen.getByRole('button', { name: /save entitlements/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(updateEntitlementsSpy).toHaveBeenCalledWith(
        'plan-pro-id',
        expect.any(Array),
      );
    });
  });
});
