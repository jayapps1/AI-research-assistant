import { createContext, useContext, useEffect, useMemo, useState, useCallback, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../../api/endpoints';
import type { Workspace } from '../../types/api';

interface WorkspaceContextValue {
  workspaces: Workspace[];
  selectedWorkspace: Workspace | null;
  selectedWorkspaceId: string;
  setSelectedWorkspaceId: (workspaceId: string) => void;
  setSelectedWorkspace: (workspace: Workspace | string) => void;
  isLoading: boolean;
  refetchWorkspaces: () => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [explicitWorkspaceId, setExplicitWorkspaceId] = useState(() => sessionStorage.getItem('raa.workspaceId') ?? '');
  const query = useQuery({ queryKey: ['workspaces'], queryFn: workspaceApi.list });
  const workspaces = useMemo(() => query.data ?? [], [query.data]);

  const ensurePersonalMutation = useMutation({
    mutationFn: workspaceApi.ensurePersonal,
    onSuccess: (personalWorkspace) => {
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      if (personalWorkspace?.id && !explicitWorkspaceId) {
        setExplicitWorkspaceId(personalWorkspace.id);
        sessionStorage.setItem('raa.workspaceId', personalWorkspace.id);
      }
    },
  });

  useEffect(() => {
    if (query.isSuccess && workspaces.length === 0 && !ensurePersonalMutation.isPending) {
      ensurePersonalMutation.mutate();
    }
  }, [query.isSuccess, workspaces.length, ensurePersonalMutation]);

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

  const selectedWorkspaceId = selectedWorkspace?.id ?? explicitWorkspaceId;

  const setSelectedWorkspaceId = useCallback((workspaceId: string) => {
    setExplicitWorkspaceId(workspaceId);
    sessionStorage.setItem('raa.workspaceId', workspaceId);
  }, []);

  const setSelectedWorkspace = useCallback((target: Workspace | string) => {
    if (typeof target === 'string') {
      setSelectedWorkspaceId(target);
    } else {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (old = []) => {
        const filtered = old.filter((w) => w.id !== target.id);
        return [target, ...filtered];
      });
      setSelectedWorkspaceId(target.id);
    }
  }, [queryClient, setSelectedWorkspaceId]);

  const value = useMemo(
    () => ({
      workspaces,
      selectedWorkspace,
      selectedWorkspaceId,
      setSelectedWorkspaceId,
      setSelectedWorkspace,
      isLoading: query.isLoading || ensurePersonalMutation.isPending,
      refetchWorkspaces: () => query.refetch(),
    }),
    [ensurePersonalMutation.isPending, query, selectedWorkspace, selectedWorkspaceId, setSelectedWorkspace, setSelectedWorkspaceId, workspaces],
  );

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error('useWorkspace must be used within WorkspaceProvider');
  return context;
}


