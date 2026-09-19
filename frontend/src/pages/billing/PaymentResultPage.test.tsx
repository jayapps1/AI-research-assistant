import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { billingApi } from '../../api/endpoints';
import { PaymentResultPage } from './PaymentResultPage';
import type { PaymentAttemptDetail } from '../../types/api';

function renderPaymentResult(search = '?reference=PA_TEST_REF_123') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/app/billing/payment-result${search}`]}>
        <PaymentResultPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('PaymentResultPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders verified success state with plan upgrade details and dashboard navigation', async () => {
    const mockSuccess: PaymentAttemptDetail = {
      paymentAttemptId: 'att-1',
      paymentIntentId: 'intent-1',
      workspaceId: 'ws-1',
      planCode: 'PRO',
      planName: 'Pro Researcher',
      expectedAmount: 50.0,
      currency: 'GHS',
      attemptNumber: 1,
      status: 'SUCCESS',
      providerStatus: 'success',
      internalReference: 'PA_TEST_REF_123',
      providerReference: 'PAYSTACK_REF_999',
      retryable: false,
      createdAt: '2026-09-16T12:00:00Z',
      completedAt: '2026-09-16T12:01:00Z',
      providerVerifiedAt: '2026-09-16T12:01:00Z',
    };

    vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue(mockSuccess);

    renderPaymentResult('?reference=PA_TEST_REF_123');

    expect(await screen.findByRole('heading', { name: /subscription upgraded!/i })).toBeInTheDocument();
    expect(screen.getByText('Pro Researcher (PRO)')).toBeInTheDocument();
    expect(screen.getByText('GHS 50.00')).toBeInTheDocument();
    expect(screen.getByText('PA_TEST_REF_123')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /continue to dashboard/i })).toBeInTheDocument();
  });

  it('renders Mobile Money pending prompt with handset approval guidance', async () => {
    const mockPending: PaymentAttemptDetail = {
      paymentAttemptId: 'att-2',
      paymentIntentId: 'intent-2',
      workspaceId: 'ws-1',
      planCode: 'PRO',
      planName: 'Pro Researcher',
      expectedAmount: 50.0,
      currency: 'GHS',
      attemptNumber: 1,
      status: 'PENDING',
      providerStatus: 'pending',
      internalReference: 'PA_MOMO_REF_456',
      retryable: false,
      createdAt: '2026-09-16T12:00:00Z',
    };

    vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue(mockPending);

    renderPaymentResult('?reference=PA_MOMO_REF_456');

    expect(await screen.findByRole('heading', { name: /waiting for payment approval/i })).toBeInTheDocument();
    expect(screen.getByText(/handset prompt sent/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /i have approved on my phone/i })).toBeInTheDocument();
  });

  it('renders failed state with diagnostic failure reason and Pay Again retry action', async () => {
    const mockFailed: PaymentAttemptDetail = {
      paymentAttemptId: 'att-3',
      paymentIntentId: 'intent-3',
      workspaceId: 'ws-1',
      planCode: 'PRO',
      planName: 'Pro Researcher',
      expectedAmount: 50.0,
      currency: 'GHS',
      attemptNumber: 1,
      status: 'FAILED',
      providerStatus: 'failed',
      internalReference: 'PA_FAIL_REF_789',
      failureCode: 'insufficient_funds',
      failureMessageSafe: 'Subscriber balance is insufficient for this purchase.',
      retryable: true,
      createdAt: '2026-09-16T12:00:00Z',
    };

    vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue(mockFailed);
    const retrySpy = vi.spyOn(billingApi, 'retry').mockResolvedValue({
      paymentIntentId: 'intent-3',
      attemptId: 'att-4',
      attemptNumber: 2,
      authorizationUrl: 'https://checkout.paystack.com/test-url-2',
      internalReference: 'PA_RETRY_REF_002',
      status: 'PENDING',
    });

    renderPaymentResult('?reference=PA_FAIL_REF_789');

    expect(await screen.findByRole('heading', { name: /payment unsuccessful/i })).toBeInTheDocument();
    expect(screen.getByText(/reason: insufficient_funds/i)).toBeInTheDocument();
    expect(screen.getByText(/subscriber balance is insufficient/i)).toBeInTheDocument();

    const payAgainBtn = screen.getByRole('button', { name: /pay again/i });
    expect(payAgainBtn).toBeInTheDocument();

    fireEvent.click(payAgainBtn);

    await waitFor(() => {
      expect(retrySpy).toHaveBeenCalledWith('intent-3');
    });
  });
});
