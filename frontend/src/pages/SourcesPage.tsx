import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  CheckCircle2,
  Download,
  Edit3,
  FileText,
  MoreVertical,
  RefreshCw,
  RotateCcw,
  Sparkles,
  Trash2,
  Upload,
} from 'lucide-react';
import { documentApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Badge, Pagination, Modal } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { pageContent } from '../utils/collections';
import { paths } from '../routes/paths';
import type { DocumentItem } from '../types/api';

interface UploadQueueItem {
  id: string;
  file: File;
  status: 'QUEUED' | 'UPLOADING' | 'READY' | 'FAILED' | 'QUOTA_EXCEEDED';
  documentCode?: string;
  errorMessage?: string | null;
}

type DialogState =
  | { type: 'details'; doc: DocumentItem }
  | { type: 'rename'; doc: DocumentItem }
  | { type: 'new-version'; doc: DocumentItem }
  | { type: 'trash'; doc: DocumentItem }
  | { type: 'permanent-delete'; doc: DocumentItem }
  | null;

export function SourcesPage() {
  const projectId = useProjectId();
  const navigate = useNavigate();
  const client = useQueryClient();
  const [page, setPage] = useState(0);
  const [showTrash, setShowTrash] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const [isUploadingQueue, setIsUploadingQueue] = useState(false);
  const [workingId, setWorkingId] = useState<string | null>(null);
  const [bulkWorking, setBulkWorking] = useState(false);
  const [dialog, setDialog] = useState<DialogState>(null);
  const [renameTitle, setRenameTitle] = useState('');
  const [bibliographicTitle, setBibliographicTitle] = useState('');
  const [authors, setAuthors] = useState('');
  const [publicationYear, setPublicationYear] = useState('');
  const [journal, setJournal] = useState('');
  const [conference, setConference] = useState('');
  const [publisher, setPublisher] = useState('');
  const [volume, setVolume] = useState('');
  const [issue, setIssue] = useState('');
  const [pages, setPages] = useState('');
  const [doi, setDoi] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [sourceType, setSourceType] = useState('');
  const [sourceKeywords, setSourceKeywords] = useState('');
  const [newVersionFile, setNewVersionFile] = useState<File | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const sourcesQuery = useQuery({
    queryKey: ['documents', projectId, page, showTrash ? 'trash' : 'active'],
    queryFn: () => documentApi.list(projectId, page, 20, showTrash ? { status: 'ARCHIVED' } : undefined),
    enabled: Boolean(projectId),
    refetchInterval: (query) => {
      const data = query.state.data;
      const items = pageContent(data) as DocumentItem[];
      const hasPending = items.some((doc) => doc.status === 'PROCESSING' || doc.status === 'UPLOADING');
      return hasPending ? 3000 : false;
    },
  });

  const detailsDoc = dialog?.type === 'details' ? dialog.doc : null;
  const versionsQuery = useQuery({
    queryKey: ['document-versions', detailsDoc?.id],
    queryFn: () => documentApi.versions(detailsDoc!.id),
    enabled: Boolean(detailsDoc?.id),
  });

  const invalidateDocuments = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: ['documents', projectId] }),
      client.invalidateQueries({ queryKey: ['project-dashboard', projectId] }),
      client.invalidateQueries({ queryKey: ['workspace-usage'] }),
      client.invalidateQueries({ queryKey: ['rag-conversations', projectId] }),
    ]);
    setSelectedIds(new Set());
  };

  if (!projectId) {
    return (
      <main className="page">
        <EmptyState title="Select a project" description="Open a research project to manage its uploaded research sources.">
          <Button asChild style={{ marginTop: 12 }}>
            <a href={paths.projects}>Browse Projects</a>
          </Button>
        </EmptyState>
      </main>
    );
  }

  if (sourcesQuery.isLoading && !sourcesQuery.data) {
    return <PageLoading label="Loading research sources..." />;
  }

  if (sourcesQuery.isError) {
    return <ErrorState title="Failed to load sources" error={sourcesQuery.error} onRetry={() => sourcesQuery.refetch()} />;
  }

  const sources = pageContent(sourcesQuery.data) as DocumentItem[];
  const totalSources = sourcesQuery.data?.totalElements ?? sources.length;
  const selectedDocs = sources.filter((doc) => selectedIds.has(doc.id));
  const allSelected = sources.length > 0 && sources.every((doc) => selectedIds.has(doc.id));

  const startBatchUpload = async () => {
    if (uploadQueue.length === 0 || isUploadingQueue) return;
    setIsUploadingQueue(true);
    setErrorMessage(null);

    const pendingItems = uploadQueue.filter((item) => item.status === 'QUEUED' || item.status === 'FAILED');
    let index = 0;

    const uploadWorker = async () => {
      while (index < pendingItems.length) {
        const currentItem = pendingItems[index++];
        setUploadQueue((prev) =>
          prev.map((item) => (item.id === currentItem.id ? { ...item, status: 'UPLOADING', errorMessage: null } : item)),
        );

        try {
          const result = await documentApi.upload(projectId, currentItem.file);
          const allocatedCode =
            result.documentCode ??
            result.docCode ??
            (result.documentNumber ? `DOC-${String(result.documentNumber).padStart(3, '0')}` : 'DOC');
          setUploadQueue((prev) =>
            prev.map((item) => (item.id === currentItem.id ? { ...item, status: 'READY', documentCode: allocatedCode } : item)),
          );
        } catch (err: any) {
          const errStatus = err?.status ?? err?.response?.status;
          const errCode = err?.code ?? err?.response?.data?.code;
          const isQuota = errStatus === 429 || errCode === 'QUOTA_EXCEEDED';
          setUploadQueue((prev) =>
            prev.map((item) =>
              item.id === currentItem.id
                ? { ...item, status: isQuota ? 'QUOTA_EXCEEDED' : 'FAILED', errorMessage: isQuota ? 'Storage quota exceeded' : err?.message || 'Upload failed' }
                : item,
            ),
          );
        }
      }
    };

    await Promise.all(Array.from({ length: Math.min(2, pendingItems.length) }, () => uploadWorker()));
    setIsUploadingQueue(false);
    await invalidateDocuments();
  };

  const runForDoc = async (doc: DocumentItem, operation: () => Promise<unknown>) => {
    setWorkingId(doc.id);
    setErrorMessage(null);
    try {
      await operation();
      setDialog(null);
      setNewVersionFile(null);
      setDeleteConfirm('');
      await invalidateDocuments();
    } catch (err: any) {
      setErrorMessage(err?.message ?? 'Operation failed.');
    } finally {
      setWorkingId(null);
    }
  };

  const downloadDocument = async (doc: DocumentItem) => {
    setWorkingId(doc.id);
    setErrorMessage(null);
    try {
      const result = await documentApi.download(doc.id);
      const url = URL.createObjectURL(result.blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = result.filename;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setErrorMessage(err?.message ?? 'Download failed.');
    } finally {
      setWorkingId(null);
    }
  };

  const openMetadataDialog = (doc: DocumentItem) => {
    setRenameTitle(doc.title ?? '');
    setBibliographicTitle(doc.bibliographicTitle ?? doc.title ?? '');
    setAuthors(doc.authors ?? '');
    setPublicationYear(doc.publicationYear ? String(doc.publicationYear) : '');
    setJournal(doc.journal ?? '');
    setConference(doc.conference ?? '');
    setPublisher(doc.publisher ?? '');
    setVolume(doc.volume ?? '');
    setIssue(doc.issue ?? '');
    setPages(doc.pages ?? '');
    setDoi(doc.doi ?? '');
    setSourceUrl(doc.url ?? '');
    setSourceType(doc.sourceType ?? '');
    setSourceKeywords(doc.keywords ?? '');
    setDialog({ type: 'rename', doc });
  };

  const bulkAction = async (kind: 'trash' | 'retry' | 'download') => {
    setBulkWorking(true);
    setErrorMessage(null);
    try {
      for (const doc of selectedDocs) {
        if (kind === 'trash') await documentApi.delete(doc.id);
        if (kind === 'retry' && canRetry(doc)) await documentApi.retryProcessing(doc.id);
        if (kind === 'download') await downloadDocument(doc);
      }
      await invalidateDocuments();
    } catch (err: any) {
      setErrorMessage(err?.message ?? 'Bulk action failed.');
    } finally {
      setBulkWorking(false);
    }
  };

  const hasQuotaExceeded = uploadQueue.some((item) => item.status === 'QUOTA_EXCEEDED');

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', showTrash ? 'Sources Trash' : 'Sources']} />

      <div className="page-header" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: '1.65rem', fontWeight: 700, margin: 0 }}>Research Sources</h1>
          <p className="muted" style={{ fontSize: '0.92rem', marginTop: 4 }}>
            Manage uploaded research papers and their immutable source versions.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <Button type="button" variant="secondary" onClick={() => setShowTrash((value) => !value)}>
            {showTrash ? <FileText size={15} /> : <Trash2 size={15} />} {showTrash ? 'Active Sources' : 'Trash'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate(paths.projectAssistant(projectId))}>
            <Sparkles size={15} /> Open Research Assistant
          </Button>
        </div>
      </div>

      {!showTrash && (
        <Card style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, gap: 12, flexWrap: 'wrap' }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <Upload size={18} /> Upload Research Sources
            </h2>
            <span className="muted" style={{ fontSize: '0.8rem' }}>PDF, DOCX, TXT up to 50MB per file</span>
          </div>

          {hasQuotaExceeded && (
            <div className="alert warning" style={{ marginBottom: 16 }}>
              <strong>Storage quota exceeded.</strong>
              <Button type="button" variant="primary" className="btn-compact" style={{ marginLeft: 12 }} onClick={() => navigate(paths.billing)}>
                Upgrade Storage
              </Button>
            </div>
          )}

          <div style={{ display: 'flex', gap: 16, alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: 260 }}>
              <Field label="Select Research Papers or Documents">
                <Input
                  type="file"
                  multiple
                  accept=".pdf,.doc,.docx,.txt"
                  onChange={(event) => {
                    if (event.target.files && event.target.files.length > 0) {
                      const newItems: UploadQueueItem[] = Array.from(event.target.files).map((file) => ({
                        id: `${file.name}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
                        file,
                        status: 'QUEUED',
                      }));
                      setUploadQueue((prev) => [...prev, ...newItems]);
                    }
                    event.target.value = '';
                  }}
                />
              </Field>
            </div>
            {uploadQueue.length > 0 && (
              <div style={{ display: 'flex', gap: 8, marginBottom: 4 }}>
                <Button type="button" variant="primary" disabled={isUploadingQueue} onClick={startBatchUpload}>
                  {isUploadingQueue ? <RefreshCw size={14} className="spin" /> : <Upload size={14} />}
                  {isUploadingQueue ? 'Uploading...' : `Upload ${uploadQueue.filter((q) => q.status === 'QUEUED' || q.status === 'FAILED').length} Files`}
                </Button>
                <Button type="button" variant="secondary" disabled={isUploadingQueue} onClick={() => setUploadQueue([])}>Clear Queue</Button>
              </div>
            )}
          </div>

          {uploadQueue.length > 0 && (
            <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 180, overflowY: 'auto' }}>
              {uploadQueue.map((item) => (
                <div key={item.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 12px', borderRadius: 6, border: '1px solid var(--border)', background: 'var(--surface-hover)', fontSize: '0.85rem', gap: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                    <FileText size={15} className="muted" />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.file.name}</span>
                    <span className="muted">{formatBytes(item.file.size)}</span>
                  </div>
                  <UploadStatus item={item} />
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {errorMessage && <div className="alert danger" style={{ marginBottom: 16 }}>{errorMessage}</div>}

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, gap: 12, flexWrap: 'wrap' }}>
        <h2 style={{ fontSize: '1.2rem', fontWeight: 600, margin: 0 }}>
          {showTrash ? 'Trash' : 'Uploaded Sources'} ({totalSources})
        </h2>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {selectedDocs.length > 0 && (
            <>
              <Button type="button" variant="secondary" disabled={bulkWorking} onClick={() => bulkAction('download')}>
                <Download size={13} /> Download
              </Button>
              {!showTrash && (
                <Button type="button" variant="secondary" disabled={bulkWorking} onClick={() => bulkAction('retry')}>
                  <RefreshCw size={13} /> Retry Eligible
                </Button>
              )}
              {!showTrash && (
                <Button type="button" variant="danger" disabled={bulkWorking} onClick={() => bulkAction('trash')}>
                  <Trash2 size={13} /> Move to Trash
                </Button>
              )}
            </>
          )}
          <Button type="button" variant="secondary" onClick={() => sourcesQuery.refetch()}>
            <RefreshCw size={13} /> Refresh
          </Button>
        </div>
      </div>

      {sources.length === 0 ? (
        <EmptyState
          title={showTrash ? 'Trash is empty.' : 'No active sources.'}
          description={showTrash ? 'Deleted sources will appear here before permanent deletion.' : 'Upload research papers to begin evidence-grounded AI synthesis.'}
        >
          {!showTrash && <Button type="button" variant="secondary" onClick={() => setShowTrash(true)} style={{ marginTop: 12 }}>View Trash</Button>}
        </EmptyState>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th style={{ width: 40 }}>
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={(event) => setSelectedIds(event.target.checked ? new Set(sources.map((doc) => doc.id)) : new Set())}
                    aria-label="Select all sources"
                  />
                </th>
                <th style={{ width: 100 }}>DOC Code</th>
                <th>Title / Filename</th>
                <th style={{ width: 150 }}>File</th>
                <th style={{ width: 130 }}>Status</th>
                <th style={{ width: 130 }}>Updated</th>
                <th style={{ width: 80, textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sources.map((doc) => (
                <tr key={doc.id}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selectedIds.has(doc.id)}
                      onChange={(event) => {
                        setSelectedIds((prev) => {
                          const next = new Set(prev);
                          if (event.target.checked) next.add(doc.id);
                          else next.delete(doc.id);
                          return next;
                        });
                      }}
                      aria-label={`Select ${docCode(doc)}`}
                    />
                  </td>
                  <td><strong style={{ fontFamily: 'monospace', fontSize: '0.88rem' }}>{docCode(doc)}</strong></td>
                  <td>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <span style={{ fontWeight: 600 }}>{doc.title ?? filename(doc)}</span>
                      <span className="muted" style={{ fontSize: '0.78rem' }}>{filename(doc)}</span>
                    </div>
                  </td>
                  <td className="muted" style={{ fontSize: '0.82rem' }}>
                    {doc.currentVersion?.mimeType ? shortMime(doc.currentVersion.mimeType) : doc.type ?? 'File'} • v{doc.currentVersion?.versionNumber ?? doc.version ?? 1} • {formatBytes(doc.currentVersion?.fileSizeBytes)}
                    {doc.pageCount ? <div>{doc.pageCount} pages • {doc.chunkCount ?? 0} chunks</div> : null}
                  </td>
                  <td><StatusBadge doc={doc} /></td>
                  <td className="muted" style={{ fontSize: '0.82rem' }}>{formatDate(doc.updatedAt ?? doc.currentVersion?.uploadedAt ?? doc.uploadedAt)}</td>
                  <td style={{ textAlign: 'right' }}>
                    <details style={{ display: 'inline-block', position: 'relative' }}>
                      <summary className="button secondary" title="Source actions" style={{ listStyle: 'none', padding: '4px 8px', cursor: 'pointer' }}>
                        <MoreVertical size={14} />
                      </summary>
                      <div style={{ position: 'absolute', right: 0, zIndex: 3, display: 'grid', gap: 4, minWidth: 190, padding: 8, border: '1px solid var(--border)', borderRadius: 6, background: 'var(--surface)', boxShadow: 'var(--shadow)' }}>
                        <MenuButton label="View details" onClick={() => setDialog({ type: 'details', doc })} />
                        <MenuButton label="Open / Preview" onClick={() => setDialog({ type: 'details', doc })} />
                        <MenuButton label="Download" onClick={() => downloadDocument(doc)} icon={<Download size={13} />} />
                        {!showTrash && <MenuButton label="Edit metadata" onClick={() => openMetadataDialog(doc)} icon={<Edit3 size={13} />} />}
                        {!showTrash && <MenuButton label="Upload New Version" onClick={() => setDialog({ type: 'new-version', doc })} icon={<Upload size={13} />} />}
                        {!showTrash && canRetry(doc) && <MenuButton label="Retry Processing" onClick={() => runForDoc(doc, () => documentApi.retryProcessing(doc.id))} icon={<RefreshCw size={13} />} />}
                        {!showTrash && <MenuButton label="Delete" destructive onClick={() => setDialog({ type: 'trash', doc })} icon={<Trash2 size={13} />} />}
                        {showTrash && <MenuButton label="Restore" onClick={() => runForDoc(doc, () => documentApi.restore(doc.id))} icon={<RotateCcw size={13} />} />}
                        {showTrash && <MenuButton label="Permanent Delete" destructive onClick={() => { setDeleteConfirm(''); setDialog({ type: 'permanent-delete', doc }); }} icon={<Trash2 size={13} />} />}
                      </div>
                    </details>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {sourcesQuery.data && <Pagination page={page} totalPages={sourcesQuery.data.totalPages ?? 1} onPageChange={setPage} />}

      <Modal title="Source Details" open={dialog?.type === 'details'} onClose={() => setDialog(null)}>
        {dialog?.type === 'details' && (
          <div style={{ display: 'grid', gap: 14 }}>
            <DetailGrid doc={dialog.doc} />
            <div>
              <h4 style={{ margin: '0 0 8px' }}>Version History</h4>
              {versionsQuery.isLoading ? (
                <span className="muted">Loading versions...</span>
              ) : (
                <div style={{ display: 'grid', gap: 8 }}>
                  {(versionsQuery.data ?? []).map((version: any) => (
                    <div key={version.versionId} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, border: '1px solid var(--border)', borderRadius: 6, padding: 10 }}>
                      <span>Version {version.versionNumber} {version.versionNumber === dialog.doc.currentVersion?.versionNumber ? '- Current' : '- Historical'}</span>
                      <span className="muted">{version.status} • {formatBytes(version.fileSizeBytes)} • {formatDate(version.uploadedAt)}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>

      <Modal title="Edit Source Metadata" open={dialog?.type === 'rename'} onClose={() => setDialog(null)}>
        {dialog?.type === 'rename' && (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              runForDoc(dialog.doc, () => documentApi.updateMetadata(dialog.doc.id, {
                title: renameTitle,
                bibliographicTitle,
                authors,
                publicationYear: publicationYear ? Number(publicationYear) : undefined,
                journal,
                conference,
                publisher,
                volume,
                issue,
                pages,
                doi,
                url: sourceUrl,
                sourceType,
                keywords: sourceKeywords,
              }));
            }}
            style={{ display: 'grid', gap: 12 }}
          >
            <Field label="Display title">
              <Input value={renameTitle} onChange={(event) => setRenameTitle(event.target.value)} maxLength={500} />
            </Field>
            <Field label="Bibliographic title">
              <Input value={bibliographicTitle} onChange={(event) => setBibliographicTitle(event.target.value)} maxLength={1000} />
            </Field>
            <Field label="Author(s)">
              <Input value={authors} onChange={(event) => setAuthors(event.target.value)} placeholder="e.g. Mensah, K.; Boateng, A." />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
              <Field label="Publication year">
                <Input value={publicationYear} onChange={(event) => setPublicationYear(event.target.value.replace(/\D/g, '').slice(0, 4))} inputMode="numeric" />
              </Field>
              <Field label="Source type">
                <Input value={sourceType} onChange={(event) => setSourceType(event.target.value)} placeholder="Journal article, thesis, report" />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
              <Field label="Journal">
                <Input value={journal} onChange={(event) => setJournal(event.target.value)} />
              </Field>
              <Field label="Conference">
                <Input value={conference} onChange={(event) => setConference(event.target.value)} />
              </Field>
            </div>
            <Field label="Publisher">
              <Input value={publisher} onChange={(event) => setPublisher(event.target.value)} />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 12 }}>
              <Field label="Volume">
                <Input value={volume} onChange={(event) => setVolume(event.target.value)} />
              </Field>
              <Field label="Issue">
                <Input value={issue} onChange={(event) => setIssue(event.target.value)} />
              </Field>
              <Field label="Pages">
                <Input value={pages} onChange={(event) => setPages(event.target.value)} />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
              <Field label="DOI">
                <Input value={doi} onChange={(event) => setDoi(event.target.value)} />
              </Field>
              <Field label="URL">
                <Input value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} />
              </Field>
            </div>
            <Field label="Keywords">
              <Input value={sourceKeywords} onChange={(event) => setSourceKeywords(event.target.value)} placeholder="Comma-separated keywords" />
            </Field>
            <p className="muted" style={{ margin: 0 }}>DOC code remains {docCode(dialog.doc)} and version/storage metadata are not changed.</p>
            <Button type="submit" disabled={workingId === dialog.doc.id}>Save</Button>
          </form>
        )}
      </Modal>

      <Modal title="Upload New Version" open={dialog?.type === 'new-version'} onClose={() => setDialog(null)}>
        {dialog?.type === 'new-version' && (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              if (newVersionFile) runForDoc(dialog.doc, () => documentApi.uploadVersion(dialog.doc.id, newVersionFile));
            }}
            style={{ display: 'grid', gap: 12 }}
          >
            <p className="muted" style={{ margin: 0 }}>{docCode(dialog.doc)} will keep the same source code and receive the next version number.</p>
            <Field label="Replacement file">
              <Input type="file" accept=".pdf,.doc,.docx,.txt" onChange={(event) => setNewVersionFile(event.target.files?.[0] ?? null)} />
            </Field>
            <Button type="submit" disabled={!newVersionFile || workingId === dialog.doc.id}>Upload Version</Button>
          </form>
        )}
      </Modal>

      <Modal title="Delete Source" open={dialog?.type === 'trash'} onClose={() => setDialog(null)}>
        {dialog?.type === 'trash' && (
          <div style={{ display: 'grid', gap: 12 }}>
            <p>Delete {docCode(dialog.doc)}?</p>
            <p className="muted" style={{ margin: 0 }}>This source will be removed from active research and AI retrieval. Its document code will never be reused.</p>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <Button type="button" variant="secondary" onClick={() => setDialog(null)}>Cancel</Button>
              <Button type="button" variant="danger" disabled={workingId === dialog.doc.id} onClick={() => runForDoc(dialog.doc, () => documentApi.delete(dialog.doc.id))}>Move to Trash</Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal title="Permanently Delete Source" open={dialog?.type === 'permanent-delete'} onClose={() => setDialog(null)}>
        {dialog?.type === 'permanent-delete' && (
          <div style={{ display: 'grid', gap: 12 }}>
            <p>Permanently delete {docCode(dialog.doc)}? This cannot be undone.</p>
            <Field label="Type DELETE to confirm">
              <Input value={deleteConfirm} onChange={(event) => setDeleteConfirm(event.target.value)} />
            </Field>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <Button type="button" variant="secondary" onClick={() => setDialog(null)}>Cancel</Button>
              <Button type="button" variant="danger" disabled={deleteConfirm !== 'DELETE' || workingId === dialog.doc.id} onClick={() => runForDoc(dialog.doc, () => documentApi.permanentDelete(dialog.doc.id))}>Permanent Delete</Button>
            </div>
          </div>
        )}
      </Modal>
    </section>
  );
}

function UploadStatus({ item }: { item: UploadQueueItem }) {
  if (item.status === 'READY') return <Badge tone="success"><CheckCircle2 size={12} style={{ marginRight: 4 }} /> {item.documentCode ?? 'Ready'}</Badge>;
  if (item.status === 'UPLOADING') return <Badge tone="info"><RefreshCw size={12} className="spin" style={{ marginRight: 4 }} /> Uploading...</Badge>;
  if (item.status === 'QUEUED') return <Badge tone="info">Queued</Badge>;
  if (item.status === 'QUOTA_EXCEEDED') return <Badge tone="warning">Quota Exceeded</Badge>;
  return <Badge tone="danger">{item.errorMessage || 'Failed'}</Badge>;
}

function MenuButton({ label, onClick, icon, destructive }: { label: string; onClick: () => void; icon?: React.ReactNode; destructive?: boolean }) {
  return (
    <button type="button" onClick={onClick} style={{ display: 'flex', alignItems: 'center', gap: 8, border: 0, background: 'transparent', textAlign: 'left', cursor: 'pointer', padding: '6px 8px', color: destructive ? 'var(--danger)' : 'inherit' }}>
      {icon} {label}
    </button>
  );
}

function StatusBadge({ doc }: { doc: DocumentItem }) {
  if (doc.status === 'READY') return <Badge tone="success">Ready</Badge>;
  if (doc.status === 'PROCESSING' || doc.status === 'UPLOADING') return <Badge tone="info"><RefreshCw size={11} className="spin" style={{ marginRight: 4 }} /> Processing</Badge>;
  if (doc.status === 'FAILED' || doc.status === 'ERROR') return <Badge tone="danger">Failed</Badge>;
  if (doc.status === 'ARCHIVED') return <Badge tone="warning">Trash</Badge>;
  return <Badge>{doc.status ?? 'Unknown'}</Badge>;
}

function DetailGrid({ doc }: { doc: DocumentItem }) {
  const details = [
    ['DOC code', docCode(doc)],
    ['Title', doc.title ?? 'Untitled'],
    ['Bibliographic title', doc.bibliographicTitle ?? 'Missing metadata'],
    ['Author(s)', doc.authors ?? 'Missing metadata'],
    ['Publication year', doc.publicationYear?.toString() ?? 'Missing metadata'],
    ['Journal', doc.journal ?? 'Missing metadata'],
    ['Conference', doc.conference ?? 'Missing metadata'],
    ['Publisher', doc.publisher ?? 'Missing metadata'],
    ['Volume/Issue/Pages', [doc.volume, doc.issue, doc.pages].filter(Boolean).join(' / ') || 'Missing metadata'],
    ['DOI', doc.doi ?? 'Missing metadata'],
    ['URL', doc.url ?? 'Missing metadata'],
    ['Source type', doc.sourceType ?? 'Missing metadata'],
    ['Keywords', doc.keywords ?? 'Missing metadata'],
    ['Original filename', filename(doc)],
    ['Current version', `v${doc.currentVersion?.versionNumber ?? doc.version ?? 1}`],
    ['MIME type', doc.currentVersion?.mimeType ?? 'Unknown'],
    ['File size', formatBytes(doc.currentVersion?.fileSizeBytes)],
    ['Uploaded by', doc.currentVersion?.uploadedBy ?? doc.uploadedBy ?? 'Unknown'],
    ['Uploaded at', formatDate(doc.currentVersion?.uploadedAt ?? doc.uploadedAt)],
    ['Processing state', doc.status ?? 'Unknown'],
    ['Page count', doc.pageCount?.toString() ?? 'Unavailable'],
    ['Chunk count', doc.chunkCount?.toString() ?? 'Unavailable'],
  ];
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'minmax(140px, 180px) 1fr', gap: 8 }}>
      {details.map(([label, value]) => (
        <div key={label} style={{ display: 'contents' }}>
          <strong>{label}</strong>
          <span>{value}</span>
        </div>
      ))}
    </div>
  );
}

function canRetry(doc: DocumentItem) {
  return doc.status === 'FAILED' || doc.status === 'PROCESSING' || doc.status === 'OCR_REQUIRED';
}

function docCode(doc: DocumentItem) {
  return doc.documentCode ?? doc.docCode ?? (doc.documentNumber ? `DOC-${String(doc.documentNumber).padStart(3, '0')}` : 'DOC');
}

function filename(doc: DocumentItem) {
  return doc.currentVersion?.originalFilename ?? doc.originalFilename ?? doc.filename ?? 'Untitled source';
}

function formatBytes(value?: number | null) {
  if (!value) return 'Unavailable';
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${Math.max(1, Math.round(value / 1024))} KB`;
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleDateString() : 'Unavailable';
}

function shortMime(value: string) {
  if (value.includes('pdf')) return 'PDF';
  if (value.includes('word')) return 'Word';
  if (value.includes('text')) return 'Text';
  return value;
}
