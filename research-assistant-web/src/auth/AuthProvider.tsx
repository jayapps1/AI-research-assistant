import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { clearTokens, getRefreshToken, setTokens } from '../api/tokens';
import type { User } from '../types/api';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface LoginInput {
  email: string;
  password: string;
  totpCode?: string;
  totpChallengeToken?: string;
}

interface AuthContextValue {
  user: User | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  login: (input: LoginInput) => Promise<void>;
  register: (input: { email: string; password: string; fullName?: string }) => Promise<void>;
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
      const response = await authApi.login(input);
      if (response.requiresTotp && !response.accessToken) {
        throw new Error('TOTP verification is required for this account.');
      }
      setTokens({ accessToken: response.accessToken, refreshToken: response.refreshToken });
      persistUser(response.user ?? { id: input.email, email: input.email });
      setStatus('authenticated');
    },
    [persistUser],
  );

  const register = useCallback(
    async (input: { email: string; password: string; fullName?: string }) => {
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
    () => ({ user, status, isAuthenticated: status === 'authenticated', login, register, logout, logoutAll, hasCapability }),
    [hasCapability, login, logout, logoutAll, register, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

function restoreUser() {
  try {
    const raw = sessionStorage.getItem('raa.user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}
