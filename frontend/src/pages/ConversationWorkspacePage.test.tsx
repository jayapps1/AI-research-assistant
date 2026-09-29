import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ConversationSidebarSection } from '../features/conversations/ConversationSidebarSection';
import type { ConversationDetail, SubmitConversationResponse } from '../types/api';
import { ConversationWorkspacePage } from './ConversationWorkspacePage';
import { conversationApi, documentApi, projectApi } from '../api/endpoints';

vi.mock('../api/endpoints', () => ({
  conversationApi: {
    list: vi.fn(),
    listProject: vi.fn(),
    create: vi.fn(),
    createProject: vi.fn(),
    get: vi.fn(),
    getProject: vi.fn(),
    submitMessage: vi.fn(),
    submitProjectMessage: vi.fn(),
    retryMessage: vi.fn(),
    retryProjectMessage: vi.fn(),
    rename: vi.fn(),
    archive: vi.fn(),
    restore: vi.fn(),
    trash: vi.fn(),
    moveToProject: vi.fn(),
  },
  documentApi: {
    list: vi.fn(),
  },
  projectApi: {
    mine: vi.fn(),
  },
}));

vi.mock('../features/projects/CreateProjectModal', () => ({
  CreateProjectModal: ({ open, onCreated }: { open: boolean; onCreated?: (project: { id: string; title: string; workspaceId: string }) => void | Promise<void> }) =>
    open ? (
      <button type="button" onClick={() => void onCreated?.({ id: 'p-new', title: 'Created Project', workspaceId: 'w-1' })}>
        Confirm Create Project
      </button>
    ) : null,
}));

const conversationApiMock = vi.mocked(conversationApi);
const documentApiMock = vi.mocked(documentApi);
const projectApiMock = vi.mocked(projectApi);

describe('ConversationWorkspacePage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    projectApiMock.mine.mockResolvedValue(pageResponse([]));
    documentApiMock.list.mockResolvedValue(pageResponse([]));
    conversationApiMock.listProject.mockResolvedValue(pageResponse([]));
  });

  it('starts a new search and displays the persisted response', async () => {
    conversationApiMock.create.mockResolvedValue(submitResponse('c-1', 'What is stratified sampling?', 'Stratified sampling divides a population into subgroups.'));

    renderConversation('/app/search');

    fireEvent.input(screen.getByRole('textbox', { name: /^message$/i }), { target: { value: 'What is stratified sampling?' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(await screen.findByText(/Stratified sampling divides/i)).toBeInTheDocument();
    expect(conversationApiMock.create).toHaveBeenCalledWith({ content: 'What is stratified sampling?' });
  });

  it('submits the composer with Enter', async () => {
    conversationApiMock.create.mockResolvedValue(
      submitResponse('c-enter', 'What is precision agriculture?', 'Precision agriculture uses sensors and data.'),
    );

    renderConversation('/app/search');

    const messageBox = screen.getByRole('textbox', { name: /^message$/i });
    fireEvent.input(messageBox, { target: { value: 'What is precision agriculture?' } });
    fireEvent.keyDown(messageBox, { key: 'Enter', code: 'Enter' });

    await waitFor(() => {
      expect(conversationApiMock.create).toHaveBeenCalledTimes(1);
    });
    expect(conversationApiMock.create).toHaveBeenCalledWith({ content: 'What is precision agriculture?' });
    expect(await screen.findByText(/uses sensors and data/i)).toBeInTheDocument();
    expect(messageBox).toHaveValue('');
  });

  it('keeps Shift+Enter as a multiline draft without submitting', async () => {
    const user = userEvent.setup();
    renderConversation('/app/search');

    const messageBox = screen.getByRole('textbox', { name: /^message$/i });
    await user.click(messageBox);
    await user.keyboard('Explain these:');
    await user.keyboard('{Shift>}{Enter}{/Shift}');
    await user.keyboard('Precision agriculture');
    await user.keyboard('{Shift>}{Enter}{/Shift}');
    await user.keyboard('Smart irrigation');

    expect(messageBox).toHaveValue('Explain these:\nPrecision agriculture\nSmart irrigation');
    expect(conversationApiMock.create).not.toHaveBeenCalled();
  });

  it('does not submit an empty Enter press', () => {
    renderConversation('/app/search');

    fireEvent.keyDown(screen.getByRole('textbox', { name: /^message$/i }), { key: 'Enter', code: 'Enter' });

    expect(conversationApiMock.create).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(/enter a question/i);
  });

  it('does not duplicate-submit rapid Enter presses while a request is pending', async () => {
    let resolveCreate!: (value: SubmitConversationResponse) => void;
    conversationApiMock.create.mockReturnValue(
      new Promise<SubmitConversationResponse>((resolve) => {
        resolveCreate = resolve;
      }),
    );

    renderConversation('/app/search');

    const messageBox = screen.getByRole('textbox', { name: /^message$/i });
    fireEvent.input(messageBox, { target: { value: 'Explain precision agriculture.' } });
    fireEvent.keyDown(messageBox, { key: 'Enter', code: 'Enter' });
    fireEvent.keyDown(messageBox, { key: 'Enter', code: 'Enter' });

    await waitFor(() => {
      expect(conversationApiMock.create).toHaveBeenCalledTimes(1);
    });

    resolveCreate(submitResponse('c-pending', 'Explain precision agriculture.', 'It combines data with farm operations.'));
    expect(await screen.findByText(/combines data with farm operations/i)).toBeInTheDocument();
  });

  it('focuses the composer on a new search', async () => {
    renderConversation('/app/search');

    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: /^message$/i })).toHaveFocus();
    });
  });

  it('loads a conversation on route refresh and continues it', async () => {
    conversationApiMock.get.mockResolvedValue(conversationDetail('c-1'));
    conversationApiMock.submitMessage.mockResolvedValue(
      submitResponse('c-1', 'Can you give an example?', 'For example, sample proportionally from each faculty.'),
    );

    renderConversation('/app/conversations/c-1');

    expect(await screen.findByRole('heading', { name: 'What is stratified sampling?' })).toBeInTheDocument();
    expect(screen.getByText(/It divides a population into strata/i)).toBeInTheDocument();

    fireEvent.input(screen.getByRole('textbox', { name: /^message$/i }), { target: { value: 'Can you give an example?' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(await screen.findByText(/sample proportionally from each faculty/i)).toBeInTheDocument();
    expect(conversationApiMock.submitMessage).toHaveBeenCalledWith('c-1', { content: 'Can you give an example?' });
  });

  it('renames, archives, restores, and trashes a conversation', async () => {
    conversationApiMock.get.mockResolvedValue(conversationDetail('c-1'));
    conversationApiMock.rename.mockResolvedValue(summary('c-1', 'Sampling Basics', 'ACTIVE'));
    conversationApiMock.archive.mockResolvedValue(summary('c-1', 'Sampling Basics', 'ARCHIVED'));
    conversationApiMock.restore.mockResolvedValue(summary('c-1', 'Sampling Basics', 'ACTIVE'));
    conversationApiMock.trash.mockResolvedValue(summary('c-1', 'Sampling Basics', 'TRASHED'));

    renderConversation('/app/conversations/c-1');

    expect(await screen.findByRole('heading', { name: 'What is stratified sampling?' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /rename/i }));
    fireEvent.input(screen.getByLabelText(/conversation title/i), { target: { value: 'Sampling Basics' } });
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));
    expect(await screen.findByRole('heading', { name: 'Sampling Basics' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /archive/i }));
    expect(await screen.findByText('Archived')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /restore/i }));
    expect(await screen.findByText('Active')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /trash/i }));
    await waitFor(() => {
      expect(conversationApiMock.trash).toHaveBeenCalledWith('c-1');
    });
    expect(await screen.findByText('Trash')).toBeInTheDocument();
  });

  it('creates a project conversation with selected ready documents', async () => {
    documentApiMock.list.mockResolvedValue(pageResponse([
      documentItem('d-1', 'DOC-001', 'Market access paper'),
      documentItem('d-2', 'DOC-002', 'Trust and logistics paper'),
    ]));
    conversationApiMock.createProject.mockResolvedValue(
      submitResponse('pc-1', 'What research gaps are supported by my uploaded papers?', 'The papers support market access gaps.', 'p-1'),
    );

    renderConversation('/app/projects/p-1/conversations');

    fireEvent.click(await screen.findByRole('button', { name: /selected sources only/i }));
    fireEvent.click(await screen.findByLabelText(/DOC-001 - Market access paper/i));
    fireEvent.click(screen.getByLabelText(/DOC-002 - Trust and logistics paper/i));
    fireEvent.input(screen.getByRole('textbox', { name: /^message$/i }), { target: { value: 'What research gaps are supported by my uploaded papers?' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(conversationApiMock.createProject).toHaveBeenCalledWith('p-1', {
        content: 'What research gaps are supported by my uploaded papers?',
        scopeType: 'SELECTED_DOCUMENTS',
        documentIds: ['d-1', 'd-2'],
      });
    });
    expect(await screen.findByText(/market access gaps/i)).toBeInTheDocument();
  });

  it('loads a project conversation on route refresh', async () => {
    conversationApiMock.getProject.mockResolvedValue(projectConversationDetail('pc-1', 'p-1'));

    renderConversation('/app/projects/p-1/conversations/pc-1');

    expect(await screen.findByRole('heading', { name: 'Project gaps' })).toBeInTheDocument();
    expect(screen.getByText(/Only project evidence was used/i)).toBeInTheDocument();
    expect(conversationApiMock.getProject).toHaveBeenCalledWith('p-1', 'pc-1');
  });

  it('continues a project conversation through the project-scoped route', async () => {
    conversationApiMock.getProject.mockResolvedValue(projectConversationDetail('pc-1', 'p-1'));
    conversationApiMock.submitProjectMessage.mockResolvedValue(
      submitResponse('pc-1', 'Use only selected sources.', 'The answer stayed inside the project.', 'p-1'),
    );

    renderConversation('/app/projects/p-1/conversations/pc-1');

    expect(await screen.findByRole('heading', { name: 'Project gaps' })).toBeInTheDocument();
    fireEvent.input(screen.getByRole('textbox', { name: /^message$/i }), { target: { value: 'Use only selected sources.' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(conversationApiMock.submitProjectMessage).toHaveBeenCalledWith('p-1', 'pc-1', {
        content: 'Use only selected sources.',
        scopeType: 'PROJECT_ALL_DOCUMENTS',
        documentIds: undefined,
      });
    });
    expect(await screen.findByText(/stayed inside the project/i)).toBeInTheDocument();
  });

  it('moves a general conversation to an authorized project', async () => {
    conversationApiMock.get.mockResolvedValue(conversationDetail('c-1'));
    projectApiMock.mine.mockResolvedValue(pageResponse([{ id: 'p-1', title: 'Farmer-to-Buyer Agricultural Marketplace' }]));
    conversationApiMock.moveToProject.mockResolvedValue(projectSummary('c-1', 'p-1', 'Farmer-to-Buyer Agricultural Marketplace'));

    renderConversation('/app/conversations/c-1');

    await screen.findByRole('heading', { name: 'What is stratified sampling?' });
    fireEvent.change(await screen.findByLabelText(/add to project/i), { target: { value: 'p-1' } });
    fireEvent.click(screen.getByRole('button', { name: /^add$/i }));

    await waitFor(() => {
      expect(conversationApiMock.moveToProject).toHaveBeenCalledWith('c-1', 'p-1');
    });
  });

  it('associates the conversation after creating a project from it', async () => {
    conversationApiMock.get.mockResolvedValue(conversationDetail('c-1'));
    conversationApiMock.moveToProject.mockResolvedValue(projectSummary('c-1', 'p-new', 'Created Project'));

    renderConversation('/app/conversations/c-1');

    await screen.findByRole('heading', { name: 'What is stratified sampling?' });
    fireEvent.click(screen.getByRole('button', { name: /create project from conversation/i }));
    fireEvent.click(await screen.findByRole('button', { name: /confirm create project/i }));

    await waitFor(() => {
      expect(conversationApiMock.moveToProject).toHaveBeenCalledWith('c-1', 'p-new');
    });
  });
});

describe('ConversationSidebarSection', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    projectApiMock.mine.mockResolvedValue(pageResponse([]));
    documentApiMock.list.mockResolvedValue(pageResponse([]));
    conversationApiMock.listProject.mockResolvedValue(pageResponse([]));
  });

  it('shows recent conversations and searches server-side', async () => {
    conversationApiMock.list
      .mockResolvedValueOnce(pageResponse([summary('c-1', 'Digital Agriculture Adoption', 'ACTIVE')]))
      .mockResolvedValue(pageResponse([summary('c-2', 'Sampling Techniques', 'ACTIVE')]));

    renderWithQuery(
      <MemoryRouter>
        <ConversationSidebarSection onNavigate={vi.fn()} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Digital Agriculture Adoption')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/search conversations/i), { target: { value: 'sampling' } });

    await waitFor(() => {
      expect(conversationApiMock.list).toHaveBeenLastCalledWith({ status: 'ACTIVE', q: 'sampling', page: 0, size: 12 });
    });
    expect(await screen.findByText('Sampling Techniques')).toBeInTheDocument();
  });
});

function renderConversation(initialEntry: string) {
  renderWithQuery(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/app/search" element={<ConversationWorkspacePage />} />
        <Route path="/app/conversations/:conversationId" element={<ConversationWorkspacePage />} />
        <Route path="/app/projects/:projectId/conversations" element={<ConversationWorkspacePage />} />
        <Route path="/app/projects/:projectId/conversations/:conversationId" element={<ConversationWorkspacePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function renderWithQuery(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

function conversationDetail(id: string): ConversationDetail {
  return {
    conversation: summary(id, 'What is stratified sampling?', 'ACTIVE'),
    messages: [
      message('m-1', 'USER', 'What is stratified sampling?', 1),
      message('m-2', 'ASSISTANT', 'It divides a population into strata before sampling.', 2),
    ],
    latestRun: {
      id: 'r-1',
      userMessageId: 'm-1',
      assistantMessageId: 'm-2',
      operationType: 'GENERAL_CHAT',
      searchScope: 'GENERAL',
      status: 'COMPLETED',
    },
  };
}

function projectConversationDetail(id: string, projectId: string): ConversationDetail {
  return {
    conversation: projectSummary(id, projectId, 'Farmer-to-Buyer Agricultural Marketplace'),
    messages: [
      message('pm-1', 'USER', 'What research gaps are supported?', 1),
      message('pm-2', 'ASSISTANT', 'Only project evidence was used.', 2),
    ],
    latestRun: {
      id: 'pr-1',
      userMessageId: 'pm-1',
      assistantMessageId: 'pm-2',
      operationType: 'PROJECT_CHAT',
      searchScope: 'PROJECT',
      status: 'COMPLETED',
      ragQueryId: 'rq-1',
    },
  };
}

function submitResponse(id: string, userContent: string, assistantContent: string, projectId?: string): SubmitConversationResponse {
  return {
    conversation: projectId ? projectSummary(id, projectId, 'Farmer-to-Buyer Agricultural Marketplace', userContent) : summary(id, userContent, 'ACTIVE'),
    userMessage: message(`user-${userContent}`, 'USER', userContent, 1),
    assistantMessage: message(`assistant-${assistantContent}`, 'ASSISTANT', assistantContent, 2),
    run: {
      id: `run-${id}`,
      userMessageId: `user-${userContent}`,
      assistantMessageId: `assistant-${assistantContent}`,
      operationType: 'GENERAL_CHAT',
      searchScope: projectId ? 'PROJECT' : 'GENERAL',
      status: 'COMPLETED',
      ragQueryId: projectId ? 'rq-1' : null,
    },
  };
}

function summary(id: string, title: string, status: 'ACTIVE' | 'ARCHIVED' | 'TRASHED') {
  return {
    id,
    userId: 'u-1',
    workspaceId: 'w-1',
    workspaceName: 'My Workspace',
    projectId: null,
    projectTitle: null,
    type: 'GENERAL' as const,
    status,
    title,
    createdAt: '2026-09-28T10:00:00Z',
    updatedAt: '2026-09-28T10:01:00Z',
    lastMessageAt: '2026-09-28T10:01:00Z',
    archivedAt: status === 'ARCHIVED' ? '2026-09-28T10:02:00Z' : null,
    deletedAt: status === 'TRASHED' ? '2026-09-28T10:03:00Z' : null,
  };
}

function projectSummary(id: string, projectId: string, projectTitle: string, title = 'Project gaps') {
  return {
    ...summary(id, title, 'ACTIVE'),
    projectId,
    projectTitle,
    type: 'PROJECT' as const,
  };
}

function documentItem(id: string, documentCode: string, title: string) {
  return {
    id,
    projectId: 'p-1',
    documentCode,
    title,
    status: 'READY',
  };
}

function message(id: string, role: 'USER' | 'ASSISTANT', content: string, sequenceNumber: number) {
  return {
    id,
    role,
    content,
    structuredContent: null,
    sequenceNumber,
    createdAt: '2026-09-28T10:00:00Z',
    updatedAt: null,
    editedAt: null,
    citations: [],
  };
}

function pageResponse<T>(content: T[]) {
  return {
    content,
    page: 0,
    size: 12,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    empty: content.length === 0,
  };
}
