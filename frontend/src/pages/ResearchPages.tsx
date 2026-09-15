import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { documentApi, researchApi } from '../api/endpoints';
import { api, isConflictError } from '../api/client';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Select, Pagination } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

const researchSections = ['Research Problem', 'Objectives', 'Research Questions', 'Hypotheses', 'Literature Review', 'Conceptual Framework', 'Theoretical Framework', 'Methodology', 'Population & Sampling', 'Data Collection Methods', 'Instruments', 'Ethics', 'Fieldwork'];

export function ResearchWorkflowPage() {
  const projectId = useProjectId();
  const methodologies = useQuery({ queryKey: ['methodologies', projectId], queryFn: () => researchApi.methodologies(projectId), enabled: Boolean(projectId) });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Research']} />
      <div className="page-header"><div><h1 className="page-title">Research workflow</h1><p className="muted">Academic artifacts are edited with revision-aware saves and AI draft preview patterns.</p></div></div>
      <div className="split">
        <Card>{researchSections.map((section) => <p key={section}><a href={`#${section}`}>{section}</a></p>)}</Card>
        <div className="grid">
          <ResearchProblem projectId={projectId} />
          <ObjectivesQuestions />
          <LiteratureMatrix projectId={projectId} />
          <Frameworks projectId={projectId} />
          <Methodology data={methodologies.data ?? []} />
          <InstrumentBuilder />
          <EthicsFieldwork projectId={projectId} />
        </div>
      </div>
    </section>
  );
}

function ResearchProblem({ projectId }: { projectId: string }) {
  const save = useMutation({ mutationFn: (body: Record<string, unknown>) => api.patch(`/projects/${projectId}`, body).then((r) => r.data) });
  return (
    <Card>
      <h2 id="Research Problem">Research Problem</h2>
      {isConflictError(save.error) ? <div className="alert warning">This item has been changed by another collaborator. Reload latest, keep local text temporarily, or compare manually if supported.</div> : null}
      <div className="editor-layout">
        <form className="form">
          <Field label="Current content"><Textarea placeholder="Problem statement content from backend artifact..." /></Field>
          <div className="toolbar"><Button type="button" onClick={() => save.mutate({})}>Save</Button><Button type="button" variant="secondary">Generate AI draft</Button><span className="save-state">{save.isPending ? 'Saving...' : save.isError ? 'Save failed' : 'Unsaved changes'}</span></div>
        </form>
        <aside className="panel"><h3>AI draft proposal</h3><p className="muted">Generated drafts are displayed here for Accept, Edit, or Reject. They never overwrite manual text automatically.</p><Button type="button" variant="secondary">Accept draft</Button></aside>
      </div>
    </Card>
  );
}

function ObjectivesQuestions() {
  return <Card><h2 id="Objectives">Objectives, questions & hypotheses</h2><div className="grid cols-3"><Field label="General objective"><Textarea /></Field><Field label="Specific objective"><Textarea /></Field><Field label="Linked question or hypothesis"><Textarea /></Field></div><div className="toolbar"><Button type="button">Add objective</Button><Button type="button" variant="secondary">AI suggestions</Button><Badge tone="info">Alignment indicators use backend data when available</Badge></div></Card>;
}

function LiteratureMatrix({ projectId }: { projectId: string }) {
  const documents = useQuery({ queryKey: ['documents', projectId], queryFn: () => documentApi.list(projectId) });
  return <Card><h2 id="Literature Review">Literature matrix & review</h2><div className="table-wrap"><table><thead><tr><th>Source</th><th>Author/year</th><th>Purpose</th><th>Methodology</th><th>Sample</th><th>Findings</th><th>Gap</th><th>Relevance</th></tr></thead><tbody>{pageContent(documents.data).map((doc) => <tr key={doc.id}><td>{doc.docCode ?? doc.filename}</td><td colSpan={7}>AI extraction appears as draft structured fields before saving.</td></tr>)}</tbody></table></div><Field label="Structured literature review section"><Textarea placeholder="Introduction, conceptual, theoretical, empirical, gap, summary..." /></Field></Card>;
}

function Frameworks({ projectId }: { projectId: string }) {
  const conceptual = useQuery({ queryKey: ['conceptual-frameworks', projectId], queryFn: () => researchApi.conceptualFrameworks(projectId) });
  const theoretical = useQuery({ queryKey: ['theoretical-frameworks', projectId], queryFn: () => researchApi.theoreticalFrameworks(projectId) });
  return <Card><h2 id="Conceptual Framework">Frameworks</h2><div className="grid cols-2"><div><h3>Conceptual</h3><RecordPreview rows={conceptual.data ?? []} empty="No conceptual frameworks" /></div><div><h3>Theoretical</h3><RecordPreview rows={theoretical.data ?? []} empty="No theoretical frameworks" /></div></div></Card>;
}

function Methodology({ data }: { data: Record<string, unknown>[] }) {
  return <Card><h2 id="Methodology">Methodology</h2><div className="grid cols-3"><Field label="Approach"><Input /></Field><Field label="Design"><Input /></Field><Field label="Population"><Input /></Field><Field label="Sampling"><Input /></Field><Field label="Validity / reliability"><Input /></Field><Field label="Ethics"><Input /></Field></div><div className="alert info">Sample-size results are displayed from server calculations only.</div><RecordPreview rows={data} empty="No methodology versions" /></Card>;
}

function InstrumentBuilder() {
  return <Card><h2 id="Instruments">Instrument builder</h2><div className="grid cols-2"><Field label="Instrument type"><select className="select"><option>Questionnaire</option><option>Interview Guide</option><option>Focus Group Guide</option><option>Observation Checklist</option></select></Field><Field label="Item type"><select className="select"><option>Likert scale</option><option>Multiple choice</option><option>Open text</option><option>Numeric</option></select></Field></div><Field label="Item text"><Textarea /></Field><div className="toolbar"><Button type="button">Add item</Button><Button type="button" variant="secondary">Map to objective</Button></div></Card>;
}

function EthicsFieldwork({ projectId }: { projectId: string }) {
  const ethics = useQuery({ queryKey: ['ethics-readiness', projectId], queryFn: () => researchApi.ethicsReadiness(projectId) });
  const participants = useQuery({ queryKey: ['participants', projectId], queryFn: () => researchApi.participants(projectId) });
  return <Card><h2 id="Ethics">Ethics, consent & fieldwork</h2><div className="alert warning">AI may help draft documents, but it never appears to approve ethics.</div><RecordPreview rows={ethics.data ? [ethics.data] : []} empty="No ethics readiness data" /><h3>Participants</h3><p className="muted">Participant screens use pseudonymous codes unless backend authorization allows more.</p><RecordPreview rows={pageContent(participants.data)} empty="No participants returned" /></Card>;
}

export function DocumentsPage() {
  const projectId = useProjectId();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const client = useQueryClient();
  const docs = useQuery({ queryKey: ['documents', projectId, { page, status }], queryFn: () => documentApi.list(projectId, page, 20, { status: status || undefined }), enabled: Boolean(projectId) });
  const upload = useMutation({
    mutationFn: () => {
      if (!file) throw new Error('Choose a file before uploading.');
      return documentApi.upload(projectId, file, title || undefined);
    },
    onSuccess: () => {
      setFile(null);
      setTitle('');
      client.invalidateQueries({ queryKey: ['documents', projectId] });
    },
  });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Documents']} />
      <div className="page-header"><h1 className="page-title">Document library</h1><Badge tone="info">Authorized backend downloads only</Badge></div>
      <div className="grid cols-2">
        <Card>
          <h2>Upload document</h2>
          {upload.error ? <ErrorState title="Upload failed" error={upload.error} /> : null}
          <form className="form" onSubmit={(event) => { event.preventDefault(); upload.mutate(); }}>
            <Field label="Title"><Input value={title} onChange={(event) => setTitle(event.target.value)} /></Field>
            <Field label="File"><Input type="file" accept=".pdf,.doc,.docx,.txt,.csv,.xlsx" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /></Field>
            <p className="muted">{file ? `${file.name} · ${Math.round(file.size / 1024)} KB` : 'Allowed file types are validated by the backend.'}</p>
            <Button type="submit" disabled={!file || upload.isPending}>{upload.isPending ? 'Uploading...' : 'Upload'}</Button>
          </form>
        </Card>
        <Card>
          <h2>Filters</h2>
          <Field label="Processing status"><Select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All</option><option>ACTIVE</option><option>ARCHIVED</option><option>PROCESSING</option><option>FAILED</option></Select></Field>
        </Card>
      </div>
      {docs.isLoading ? <PageLoading /> : docs.isError ? <ErrorState error={docs.error} /> : <div className="table-wrap"><table><thead><tr><th>DOC code</th><th>Filename</th><th>Version</th><th>Processing</th><th>Semantic index</th><th>Actions</th></tr></thead><tbody>{pageContent(docs.data).map((doc) => <tr key={doc.id}><td>{doc.docCode}</td><td>{doc.filename ?? doc.originalFilename ?? doc.title}</td><td>{doc.version ?? doc.currentVersion}</td><td>{doc.processingStatus}</td><td>{doc.semanticIndexStatus}</td><td><Button asChild variant="secondary"><a href={documentApi.downloadUrl(doc.id)} rel="noopener noreferrer">Download</a></Button></td></tr>)}</tbody></table></div>}
      <Pagination page={docs.data?.page ?? page} totalPages={docs.data?.totalPages ?? 1} onPageChange={setPage} />
    </section>
  );
}

function RecordPreview({ rows, empty }: { rows: Record<string, unknown>[]; empty: string }) {
  if (!rows.length) return <EmptyState title={empty} />;
  return <div className="grid">{rows.slice(0, 4).map((row, index) => <div className="panel" key={index}><p>{displayValue(row.title ?? row.name ?? row.status ?? row.id)}</p></div>)}</div>;
}
