import { useDeferredValue, useState, type KeyboardEvent } from 'react';
import { NavLink } from 'react-router-dom';
import { Archive, MessageSquare, Plus, RotateCcw, Search, Trash2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { conversationApi } from '../../api/endpoints';
import type { ConversationStatus } from '../../types/api';
import { paths } from '../../routes/paths';

const statusOptions: Array<{ value: ConversationStatus; label: string }> = [
  { value: 'ACTIVE', label: 'Recent' },
  { value: 'ARCHIVED', label: 'Archived' },
  { value: 'TRASHED', label: 'Trash' },
];

export function ConversationSidebarSection({ onNavigate }: { onNavigate: () => void }) {
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<ConversationStatus>('ACTIVE');
  const [page, setPage] = useState(0);
  const deferredQuery = useDeferredValue(query.trim());

  const conversationsQuery = useQuery({
    queryKey: ['conversations', 'sidebar', status, deferredQuery, page],
    queryFn: () => conversationApi.list({ status, q: deferredQuery || undefined, page, size: 12 }),
  });

  const conversations = conversationsQuery.data?.content ?? [];
  const totalPages = conversationsQuery.data?.totalPages ?? 0;
  const StatusIcon = status === 'ARCHIVED' ? Archive : status === 'TRASHED' ? Trash2 : MessageSquare;

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    setPage(0);
  }

  return (
    <div className="nav-group conversation-sidebar-section">
      <div className="nav-title scholar-title">SKILITE SCHOLAR</div>
      <NavLink className="button primary conversation-new-search" to={paths.search} onClick={onNavigate}>
        <Plus size={16} aria-hidden />
        <span>New Search</span>
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
          aria-label="Search conversations"
        />
      </label>

      <div className="conversation-status-tabs" aria-label="Conversation status">
        {statusOptions.map((option) => (
          <button
            key={option.value}
            type="button"
            className={option.value === status ? 'active' : ''}
            onClick={() => {
              setStatus(option.value);
              setPage(0);
            }}
          >
            {option.label}
          </button>
        ))}
      </div>

      <div className="nav-title conversation-list-title">
        <StatusIcon size={13} aria-hidden />
        <span>{statusOptions.find((option) => option.value === status)?.label ?? 'Recent'}</span>
      </div>

      <div className="conversation-link-list" aria-busy={conversationsQuery.isFetching}>
        {conversations.map((conversation) => (
          <NavLink
            key={conversation.id}
            className="conversation-link"
            to={paths.conversation(conversation.id)}
            onClick={onNavigate}
            title={conversation.title}
          >
            <span>{conversation.title || 'Untitled conversation'}</span>
          </NavLink>
        ))}
        {!conversationsQuery.isLoading && conversations.length === 0 ? (
          <div className="conversation-empty">
            {deferredQuery ? 'No matches' : status === 'ACTIVE' ? 'No recent conversations' : `No ${status.toLowerCase()} conversations`}
          </div>
        ) : null}
      </div>

      {totalPages > 1 ? (
        <div className="conversation-sidebar-pagination">
          <button type="button" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
            <RotateCcw size={12} aria-hidden />
            Prev
          </button>
          <span>{page + 1}/{totalPages}</span>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>
            Next
          </button>
        </div>
      ) : null}
    </div>
  );
}
