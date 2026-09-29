import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Archive, Edit3, FileText, FolderPlus, MoveRight, Plus, RotateCcw, Search, Send, Trash2 } from 'lucide-react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import { conversationApi, documentApi, projectApi } from '../api/endpoints';
import { AssistantResponse, AssistantSources } from '../components/AssistantResponse';
import { Badge, Button, Field, Input, LoadingButton, Select, Textarea } from '../components/ui';
import { ErrorState, PageLoading } from '../components/states';
import { CreateProjectModal } from '../features/projects/CreateProjectModal';
import { paths } from '../routes/paths';
import type {
  ConversationDetail,
  ConversationMessage,
  ConversationSourceScope,
  ConversationStatus,
  DocumentItem,
  ResearchProject,
  SubmitConversationResponse,
} from '../types/api';

export function ConversationWorkspacePage() {
  const { projectId, conversationId } = useParams<{ projectId?: string; conversationId?: string }>();
  const isNewSearch = !conversationId;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState('');
  const [renameOpen, setRenameOpen] = useState(false);
  const [titleDraft, setTitleDraft] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [sourceScope, setSourceScope] = useState<ConversationSourceScope>('PROJECT_ALL_DOCUMENTS');
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [moveTargetProjectId, setMoveTargetProjectId] = useState('');
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const threadRef = useRef<HTMLElement | null>(null);
  const composerRef = useRef<HTMLTextAreaElement | null>(null);
  const shouldAutoScrollRef = useRef(true);
  const shouldFocusComposerRef = useRef(false);
  const submitInFlightRef = useRef(false);
  const submittedDraftRef = useRef('');

  const conversationQuery = useQuery({
    queryKey: ['conversation', projectId ?? 'general', conversationId],
    queryFn: () =>
      projectId
        ? conversationApi.getProject(projectId, conversationId!)
        : conversationApi.get(conversationId!),
    enabled: Boolean(conversationId),
  });

  const detail = conversationQuery.data;
  const conversation = detail?.conversation;
  const messages = useMemo(() => detail?.messages ?? [], [detail?.messages]);
  const projectMode = Boolean(projectId) || conversation?.type === 'PROJECT' || Boolean(conversation?.projectId);
  const effectiveProjectId = projectId ?? conversation?.projectId ?? '';
  const failedRun = detail?.latestRun?.status === 'FAILED' ? detail.latestRun : null;
  const lastUserMessage = useMemo(
    () => [...messages].reverse().find((message) => message.role === 'USER'),
    [messages],
  );

  const readyDocumentsQuery = useQuery({
    queryKey: ['documents', effectiveProjectId, 'ready-for-conversation'],
    queryFn: () => documentApi.list(effectiveProjectId, 0, 100, { status: 'READY' }),
    enabled: Boolean(effectiveProjectId) && projectMode,
  });

  const authorizedProjectsQuery = useQuery({
    queryKey: ['projects', 'mine', 'conversation-move'],
    queryFn: () => projectApi.mine(0, 100),
    enabled: Boolean(conversationId) && !conversation?.projectId,
  });

  const readyDocuments = readyDocumentsQuery.data?.content ?? [];
  const authorizedProjects = authorizedProjectsQuery.data?.content ?? [];

  const createMutation = useMutation({
    mutationFn: (content: string) => {
      if (projectId) {
        return conversationApi.createProject(projectId, projectPayload(content));
      }
      return conversationApi.create({ content });
    },
    onSuccess: (response) => {
      cacheSubmitResponse(response);
      invalidateConversationLists(response.conversation.projectId ?? projectId);
      setDraft('');
      setFormError(null);
      shouldFocusComposerRef.current = true;
      navigate(conversationPath(response.conversation.id, response.conversation.projectId ?? projectId), { replace: true });
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'Unable to start this conversation.');
      setDraft((current) => current || submittedDraftRef.current);
      shouldFocusComposerRef.current = true;
    },
    onSettled: () => {
      submitInFlightRef.current = false;
    },
  });

  const sendMutation = useMutation({
    mutationFn: ({ id, content }: { id: string; content: string }) =>
      projectId
        ? conversationApi.submitProjectMessage(projectId, id, projectPayload(content))
        : conversationApi.submitMessage(id, projectPayload(content)),
    onSuccess: (response) => {
      cacheSubmitResponse(response);
      invalidateConversationLists(response.conversation.projectId ?? projectId);
      setDraft('');
      setFormError(null);
      shouldFocusComposerRef.current = true;
    },
    onError: (error) => {
      setFormError(error instanceof Error ? error.message : 'Unable to send this message.');
      setDraft((current) => current || submittedDraftRef.current);
      shouldFocusComposerRef.current = true;
    },
    onSettled: () => {
      submitInFlightRef.current = false;
    },
  });

  const retryMutation = useMutation({
    mutationFn: ({ id, messageId }: { id: string; messageId: string }) =>
      projectId
        ? conversationApi.retryProjectMessage(projectId, id, messageId)
        : conversationApi.retryMessage(id, messageId),
    onSuccess: (response) => {
      cacheSubmitResponse(response);
      invalidateConversationLists(response.conversation.projectId ?? projectId);
      setFormError(null);
    },
    onError: (error) => setFormError(error instanceof Error ? error.message : 'Retry failed.'),
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) => conversationApi.rename(id, { title }),
    onSuccess: (updated) => {
      queryClient.setQueryData<ConversationDetail>(['conversation', projectId ?? 'general', updated.id], (current) =>
        current ? { ...current, conversation: updated } : current,
      );
      invalidateConversationLists(updated.projectId ?? projectId);
      setRenameOpen(false);
    },
    onError: (error) => setFormError(error instanceof Error ? error.message : 'Rename failed.'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'archive' | 'restore' | 'trash' }) => conversationApi[action](id),
    onSuccess: (updated) => {
      queryClient.setQueryData<ConversationDetail>(['conversation', projectId ?? 'general', updated.id], (current) =>
        current ? { ...current, conversation: updated } : current,
      );
      invalidateConversationLists(updated.projectId ?? projectId);
    },
    onError: (error) => setFormError(error instanceof Error ? error.message : 'Conversation action failed.'),
  });

  const moveMutation = useMutation({
    mutationFn: ({ id, targetProjectId }: { id: string; targetProjectId: string }) =>
      conversationApi.moveToProject(id, targetProjectId),
    onSuccess: (updated) => {
      queryClient.setQueryData<ConversationDetail>(['conversation', projectId ?? 'general', updated.id], (current) =>
        current ? { ...current, conversation: updated } : current,
      );
      invalidateConversationLists(updated.projectId);
      if (updated.projectId) navigate(paths.projectConversation(updated.projectId, updated.id));
    },
    onError: (error) => setFormError(error instanceof Error ? error.message : 'Unable to add conversation to project.'),
  });

  const isBusy = createMutation.isPending || sendMutation.isPending || retryMutation.isPending;
  const canSend = isNewSearch || conversation?.status === 'ACTIVE';

  function projectPayload(content: string) {
    if (!projectMode) return { content };
    return {
      content,
      scopeType: sourceScope,
      documentIds: sourceScope === 'SELECTED_DOCUMENTS' ? selectedDocuments : undefined,
    };
  }

  function conversationPath(id: string, targetProjectId?: string | null) {
    return targetProjectId ? paths.projectConversation(targetProjectId, id) : paths.conversation(id);
  }

  function invalidateConversationLists(targetProjectId?: string | null) {
    queryClient.invalidateQueries({ queryKey: ['conversations'] });
    if (targetProjectId) {
      queryClient.invalidateQueries({ queryKey: ['project-conversations', targetProjectId] });
    }
  }

  function cacheSubmitResponse(response: SubmitConversationResponse) {
    queryClient.setQueryData<ConversationDetail>(['conversation', response.conversation.projectId ?? projectId ?? 'general', response.conversation.id], (current) => ({
      conversation: response.conversation,
      messages: mergeMessages(current?.messages ?? [], response.userMessage, response.assistantMessage ?? undefined),
      latestRun: response.run,
    }));
  }

  function mergeMessages(current: ConversationMessage[], ...incoming: Array<ConversationMessage | undefined>) {
    const byId = new Map(current.map((message) => [message.id, message]));
    for (const message of incoming) {
      if (message) byId.set(message.id, message);
    }
    return [...byId.values()].sort((a, b) => a.sequenceNumber - b.sequenceNumber);
  }

  function submitDraft() {
    if (submitInFlightRef.current || isBusy || !canSend) return false;

    const content = draft.trim();
    if (!content) {
      setFormError('Enter a question before sending.');
      return false;
    }

    if (projectMode && sourceScope === 'SELECTED_DOCUMENTS' && selectedDocuments.length === 0) {
      setFormError('Select at least one ready document or switch to All Ready Sources.');
      return false;
    }

    submitInFlightRef.current = true;
    submittedDraftRef.current = draft;
    shouldAutoScrollRef.current = true;
    setDraft('');

    if (isNewSearch) {
      createMutation.mutate(content);
      return true;
    }

    if (!conversationId) {
      submitInFlightRef.current = false;
      return false;
    }

    sendMutation.mutate({ id: conversationId, content });
    return true;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submitDraft();
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    submitDraft();
  }

  function handleThreadScroll() {
    const thread = threadRef.current;
    if (!thread) return;
    shouldAutoScrollRef.current = isNearBottom(thread);
  }

  function scrollThreadToBottom() {
    const thread = threadRef.current;
    if (!thread) return;

    if (typeof thread.scrollTo === 'function') {
      thread.scrollTo({ top: thread.scrollHeight, behavior: 'smooth' });
      return;
    }

    thread.scrollTop = thread.scrollHeight;
  }

  function isNearBottom(element: HTMLElement) {
    return element.scrollHeight - element.scrollTop - element.clientHeight < 160;
  }

  function resizeComposerTextarea() {
    const textarea = composerRef.current;
    if (!textarea) return;

    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 160)}px`;
    textarea.style.overflowY = textarea.scrollHeight > 160 ? 'auto' : 'hidden';
  }

  useEffect(() => {
    if (!isNewSearch) return undefined;
    const timeoutId = window.setTimeout(() => {
      composerRef.current?.focus({ preventScroll: true });
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [isNewSearch, projectId]);

  useEffect(() => {
    resizeComposerTextarea();
  }, [draft]);

  useEffect(() => {
    shouldAutoScrollRef.current = true;
    const schedule = window.requestAnimationFrame ?? ((callback: FrameRequestCallback) => window.setTimeout(callback, 0));
    const cancel = window.cancelAnimationFrame ?? window.clearTimeout;
    const frameId = schedule(scrollThreadToBottom);
    return () => cancel(frameId);
  }, [conversationId]);

  useEffect(() => {
    if (!shouldAutoScrollRef.current) return undefined;
    const schedule = window.requestAnimationFrame ?? ((callback: FrameRequestCallback) => window.setTimeout(callback, 0));
    const cancel = window.cancelAnimationFrame ?? window.clearTimeout;
    const frameId = schedule(scrollThreadToBottom);
    return () => cancel(frameId);
  }, [messages.length, isBusy]);

  useEffect(() => {
    if (isBusy || !shouldFocusComposerRef.current) return;
    shouldFocusComposerRef.current = false;
    composerRef.current?.focus({ preventScroll: true });
  }, [isBusy, conversationId]);

  function handleRetry() {
    const messageId = failedRun?.userMessageId ?? lastUserMessage?.id;
    if (!conversationId || !messageId) return;
    retryMutation.mutate({ id: conversationId, messageId });
  }

  function handleRename(event: FormEvent) {
    event.preventDefault();
    if (!conversationId) return;
    const title = titleDraft.trim();
    if (!title) {
      setFormError('Enter a title.');
      return;
    }
    renameMutation.mutate({ id: conversationId, title });
  }

  async function handleProjectCreated(project: ResearchProject) {
    if (!conversationId) return;
    const updated = await conversationApi.moveToProject(conversationId, project.id);
    queryClient.setQueryData<ConversationDetail>(['conversation', projectId ?? 'general', updated.id], (current) =>
      current ? { ...current, conversation: updated } : current,
    );
    invalidateConversationLists(project.id);
    navigate(paths.projectConversation(project.id, updated.id));
  }

  if (conversationQuery.isLoading && !isNewSearch) {
    return <PageLoading label="Loading conversation" />;
  }

  if (conversationQuery.isError && !isNewSearch) {
    return (
      <main className="page conversation-page">
        <ErrorState title="Failed to load conversation" error={conversationQuery.error} onRetry={() => conversationQuery.refetch()} />
      </main>
    );
  }

  return (
    <main className="page conversation-page">
      <div className={projectMode ? 'conversation-project-layout' : 'conversation-layout'}>
        {projectMode && effectiveProjectId ? (
          <ProjectConversationPanel projectId={effectiveProjectId} currentConversationId={conversationId} />
        ) : null}

        <div className="conversation-workspace">
          <header className="conversation-header">
            <div>
              <div className="conversation-kicker">{projectMode ? 'PROJECT ASSISTANT' : 'SKILITE SCHOLAR'}</div>
              <h1 className="page-title">
                {isNewSearch ? (projectMode ? 'New Project Conversation' : 'New Search') : conversation?.title ?? 'Conversation'}
              </h1>
              {!isNewSearch && conversation ? (
                <div className="conversation-meta">
                  <StatusBadge status={conversation.status} />
                  {conversation.projectTitle ? <span>{conversation.projectTitle}</span> : null}
                  {conversation.lastMessageAt ? <span>Updated {formatDate(conversation.lastMessageAt)}</span> : null}
                </div>
              ) : null}
            </div>

            {!isNewSearch && conversation ? (
              <div className="toolbar conversation-actions">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setTitleDraft(conversation.title);
                    setRenameOpen((value) => !value);
                  }}
                >
                  <Edit3 size={15} /> Rename
                </Button>
                {conversation.status === 'ACTIVE' ? (
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: conversation.id, action: 'archive' })}
                  >
                    <Archive size={15} /> Archive
                  </Button>
                ) : (
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: conversation.id, action: 'restore' })}
                  >
                    <RotateCcw size={15} /> Restore
                  </Button>
                )}
                {conversation.status !== 'TRASHED' ? (
                  <Button
                    type="button"
                    variant="danger"
                    disabled={statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ id: conversation.id, action: 'trash' })}
                  >
                    <Trash2 size={15} /> Trash
                  </Button>
                ) : null}
              </div>
            ) : null}
          </header>

          {!isNewSearch && conversation && !conversation.projectId ? (
            <section className="conversation-project-actions" aria-label="Project actions">
              <Field label="Add to Project">
                <div className="conversation-inline-controls">
                  <Select value={moveTargetProjectId} onChange={(event) => setMoveTargetProjectId(event.target.value)}>
                    <option value="">Select authorized project</option>
                    {authorizedProjects.map((project) => (
                      <option key={project.id} value={project.id}>{project.title}</option>
                    ))}
                  </Select>
                  <LoadingButton
                    type="button"
                    variant="secondary"
                    loading={moveMutation.isPending}
                    loadingLabel="Adding..."
                    disabled={!moveTargetProjectId}
                    onClick={() => moveMutation.mutate({ id: conversation.id, targetProjectId: moveTargetProjectId })}
                  >
                    <MoveRight size={15} /> Add
                  </LoadingButton>
                  <Button type="button" variant="secondary" onClick={() => setCreateProjectOpen(true)}>
                    <FolderPlus size={15} /> Create Project from Conversation
                  </Button>
                </div>
              </Field>
            </section>
          ) : null}

          {renameOpen && conversation ? (
            <form className="conversation-rename-panel" onSubmit={handleRename}>
              <Input
                value={titleDraft}
                onChange={(event) => setTitleDraft(event.target.value)}
                aria-label="Conversation title"
                maxLength={255}
              />
              <LoadingButton type="submit" loading={renameMutation.isPending} loadingLabel="Saving...">
                Save
              </LoadingButton>
              <Button type="button" variant="secondary" onClick={() => setRenameOpen(false)}>
                Cancel
              </Button>
            </form>
          ) : null}

          <section ref={threadRef} className="conversation-thread" aria-label="Conversation messages" onScroll={handleThreadScroll}>
            {messages.length === 0 ? (
              <div className="conversation-start">
                <h2>{projectMode ? 'Ask about this project' : 'Ask a general research question'}</h2>
                <p className="muted">
                  {projectMode
                    ? 'Project answers use authorized ready project sources only.'
                    : 'Start with a question that does not require project sources or web search.'}
                </p>
              </div>
            ) : (
              messages.map((message) => <MessageBubble key={message.id} message={message} />)
            )}
            {isBusy ? (
              <div className="conversation-message assistant">
                <div className="conversation-message-role">Assistant</div>
                <div className="conversation-message-body muted">Generating response...</div>
              </div>
            ) : null}
          </section>

          {failedRun ? (
            <div className="conversation-failure" role="alert">
              <div>
                <strong>Generation failed</strong>
                <p>{failedRun.failureMessage || 'No assistant message was created. You can retry the last user message.'}</p>
              </div>
              <LoadingButton type="button" variant="secondary" loading={retryMutation.isPending} loadingLabel="Retrying..." onClick={handleRetry}>
                Retry
              </LoadingButton>
            </div>
          ) : null}

          {projectMode ? (
            <ProjectSourceControls
              sourceScope={sourceScope}
              onScopeChange={setSourceScope}
              readyDocuments={readyDocuments}
              selectedDocuments={selectedDocuments}
              onSelectedDocumentsChange={setSelectedDocuments}
            />
          ) : null}

          {formError ? <div className="conversation-form-error" role="alert">{formError}</div> : null}

          <form className="conversation-composer" onSubmit={handleSubmit}>
            <Textarea
              ref={composerRef}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={handleComposerKeyDown}
              placeholder={canSend ? (projectMode ? 'Ask about this project...' : 'Ask a general AI question...') : 'Restore this conversation before continuing.'}
              aria-label="Message"
              disabled={!canSend || isBusy}
              rows={1}
            />
            <div className="conversation-composer-footer">
              {!canSend ? <span className="muted">Archived and trashed conversations are read-only until restored.</span> : <span />}
              <LoadingButton type="submit" loading={isBusy} loadingLabel="Sending..." disabled={!canSend}>
                <Send size={15} /> Send
              </LoadingButton>
            </div>
          </form>
        </div>
      </div>

      {createProjectOpen ? (
        <CreateProjectModal
          open={createProjectOpen}
          onClose={() => setCreateProjectOpen(false)}
          defaultWorkspaceId={conversation?.workspaceId ?? undefined}
          onCreated={handleProjectCreated}
        />
      ) : null}
    </main>
  );
}

function ProjectConversationPanel({ projectId, currentConversationId }: { projectId: string; currentConversationId?: string }) {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const conversationsQuery = useQuery({
    queryKey: ['project-conversations', projectId, query.trim(), page],
    queryFn: () => conversationApi.listProject(projectId, { status: 'ACTIVE', q: query.trim() || undefined, page, size: 10 }),
  });
  const conversations = conversationsQuery.data?.content ?? [];

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    setPage(0);
  }

  return (
    <aside className="project-conversation-panel" aria-label="Project conversations">
      <NavLink className="button primary" to={paths.projectConversations(projectId)}>
        <Plus size={15} /> New Project Conversation
      </NavLink>
      <label className="conversation-search-field">
        <Search size={15} aria-hidden />
        <input
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setPage(0);
          }}
          onKeyDown={handleSearchKeyDown}
          placeholder="Search conversations"
          aria-label="Search project conversations"
        />
      </label>
      <div className="nav-title conversation-list-title">Recent Project Conversations</div>
      <div className="conversation-link-list" aria-busy={conversationsQuery.isFetching}>
        {conversations.map((conversation) => (
          <NavLink
            key={conversation.id}
            className={`conversation-link ${currentConversationId === conversation.id ? 'active' : ''}`}
            to={paths.projectConversation(projectId, conversation.id)}
            title={conversation.title}
          >
            <span>{conversation.title || 'Untitled conversation'}</span>
          </NavLink>
        ))}
        {!conversationsQuery.isLoading && conversations.length === 0 ? (
          <div className="conversation-empty">{query.trim() ? 'No matches' : 'No project conversations'}</div>
        ) : null}
      </div>
      {(conversationsQuery.data?.totalPages ?? 0) > 1 ? (
        <div className="conversation-sidebar-pagination">
          <button type="button" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Prev</button>
          <span>{page + 1}/{conversationsQuery.data?.totalPages ?? 1}</span>
          <button type="button" disabled={page + 1 >= (conversationsQuery.data?.totalPages ?? 1)} onClick={() => setPage((value) => value + 1)}>Next</button>
        </div>
      ) : null}
    </aside>
  );
}

function ProjectSourceControls({
  sourceScope,
  onScopeChange,
  readyDocuments,
  selectedDocuments,
  onSelectedDocumentsChange,
}: {
  sourceScope: ConversationSourceScope;
  onScopeChange: (scope: ConversationSourceScope) => void;
  readyDocuments: DocumentItem[];
  selectedDocuments: string[];
  onSelectedDocumentsChange: (ids: string[]) => void;
}) {
  function toggleDocument(documentId: string) {
    onSelectedDocumentsChange(
      selectedDocuments.includes(documentId)
        ? selectedDocuments.filter((id) => id !== documentId)
        : [...selectedDocuments, documentId],
    );
  }

  return (
    <section className="project-source-controls" aria-label="Project source scope">
      <div className="conversation-status-tabs" aria-label="Source scope">
        <button
          type="button"
          className={sourceScope === 'PROJECT_ALL_DOCUMENTS' ? 'active' : ''}
          onClick={() => onScopeChange('PROJECT_ALL_DOCUMENTS')}
        >
          All Ready Sources
        </button>
        <button
          type="button"
          className={sourceScope === 'SELECTED_DOCUMENTS' ? 'active' : ''}
          onClick={() => onScopeChange('SELECTED_DOCUMENTS')}
        >
          Selected Sources Only
        </button>
      </div>
      {sourceScope === 'SELECTED_DOCUMENTS' ? (
        <div className="project-source-list">
          {readyDocuments.map((document) => (
            <label key={document.id} className="project-source-option">
              <input
                type="checkbox"
                checked={selectedDocuments.includes(document.id)}
                onChange={() => toggleDocument(document.id)}
              />
              <FileText size={14} aria-hidden />
              <span>{document.documentCode ?? document.docCode ?? 'DOC'} - {document.title ?? document.bibliographicTitle ?? 'Untitled source'}</span>
            </label>
          ))}
          {readyDocuments.length === 0 ? <div className="conversation-empty">No ready sources available</div> : null}
        </div>
      ) : null}
    </section>
  );
}

function MessageBubble({ message }: { message: ConversationMessage }) {
  const isUser = message.role === 'USER';
  return (
    <article className={`conversation-message ${isUser ? 'user' : 'assistant'}`}>
      <div className="conversation-message-role">{isUser ? 'You' : 'Assistant'}</div>
      {isUser ? (
        <div className="conversation-message-body">{message.content}</div>
      ) : (
        <div className="conversation-message-body">
          <AssistantResponse content={message.content} />
        </div>
      )}
      {!isUser && message.citations?.length ? (
        <AssistantSources
          sources={message.citations}
          renderSource={(citation, index) => (
            <div className="citation">
              <strong>[{citation.number ?? index + 1}] {citation.formattedCitation ?? citation.documentCode ?? 'Source'}</strong>
              <p className="muted">
                {citation.documentTitle ?? citation.title ?? 'Project source'}
                {citation.pageNumber ? `, p. ${citation.pageNumber}` : ''}
              </p>
              {citation.supportingExcerpt ? <p>{citation.supportingExcerpt}</p> : null}
            </div>
          )}
        />
      ) : null}
    </article>
  );
}

function StatusBadge({ status }: { status: ConversationStatus }) {
  if (status === 'ARCHIVED') return <Badge tone="warning">Archived</Badge>;
  if (status === 'TRASHED') return <Badge tone="danger">Trash</Badge>;
  return <Badge tone="success">Active</Badge>;
}

function formatDate(value: string) {
  return new Date(value).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
