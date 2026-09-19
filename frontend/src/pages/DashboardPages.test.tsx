import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { HomeDashboard, ProjectsPage, ProjectDashboard } from './DashboardPages';
import { ResearchWorkflowPage } from './ResearchPages';
import { dashboardApi, projectApi, researchApi, workspaceApi } from '../api/endpoints';
import { WorkspaceProvider } from '../features/workspaces/WorkspaceProvider';

vi.mock('../api/endpoints', () => ({
  dashboardApi: {
    userDashboard: vi.fn(),
    workspaceDashboard: vi.fn(),
    projectDashboard: vi.fn(),
    researchProgress: vi.fn(),
  },
  projectApi: {
    list: vi.fn(),
    mine: vi.fn(),
    create: vi.fn(),
    get: vi.fn(),
    members: vi.fn(),
    tasks: vi.fn(),
    allMyTasks: vi.fn(),
  },
  workspaceApi: {
    list: vi.fn(),
    ensurePersonal: vi.fn(),
    getPersonal: vi.fn(),
    members: vi.fn(),
  },
  documentApi: {
    list: vi.fn(),
    mine: vi.fn(),
    downloadUrl: vi.fn((id) => `/documents/${id}/download`),
  },
  researchApi: {
    methodologies: vi.fn(),
    conceptualFrameworks: vi.fn(),
    theoreticalFrameworks: vi.fn(),
    ethicsReadiness: vi.fn(),
    participants: vi.fn(),
  },
  billingApi: {
    usage: vi.fn(),
    subscription: vi.fn(),
  },
}));

function renderWithProviders(ui: React.ReactElement, { route = '/app' } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <WorkspaceProvider>
          {ui}
        </WorkspaceProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('DashboardPages Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();

    vi.mocked(workspaceApi.list).mockResolvedValue([
      { id: 'ws-123', name: 'Personal Workspace', type: 'PERSONAL' },
    ]);
  });

  describe('HomeDashboard', () => {
    it('renders real user greeting, workspace context, and stat cards', async () => {
      vi.mocked(dashboardApi.userDashboard).mockResolvedValue({
        greeting: 'Good morning, Dr. Jane Doe!',
        currentWorkspace: { id: 'ws-123', name: 'Personal Workspace', type: 'PERSONAL' },
        activeProjectCount: 3,
        openTaskCount: 7,
        documentCount: 14,
        unreadNotifications: 2,
        aiUsage: {
          requestsToday: 42,
          requestsThisMonth: 180,
          tokensThisMonth: 45000,
          requestLimit: 1000,
          tokenLimit: 500000,
          isUnlimited: false,
        },
        recentProjects: [
          {
            id: 'proj-1',
            title: 'Neural Networks in Oncology',
            status: 'ACTIVE',
            currentUserRole: 'LEAD',
            updatedAt: new Date().toISOString(),
          },
        ],
        recentTasks: [
          {
            id: 'task-1',
            projectId: 'proj-1',
            projectTitle: 'Neural Networks in Oncology',
            title: 'Audit literature matrix',
            status: 'IN_PROGRESS',
            priority: 'HIGH',
            dueDate: '2026-10-01',
            overdue: false,
          },
        ],
        recentActivities: [
          {
            id: 'act-1',
            projectId: 'proj-1',
            projectTitle: 'Neural Networks in Oncology',
            type: 'PROJECT_CREATED',
            summary: 'created the research project',
            actorName: 'Dr. Jane Doe',
            occurredAt: new Date().toISOString(),
          },
        ],
      });

      renderWithProviders(<HomeDashboard />);

      expect(await screen.findByText('Good morning, Dr. Jane Doe!')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
      expect(screen.getByText('14')).toBeInTheDocument();
      expect(screen.getByText('42')).toBeInTheDocument();
      expect(screen.getByText('Neural Networks in Oncology')).toBeInTheDocument();
      expect(screen.getByText('Audit literature matrix')).toBeInTheDocument();
    });

    it('opens CreateProjectModal when New Research Project button is clicked', async () => {
      const user = userEvent.setup();
      vi.mocked(dashboardApi.userDashboard).mockResolvedValue({
        greeting: 'Welcome back',
        currentWorkspace: { id: 'ws-123', name: 'Personal Workspace', type: 'PERSONAL' },
        activeProjectCount: 0,
        openTaskCount: 0,
        documentCount: 0,
        unreadNotifications: 0,
        aiUsage: {
          requestsToday: 0,
          requestsThisMonth: 0,
          tokensThisMonth: 0,
          requestLimit: 100,
          tokenLimit: 100000,
          isUnlimited: false,
        },
        recentProjects: [],
        recentTasks: [],
        recentActivities: [],
      });

      renderWithProviders(<HomeDashboard />);

      await waitFor(() => {
        expect(screen.getByText('Welcome back')).toBeInTheDocument();
      });

      const newProjectBtn = screen.getAllByRole('button', { name: /New Research Project/i })[0];
      await user.click(newProjectBtn);

      expect(await screen.findByRole('dialog', { name: /Create Research Project/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/Research Project Title/i)).toBeInTheDocument();
    });
  });

  describe('ProjectsPage and Project Creation Flow', () => {
    it('lists existing projects and allows creating a real project that calls backend API', async () => {
      const user = userEvent.setup();

      vi.mocked(projectApi.list).mockResolvedValue({
        content: [
          {
            id: 'proj-abc',
            title: 'Empirical Study on Climate Adaptation',
            description: 'Field survey across 5 agricultural zones',
            status: 'ACTIVE',
            currentUserRole: 'LEAD',
            updatedAt: new Date().toISOString(),
          },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      });

      vi.mocked(projectApi.create).mockResolvedValue({
        id: 'proj-new-999',
        title: 'Cognitive Biases in Peer Review',
        description: 'Double-blind experimental setup',
        status: 'DRAFT',
        currentUserRole: 'LEAD',
      });

      renderWithProviders(
        <Routes>
          <Route path="/app/projects" element={<ProjectsPage />} />
          <Route path="/app/projects/:projectId" element={<div data-testid="project-dashboard-routed">Project Dashboard Loaded</div>} />
        </Routes>,
        { route: '/app/projects' }
      );

      expect(await screen.findByText('Empirical Study on Climate Adaptation')).toBeInTheDocument();

      const newProjectBtn = screen.getByRole('button', { name: /New Research Project/i });
      await user.click(newProjectBtn);

      expect(await screen.findByRole('dialog', { name: /Create Research Project/i })).toBeInTheDocument();

      const titleInput = screen.getByLabelText(/Research Project Title/i);
      await user.type(titleInput, 'Cognitive Biases in Peer Review');

      const descInput = screen.getByLabelText(/Description/i);
      await user.type(descInput, 'Double-blind experimental setup');

      const submitBtn = screen.getByRole('button', { name: /Create Project & Launch/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(projectApi.create).toHaveBeenCalledWith(
          'ws-123',
          expect.objectContaining({
            title: 'Cognitive Biases in Peer Review',
            description: 'Double-blind experimental setup',
          })
        );
      });

      expect(await screen.findByTestId('project-dashboard-routed')).toBeInTheDocument();
    }, 15000);
  });

  describe('ProjectDashboard', () => {
    it('renders project metrics, role badge, and research readiness', async () => {
      vi.mocked(dashboardApi.projectDashboard).mockResolvedValue({
        project: {
          id: 'proj-xyz',
          title: 'Quantum Computing Simulation Benchmarks',
          description: 'Evaluating decoherence time series',
          status: 'ACTIVE',
        },
        currentUserRole: 'LEAD',
        memberCount: 4,
        documents: { total: 12, ready: 10, processing: 2, failed: 0 },
        tasks: { total: 8, todo: 2, inProgress: 3, inReview: 1, completed: 2, overdue: 0 },
        researchProgress: {
          completedStages: 6,
          totalStages: 18,
          percentComplete: 33,
          nextIncompleteStage: 'Measurement Instruments',
          nextStageUrl: '/app/projects/proj-xyz/research#Instruments',
        },
        recentActivities: [],
      });

      renderWithProviders(
        <Routes>
          <Route path="/app/projects/:projectId" element={<ProjectDashboard />} />
        </Routes>,
        { route: '/app/projects/proj-xyz' }
      );

      expect(await screen.findByText('Quantum Computing Simulation Benchmarks')).toBeInTheDocument();
      expect(screen.getByText('Evaluating decoherence time series')).toBeInTheDocument();
      expect(screen.getByText('LEAD')).toBeInTheDocument();
      expect(screen.getByText('33%')).toBeInTheDocument();
      expect(screen.getByText('Measurement Instruments')).toBeInTheDocument();
      expect(screen.getByText('10 Ready')).toBeInTheDocument();
    });
  });

  describe('ResearchWorkflowPage', () => {
    it('renders 18 academic research stages and next stage CTA', async () => {
      vi.mocked(dashboardApi.researchProgress).mockResolvedValue({
        projectId: 'proj-123',
        projectTitle: 'AI in Higher Education',
        completedStages: 2,
        totalStages: 18,
        percentComplete: 11,
        nextIncompleteStage: 'Literature Review',
        nextStageUrl: '#Literature Review',
        stages: [
          {
            number: 1,
            key: 'PROBLEM_STATEMENT',
            name: 'Problem Statement & Gap Formulation',
            status: 'COMPLETE',
            description: 'Define empirical gap',
            itemCount: 1,
            actionUrl: '#Research Problem',
          },
          {
            number: 2,
            key: 'OBJECTIVES_HYPOTHESES',
            name: 'Research Objectives & Hypotheses',
            status: 'COMPLETE',
            description: 'Specific empirical sub-aims',
            itemCount: 3,
            actionUrl: '#Objectives',
          },
          {
            number: 3,
            key: 'LITERATURE_REVIEW',
            name: 'Systematic Literature Review',
            status: 'IN_PROGRESS',
            description: 'Thematic synthesis',
            itemCount: 8,
            actionUrl: '#Literature Review',
          },
        ],
      });

      vi.mocked(researchApi.methodologies).mockResolvedValue([]);

      renderWithProviders(
        <Routes>
          <Route path="/app/projects/:projectId/research" element={<ResearchWorkflowPage />} />
        </Routes>,
        { route: '/app/projects/proj-123/research' }
      );

      expect(await screen.findByText('Academic Research Workflow')).toBeInTheDocument();
      expect(screen.getByText('11%')).toBeInTheDocument();
      expect(screen.getByText('Problem Statement & Gap Formulation')).toBeInTheDocument();
      expect(screen.getByText('Systematic Literature Review')).toBeInTheDocument();
      expect(screen.getByText(/Continue: Literature Review/i)).toBeInTheDocument();
    });
  });
});
