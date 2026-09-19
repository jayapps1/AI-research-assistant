import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { analysisApi, datasetApi, documentApi, ragApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Textarea, Badge, Drawer, Select } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';
import type { Citation, RagAnswer } from '../types/api';

export function AiAssistantPage() {
  const projectId = useProjectId();
  const [conversationId, setConversationId] = useState('');
  const [answer, setAnswer] = useState<RagAnswer | null>(null);
  const [scopeType, setScopeType] = useState('ALL_PROJECT_DOCUMENTS');
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [activeCitation, setActiveCitation] = useState<Citation | null>(null);
  const conversations = useQuery({ queryKey: ['rag-conversations', projectId], queryFn: () => ragApi.conversations(projectId), enabled: Boolean(projectId) });
  const documents = useQuery({ queryKey: ['documents', projectId], queryFn: () => documentApi.list(projectId), enabled: Boolean(projectId) });
  const createConversation = useMutation({ mutationFn: () => ragApi.createConversation(projectId, { title: 'Research question' }), onSuccess: (data) => setConversationId(data.id) });
  const ask = useMutation({
    mutationFn: (question: string) => ragApi.ask(conversationId, {
      question,
      scopeType,
      documentIds: scopeType === 'SELECTED_DOCUMENTS' ? selectedDocuments : undefined,
      evidenceLimit: 8,
    }),
    onSuccess: setAnswer,
  });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'AI Assistant']} />
      <h1 className="page-title">AI research assistant</h1>
      <div className="split">
        <Card><h2>Conversations</h2>{conversations.data?.map((c) => <p key={c.id}><button className="button secondary" type="button" onClick={() => setConversationId(c.id)}>{c.title ?? c.id}</button></p>)}<Button type="button" onClick={() => createConversation.mutate()}>New conversation</Button></Card>
        <Card>
          <h2>Question / answer</h2>
          <div className="grid cols-2">
            <Field label="Retrieval scope">
              <Select value={scopeType} onChange={(event) => setScopeType(event.target.value)}>
                <option value="ALL_PROJECT_DOCUMENTS">All project documents</option>
                <option value="SELECTED_DOCUMENTS">Selected documents</option>
              </Select>
            </Field>
            {scopeType === 'SELECTED_DOCUMENTS' ? (
              <Field label="Documents">
                <Select multiple value={selectedDocuments} onChange={(event) => setSelectedDocuments(Array.from(event.currentTarget.selectedOptions).map((option) => option.value))}>
                  {pageContent(documents.data).map((doc) => (
                    <option key={doc.id} value={doc.id}>
                      {doc.documentCode ?? doc.docCode ?? 'DOC'} · {doc.currentVersion?.originalFilename ?? doc.title ?? doc.filename ?? 'Untitled'}
                      {doc.status !== 'READY' ? ` [${doc.status ?? 'PROCESSING'}]` : ''}
                    </option>
                  ))}
                </Select>
              </Field>
            ) : null}
          </div>
          {ask.error ? (() => {
            const err = ask.error as any;
            const message = err?.message || err?.response?.data?.message || '';
            const status = err?.status || err?.response?.status;
            const code = err?.code || err?.response?.data?.code;
            if (message.includes('AI provider is not configured') || message.includes('Grounded answer generation is not enabled') || code === 'CAPABILITY_UNAVAILABLE') {
              return <div className="alert warning">AI provider is not configured.</div>;
            }
            if (status === 429 || code === 'QUOTA_EXCEEDED') {
              return <div className="alert warning">AI request quota exceeded for this billing period. Please upgrade your plan.</div>;
            }
            return <ErrorState title="AI request failed" error={ask.error} />;
          })() : null}
          <QuestionForm disabled={!conversationId || ask.isPending} onAsk={(question) => ask.mutate(question)} />
          {ask.isPending ? <div className="alert info">Retrieving project evidence and generating a grounded answer...</div> : null}
          {answer ? <AnswerDisplay answer={answer} onCitationClick={setActiveCitation} /> : <EmptyState title="Ask a grounded question" description="The browser sends the request to Spring Boot; it never calls OpenAI directly." />}
        </Card>
      </div>
      <Drawer title="Evidence" open={Boolean(activeCitation)} onClose={() => setActiveCitation(null)}>
        {activeCitation ? (
          <div className="grid">
            <p><strong>{activeCitation.documentCode ?? activeCitation.docCode}</strong> · Version {activeCitation.versionNumber ?? 'current'} · Page {activeCitation.pageNumber ?? activeCitation.page ?? 'not provided'}</p>
            <p>{activeCitation.documentTitle}</p>
            <div className="panel">{activeCitation.supportingExcerpt ?? activeCitation.snippet ?? activeCitation.quote ?? 'No excerpt returned.'}</div>
            {activeCitation.documentId && (
              <Button asChild variant="secondary">
                <a href={documentApi.downloadUrl(activeCitation.documentId)} rel="noopener noreferrer">
                  Open Source Document
                </a>
              </Button>
            )}
          </div>
        ) : null}
      </Drawer>
    </section>
  );
}

function QuestionForm({ disabled, onAsk }: { disabled?: boolean; onAsk: (question: string) => void }) {
  const [question, setQuestion] = useState('');
  return <form className="form" onSubmit={(event) => { event.preventDefault(); onAsk(question); }}><Field label="Research question"><Textarea value={question} onChange={(event) => setQuestion(event.target.value)} /></Field><Button type="submit" disabled={disabled}>Ask with project evidence</Button></form>;
}

function AnswerDisplay({ answer, onCitationClick }: { answer: RagAnswer; onCitationClick: (citation: Citation) => void }) {
  const citations = answer.citations ?? answer.evidence ?? [];
  const insufficient = answer.status === 'INSUFFICIENT_EVIDENCE' || !answer.answer;
  if (insufficient) return <div className="alert info">The available project sources do not contain enough evidence to answer this question.</div>;
  return <div className="grid"><Badge tone="success">{answer.status ?? 'COMPLETED'}</Badge><div className="panel"><p>{answer.answer}</p></div><h3>Evidence / citations</h3>{citations.map((citation, index) => <CitationCard key={citation.id ?? index} citation={citation} onClick={() => onCitationClick(citation)} />)}</div>;
}

function CitationCard({ citation, onClick }: { citation: Citation; onClick: () => void }) {
  return <button type="button" className="citation" onClick={onClick}><strong>[{citation.number ?? '?'}] {citation.documentCode ?? citation.docCode ?? 'Source'}</strong> · Version {citation.versionNumber ?? 'current'} · Page {citation.pageNumber ?? citation.page ?? 'n/a'}<p className="muted">{citation.supportingExcerpt ?? citation.snippet ?? citation.quote}</p></button>;
}

export function AnalysisPage() {
  const projectId = useProjectId();
  const runs = useQuery({ queryKey: ['analysis-runs', projectId], queryFn: () => analysisApi.runs(projectId), enabled: Boolean(projectId) });
  const datasets = useQuery({ queryKey: ['datasets', projectId], queryFn: () => datasetApi.list(projectId), enabled: Boolean(projectId) });
  const findings = useQuery({ queryKey: ['findings', projectId], queryFn: () => analysisApi.findings(projectId), enabled: Boolean(projectId) });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Analysis']} />
      <h1 className="page-title">Data & analysis</h1>
      <div className="grid cols-2">
        <Card><h2>Datasets</h2><p className="muted">Server pagination and summaries prevent loading thousands of rows into the browser.</p><RecordRows rows={pageContent(datasets.data) as unknown as Record<string, unknown>[]} /></Card>
        <Card><h2>Analysis plan</h2><div className="grid cols-2"><Field label="Dataset"><select className="select"><option>Select backend dataset</option></select></Field><Field label="Missing data policy"><select className="select"><option>Backend default</option></select></Field></div><Button type="button">Request descriptive statistics</Button></Card>
        <Card><h2>Statistical test planner</h2><div className="alert info">Candidate tests, assumptions, warnings, and blocking compatibility errors are backend values.</div><RecordRows rows={pageContent(runs.data)} /></Card>
        <Card><h2>Findings to discussion</h2><RecordRows rows={pageContent(findings.data)} /></Card>
        <Card><h2>Qualitative analysis</h2><Field label="Codebook / memos"><Textarea /></Field><div className="toolbar"><Button type="button" variant="secondary">AI code suggestions</Button><Button type="button" variant="secondary">Accept selected suggestions</Button></div></Card>
        <TraceabilityMatrix projectId={projectId} />
      </div>
    </section>
  );
}

function TraceabilityMatrix({ projectId }: { projectId: string }) {
  const traceability = useQuery({ queryKey: ['traceability', projectId], queryFn: () => analysisApi.traceability(projectId) });
  const rows = Array.isArray(traceability.data?.rows) ? traceability.data.rows as Record<string, unknown>[] : [];
  return <Card><h2>Traceability matrix</h2><div className="table-wrap"><table><thead><tr><th>Objective</th><th>Question</th><th>Instrument</th><th>Variable</th><th>Analysis</th><th>Finding</th><th>Conclusion</th><th>Recommendation</th></tr></thead><tbody>{rows.map((row, index) => <tr key={index}><td>{displayValue(row.objective ?? row.objectiveText)}</td><td>{displayValue(row.question ?? row.questionId)}</td><td>{displayValue(row.instrument)}</td><td>{displayValue(row.variable)}</td><td>{displayValue(row.analysis ?? row.analysisCount)}</td><td>{displayValue(row.finding ?? row.findingCount)}</td><td>{displayValue(row.conclusion ?? row.conclusionCount)}</td><td>{displayValue(row.recommendation ?? row.recommendationCount)}</td></tr>)}</tbody></table></div></Card>;
}

function RecordRows({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No backend records returned" />;
  return <div className="grid">{rows.slice(0, 8).map((row, index) => <p key={index}><Badge>{displayValue(row.status ?? row.type ?? 'Record')}</Badge> {displayValue(row.title ?? row.name ?? row.id)}</p>)}</div>;
}
