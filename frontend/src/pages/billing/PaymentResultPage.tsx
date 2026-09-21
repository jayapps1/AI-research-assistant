import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import {
  CheckCircle2,
  XCircle,
  RefreshCw,
  ArrowRight,
  Smartphone,
  AlertCircle,
} from 'lucide-react';
import { billingApi } from '../../api/endpoints';
import { Button, Card, LoadingButton } from '../../components/ui';
import type { PaymentAttemptDetail } from '../../types/api';
import { parseCanonicalReference, formatPaymentError } from './paymentResultUtils';

export function PaymentResultPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const rawReference = searchParams.get('reference');
  const reference = useMemo(() => parseCanonicalReference(searchParams), [searchParams]);

  // Ensure clean URL without duplicate or conflicting params
  useEffect(() => {
    if (reference && (searchParams.get('reference') !== reference || searchParams.has('trxref'))) {
      setSearchParams({ reference }, { replace: true });
    }
  }, [reference, searchParams, setSearchParams]);

  const [loading, setLoading] = useState(Boolean(reference));
  const [attempt, setAttempt] = useState<PaymentAttemptDetail | null>(null);
  const [error, setError] = useState<string | null>(
    reference
      ? null
      : rawReference?.includes(',')
      ? 'Invalid payment reference: multiple or conflicting references found in URL.'
      : 'No transaction reference found in URL.'
  );
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);
  const [pollCount, setPollCount] = useState(0);

  const invalidateBillingCaches = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['billing'] });
    queryClient.invalidateQueries({ queryKey: ['workspace'] });
    queryClient.invalidateQueries({ queryKey: ['subscription'] });
    queryClient.invalidateQueries({ queryKey: ['credits'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  }, [queryClient]);

  useEffect(() => {
    if (!reference) return;
    let active = true;

    billingApi
      .verifyAttemptByReference(reference)
      .then((data) => {
        if (active) {
          setAttempt(data);
          setLoading(false);
          if (data.status === 'SUCCESS' || data.status === 'SUCCEEDED') {
            invalidateBillingCaches();
          }
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setError(formatPaymentError(err));
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [reference, invalidateBillingCaches]);

  const handleRetry = async () => {
    if (!reference) return;
    setLoading(true);
    setError(null);
    try {
      const data = await billingApi.verifyAttemptByReference(reference);
      setAttempt(data);
      if (data.status === 'SUCCESS' || data.status === 'SUCCEEDED') {
        invalidateBillingCaches();
      }
    } catch (err: unknown) {
      setError(formatPaymentError(err));
    } finally {
      setLoading(false);
    }
  };

  // Polling for MoMo pending state
  useEffect(() => {
    if (attempt && (attempt.status === 'PENDING' || attempt.status === 'CREATED') && pollCount < 10) {
      const timer = setTimeout(() => {
        setPollCount((prev) => prev + 1);
        if (reference) {
          billingApi
            .verifyAttemptByReference(reference)
            .then((data) => {
              setAttempt(data);
              if (data.status === 'SUCCESS' || data.status === 'SUCCEEDED') {
                invalidateBillingCaches();
              }
            })
            .catch(() => {});
        }
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [attempt, pollCount, reference, invalidateBillingCaches]);

  const handlePayAgain = async () => {
    if (!attempt?.paymentIntentId) return;
    try {
      setRetrying(true);
      setRetryError(null);
      const res = await billingApi.retry(attempt.paymentIntentId);
      if (res.authorizationUrl) {
        window.location.href = res.authorizationUrl;
      } else {
        setRetryError('Unable to generate a new checkout URL. Please return to billing.');
      }
    } catch (err: unknown) {
      setRetryError(err instanceof Error ? err.message : 'Failed to initiate payment retry.');
    } finally {
      setRetrying(false);
    }
  };

  const isSuccess = attempt?.status === 'SUCCESS' || attempt?.status === 'SUCCEEDED';
  const isPending = attempt?.status === 'PENDING' || attempt?.status === 'CREATED';
  const isFailed = attempt?.status === 'FAILED' || attempt?.status === 'CANCELLED';

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '75vh',
        padding: '2rem 1rem',
      }}
    >
      <Card
        style={{
          width: '100%',
          maxWidth: '560px',
          padding: '2.5rem 2rem',
          textAlign: 'center',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)',
        }}
      >
        {/* Loading Initial State */}
        {loading && !attempt && (
          <div style={{ padding: '2rem 0' }}>
            <RefreshCw className="w-10 h-10 animate-spin text-primary" style={{ width: '40px', height: '40px', margin: '0 auto 1rem', animation: 'spin 1s linear infinite' }} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: 600, margin: '0 0 0.5rem' }}>Verifying Payment Status...</h2>
            <p className="text-sm muted" style={{ margin: 0 }}>
              Communicating with Paystack to confirm transaction <code>{reference}</code>.
            </p>
          </div>
        )}

        {/* Error fetching status */}
        {!loading && error && (
          <div style={{ padding: '1rem 0' }}>
            <AlertCircle className="w-12 h-12 text-danger" style={{ width: '48px', height: '48px', margin: '0 auto 1rem', color: '#ef4444' }} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.5rem' }}>Verification Notice</h2>
            <p className="text-sm muted" style={{ marginBottom: '1.5rem' }}>{error}</p>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <Button variant="secondary" onClick={handleRetry}>
                <RefreshCw className="w-4 h-4 mr-1 inline" /> Retry Verification
              </Button>
              <Button variant="primary" onClick={() => navigate('/app/billing')}>
                Return to Billing
              </Button>
            </div>
          </div>
        )}

        {/* 1. SUCCESS STATE */}
        {isSuccess && attempt && (
          <div>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                backgroundColor: '#064e3b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <CheckCircle2 className="w-10 h-10 text-emerald-400" style={{ width: '40px', height: '40px', color: '#34d399' }} />
            </div>

            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, margin: '0 0 0.5rem' }}>
              Payment Successful
            </h1>
            <p className="text-sm muted" style={{ marginBottom: '1.5rem' }}>
              Your {attempt.planName || attempt.planCode} plan is now active.
            </p>

            {/* Receipt Summary Box */}
            <div
              style={{
                backgroundColor: 'var(--card-subtle, #181818)',
                borderRadius: '8px',
                padding: '1.25rem',
                textAlign: 'left',
                marginBottom: '1.75rem',
                border: '1px solid var(--border, #2a2a2a)',
                fontSize: '0.9rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span className="muted">Plan:</span>
                <span style={{ fontWeight: 600 }}>{attempt.planName} ({attempt.planCode})</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span className="muted">Amount:</span>
                <span style={{ fontWeight: 600 }}>{attempt.currency} {Number(attempt.expectedAmount).toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span className="muted">Reference:</span>
                <code style={{ fontSize: '0.8rem' }}>{attempt.internalReference}</code>
              </div>
              {attempt.providerReference && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="muted">Paystack Ref:</span>
                  <code style={{ fontSize: '0.8rem' }}>{attempt.providerReference}</code>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <Button variant="secondary" onClick={() => navigate('/app/billing')}>
                View Billing Details
              </Button>
              <Button variant="primary" onClick={() => navigate('/app/billing')}>
                Continue to Billing <ArrowRight className="w-4 h-4 ml-1 inline" />
              </Button>
            </div>
          </div>
        )}


        {/* 2. PENDING / MOBILE MONEY STATE */}
        {isPending && attempt && (
          <div>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                backgroundColor: '#3b2505',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <Smartphone className="w-8 h-8 text-amber-400" style={{ width: '32px', height: '32px', color: '#fbbf24' }} />
            </div>

            <h1 style={{ fontSize: '1.4rem', fontWeight: 700, margin: '0 0 0.5rem' }}>
              Waiting for Payment Approval
            </h1>
            <div
              style={{
                backgroundColor: '#2b1b05',
                border: '1px solid #78350f',
                borderRadius: '6px',
                padding: '1rem',
                color: '#fef3c7',
                marginBottom: '1.5rem',
                fontSize: '0.9rem',
              }}
            >
              <strong>Handset Prompt Sent:</strong> If you paid via Mobile Money, please approve the payment prompt on your phone (enter your MoMo PIN).
            </div>

            <p className="text-xs muted" style={{ marginBottom: '1.5rem' }}>
              Checking automatically... (Attempt {pollCount + 1}/10)
            </p>

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <Button variant="primary" onClick={handleRetry} disabled={loading}>
                <RefreshCw className="w-4 h-4 mr-1 inline" />
                I Have Approved on My Phone
              </Button>
              <Button variant="secondary" onClick={() => navigate('/app/billing')}>
                Back to Billing
              </Button>
            </div>
          </div>
        )}

        {/* 3. FAILED STATE */}
        {isFailed && attempt && (
          <div>
            <div
              style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                backgroundColor: '#450a0a',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1.25rem',
              }}
            >
              <XCircle className="w-10 h-10 text-red-500" style={{ width: '40px', height: '40px', color: '#ef4444' }} />
            </div>

            <h1 style={{ fontSize: '1.4rem', fontWeight: 700, margin: '0 0 0.5rem' }}>
              Payment Unsuccessful
            </h1>
            <p className="text-sm muted" style={{ marginBottom: '1.25rem' }}>
              The payment could not be completed. No subscription change has been made to your workspace.
            </p>

            {/* Diagnostic Box */}
            <div
              style={{
                backgroundColor: 'var(--card-subtle, #181818)',
                borderRadius: '8px',
                padding: '1rem',
                textAlign: 'left',
                marginBottom: '1.5rem',
                border: '1px solid #3b1d1d',
                fontSize: '0.85rem',
              }}
            >
              <div style={{ color: '#f87171', fontWeight: 600, marginBottom: '0.25rem' }}>
                Reason: {attempt.failureCode || 'Transaction Not Completed'}
              </div>
              <div className="muted">
                {attempt.failureMessageSafe || 'The payment was declined or timed out. You can retry safely.'}
              </div>
            </div>

            {retryError && (
              <div className="badge danger" style={{ display: 'block', marginBottom: '1rem', padding: '0.5rem' }}>
                {retryError}
              </div>
            )}

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <LoadingButton
                variant="primary"
                loading={retrying}
                onClick={handlePayAgain}
              >
                <RefreshCw className="w-4 h-4 mr-1 inline" />
                Pay Again
              </LoadingButton>
              <Button variant="secondary" onClick={() => navigate('/app/billing')}>
                Return to Billing
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
