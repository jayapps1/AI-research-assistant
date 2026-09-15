import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { workspaceApi } from '../../api/endpoints';
import type { Workspace } from '../../types/api';

interface WorkspaceContextValue {
  workspaces: Workspace[];
  selectedWorkspace: Workspace | null;
  selectedWorkspaceId: string;
  setSelectedWorkspaceId: (workspaceId: string) => void;
  isLoading: boolean;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [selectedWorkspaceId, setSelectedWorkspaceIdState] = useState(() => sessionStorage.getItem('raa.workspaceId') ?? '');
  const query = useQuery({ queryKey: ['workspaces'], queryFn: workspaceApi.list });
  const workspaces = useMemo(() => query.data ?? [], [query.data]);
  const selectedWorkspace = workspaces.find((workspace) => workspace.id === selectedWorkspaceId) ?? workspaces[0] ?? null;

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
      isLoading: query.isLoading,
    }),
    [query.isLoading, selectedWorkspace, selectedWorkspaceId, workspaces],
  );

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (!context) throw new Error('useWorkspace must be used within WorkspaceProvider');
  return context;
}
