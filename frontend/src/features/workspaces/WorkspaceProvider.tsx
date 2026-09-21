import { createContext, useContext, useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../../api/endpoints';
import { useOptionalAuth } from '../../auth/AuthProvider';
import type { Workspace, User } from '../../types/api';

interface WorkspaceContextValue {
  workspaces: Workspace[];
  authorizedWorkspaces: Workspace[];
  selectedWorkspace: Workspace | null;
  selectedWorkspaceId: string;
  setSelectedWorkspaceId: (workspaceId: string) => void;
  setSelectedWorkspace: (workspace: Workspace | string) => void;
  isLoading: boolean;
  loading: boolean;
  error: unknown;
  refetchWorkspaces: () => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

function getSessionUser(): User | null {
  try {
    const raw = sessionStorage.getItem('raa.user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

function getSavedWorkspaceId(userId?: string): string {
  if (userId) {
    const namespaced = sessionStorage.getItem(`selectedWorkspace:${userId}`);
    if (namespaced) return namespaced;
  }
  return sessionStorage.getItem('raa.workspaceId') ?? '';
}

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const auth = useOptionalAuth();
  const user = auth?.user ?? getSessionUser();
  const isAuthenticated = auth !== null ? auth.isAuthenticated : true;
  const userId = user?.id;

  const [explicitWorkspaceId, setExplicitWorkspaceId] = useState<string>(() => getSavedWorkspaceId(userId));
  const [prevUserId, setPrevUserId] = useState(userId);

  // Keep explicitWorkspaceId aligned whenever authenticated user changes
  if (userId !== prevUserId) {
    setPrevUserId(userId);
    setExplicitWorkspaceId(userId && isAuthenticated ? getSavedWorkspaceId(userId) : '');
  }

  const query = useQuery({
    queryKey: ['workspaces'],
    queryFn: workspaceApi.list,
    enabled: isAuthenticated,
  });

  const workspaces = useMemo(() => query.data ?? [], [query.data]);

  const ensurePersonalMutation = useMutation({
    mutationFn: workspaceApi.ensurePersonal,
    onSuccess: (personalWorkspace) => {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (old = []) => {
        const exists = old.some((w) => w.id === personalWorkspace.id);
        return exists ? old : [personalWorkspace, ...old];
      });
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      if (personalWorkspace?.id && userId) {
        setExplicitWorkspaceId(personalWorkspace.id);
        sessionStorage.setItem(`selectedWorkspace:${userId}`, personalWorkspace.id);
      }
    },
  });

  useEffect(() => {
    if (isAuthenticated && query.isSuccess && workspaces.length === 0 && !ensurePersonalMutation.isPending) {
      ensurePersonalMutation.mutate();
    }
  }, [isAuthenticated, query.isSuccess, workspaces.length, ensurePersonalMutation]);

  // Initial & canonical workspace selection resolution
  const selectedWorkspace = useMemo<Workspace | null>(() => {
    if (workspaces.length === 0) {
      return null;
    }

    // 1. If saved/current selected workspace ID exists and user is authorized, use it
    if (explicitWorkspaceId) {
      const match = workspaces.find((w) => w.id === explicitWorkspaceId);
      if (match) return match;
    }

    // 2. Else if exactly one workspace: select it
    if (workspaces.length === 1) {
      return workspaces[0];
    }

    // 3. Else if personal workspace exists: select personal workspace
    const personal = workspaces.find((w) => w.type === 'PERSONAL');
    if (personal) {
      return personal;
    }

    // 4. Else: select first authorized workspace
    return workspaces[0] ?? null;
  }, [workspaces, explicitWorkspaceId]);

  // Selected workspace ID is strictly the canonical selected workspace's ID, or empty string.
  // Never expose an unvalidated or stale UUID before workspaces are verified.
  const selectedWorkspaceId = selectedWorkspace?.id ?? '';

  // Synchronize storage when canonical workspace resolves
  useEffect(() => {
    if (!userId) return;

    if (selectedWorkspace?.id) {
      sessionStorage.setItem(`selectedWorkspace:${userId}`, selectedWorkspace.id);
      sessionStorage.setItem('raa.workspaceId', selectedWorkspace.id);
    } else if (workspaces.length > 0 && explicitWorkspaceId && !workspaces.some((w) => w.id === explicitWorkspaceId)) {
      // Saved ID is invalid for the current user
      sessionStorage.removeItem(`selectedWorkspace:${userId}`);
      sessionStorage.removeItem('raa.workspaceId');
    }
  }, [userId, selectedWorkspace?.id, explicitWorkspaceId, workspaces]);

  const invalidateDependentQueries = useCallback(
    (targetWorkspaceId: string) => {
      queryClient.invalidateQueries({ queryKey: ['workspace', targetWorkspaceId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      queryClient.invalidateQueries({ queryKey: ['research'] });
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['billing'] });
      queryClient.invalidateQueries({ queryKey: ['usage'] });
      queryClient.invalidateQueries({ queryKey: ['ai'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    [queryClient],
  );

  const setSelectedWorkspaceId = useCallback(
    (workspaceId: string) => {
      setExplicitWorkspaceId(workspaceId);
      if (userId) {
        sessionStorage.setItem(`selectedWorkspace:${userId}`, workspaceId);
      }
      sessionStorage.setItem('raa.workspaceId', workspaceId);
      invalidateDependentQueries(workspaceId);
    },
    [userId, invalidateDependentQueries],
  );

  const setSelectedWorkspace = useCallback(
    (target: Workspace | string) => {
      if (typeof target === 'string') {
        setSelectedWorkspaceId(target);
      } else {
        queryClient.setQueryData<Workspace[]>(['workspaces'], (old = []) => {
          const filtered = old.filter((w) => w.id !== target.id);
          return [target, ...filtered];
        });
        setExplicitWorkspaceId(target.id);
        if (userId) {
          sessionStorage.setItem(`selectedWorkspace:${userId}`, target.id);
        }
        sessionStorage.setItem('raa.workspaceId', target.id);
        invalidateDependentQueries(target.id);
      }
    },
    [queryClient, userId, setSelectedWorkspaceId, invalidateDependentQueries],
  );

  const value = useMemo(
    () => ({
      workspaces,
      authorizedWorkspaces: workspaces,
      selectedWorkspace,
      selectedWorkspaceId,
      setSelectedWorkspaceId,
      setSelectedWorkspace,
      isLoading: query.isLoading || ensurePersonalMutation.isPending,
      loading: query.isLoading || ensurePersonalMutation.isPending,
      error: query.error,
      refetchWorkspaces: () => query.refetch(),
    }),
    [
      workspaces,
      selectedWorkspace,
      selectedWorkspaceId,
      setSelectedWorkspaceId,
      setSelectedWorkspace,
      query,
      ensurePersonalMutation.isPending,
    ],
  );

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error('useWorkspace must be used within WorkspaceProvider');
  return context;
}
