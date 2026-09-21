export function parseCanonicalReference(searchParams: URLSearchParams): string | null {
  const refParam = searchParams.get('reference');
  const trxrefParam = searchParams.get('trxref');

  let raw = refParam || trxrefParam;
  if (!raw) return null;

  raw = raw.trim();
  if (raw.includes(',')) {
    const parts = raw.split(',').map((p) => p.trim()).filter(Boolean);
    const unique = Array.from(new Set(parts));
    if (unique.length === 1) {
      return unique[0];
    }
    return null;
  }

  if (refParam && trxrefParam) {
    const cleanRef = refParam.trim();
    const cleanTrx = trxrefParam.trim();
    if (cleanRef !== cleanTrx && !cleanRef.includes(',') && !cleanTrx.includes(',')) {
      return null;
    }
  }

  return raw;
}

export function formatPaymentError(err: unknown): string {
  if (!err) return 'Unable to verify payment status.';
  const msg = err instanceof Error ? err.message : String(err);
  if (msg.includes('INVALID_PAYMENT_REFERENCE') || msg.includes(',')) {
    return 'Invalid payment reference format. Multiple or conflicting references are not supported.';
  }
  if (msg.includes('REFERENCE_MISSING')) {
    return 'No transaction reference was provided for verification.';
  }
  if (msg.includes('Payment attempt not found') || msg.includes('PAYMENT_ATTEMPT_NOT_FOUND')) {
    return 'Payment attempt not found. The transaction may have expired or was not completed.';
  }
  if (msg.includes('VERIFICATION_MISMATCH') || msg.includes('PAYMENT_AMOUNT_MISMATCH')) {
    return 'Payment verification failed: transaction details did not match expected amount or currency.';
  }
  if (msg.includes('PAYMENT_CURRENCY_MISMATCH')) {
    return 'Payment verification failed: currency mismatch.';
  }
  if (msg.includes('PAYMENT_ALREADY_PROCESSED')) {
    return 'This payment has already been verified and processed.';
  }
  return msg;
}
