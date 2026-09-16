import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { clearTokens, getRefreshToken, setTokens } from '../api/tokens';
import type { AuthTokenResponse, LoginChallengeResponse, LoginResponse, User } from '../types/api';
import { TotpChallengeRequired } from './errors';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface LoginInput {
  email: string;
  password?: string;
  totpCode?: string;
  authenticationMethod?: 'PASSWORD' | 'TOTP' | 'PASSWORD_AND_TOTP';
}

interface AuthContextValue {
  user: User | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  login: (input: LoginInput) => Promise<void>;
  completeTotpChallenge: (input: { challengeId: string; totpCode: string }) => Promise<void>;
  register: (input: { email: string; password: string; firstName?: string; lastName?: string; locale?: string }) => Promise<void>;
  logout: () => Promise<void>;
  logoutAll: () => Promise<void>;
  hasCapability: (capability: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => restoreUser());
  const [status, setStatus] = useState<AuthStatus>(() => (getRefreshToken() ? 'authenticated' : 'anonymous'));

  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      setStatus('anonymous');
    };
    window.addEventListener('raa:session-expired', onExpired);
    return () => window.removeEventListener('raa:session-expired', onExpired);
  }, []);

  const persistUser = useCallback((nextUser: User | null) => {
    setUser(nextUser);
    if (nextUser) sessionStorage.setItem('raa.user', JSON.stringify(nextUser));
    else sessionStorage.removeItem('raa.user');
  }, []);

  const login = useCallback(
    async (input: LoginInput) => {
      const response = await authApi.login({
        email: input.email,
        password: input.password,
        totpCode: input.totpCode,
        authenticationMethod: input.authenticationMethod ?? 'PASSWORD',
      });
      if (isLoginChallenge(response)) {
        throw new TotpChallengeRequired(response.challengeId, response.expiresIn);
      }
      const tokenResponse = response as AuthTokenResponse;
      setTokens({ accessToken: tokenResponse.accessToken, refreshToken: tokenResponse.refreshToken });
      persistUser(tokenResponse.user ?? { id: input.email, email: input.email, roles: [] });
      setStatus('authenticated');
    },
    [persistUser],
  );

  const completeTotpChallenge = useCallback(
    async (input: { challengeId: string; totpCode: string }) => {
      const response = await authApi.completeTotpLoginChallenge(input);
      setTokens({ accessToken: response.accessToken, refreshToken: response.refreshToken });
      persistUser(response.user ?? null);
      setStatus('authenticated');
    },
    [persistUser],
  );

  const register = useCallback(
    async (input: { email: string; password: string; firstName?: string; lastName?: string; locale?: string }) => {
      await authApi.register(input);
      await login({ email: input.email, password: input.password });
    },
    [login],
  );

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    clearTokens();
    persistUser(null);
    setStatus('anonymous');
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined);
    }
  }, [persistUser]);

  const logoutAll = useCallback(async () => {
    await authApi.logoutAll().catch(() => undefined);
    clearTokens();
    persistUser(null);
    setStatus('anonymous');
  }, [persistUser]);

  const hasCapability = useCallback(
    (capability: string) => {
      const roles = [...(user?.roles ?? []), ...(user?.systemRoles ?? [])].map((role) => role.toUpperCase());
      if (capability === 'admin') return roles.includes('SYSTEM_ADMIN') || roles.includes('ADMIN');
      return roles.includes(capability.toUpperCase());
    },
    [user],
  );

  const value = useMemo(
    () => ({ user, status, isAuthenticated: status === 'authenticated', login, completeTotpChallenge, register, logout, logoutAll, hasCapability }),
    [completeTotpChallenge, hasCapability, login, logout, logoutAll, register, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

function isLoginChallenge(response: LoginResponse): response is LoginChallengeResponse {
  return 'challengeId' in response && response.status === 'TOTP_REQUIRED';
}

function restoreUser() {
  try {
    const raw = sessionStorage.getItem('raa.user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}
