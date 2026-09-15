import { describe, expect, it } from 'vitest';
import type { AxiosError } from 'axios';
import { mapApiError } from './client';
import type { ApiErrorResponse } from '../types/api';

describe('mapApiError', () => {
  it('maps quota and validation backend errors to user-safe messages', () => {
    const error = {
      response: {
        status: 429,
        data: {
          status: 429,
          errorCode: 'TOO_MANY_REQUESTS',
          message: 'QUOTA_EXCEEDED',
          validationErrors: { feature: 'AI_REQUESTS' },
        } satisfies ApiErrorResponse,
        headers: {},
      },
    } as unknown as AxiosError<ApiErrorResponse>;

    const mapped = mapApiError(error);

    expect(mapped.message).toBe('Quota exceeded for this feature.');
    expect(mapped.status).toBe(429);
    expect(mapped.validationErrors?.feature).toBe('AI_REQUESTS');
  });

  it('does not expose raw stack traces for session expiry', () => {
    const error = { response: { status: 401, data: { status: 401, message: 'stack trace' }, headers: {} } } as unknown as AxiosError<ApiErrorResponse>;
    expect(mapApiError(error).message).toBe('Your session has expired. Sign in again to continue.');
  });
});
