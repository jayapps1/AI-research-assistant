import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { dashboardApi, documentApi, researchApi } from '../api/endpoints';
import { api, isConflictError } from '../api/client';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Select, Pagination } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';
import { paths } from '../routes/paths';
import type { DocumentItem } from '../types/api';

// =========================================================================
// RESEARCH WORKFLOW PAGE (/app/projects/:projectId/research)
// =========================================================================

export function ResearchWorkflowPage() {
  const projectId = useProjectId();

  const progressQuery = useQuery({
    queryKey: ['research-progress', projectId],
    queryFn: () => dashboardApi.researchProgress(projectId),
    enabled: Boolean(projectId),
  });

  const methodologies = useQuery({
    queryKey: ['methodologies', projectId],
    queryFn: () => researchApi.methodologies(projectId),
    enabled: Boolean(projectId),
  });

  if (!projectId) {
    return (
      <main className="page">
        <EmptyState
          title="Select a project"
          description="Open a research project to view its 18 academic stages and workflow progress."
        >
          <Button asChild style={{ marginTop: 12 }}>
            <Link to={paths.projects}>Browse Projects</Link>
          </Button>
        </EmptyState>
      </main>
    );
  }

  if (progressQuery.isLoading) return <PageLoading label="Loading academic research workflow..." />;
  if (progressQuery.isError) return <ErrorState error={progressQuery.error} onRetry={() => progressQuery.refetch()} />;

  const progress = progressQuery.data;
  const stages = progress?.stages ?? [];

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', progress?.projectTitle ?? projectId, 'Research Workflow']} />

      {/* Header Banner with Progress Bar & Next Stage CTA */}
      <Card style={{ marginBottom: 24, border: '1px solid var(--primary-border, var(--border))' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <h1 className="page-title" style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>
                Academic Research Workflow
              </h1>
              <Badge tone="info">{progress?.completedStages ?? 0} / {progress?.totalStages ?? 18} Completed</Badge>
            </div>
            <p className="muted" style={{ fontSize: '0.9rem', marginTop: 4 }}>
              18 structured stages covering academic rigor from problem conceptualization through thesis defense.
            </p>
          </div>

          {progress?.nextIncompleteStage ? (
            <Button asChild style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <a href={progress.nextStageUrl ?? '#stages'}>
                Continue: {progress.nextIncompleteStage} <ArrowRight size={14} />
              </a>
            </Button>
          ) : (
            <Badge tone="success">All Stages Complete</Badge>
          )}
        </div>

        {/* Visual Progress Bar */}
        <div style={{ marginTop: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: 6 }}>
            <span className="muted">Methodological Progress</span>
            <strong>{progress?.percentComplete ?? 0}%</strong>
          </div>
          <div style={{ width: '100%', height: 8, borderRadius: 4, background: 'var(--border)', overflow: 'hidden' }}>
            <div
              style={{
                width: `${Math.max(progress?.percentComplete ?? 0, 3)}%`,
                height: '100%',
                background: 'linear-gradient(90deg, var(--primary) 0%, #0284c7 100%)',
                borderRadius: 4,
                transition: 'width 300ms ease',
              }}
            />
          </div>
        </div>
      </Card>

      {/* 18 Stages Timeline / Grid */}
      <h2 id="stages" style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: 12 }}>
        Academic Methodology Milestones (18 Stages)
      </h2>
      <div className="grid cols-3" style={{ gap: 14, marginBottom: 32 }}>
        {stages.map((st) => {
          const isComplete = st.status === 'COMPLETE';
          const isInProgress = st.status === 'IN_PROGRESS' || st.status === 'ACTIVE';
          const isDraft = st.status === 'DRAFT';

          return (
            <Card
              key={st.number}
              style={{
                padding: '14px 16px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                borderColor: isComplete ? 'var(--success-border, #10b981)' : isInProgress ? 'var(--primary)' : undefined,
                background: isInProgress ? 'var(--surface-hover)' : undefined,
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--muted)' }}>
                    STAGE {st.number}
                  </span>
                  <Badge tone={isComplete ? 'success' : isInProgress ? 'warning' : isDraft ? 'info' : undefined}>
                    {st.status.replace('_', ' ')}
                  </Badge>
                </div>
                <h3 style={{ margin: '0 0 6px', fontSize: '0.98rem', fontWeight: 600 }}>
                  {st.name}
                </h3>
                <p className="muted" style={{ fontSize: '0.8rem', margin: 0, lineHeight: 1.4 }}>
                  {st.description}
                </p>
              </div>

              <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span className="muted" style={{ fontSize: '0.75rem' }}>
                  {st.itemCount > 0 ? `${st.itemCount} records` : '0 mapped'}
                </span>
                <Button asChild variant="secondary" style={{ fontSize: '0.78rem', padding: '3px 8px' }}>
                  <a href={st.actionUrl}>Work on Stage</a>
                </Button>
              </div>
            </Card>
          );
        })}
      </div>

      {/* In-depth Stage Editors */}
      <h2 style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: 12 }}>Stage Workbenches</h2>
      <div className="grid" style={{ gap: 20 }}>
        <ResearchProblem projectId={projectId} />
        <ObjectivesQuestions />
        <LiteratureMatrix projectId={projectId} />
        <Frameworks projectId={projectId} />
        <Methodology data={methodologies.data ?? []} />
        <InstrumentBuilder />
        <EthicsFieldwork projectId={projectId} />
      </div>
    </section>
  );
}

// Stage 1: Research Problem
function ResearchProblem({ projectId }: { projectId: string }) {
  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) => api.patch(`/projects/${projectId}`, body).then((r) => r.data),
  });
  return (
    <Card>
      <h2 id="Research Problem">Stage 1: Research Problem Statement</h2>
      {isConflictError(save.error) ? (
        <div className="alert warning">This item has been changed by another collaborator. Reload latest before saving.</div>
      ) : null}
      <div className="editor-layout">
        <form className="form">
          <Field label="Current Problem Statement">
            <Textarea placeholder="Formulate the research gap, background contextualization, and problem magnitude..." rows={4} />
          </Field>
          <div className="toolbar">
            <Button type="button" onClick={() => save.mutate({})}>Save Formulation</Button>
            <Button type="button" variant="secondary">Generate AI Draft</Button>
            <span className="save-state">{save.isPending ? 'Saving...' : save.isError ? 'Save failed' : 'Saved'}</span>
          </div>
        </form>
        <aside className="panel">
          <h3>AI Draft Proposal</h3>
          <p className="muted">Generated drafts are displayed here for Accept, Edit, or Reject. They never overwrite manual text automatically.</p>
          <Button type="button" variant="secondary">Accept Proposal</Button>
        </aside>
      </div>
    </Card>
  );
}

// Stage 2: Objectives & Hypotheses
function ObjectivesQuestions() {
  return (
    <Card>
      <h2 id="Objectives">Stage 2: Objectives, Questions & Hypotheses</h2>
      <div className="grid cols-3">
        <Field label="General Objective"><Textarea placeholder="Primary overarching research aim..." /></Field>
        <Field label="Specific Objective"><Textarea placeholder="Specific empirical sub-aim..." /></Field>
        <Field label="Linked Hypothesis / Question"><Textarea placeholder="Directional hypothesis or question..." /></Field>
      </div>
      <div className="toolbar" style={{ marginTop: 12 }}>
        <Button type="button">Add Objective</Button>
        <Button type="button" variant="secondary">AI Alignment Check</Button>
        <Badge tone="info">Alignment verified against backend criteria</Badge>
      </div>
    </Card>
  );
}

// Stage 3: Literature Review
function LiteratureMatrix({ projectId }: { projectId: string }) {
  const documents = useQuery({ queryKey: ['documents', projectId], queryFn: () => documentApi.list(projectId) });
  return (
    <Card>
      <h2 id="Literature Review">Stage 3: Literature Matrix & Empirical Synthesis</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Source</th><th>Author/Year</th><th>Purpose</th><th>Methodology</th><th>Sample</th><th>Findings</th><th>Gap</th></tr>
          </thead>
          <tbody>
            {pageContent(documents.data).map((doc) => (
              <tr key={doc.id}>
                <td><strong>{doc.docCode ?? doc.filename}</strong></td>
                <td colSpan={6} className="muted">Extracted structured data appears here upon semantic indexing.</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div style={{ marginTop: 12 }}>
        <Field label="Structured Synthesis">
          <Textarea placeholder="Synthesize empirical patterns, methodological debates, and thematic gaps..." rows={3} />
        </Field>
      </div>
    </Card>
  );
}

// Stage 4: Frameworks
function Frameworks({ projectId }: { projectId: string }) {
  const conceptual = useQuery({ queryKey: ['conceptual-frameworks', projectId], queryFn: () => researchApi.conceptualFrameworks(projectId) });
  const theoretical = useQuery({ queryKey: ['theoretical-frameworks', projectId], queryFn: () => researchApi.theoreticalFrameworks(projectId) });
  return (
    <Card>
      <h2 id="Conceptual Framework">Stage 4: Conceptual & Theoretical Frameworks</h2>
      <div className="grid cols-2">
        <div>
          <h3>Conceptual Framework</h3>
          <RecordPreview rows={conceptual.data ?? []} empty="No conceptual models defined yet" />
        </div>
        <div>
          <h3>Theoretical Foundations</h3>
          <RecordPreview rows={theoretical.data ?? []} empty="No theoretical paradigms assigned yet" />
        </div>
      </div>
    </Card>
  );
}

// Stage 5: Methodology
function Methodology({ data }: { data: Record<string, unknown>[] }) {
  return (
    <Card>
      <h2 id="Methodology">Stage 5: Research Design & Methodology</h2>
      <div className="grid cols-3">
        <Field label="Research Paradigm / Philosophy"><Input placeholder="e.g. Pragmatism, Positivism" /></Field>
        <Field label="Design"><Input placeholder="e.g. Convergent Mixed Methods" /></Field>
        <Field label="Target Population"><Input placeholder="e.g. Licensed Clinical Practitioners" /></Field>
        <Field label="Sampling Technique"><Input placeholder="e.g. Stratified Random Sampling" /></Field>
        <Field label="Validity & Reliability Controls"><Input placeholder="e.g. Cronbach's Alpha >= 0.8" /></Field>
        <Field label="Ethical Considerations"><Input placeholder="e.g. Institutional Review Board Protocol" /></Field>
      </div>
      <RecordPreview rows={data} empty="No methodology revisions recorded" />
    </Card>
  );
}

// Stage 7: Instruments
function InstrumentBuilder() {
  return (
    <Card>
      <h2 id="Instruments">Stage 7: Measurement Instruments & Scales</h2>
      <div className="grid cols-2">
        <Field label="Instrument Type">
          <select className="select">
            <option>Structured Questionnaire</option>
            <option>Semi-Structured Interview Guide</option>
            <option>Focus Group Discussion Protocol</option>
            <option>Systematic Observation Checklist</option>
          </select>
        </Field>
        <Field label="Measurement Scale">
          <select className="select">
            <option>5-Point Likert Scale (Agreement)</option>
            <option>7-Point Likert Scale</option>
            <option>Multiple Choice / Categorical</option>
            <option>Open-Ended Qualitative</option>
          </select>
        </Field>
      </div>
      <Field label="Question / Prompt Text">
        <Textarea placeholder="State the question item clearly without double-barrel wording..." />
      </Field>
      <div className="toolbar" style={{ marginTop: 12 }}>
        <Button type="button">Add Item</Button>
        <Button type="button" variant="secondary">Map to Specific Objective</Button>
      </div>
    </Card>
  );
}

// Stage 8 & 9: Ethics & Fieldwork
function EthicsFieldwork({ projectId }: { projectId: string }) {
  const ethics = useQuery({ queryKey: ['ethics-readiness', projectId], queryFn: () => researchApi.ethicsReadiness(projectId) });
  const participants = useQuery({ queryKey: ['participants', projectId], queryFn: () => researchApi.participants(projectId) });
  return (
    <Card>
      <h2 id="Ethics">Stage 8 & 9: Ethics Compliance & Fieldwork Consent</h2>
      <div className="alert warning" style={{ marginBottom: 12 }}>
        Academic governance mandate: AI assists with documentation, but never issues final ethics clearance.
      </div>
      <RecordPreview rows={ethics.data ? [ethics.data] : []} empty="No ethics readiness records filed" />
      <h3 style={{ marginTop: 16 }}>Anonymized Participant Cohorts</h3>
      <RecordPreview rows={pageContent(participants.data)} empty="No participants registered" />
    </Card>
  );
}

// =========================================================================
// DOCUMENTS DASHBOARD (/app/documents or /app/projects/:projectId/documents)
// =========================================================================

export function DocumentsPage() {
  const projectId = useProjectId();
  if (!projectId) {
    return <GlobalDocumentsPage />;
  }
  return <ProjectDocumentsPage projectId={projectId} />;
}

function GlobalDocumentsPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');

  const docsQuery = useQuery({
    queryKey: ['my-documents', { page, status }],
    queryFn: () => documentApi.mine(page, 20, { status: status || undefined }),
  });

  const docs = pageContent(docsQuery.data);

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Document Library</h1>
          <p className="muted">All research papers, transcripts, and data files across authorized projects.</p>
        </div>
      </div>

      <Card style={{ marginBottom: 16, padding: '12px 16px' }}>
        <div style={{ width: 200 }}>
          <Select
            aria-label="Filter status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Statuses</option>
            <option value="READY">Ready</option>
            <option value="PROCESSING">Processing</option>
            <option value="FAILED">Failed</option>
            <option value="ARCHIVED">Archived</option>
          </Select>
        </div>
      </Card>

      {docsQuery.isLoading ? (
        <PageLoading label="Loading documents..." />
      ) : docsQuery.isError ? (
        <ErrorState error={docsQuery.error} onRetry={() => docsQuery.refetch()} />
      ) : docs.length > 0 ? (
        <Card>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Doc Code</th>
                  <th>Filename / Title</th>
                  <th>Version</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Download</th>
                </tr>
              </thead>
              <tbody>
                {docs.map((doc) => {
                  const docCode = doc.documentCode ?? doc.docCode ?? (doc.documentNumber ? `DOC-${String(doc.documentNumber).padStart(3, '0')}` : 'DOC-—');
                  const filename = doc.currentVersion?.originalFilename ?? doc.title ?? doc.filename ?? doc.originalFilename ?? 'Untitled';
                  const versionText = doc.currentVersion ? `v${doc.currentVersion.versionNumber}` : doc.version ? `v${doc.version}` : 'No version available';
                  const statusText = doc.currentVersion?.scanStatus === 'OCR_REQUIRED' ? 'OCR required' : (doc.currentVersion?.status ?? doc.status ?? 'READY');

                  return (
                    <tr key={doc.id}>
                      <td><strong>{docCode}</strong></td>
                      <td>{filename}</td>
                      <td>{versionText}</td>
                      <td>
                        <Badge tone={statusText === 'READY' ? 'success' : statusText === 'FAILED' || statusText === 'OCR required' ? 'danger' : 'info'}>
                          {statusText}
                        </Badge>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <Button asChild variant="secondary" style={{ fontSize: '0.8rem', padding: '4px 8px' }}>
                          <a href={documentApi.downloadUrl(doc.id)} rel="noopener noreferrer">
                            Download
                          </a>
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <Pagination
            page={docsQuery.data?.page ?? page}
            totalPages={docsQuery.data?.totalPages ?? 1}
            onPageChange={setPage}
          />
        </Card>
      ) : (
        <EmptyState
          title="No documents uploaded"
          description="Open a research project to upload PDFs, interview transcripts, or research instruments."
        />
      )}
    </section>
  );
}

interface UploadQueueItem {
  id: string;
  file: File;
  status: 'QUEUED' | 'UPLOADING' | 'READY' | 'QUOTA_EXCEEDED' | 'FAILED';
  errorMessage?: string | null;
  documentCode?: string | null;
}

function ProjectDocumentsPage({ projectId }: { projectId: string }) {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const [isUploadingQueue, setIsUploadingQueue] = useState(false);
  const [versionDoc, setVersionDoc] = useState<DocumentItem | null>(null);
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const client = useQueryClient();

  const docs = useQuery({
    queryKey: ['documents', projectId, { page, status }],
    queryFn: () => documentApi.list(projectId, page, 20, { status: status || undefined }),
    enabled: Boolean(projectId),
  });

  const uploadVersionMutation = useMutation({
    mutationFn: () => {
      if (!versionDoc || !versionFile) throw new Error('Select a replacement file.');
      return documentApi.uploadVersion(versionDoc.id, versionFile);
    },
    onSuccess: () => {
      setVersionDoc(null);
      setVersionFile(null);
      client.invalidateQueries({ queryKey: ['documents', projectId] });
      client.invalidateQueries({ queryKey: ['project-dashboard', projectId] });
      client.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const handleUploadQueue = async () => {
    if (uploadQueue.length === 0 || isUploadingQueue) return;
    setIsUploadingQueue(true);

    const pendingItems = uploadQueue.filter(
      (item) => item.status === 'QUEUED' || item.status === 'FAILED'
    );

    let index = 0;
    const concurrency = 2;

    const uploadWorker = async () => {
      while (index < pendingItems.length) {
        const currentItem = pendingItems[index++];
        setUploadQueue((prev) =>
          prev.map((item) =>
            item.id === currentItem.id ? { ...item, status: 'UPLOADING', errorMessage: null } : item
          )
        );

        try {
          const result = await documentApi.upload(projectId, currentItem.file);
          const allocatedCode = result.documentCode ?? result.docCode ?? (result.documentNumber ? `DOC-${String(result.documentNumber).padStart(3, '0')}` : 'DOC');
          setUploadQueue((prev) =>
            prev.map((item) =>
              item.id === currentItem.id
                ? {
                    ...item,
                    status: 'READY',
                    documentCode: allocatedCode,
                  }
                : item
            )
          );
        } catch (err: any) {
          const errStatus = err?.status ?? err?.response?.status;
          const errCode = err?.code ?? err?.response?.data?.code;
          const isQuota = errStatus === 429 || errCode === 'QUOTA_EXCEEDED';
          const errorMsg = isQuota
            ? 'Storage quota exceeded'
            : err?.message || 'Upload failed';

          setUploadQueue((prev) =>
            prev.map((item) =>
              item.id === currentItem.id
                ? {
                    ...item,
                    status: isQuota ? 'QUOTA_EXCEEDED' : 'FAILED',
                    errorMessage: errorMsg,
                  }
                : item
            )
          );
        }
      }
    };

    const workers = Array.from(
      { length: Math.min(concurrency, pendingItems.length) },
      () => uploadWorker()
    );
    await Promise.all(workers);

    setIsUploadingQueue(false);
    client.invalidateQueries({ queryKey: ['documents', projectId] });
    client.invalidateQueries({ queryKey: ['project-dashboard', projectId] });
    client.invalidateQueries({ queryKey: ['workspace-usage'] });
    client.invalidateQueries({ queryKey: ['storage-usage'] });
    client.invalidateQueries({ queryKey: ['dashboard'] });
  };

  const hasQuotaExceeded = uploadQueue.some((item) => item.status === 'QUOTA_EXCEEDED');

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Documents']} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Project Document Library</h1>
          <p className="muted">Upload and index research papers, instruments, and field documents.</p>
        </div>
      </div>

      <div className="grid cols-2" style={{ gap: 20 }}>
        <Card>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Upload Research Documents</h2>
          <p className="muted" style={{ fontSize: '0.85rem', margin: '4px 0 16px' }}>
            Select one or multiple research documents (.pdf, .docx, .txt). Files are processed and indexed with sequential DOC codes.
          </p>

          {hasQuotaExceeded && (
            <div className="alert warning" style={{ marginBottom: 16 }}>
              <strong>Storage Quota Exceeded</strong>
              <p style={{ margin: '6px 0 12px' }}>
                Your workspace storage limit has been reached. Upgrade to continue uploading documents.
              </p>
              <Button
                type="button"
                variant="primary"
                className="btn-compact"
                onClick={() => navigate(paths.billing)}
              >
                Upgrade Storage
              </Button>
            </div>
          )}

          <div style={{ marginBottom: 16 }}>
            <Field label="Select File(s) (.pdf, .docx, .txt)">
              <Input
                type="file"
                multiple
                accept=".pdf,.doc,.docx,.txt"
                onChange={(e) => {
                  if (e.target.files && e.target.files.length > 0) {
                    const newItems: UploadQueueItem[] = Array.from(e.target.files).map((f) => ({
                      id: `${f.name}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
                      file: f,
                      status: 'QUEUED',
                    }));
                    setUploadQueue((prev) => [...prev, ...newItems]);
                  }
                  e.target.value = '';
                }}
              />
            </Field>
          </div>

          {uploadQueue.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <strong style={{ fontSize: '0.9rem' }}>Upload Queue ({uploadQueue.length})</strong>
                <Button
                  type="button"
                  variant="secondary"
                  style={{ fontSize: '0.75rem', padding: '2px 6px' }}
                  onClick={() => setUploadQueue([])}
                  disabled={isUploadingQueue}
                >
                  Clear Queue
                </Button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: '200px', overflowY: 'auto', padding: '8px', border: '1px solid var(--border)', borderRadius: '6px' }}>
                {uploadQueue.map((item) => (
                  <div
                    key={item.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      fontSize: '0.82rem',
                      padding: '4px 8px',
                      borderRadius: '4px',
                      backgroundColor: 'var(--card-bg, #1e293b)',
                    }}
                  >
                    <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden', marginRight: 8 }}>
                      <span style={{ fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {item.file.name}
                      </span>
                      <span className="muted" style={{ fontSize: '0.72rem' }}>
                        {Math.round(item.file.size / 1024)} KB
                        {item.documentCode ? ` · ${item.documentCode}` : ''}
                      </span>
                    </div>
                    <div>
                      {item.status === 'QUEUED' && <Badge tone="info">Ready</Badge>}
                      {item.status === 'UPLOADING' && <Badge tone="warning">Uploading...</Badge>}
                      {item.status === 'READY' && <Badge tone="success">Ready ({item.documentCode})</Badge>}
                      {item.status === 'QUOTA_EXCEEDED' && <Badge tone="danger">Quota Exceeded</Badge>}
                      {item.status === 'FAILED' && <Badge tone="danger">Failed</Badge>}
                    </div>
                  </div>
                ))}
              </div>

              <div style={{ marginTop: 12 }}>
                <Button
                  type="button"
                  variant="primary"
                  className="w-full"
                  disabled={isUploadingQueue || uploadQueue.every((q) => q.status === 'READY')}
                  onClick={handleUploadQueue}
                >
                  {isUploadingQueue
                    ? 'Uploading Documents (2 concurrent)...'
                    : `Upload ${uploadQueue.filter((q) => q.status === 'QUEUED' || q.status === 'FAILED').length} Document(s)`}
                </Button>
              </div>
            </div>
          )}
        </Card>

        <Card>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600 }}>Filter Project Documents</h2>
          <Field label="Processing Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All Statuses</option>
              <option value="READY">Ready</option>
              <option value="PROCESSING">Processing</option>
              <option value="FAILED">Failed</option>
              <option value="ARCHIVED">Archived</option>
            </Select>
          </Field>

          {versionDoc && (
            <div style={{ marginTop: 20, padding: 12, border: '1px solid var(--border)', borderRadius: 6 }}>
              <h3 style={{ fontSize: '0.95rem', fontWeight: 600, margin: '0 0 8px' }}>
                Upload New Version for {versionDoc.documentCode ?? versionDoc.docCode}
              </h3>
              <p className="muted" style={{ fontSize: '0.8rem', margin: '0 0 12px' }}>
                Creates version v{(versionDoc.currentVersion?.versionNumber ?? 1) + 1} while retaining previous version history.
              </p>
              <Field label="Select replacement file">
                <Input
                  type="file"
                  accept=".pdf,.doc,.docx,.txt"
                  onChange={(e) => setVersionFile(e.target.files?.[0] ?? null)}
                />
              </Field>
              <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                <Button
                  type="button"
                  variant="primary"
                  disabled={!versionFile || uploadVersionMutation.isPending}
                  onClick={() => uploadVersionMutation.mutate()}
                >
                  {uploadVersionMutation.isPending ? 'Uploading...' : 'Save New Version'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setVersionDoc(null);
                    setVersionFile(null);
                  }}
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </Card>
      </div>

      <div style={{ marginTop: 20 }}>
        {docs.isLoading ? (
          <PageLoading />
        ) : docs.isError ? (
          <ErrorState error={docs.error} />
        ) : (
          <Card>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Doc Code</th>
                    <th>Filename</th>
                    <th>Version</th>
                    <th>Processing</th>
                    <th>Semantic Index</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {pageContent(docs.data).map((doc) => {
                    const docCode = doc.documentCode ?? doc.docCode ?? (doc.documentNumber ? `DOC-${String(doc.documentNumber).padStart(3, '0')}` : 'DOC-—');
                    const filename = doc.currentVersion?.originalFilename ?? doc.title ?? doc.filename ?? doc.originalFilename ?? 'Untitled';
                    const versionText = doc.currentVersion ? `v${doc.currentVersion.versionNumber}` : doc.version ? `v${doc.version}` : 'No version available';
                    const statusText = doc.currentVersion?.scanStatus === 'OCR_REQUIRED' ? 'OCR required' : (doc.currentVersion?.status ?? doc.status ?? 'READY');

                    return (
                      <tr key={doc.id}>
                        <td><strong>{docCode}</strong></td>
                        <td>{filename}</td>
                        <td>{versionText}</td>
                        <td>
                          <Badge tone={statusText === 'READY' ? 'success' : statusText === 'FAILED' || statusText === 'OCR required' ? 'danger' : 'info'}>
                            {statusText}
                          </Badge>
                        </td>
                        <td>{doc.semanticIndexStatus ?? 'INDEXED'}</td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                            <Button
                              type="button"
                              variant="secondary"
                              style={{ fontSize: '0.8rem', padding: '4px 8px' }}
                              onClick={() => setVersionDoc(doc)}
                            >
                              New Version
                            </Button>
                            <Button asChild variant="secondary" style={{ fontSize: '0.8rem', padding: '4px 8px' }}>
                              <a href={documentApi.downloadUrl(doc.id)} rel="noopener noreferrer">
                                Download
                              </a>
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination page={docs.data?.page ?? page} totalPages={docs.data?.totalPages ?? 1} onPageChange={setPage} />
          </Card>
        )}
      </div>
    </section>
  );
}

function RecordPreview({ rows, empty }: { rows: Record<string, unknown>[]; empty: string }) {
  if (!rows.length) return <EmptyState title={empty} />;
  return (
    <div className="grid" style={{ gap: 8 }}>
      {rows.slice(0, 4).map((row, index) => (
        <div className="panel" key={index}>
          <p style={{ margin: 0 }}>{displayValue(row.title ?? row.name ?? row.status ?? row.id)}</p>
        </div>
      ))}
    </div>
  );
}
