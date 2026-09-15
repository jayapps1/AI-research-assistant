import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthProvider';
import { PageLoading, PermissionDenied } from '../components/states';

export function ProtectedRoute() {
  const auth = useAuth();
  const location = useLocation();
  if (auth.status === 'loading') return <PageLoading label="Checking your session" />;
  if (!auth.isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}

export function AdminRoute() {
  const auth = useAuth();
  if (!auth.hasCapability('admin')) return <PermissionDenied />;
  return <Outlet />;
}
