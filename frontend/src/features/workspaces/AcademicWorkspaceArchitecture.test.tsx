import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { projectApi, reportApi, workspaceApi } from '../../api/endpoints';
import { clearTokens, setTokens } from '../../api/tokens';
import { ThemeProvider } from '../../app/ThemeProvider';
import { AuthProvider } from '../../auth/AuthProvider';
import { WorkspaceProvider } from './WorkspaceProvider';
import { ActiveProjectProvider } from '../projects/ActiveProjectProvider';
import { AppShell } from '../../layouts/AppShell';
import { CreateProjectModal } from '../projects/CreateProjectModal';
import { ProjectsPage } from '../../pages/DashboardPages';

vi.mock('../../api/endpoints', () => ({
  projectApi: {
    list: vi.fn(),
    mine: vi.fn(),
    create: vi.fn(),
    get: vi.fn(),
    update: vi.fn(),
    archive: vi.fn(),
    trash: vi.fn(),
    restore: vi.fn(),
    permanentDelete: vi.fn(),
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
  reportApi: {
    templates: vi.fn(),
  },
  dashboardApi: {
    userDashboard: vi.fn(),
    workspaceDashboard: vi.fn(),
    projectDashboard: vi.fn(),
    researchProgress: vi.fn(),
  },
  billingApi: {
    usage: vi.fn(),
    subscription: vi.fn(),
  },
  documentApi: {
    list: vi.fn(),
    mine: vi.fn(),
  },
  researchApi: {
    methodologies: vi.fn(),
    conceptualFrameworks: vi.fn(),
    theoreticalFrameworks: vi.fn(),
    ethicsReadiness: vi.fn(),
    participants: vi.fn(),
  },
  notificationApi: {
    unreadCount: vi.fn().mockResolvedValue({ count: 0 }),
    list: vi.fn().mockResolvedValue({ content: [] }),
  },
}));

function renderWithClient(ui: React.ReactElement, initialRoute = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <ThemeProvider>
          <AuthProvider>
            <WorkspaceProvider>
              <ActiveProjectProvider>
                {ui}
              </ActiveProjectProvider>
            </WorkspaceProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('Academic Workspace Architecture & Types', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
    vi.clearAllMocks();
    setTokens({ accessToken: 'test-token', refreshToken: 'refresh-token' });
    sessionStorage.setItem(
      'raa.user',
      JSON.stringify({ id: 'u-1', email: 'scholar@example.com', firstName: 'Scholar', systemRoles: [] })
    );
    sessionStorage.setItem('raa.workspaceId', 'ws-test-1');

    vi.mocked(workspaceApi.list).mockResolvedValue([
      { id: 'ws-test-1', name: 'University Workspace', type: 'ORGANIZATION', status: 'ACTIVE', role: 'OWNER', createdAt: '2026-01-01T00:00:00Z' },
    ]);
    vi.mocked(projectApi.list).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 1,
    });
    vi.mocked(reportApi.templates).mockResolvedValue([
      { id: 'tpl-research', name: 'Generic Academic Research Report', type: 'RESEARCH_REPORT', supportedWorkspaceTypes: 'ACADEMIC_RESEARCH' },
      { id: 'tpl-project', name: 'Generic Academic Project Report', type: 'ACADEMIC_PROJECT_REPORT', supportedWorkspaceTypes: 'ACADEMIC_PROJECT' },
      { id: 'tpl-coursework', name: 'Generic Coursework Document', type: 'COURSEWORK', supportedWorkspaceTypes: 'COURSEWORK' },
    ]);
  });

  it('renders workspace type selector with all 3 academic workspace types', async () => {
    renderWithClient(
      <CreateProjectModal open={true} onClose={vi.fn()} defaultWorkspaceId="ws-test-1" />
    );

    expect(await screen.findByText('What are you working on?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Academic Research/i })).toBeInTheDocument();
    expect(screen.getByText(/Conduct a formal academic study and produce a research report/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Academic Project/i })).toBeInTheDocument();
    expect(screen.getByText(/Final-year project, capstone or institution-required academic project/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Coursework/i })).toBeInTheDocument();
    expect(screen.getByText(/Assignment, term paper, essay, case study or class exercise/i)).toBeInTheDocument();
  });

  it('configures Academic Research workspace with topic, aim, domain, and research report template', async () => {
    const user = userEvent.setup();
    const createSpy = vi.mocked(projectApi.create).mockResolvedValue({
      id: 'p-res-1',
      title: 'Digital Agriculture Adoption Among Smallholder Farmers',
      workspaceType: 'ACADEMIC_RESEARCH',
      finalDocumentLabel: 'Research Report',
      workAreaLabel: 'Study Design',
      status: 'DRAFT',
    });

    renderWithClient(
      <CreateProjectModal open={true} onClose={vi.fn()} defaultWorkspaceId="ws-test-1" />
    );

    // Select Academic Research card (default active)
    await user.click(screen.getByRole('button', { name: /Academic Research/i }));
    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 2: Form fields
    expect(screen.getByLabelText(/Title \/ Research Topic/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Study Area \/ Domain/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Research Type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Research Aim/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Keywords/i)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Title \/ Research Topic/i), 'Digital Agriculture Adoption Among Smallholder Farmers');
    await user.type(screen.getByLabelText(/Study Area \/ Domain/i), 'Agricultural Economics');
    await user.type(screen.getByLabelText(/Research Aim/i), 'Examine tech adoption barriers among smallholders.');

    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 3: Template & citation
    expect(await screen.findByText(/Research Report Setup/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Research Report Template/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Create Workspace/i }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        'ws-test-1',
        expect.objectContaining({
          title: 'Digital Agriculture Adoption Among Smallholder Farmers',
          workspaceType: 'ACADEMIC_RESEARCH',
          studyArea: 'Agricultural Economics',
          researchAim: 'Examine tech adoption barriers among smallholders.',
        })
      );
    });
  });

  it('configures Academic Project workspace with project title, project type, institution, supervisor, and project report', async () => {
    const user = userEvent.setup();
    const createSpy = vi.mocked(projectApi.create).mockResolvedValue({
      id: 'p-proj-1',
      title: 'Farmer-to-Buyer Agricultural Marketplace',
      workspaceType: 'ACADEMIC_PROJECT',
      projectType: 'SOFTWARE_SYSTEM_DEVELOPMENT',
      finalDocumentLabel: 'Project Report',
      workAreaLabel: 'Project Work',
      status: 'DRAFT',
    });

    renderWithClient(
      <CreateProjectModal open={true} onClose={vi.fn()} defaultWorkspaceId="ws-test-1" />
    );

    // Step 1: Select Academic Project
    await user.click(screen.getByRole('button', { name: /Academic Project/i }));
    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 2: Form fields
    expect(screen.getByLabelText(/Project Title \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Project Type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Institution/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Department/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Programme/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Supervisor/i)).toBeInTheDocument();

    // Verify research methodology fields are NOT forced
    expect(screen.queryByLabelText(/Research Aim/i)).toBeNull();

    await user.type(screen.getByLabelText(/Project Title \*/i), 'Farmer-to-Buyer Agricultural Marketplace');
    await user.selectOptions(screen.getByLabelText(/Project Type/i), 'SOFTWARE_SYSTEM_DEVELOPMENT');
    await user.type(screen.getByLabelText(/Institution/i), 'Technical University');
    await user.type(screen.getByLabelText(/Programme/i), 'BSc Computer Science');
    await user.type(screen.getByLabelText(/Supervisor/i), 'Dr. Kwame Mensah');

    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 3: Template setup
    expect(await screen.findByText(/Project Report Setup/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Project Report Template/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Create Workspace/i }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        'ws-test-1',
        expect.objectContaining({
          title: 'Farmer-to-Buyer Agricultural Marketplace',
          workspaceType: 'ACADEMIC_PROJECT',
          projectType: 'SOFTWARE_SYSTEM_DEVELOPMENT',
          institution: 'Technical University',
          programme: 'BSc Computer Science',
          supervisor: 'Dr. Kwame Mensah',
        })
      );
    });
  });

  it('configures Coursework workspace without forced methodology, aim, or hypotheses', async () => {
    const user = userEvent.setup();
    const createSpy = vi.mocked(projectApi.create).mockResolvedValue({
      id: 'p-cw-1',
      title: 'Software Development Methodologies Assignment',
      workspaceType: 'COURSEWORK',
      courseName: 'Software Engineering',
      courseCode: 'SENG 401',
      finalDocumentLabel: 'Coursework Document',
      workAreaLabel: 'Notes / Work',
      status: 'DRAFT',
    });

    renderWithClient(
      <CreateProjectModal open={true} onClose={vi.fn()} defaultWorkspaceId="ws-test-1" />
    );

    // Step 1: Select Coursework
    await user.click(screen.getByRole('button', { name: /Coursework/i }));
    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 2: Coursework fields
    expect(screen.getByLabelText(/Assignment \/ Coursework Title \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description \/ Instructions/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Course Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Course Code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Lecturer/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Deadline/i)).toBeInTheDocument();

    // Verify research methodology fields are NOT present
    expect(screen.queryByLabelText(/Research Aim/i)).toBeNull();
    expect(screen.queryByLabelText(/Study Area \/ Domain/i)).toBeNull();

    await user.type(screen.getByLabelText(/Assignment \/ Coursework Title \*/i), 'Software Development Methodologies Assignment');
    await user.type(screen.getByLabelText(/Course Name/i), 'Software Engineering');
    await user.type(screen.getByLabelText(/Course Code/i), 'SENG 401');
    await user.type(screen.getByLabelText(/Lecturer/i), 'Prof. Alan Turing');

    await user.click(screen.getByRole('button', { name: /Continue/i }));

    // Step 3: Template setup
    expect(await screen.findByText(/Coursework Document Setup/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Coursework Document Template/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Create Workspace/i }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        'ws-test-1',
        expect.objectContaining({
          title: 'Software Development Methodologies Assignment',
          workspaceType: 'COURSEWORK',
          courseName: 'Software Engineering',
          courseCode: 'SENG 401',
          lecturer: 'Prof. Alan Turing',
        })
      );
    });
  });

  it('renders type badges and filters on My Workspaces page', async () => {
    vi.mocked(projectApi.list).mockResolvedValue({
      content: [
        {
          id: 'proj-1',
          title: 'Digital Agriculture Study',
          workspaceType: 'ACADEMIC_RESEARCH',
          workspaceTypeLabel: 'Academic Research',
          status: 'ACTIVE',
          currentUserRole: 'LEAD',
          updatedAt: '2026-09-30T10:00:00Z',
        },
        {
          id: 'proj-2',
          title: 'Campus Logistics System',
          workspaceType: 'ACADEMIC_PROJECT',
          workspaceTypeLabel: 'Academic Project',
          projectType: 'SOFTWARE_SYSTEM_DEVELOPMENT',
          status: 'ACTIVE',
          currentUserRole: 'LEAD',
          updatedAt: '2026-09-30T10:00:00Z',
        },
        {
          id: 'proj-3',
          title: 'Agile vs Waterfall Essay',
          workspaceType: 'COURSEWORK',
          workspaceTypeLabel: 'Coursework',
          courseName: 'Software Engineering',
          status: 'DRAFT',
          currentUserRole: 'LEAD',
          updatedAt: '2026-09-30T10:00:00Z',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 3,
      totalPages: 1,
    });

    renderWithClient(
      <Routes>
        <Route path="/app/projects" element={<ProjectsPage />} />
      </Routes>,
      '/app/projects'
    );

    expect(await screen.findByText('Digital Agriculture Study')).toBeInTheDocument();
    expect(screen.getByText('Campus Logistics System')).toBeInTheDocument();
    expect(screen.getByText('Agile vs Waterfall Essay')).toBeInTheDocument();

    // Verify type filter dropdown exists with all 3 academic workspace types
    const typeFilter = screen.getByLabelText(/Filter workspace type/i);
    expect(typeFilter).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Academic Research/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Academic Project/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Coursework/i })).toBeInTheDocument();
  });

  it('adapts navigation labels in AppShell based on workspace type', async () => {
    const courseworkProject = {
      id: 'proj-cw',
      title: 'Operating Systems Coursework',
      workspaceType: 'COURSEWORK' as const,
      workspaceTypeLabel: 'Coursework',
      finalDocumentLabel: 'Coursework Document',
      workAreaLabel: 'Notes / Work',
      status: 'ACTIVE' as const,
      currentUserRole: 'LEAD' as const,
    };

    vi.mocked(projectApi.get).mockResolvedValue(courseworkProject);
    vi.mocked(projectApi.list).mockResolvedValue({
      content: [courseworkProject],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });

    renderWithClient(
      <Routes>
        <Route path="/app/projects/:projectId" element={<AppShell />} />
      </Routes>,
      '/app/projects/proj-cw'
    );

    const courseworkDocs = await screen.findAllByText('Coursework Document');
    expect(courseworkDocs.length).toBeGreaterThanOrEqual(1);
    const notesWorks = screen.getAllByText('Notes / Work');
    expect(notesWorks.length).toBeGreaterThanOrEqual(1);
  });
});
