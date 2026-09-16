import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { config } from '../app/config';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './tokens';
import type { ApiClientError, ApiErrorResponse, AuthTokenResponse } from '../types/api';

export const api = axios.create({
  baseURL: `${config.apiBaseUrl.replace(/\/$/, '')}/api/v1`,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let refreshPromise: Promise<string | null> | null = null;

api.interceptors.request.use((request) => {
  const token = getAccessToken();
  request.headers.set('X-Request-ID', crypto.randomUUID());
  if (token) request.headers.set('Authorization', `Bearer ${token}`);
  return request;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retry && getRefreshToken()) {
      original._retry = true;
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const token = await refreshPromise;
      if (token) {
        original.headers.set('Authorization', `Bearer ${token}`);
        return api(original);
      }
    }
    throw mapApiError(error);
  },
);

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  try {
    const response = await axios.post<AuthTokenResponse>(
      `${config.apiBaseUrl.replace(/\/$/, '')}/api/v1/auth/refresh`,
      { refreshToken },
      { timeout: 15_000 },
    );
    setTokens({ accessToken: response.data.accessToken, refreshToken: response.data.refreshToken });
    return response.data.accessToken;
  } catch {
    clearTokens();
    window.dispatchEvent(new Event('raa:session-expired'));
    return null;
  }
}

export function mapApiError(error: AxiosError<ApiErrorResponse>): ApiClientError {
  const payload = error.response?.data;
  const status = error.response?.status;
  const message = friendlyMessage(status, payload);
  const mapped = new Error(message) as ApiClientError;
  mapped.status = status;
  mapped.code = payload?.errorCode;
  mapped.requestId = payload?.requestId ?? error.response?.headers['x-request-id'];
  mapped.validationErrors = payload?.validationErrors;
  mapped.metadata = payload?.validationErrors;
  return mapped;
}

function friendlyMessage(status?: number, payload?: ApiErrorResponse) {
  const code = payload?.errorCode;
  const rawMessage = payload?.message ?? payload?.detail;
  const isStackTrace = rawMessage && /stack trace|exception|\bat\b\s+[a-z0-9_$.]+|\bjava\./i.test(rawMessage);
  const message = isStackTrace ? undefined : rawMessage;

  if (status === 401) {
    if (
      message === 'Invalid credentials.' ||
      message?.toLowerCase().includes('inactive') ||
      message?.toLowerCase().includes('suspended') ||
      message?.toLowerCase().includes('authenticator')
    ) {
      return message;
    }
    return 'Your session has expired. Sign in again to continue.';
  }
  if (status === 403) return message ?? 'You do not have permission to perform this action.';
  if (status === 404) return message ?? 'The requested item could not be found.';
  if (status === 409 && message?.includes('expectedVersion')) return 'This item has been changed by another collaborator.';
  if (status === 409) return message ?? 'The request conflicts with the current server state.';
  if (status === 400 || status === 422) return message ?? 'Please review the highlighted fields.';
  if (status === 429 || code === 'TOO_MANY_REQUESTS') return message === 'QUOTA_EXCEEDED' ? 'Quota exceeded for this feature.' : 'Too many requests. Try again shortly.';
  if (status === 503) return message ?? 'This service is temporarily unavailable.';
  return message ?? 'Something went wrong. Please try again.';
}

export function isConflictError(error: unknown) {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === 409;
}
