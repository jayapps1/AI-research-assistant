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

    expect(await screen.findByRole('heading', { name: /payment successful/i })).toBeInTheDocument();
    expect(screen.getByText('Pro Researcher (PRO)')).toBeInTheDocument();
    expect(screen.getByText('GHS 50.00')).toBeInTheDocument();
    expect(screen.getByText('PA_TEST_REF_123')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /continue to billing/i })).toBeInTheDocument();
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

  it('passes single canonical reference when URL has ?reference=RA-123', async () => {
    const verifySpy = vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue({
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
      internalReference: 'RA-123',
      retryable: false,
      createdAt: '2026-09-16T12:00:00Z',
    });

    renderPaymentResult('?reference=RA-123');

    await waitFor(() => {
      expect(verifySpy).toHaveBeenCalledWith('RA-123');
    });
  });

  it('passes single canonical reference when URL has both reference and trxref', async () => {
    const verifySpy = vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue({
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
      internalReference: 'RA-123',
      retryable: false,
      createdAt: '2026-09-16T12:00:00Z',
    });

    renderPaymentResult('?reference=RA-123&trxref=RA-123');

    await waitFor(() => {
      expect(verifySpy).toHaveBeenCalledWith('RA-123');
      expect(verifySpy).not.toHaveBeenCalledWith('RA-123,RA-123');
    });
  });

  it('normalizes comma-duplicated reference from URL to single canonical reference', async () => {
    const verifySpy = vi.spyOn(billingApi, 'verifyAttemptByReference').mockResolvedValue({
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
      internalReference: 'RA-123',
      retryable: false,
      createdAt: '2026-09-16T12:00:00Z',
    });

    renderPaymentResult('?reference=RA-123,RA-123');

    await waitFor(() => {
      expect(verifySpy).toHaveBeenCalledWith('RA-123');
      expect(verifySpy).not.toHaveBeenCalledWith('RA-123,RA-123');
    });
  });

  it('reuses the same single canonical reference across repeated Retry clicks', async () => {
    const verifySpy = vi.spyOn(billingApi, 'verifyAttemptByReference')
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce({
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
        internalReference: 'RA-123',
        retryable: false,
        createdAt: '2026-09-16T12:00:00Z',
      });

    renderPaymentResult('?reference=RA-123,RA-123');

    // First attempt fails, shows error and retry button
    expect(await screen.findByRole('heading', { name: /verification notice/i })).toBeInTheDocument();
    expect(verifySpy).toHaveBeenCalledWith('RA-123');

    // Click Retry Verification
    const retryBtn = screen.getByRole('button', { name: /retry verification/i });
    fireEvent.click(retryBtn);

    // Second call must still receive exactly "RA-123"
    await waitFor(() => {
      expect(verifySpy).toHaveBeenLastCalledWith('RA-123');
      expect(verifySpy).toHaveBeenCalledTimes(2);
    });
  });
});
