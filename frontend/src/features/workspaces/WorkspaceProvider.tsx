import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workspaceApi } from '../../api/endpoints';
import type { Workspace } from '../../types/api';

interface WorkspaceContextValue {
  workspaces: Workspace[];
  selectedWorkspace: Workspace | null;
  selectedWorkspaceId: string;
  setSelectedWorkspaceId: (workspaceId: string) => void;
  isLoading: boolean;
  refetchWorkspaces: () => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [selectedWorkspaceId, setSelectedWorkspaceIdState] = useState(() => sessionStorage.getItem('raa.workspaceId') ?? '');
  const query = useQuery({ queryKey: ['workspaces'], queryFn: workspaceApi.list });
  const workspaces = useMemo(() => query.data ?? [], [query.data]);

  const ensurePersonalMutation = useMutation({
    mutationFn: workspaceApi.ensurePersonal,
    onSuccess: (personalWorkspace) => {
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      if (personalWorkspace?.id && !selectedWorkspaceId) {
        setSelectedWorkspaceId(personalWorkspace.id);
      }
    },
  });

  useEffect(() => {
    if (query.isSuccess && workspaces.length === 0 && !ensurePersonalMutation.isPending) {
      ensurePersonalMutation.mutate();
    }
  }, [query.isSuccess, workspaces.length, ensurePersonalMutation]);

  const effectiveWorkspaceId = useMemo(() => {
    if (workspaces.length === 0) return selectedWorkspaceId;
    const exists = workspaces.some((w) => w.id === selectedWorkspaceId);
    return exists ? selectedWorkspaceId : workspaces[0].id;
  }, [workspaces, selectedWorkspaceId]);

  const selectedWorkspace = useMemo(() => {
    return workspaces.find((workspace) => workspace.id === effectiveWorkspaceId) ?? workspaces[0] ?? null;
  }, [workspaces, effectiveWorkspaceId]);

  const setSelectedWorkspaceId = (workspaceId: string) => {
    setSelectedWorkspaceIdState(workspaceId);
    sessionStorage.setItem('raa.workspaceId', workspaceId);
  };

  const value = useMemo(
    () => ({
      workspaces,
      selectedWorkspace,
      selectedWorkspaceId: selectedWorkspace?.id ?? selectedWorkspaceId,
      setSelectedWorkspaceId,
      isLoading: query.isLoading || ensurePersonalMutation.isPending,
      refetchWorkspaces: () => query.refetch(),
    }),
    [query, ensurePersonalMutation.isPending, selectedWorkspace, selectedWorkspaceId, workspaces],
  );

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error('useWorkspace must be used within WorkspaceProvider');
  return context;
}

