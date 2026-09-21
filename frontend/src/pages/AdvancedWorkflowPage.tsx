import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { dashboardApi, documentApi, projectApi, researchApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge } from '../components/ui';
import { EmptyState, ErrorState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { pageContent } from '../utils/collections';
import { paths } from '../routes/paths';

export function AdvancedWorkflowPage() {
  const projectId = useProjectId();

  const progressQuery = useQuery({
    queryKey: ['research-progress', projectId],
    queryFn: () => dashboardApi.researchProgress(projectId),
    enabled: Boolean(projectId),
  });

  if (!projectId) {
    return (
      <main className="page">
        <EmptyState
          title="Select a project"
          description="Open a research project to view its 18 academic methodology stages."
        >
          <Button asChild style={{ marginTop: 12 }}>
            <Link to={paths.projects}>Browse Projects</Link>
          </Button>
        </EmptyState>
      </main>
    );
  }

  if (progressQuery.isLoading) return <PageLoading label="Loading academic research methodology..." />;
  if (progressQuery.isError) return <ErrorState error={progressQuery.error} onRetry={() => progressQuery.refetch()} />;

  const progress = progressQuery.data;
  const stages = progress?.stages ?? [];

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', progress?.projectTitle ?? projectId, 'Advanced Research Workflow']} />

      {/* Header Banner */}
      <Card style={{ marginBottom: 24, border: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <h1 className="page-title" style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>
                Advanced Research Methodology Workflow
              </h1>
              <Badge tone="info">{progress?.completedStages ?? 0} / {progress?.totalStages ?? 18} Completed</Badge>
            </div>
            <p className="muted" style={{ fontSize: '0.9rem', marginTop: 4 }}>
              18 academic methodology milestones covering rigorous research execution from conceptualization to defense.
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
        Methodology Milestones (18 Stages)
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
                  {st.itemCount > 0 ? `${st.itemCount} records` : 'Not started'}
                </span>
                <Button asChild variant="secondary" style={{ fontSize: '0.78rem', padding: '3px 8px' }}>
                  <a href={st.actionUrl}>Work on Stage</a>
                </Button>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Stage Workbenches */}
      <h2 style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: 12 }}>Detailed Stage Workbenches</h2>
      <div className="grid" style={{ gap: 20 }}>
        <ResearchProblemSection projectId={projectId} />
        <ObjectivesSection />
        <LiteratureMatrixSection projectId={projectId} />
        <FrameworksSection projectId={projectId} />
        <MethodologySection />
      </div>
    </section>
  );
}

function ResearchProblemSection({ projectId }: { projectId: string }) {
  const [statement, setStatement] = useState('');
  const save = useMutation({
    mutationFn: (body: Record<string, unknown>) => projectApi.update(projectId, body),
  });
  return (
    <Card>
      <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 12px' }}>Stage 1: Research Problem Statement</h3>
      <Field label="Problem Statement Formulation">
        <Textarea
          placeholder="Formulate the research gap, background contextualization, and problem magnitude..."
          rows={4}
          value={statement}
          onChange={(e) => setStatement(e.target.value)}
        />
      </Field>
      <div style={{ marginTop: 10, display: 'flex', gap: 10 }}>
        <Button type="button" onClick={() => save.mutate({ description: statement })}>
          {save.isPending ? 'Saving...' : 'Save Problem Statement'}
        </Button>
      </div>
    </Card>
  );
}

function ObjectivesSection() {
  return (
    <Card>
      <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 12px' }}>Stage 2: Objectives, Questions & Hypotheses</h3>
      <div className="grid cols-3" style={{ gap: 12 }}>
        <Field label="General Objective"><Textarea placeholder="Primary overarching research aim..." rows={3} /></Field>
        <Field label="Specific Objective"><Textarea placeholder="Specific empirical sub-aim..." rows={3} /></Field>
        <Field label="Linked Hypothesis / Question"><Textarea placeholder="Directional hypothesis or question..." rows={3} /></Field>
      </div>
    </Card>
  );
}

function LiteratureMatrixSection({ projectId }: { projectId: string }) {
  const documents = useQuery({ queryKey: ['documents', projectId], queryFn: () => documentApi.list(projectId) });
  return (
    <Card>
      <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 12px' }}>Stage 3: Literature Matrix & Empirical Synthesis</h3>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Source</th><th>Status</th><th>Pages</th><th>Date Uploaded</th></tr>
          </thead>
          <tbody>
            {pageContent(documents.data).map((doc: any) => (
              <tr key={doc.id}>
                <td><strong>{doc.documentCode ?? doc.docCode ?? doc.filename}</strong> · {doc.title ?? doc.filename}</td>
                <td><Badge tone={doc.status === 'READY' ? 'success' : 'info'}>{doc.status}</Badge></td>
                <td>{doc.currentVersion?.pageCount ?? doc.pageCount ?? '—'}</td>
                <td className="muted">{doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function FrameworksSection({ projectId }: { projectId: string }) {
  const conceptual = useQuery({ queryKey: ['conceptual-frameworks', projectId], queryFn: () => researchApi.conceptualFrameworks(projectId) });
  const theoretical = useQuery({ queryKey: ['theoretical-frameworks', projectId], queryFn: () => researchApi.theoreticalFrameworks(projectId) });
  return (
    <Card>
      <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 12px' }}>Stage 4: Conceptual & Theoretical Frameworks</h3>
      <div className="grid cols-2" style={{ gap: 16 }}>
        <div>
          <h4>Conceptual Framework</h4>
          <p className="muted" style={{ fontSize: '0.85rem' }}>
            {conceptual.data?.length ? `${conceptual.data.length} models defined` : 'No conceptual models defined yet.'}
          </p>
        </div>
        <div>
          <h4>Theoretical Foundations</h4>
          <p className="muted" style={{ fontSize: '0.85rem' }}>
            {theoretical.data?.length ? `${theoretical.data.length} paradigms assigned` : 'No theoretical paradigms assigned yet.'}
          </p>
        </div>
      </div>
    </Card>
  );
}

function MethodologySection() {
  return (
    <Card>
      <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 12px' }}>Stage 5: Research Design & Methodology</h3>
      <div className="grid cols-3" style={{ gap: 12 }}>
        <Field label="Research Paradigm"><Input placeholder="e.g. Pragmatism, Positivism" /></Field>
        <Field label="Design"><Input placeholder="e.g. Convergent Mixed Methods" /></Field>
        <Field label="Target Population"><Input placeholder="e.g. Licensed Practitioners" /></Field>
        <Field label="Sampling Technique"><Input placeholder="e.g. Stratified Random Sampling" /></Field>
        <Field label="Reliability Controls"><Input placeholder="e.g. Cronbach's Alpha >= 0.8" /></Field>
        <Field label="Ethical Review"><Input placeholder="e.g. IRB Protocol Approved" /></Field>
      </div>
    </Card>
  );
}
