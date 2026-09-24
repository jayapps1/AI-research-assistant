import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import { projectApi } from '../../api/endpoints';
import { useOptionalAuth } from '../../auth/AuthProvider';
import { useWorkspace } from '../workspaces/WorkspaceProvider';
import type { ResearchProject, User } from '../../types/api';

interface ActiveProjectContextValue {
  workspaceId: string;
  projectId: string;
  activeProjectId: string;
  project: ResearchProject | null;
  activeProject: ResearchProject | null;
  authorizedProjects: ResearchProject[];
  setActiveProject: (project: ResearchProject | string | null) => void;
  loading: boolean;
  error: unknown;
}

const ActiveProjectContext = createContext<ActiveProjectContextValue | null>(null);

function getSessionUser(): User | null {
  try {
    const raw = sessionStorage.getItem('raa.user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

function storageKey(userId: string | undefined, workspaceId: string) {
  return userId && workspaceId ? `activeProject:${userId}:${workspaceId}` : '';
}

function projectIdFromPath(pathname: string) {
  return pathname.match(/\/(?:app\/)?projects\/([^/]+)/)?.[1] ?? '';
}

export function ActiveProjectProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const auth = useOptionalAuth();
  const user = auth?.user ?? getSessionUser();
  const userId = user?.id;
  const { selectedWorkspaceId } = useWorkspace();
  const location = useLocation();
  const routeProjectId = projectIdFromPath(location.pathname);
  const [explicitProjectId, setExplicitProjectId] = useState('');

  const projectsQuery = useQuery({
    queryKey: ['projects', selectedWorkspaceId, 'authorized'],
    queryFn: () => projectApi.list(selectedWorkspaceId, 0, 100),
    enabled: Boolean(selectedWorkspaceId),
  });

  const authorizedProjects = useMemo(
    () => projectsQuery.data?.content ?? [],
    [projectsQuery.data],
  );

  useEffect(() => {
    if (!selectedWorkspaceId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setExplicitProjectId('');
      return;
    }
    const key = storageKey(userId, selectedWorkspaceId);
    setExplicitProjectId(key ? localStorage.getItem(key) ?? '' : '');
  }, [selectedWorkspaceId, userId]);

  useEffect(() => {
    if (routeProjectId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setExplicitProjectId(routeProjectId);
      const key = storageKey(userId, selectedWorkspaceId);
      if (key) localStorage.setItem(key, routeProjectId);
    }
  }, [routeProjectId, selectedWorkspaceId, userId]);

  const resolvedProject = useMemo(() => {
    if (!authorizedProjects.length) return null;
    const candidateId = routeProjectId || explicitProjectId;
    if (candidateId) {
      const match = authorizedProjects.find((project) => project.id === candidateId);
      if (match) return match;
    }
    return null;
  }, [authorizedProjects, explicitProjectId, routeProjectId]);

  useEffect(() => {
    if (!selectedWorkspaceId || projectsQuery.isLoading) return;
    if (!explicitProjectId) return;
    const stillAuthorized = authorizedProjects.some((project) => project.id === explicitProjectId);
    if (!stillAuthorized) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setExplicitProjectId('');
      const key = storageKey(userId, selectedWorkspaceId);
      if (key) localStorage.removeItem(key);
    }
  }, [authorizedProjects, explicitProjectId, projectsQuery.isLoading, selectedWorkspaceId, userId]);

  const setActiveProject = useCallback(
    (target: ResearchProject | string | null) => {
      const nextProjectId = typeof target === 'string' ? target : target?.id ?? '';
      setExplicitProjectId(nextProjectId);
      const key = storageKey(userId, selectedWorkspaceId);
      if (key) {
        if (nextProjectId) localStorage.setItem(key, nextProjectId);
        else localStorage.removeItem(key);
      }
      if (nextProjectId) {
        queryClient.invalidateQueries({ queryKey: ['project', nextProjectId] });
        queryClient.invalidateQueries({ queryKey: ['documents', nextProjectId] });
        queryClient.invalidateQueries({ queryKey: ['research', nextProjectId] });
        queryClient.invalidateQueries({ queryKey: ['reports', nextProjectId] });
        queryClient.invalidateQueries({ queryKey: ['references', nextProjectId] });
      }
    },
    [queryClient, selectedWorkspaceId, userId],
  );

  const value = useMemo<ActiveProjectContextValue>(
    () => ({
      workspaceId: selectedWorkspaceId,
      projectId: resolvedProject?.id ?? '',
      activeProjectId: resolvedProject?.id ?? '',
      project: resolvedProject,
      activeProject: resolvedProject,
      authorizedProjects,
      setActiveProject,
      loading: projectsQuery.isLoading,
      error: projectsQuery.error,
    }),
    [authorizedProjects, projectsQuery.error, projectsQuery.isLoading, resolvedProject, selectedWorkspaceId, setActiveProject],
  );

  return <ActiveProjectContext.Provider value={value}>{children}</ActiveProjectContext.Provider>;
}

export function useActiveProject() {
  const context = useContext(ActiveProjectContext);
  if (!context) throw new Error('useActiveProject must be used within ActiveProjectProvider');
  return context;
}

export function useOptionalActiveProject() {
  return useContext(ActiveProjectContext);
}
