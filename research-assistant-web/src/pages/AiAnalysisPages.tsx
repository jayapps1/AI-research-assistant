import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { analysisApi, ragApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Textarea, Badge } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue } from '../utils/collections';
import type { Citation, RagAnswer } from '../types/api';

export function AiAssistantPage() {
  const projectId = useProjectId();
  const [conversationId, setConversationId] = useState('');
  const [answer, setAnswer] = useState<RagAnswer | null>(null);
  const conversations = useQuery({ queryKey: ['rag-conversations', projectId], queryFn: () => ragApi.conversations(projectId), enabled: Boolean(projectId) });
  const createConversation = useMutation({ mutationFn: () => ragApi.createConversation(projectId, { title: 'Research question' }), onSuccess: (data) => setConversationId(data.id) });
  const ask = useMutation({ mutationFn: (question: string) => ragApi.ask(conversationId, { question, documentScope: 'ALL_PROJECT_DOCUMENTS' }), onSuccess: setAnswer });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'AI Assistant']} />
      <h1 className="page-title">AI research assistant</h1>
      <div className="split">
        <Card><h2>Conversations</h2>{conversations.data?.map((c) => <p key={c.id}><button className="button secondary" type="button" onClick={() => setConversationId(c.id)}>{c.title ?? c.id}</button></p>)}<Button type="button" onClick={() => createConversation.mutate()}>New conversation</Button></Card>
        <Card>
          <h2>Question / answer</h2>
          {ask.error ? <ErrorState title="AI request failed" error={ask.error} /> : null}
          <QuestionForm disabled={!conversationId || ask.isPending} onAsk={(question) => ask.mutate(question)} />
          {answer ? <AnswerDisplay answer={answer} /> : <EmptyState title="Ask a grounded question" description="The browser sends the request to Spring Boot; it never calls OpenAI directly." />}
        </Card>
      </div>
    </section>
  );
}

function QuestionForm({ disabled, onAsk }: { disabled?: boolean; onAsk: (question: string) => void }) {
  const [question, setQuestion] = useState('');
  return <form className="form" onSubmit={(event) => { event.preventDefault(); onAsk(question); }}><Field label="Research question"><Textarea value={question} onChange={(event) => setQuestion(event.target.value)} /></Field><Button type="submit" disabled={disabled}>Ask with project evidence</Button></form>;
}

function AnswerDisplay({ answer }: { answer: RagAnswer }) {
  const citations = answer.citations ?? answer.evidence ?? [];
  const insufficient = answer.status === 'INSUFFICIENT_EVIDENCE' || !answer.answer;
  if (insufficient) return <div className="alert warning">The available project sources do not contain enough evidence to answer this question. Select additional documents, upload sources, or rephrase the question.</div>;
  return <div className="grid"><div className="panel"><p>{answer.answer}</p></div><h3>Evidence / citations</h3>{citations.map((citation, index) => <CitationCard key={citation.id ?? index} citation={citation} />)}</div>;
}

function CitationCard({ citation }: { citation: Citation }) {
  return <button type="button" className="citation" onClick={() => undefined}><strong>{citation.docCode ?? 'Source'}</strong> {citation.documentTitle} {citation.page ? `p. ${citation.page}` : ''}<p className="muted">{citation.snippet ?? citation.quote}</p></button>;
}

export function AnalysisPage() {
  const projectId = useProjectId();
  const runs = useQuery({ queryKey: ['analysis-runs', projectId], queryFn: () => analysisApi.runs(projectId), enabled: Boolean(projectId) });
  const datasets = useQuery({ queryKey: ['datasets', projectId], queryFn: () => analysisApi.datasets(projectId), enabled: Boolean(projectId) });
  const findings = useQuery({ queryKey: ['findings', projectId], queryFn: () => analysisApi.findings(projectId), enabled: Boolean(projectId) });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Analysis']} />
      <h1 className="page-title">Data & analysis</h1>
      <div className="grid cols-2">
        <Card><h2>Datasets</h2><p className="muted">Server pagination and summaries prevent loading thousands of rows into the browser.</p><RecordRows rows={datasets.data ?? []} /></Card>
        <Card><h2>Analysis plan</h2><div className="grid cols-2"><Field label="Dataset"><select className="select"><option>Select backend dataset</option></select></Field><Field label="Missing data policy"><select className="select"><option>Backend default</option></select></Field></div><Button type="button">Request descriptive statistics</Button></Card>
        <Card><h2>Statistical test planner</h2><div className="alert info">Candidate tests, assumptions, warnings, and blocking compatibility errors are backend values.</div><RecordRows rows={runs.data ?? []} /></Card>
        <Card><h2>Findings to discussion</h2><RecordRows rows={findings.data ?? []} /></Card>
        <Card><h2>Qualitative analysis</h2><Field label="Codebook / memos"><Textarea /></Field><div className="toolbar"><Button type="button" variant="secondary">AI code suggestions</Button><Button type="button" variant="secondary">Accept selected suggestions</Button></div></Card>
        <TraceabilityMatrix projectId={projectId} />
      </div>
    </section>
  );
}

function TraceabilityMatrix({ projectId }: { projectId: string }) {
  const traceability = useQuery({ queryKey: ['traceability', projectId], queryFn: () => analysisApi.traceability(projectId) });
  return <Card><h2>Traceability matrix</h2><div className="table-wrap"><table><thead><tr><th>Objective</th><th>Question</th><th>Instrument</th><th>Variable</th><th>Analysis</th><th>Finding</th><th>Conclusion</th><th>Recommendation</th></tr></thead><tbody>{(traceability.data ?? []).map((row, index) => <tr key={index}><td>{displayValue(row.objective)}</td><td>{displayValue(row.question)}</td><td>{displayValue(row.instrument)}</td><td>{displayValue(row.variable)}</td><td>{displayValue(row.analysis)}</td><td>{displayValue(row.finding)}</td><td>{displayValue(row.conclusion)}</td><td>{displayValue(row.recommendation)}</td></tr>)}</tbody></table></div></Card>;
}

function RecordRows({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No backend records returned" />;
  return <div className="grid">{rows.slice(0, 8).map((row, index) => <p key={index}><Badge>{displayValue(row.status ?? row.type ?? 'Record')}</Badge> {displayValue(row.title ?? row.name ?? row.id)}</p>)}</div>;
}
