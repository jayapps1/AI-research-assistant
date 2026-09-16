import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Inbox,
  Search,
  Send,
} from 'lucide-react';
import { publicApi } from '../../api/public';
import type {
  ContactSubmissionStatus,
  ContactResponseChannel,
} from '../../types/publicSite';

export function AdminContactInboxPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Detail queries and reply states
  const [replyMessage, setReplyMessage] = useState<string>('');
  const [replyChannel, setReplyChannel] = useState<ContactResponseChannel>('EMAIL');

  const { data: submissionsPage, isLoading } = useQuery({
    queryKey: ['adminContactSubmissions', statusFilter, searchQuery, page],
    queryFn: () =>
      publicApi.getAdminContactSubmissions({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: searchQuery || undefined,
        page,
        size: 15,
      }),
  });

  const { data: activeDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['adminContactDetail', selectedId],
    queryFn: () => (selectedId ? publicApi.getAdminContactSubmissionDetail(selectedId) : null),
    enabled: !!selectedId,
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      publicApi.updateAdminContactSubmissionStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminContactSubmissions'] });
      queryClient.invalidateQueries({ queryKey: ['adminContactDetail', selectedId] });
    },
  });

  const replyMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: { messageBody: string; channel?: string } }) =>
      publicApi.replyToAdminContactSubmission(id, payload),
    onSuccess: () => {
      setReplyMessage('');
      queryClient.invalidateQueries({ queryKey: ['adminContactSubmissions'] });
      queryClient.invalidateQueries({ queryKey: ['adminContactDetail', selectedId] });
    },
  });

  const getStatusBadgeClass = (status: ContactSubmissionStatus) => {
    switch (status) {
      case 'NEW':
        return 'badge-primary';
      case 'READ':
        return 'badge-secondary';
      case 'IN_PROGRESS':
        return 'badge-warning';
      case 'RESOLVED':
        return 'badge-success';
      case 'CLOSED':
        return 'badge-subtle';
      case 'SPAM':
        return 'badge-danger';
      default:
        return 'badge-subtle';
    }
  };

  return (
    <div className="admin-contact-inbox-page p-6" id="admin-contact-inbox-root">
      <div className="admin-header mb-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Inbox size={24} className="text-primary" />
          <span>Contact Submissions & Inquiries</span>
        </h1>
        <p className="text-muted">
          Manage visitor and institutional messages, reference codes, internal review notes, and email replies.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="filter-bar flex flex-wrap gap-4 items-center justify-between mb-6">
        <div className="search-input-wrap flex items-center gap-2 flex-1 max-w-md">
          <Search size={18} className="text-muted" />
          <input
            type="text"
            className="input w-full"
            placeholder="Search by reference code, name, email, or subject..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <div className="flex gap-2">
          {['ALL', 'NEW', 'READ', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((st) => (
            <button
              key={st}
              type="button"
              className={`btn btn-sm ${statusFilter === st ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => {
                setStatusFilter(st);
                setPage(0);
              }}
            >
              {st === 'ALL' ? 'All Inquiries' : st}
            </button>
          ))}
        </div>
      </div>

      {/* Main Split Grid: Table on Left, Detail on Right */}
      <div className="inbox-layout-grid grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Submissions Table */}
        <div className={`${selectedId ? 'lg:col-span-5' : 'lg:col-span-12'} card p-4 bg-card border rounded-lg`}>
          {isLoading ? (
            <p className="p-4 text-center">Loading submissions...</p>
          ) : !submissionsPage?.content || submissionsPage.content.length === 0 ? (
            <div className="text-center p-8 text-muted">
              <Inbox size={32} className="mx-auto mb-2 text-muted" />
              <p>No contact submissions match the current filter.</p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table w-full text-sm">
                <thead>
                  <tr>
                    <th>Ref Code</th>
                    <th>Sender</th>
                    {!selectedId && <th>Subject</th>}
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {submissionsPage.content.map((sub) => {
                    const isSelected = sub.id === selectedId;
                    return (
                      <tr
                        key={sub.id}
                        className={`cursor-pointer hover:bg-muted/50 ${isSelected ? 'bg-primary/10 font-medium' : ''}`}
                        onClick={() => setSelectedId(sub.id)}
                      >
                        <td className="font-mono text-xs">{sub.referenceCode}</td>
                        <td>
                          <div className="font-medium truncate max-w-[120px]">{sub.name}</div>
                          <div className="text-xs text-muted truncate max-w-[120px]">{sub.email}</div>
                        </td>
                        {!selectedId && (
                          <td className="max-w-[180px] truncate">{sub.subject}</td>
                        )}
                        <td>
                          <span className={`badge ${getStatusBadgeClass(sub.status)}`}>
                            {sub.status}
                          </span>
                        </td>
                        <td className="text-xs text-muted">
                          {new Date(sub.submittedAt).toLocaleDateString()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {/* Pagination controls */}
              <div className="flex justify-between items-center mt-4 pt-4 border-t text-xs text-muted">
                <span>
                  Page {page + 1} of {submissionsPage.totalPages || 1} ({submissionsPage.totalElements} items)
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    disabled={page + 1 >= (submissionsPage.totalPages || 1)}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Submission Detail View */}
        {selectedId && (
          <div className="lg:col-span-7 card p-6 bg-card border rounded-lg flex flex-col">
            {detailLoading || !activeDetail ? (
              <p className="p-8 text-center">Loading inquiry details...</p>
            ) : (
              <div className="submission-detail-pane flex flex-col h-full">
                {/* Header with Reference Code and Status Changer */}
                <div className="flex justify-between items-start border-b pb-4 mb-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-bold bg-muted px-2 py-0.5 rounded">
                        {activeDetail.referenceCode}
                      </span>
                      <span className={`badge ${getStatusBadgeClass(activeDetail.status)}`}>
                        {activeDetail.status}
                      </span>
                    </div>
                    <h2 className="text-xl font-bold mt-2">{activeDetail.subject}</h2>
                  </div>

                  <div className="flex items-center gap-2">
                    <select
                      className="input select text-xs py-1"
                      value={activeDetail.status}
                      onChange={(e) =>
                        updateStatusMutation.mutate({
                          id: activeDetail.id,
                          status: e.target.value,
                        })
                      }
                      aria-label="Update status"
                    >
                      <option value="NEW">NEW</option>
                      <option value="READ">READ</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="RESOLVED">RESOLVED</option>
                      <option value="CLOSED">CLOSED</option>
                      <option value="SPAM">SPAM</option>
                    </select>

                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={() => setSelectedId(null)}
                      title="Close detail pane"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                {/* Sender Metadata Bar */}
                <div className="grid grid-cols-2 gap-3 text-xs bg-muted/40 p-3 rounded-lg mb-4">
                  <div>
                    <span className="text-muted block">Sender:</span>
                    <strong>{activeDetail.name}</strong> ({activeDetail.email})
                  </div>
                  <div>
                    <span className="text-muted block">Academic Role:</span>
                    <span>{activeDetail.academicRole || 'Not specified'}</span>
                  </div>
                  <div>
                    <span className="text-muted block">Organization:</span>
                    <span>{activeDetail.organization || 'Not specified'}</span>
                  </div>
                  <div>
                    <span className="text-muted block">Submitted Date:</span>
                    <span>{new Date(activeDetail.submittedAt).toLocaleString()}</span>
                  </div>
                </div>

                {/* Message Content */}
                <div className="submission-body bg-muted/20 p-4 rounded-lg border mb-6 flex-1 overflow-auto">
                  <h4 className="font-medium text-xs text-muted mb-2 uppercase tracking-wide">Inquiry Message:</h4>
                  <p className="whitespace-pre-wrap leading-relaxed">{activeDetail.message}</p>
                </div>

                {/* Thread Responses & Notes */}
                {activeDetail.responses && activeDetail.responses.length > 0 && (
                  <div className="responses-history mb-6">
                    <h4 className="font-semibold text-sm mb-3">Communication & Internal Notes History:</h4>
                    <div className="space-y-3 max-h-48 overflow-auto">
                      {activeDetail.responses.map((resp) => (
                        <div
                          key={resp.id}
                          className={`p-3 rounded-lg text-xs border ${
                            resp.channel === 'INTERNAL_NOTE'
                              ? 'bg-warning/10 border-warning/30'
                              : 'bg-primary/10 border-primary/30'
                          }`}
                        >
                          <div className="flex justify-between font-medium mb-1">
                            <span className="flex items-center gap-1">
                              {resp.channel === 'INTERNAL_NOTE' ? '📝 Internal Note' : '✉️ Email Sent'}
                              {resp.respondedByName && ` by ${resp.respondedByName}`}
                            </span>
                            <span className="text-muted">{new Date(resp.createdAt).toLocaleString()}</span>
                          </div>
                          <p className="whitespace-pre-wrap">{resp.messageBody}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Reply / Note Composer */}
                <div className="reply-composer border-t pt-4">
                  <div className="flex justify-between items-center mb-2">
                    <div className="flex gap-4 text-xs font-medium">
                      <label className="flex items-center gap-1 cursor-pointer">
                        <input
                          type="radio"
                          name="channel"
                          value="EMAIL"
                          checked={replyChannel === 'EMAIL'}
                          onChange={() => setReplyChannel('EMAIL')}
                        />
                        <span>Email Submitter ({activeDetail.email})</span>
                      </label>
                      <label className="flex items-center gap-1 cursor-pointer">
                        <input
                          type="radio"
                          name="channel"
                          value="INTERNAL_NOTE"
                          checked={replyChannel === 'INTERNAL_NOTE'}
                          onChange={() => setReplyChannel('INTERNAL_NOTE')}
                        />
                        <span>Internal Staff Note</span>
                      </label>
                    </div>
                  </div>

                  <textarea
                    rows={3}
                    className="input textarea w-full text-sm"
                    placeholder={
                      replyChannel === 'INTERNAL_NOTE'
                        ? 'Write an internal note for administrative staff...'
                        : `Compose email response to ${activeDetail.name}...`
                    }
                    value={replyMessage}
                    onChange={(e) => setReplyMessage(e.target.value)}
                  />

                  <div className="flex justify-end mt-2">
                    <button
                      type="button"
                      className="btn btn-primary btn-sm flex items-center gap-2"
                      disabled={!replyMessage.trim() || replyMutation.isPending}
                      onClick={() =>
                        replyMutation.mutate({
                          id: activeDetail.id,
                          payload: {
                            messageBody: replyMessage.trim(),
                            channel: replyChannel,
                          },
                        })
                      }
                    >
                      <Send size={14} />
                      <span>{replyMutation.isPending ? 'Sending...' : 'Record Response'}</span>
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
