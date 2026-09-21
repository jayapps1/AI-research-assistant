import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  Copy,
  Download,
  FileText,
  ListOrdered,
  RefreshCw,
  Save,
  Sparkles,
} from 'lucide-react';
import { analysisApi, notificationApi, projectApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Select, Modal } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

export function ReportPage() {
  const projectId = useProjectId();
  const queryClient = useQueryClient();

  const [selectedReportId, setSelectedReportId] = useState('');
  const [selectedChapterId, setSelectedChapterId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [sectionContent, setSectionContent] = useState('');
  const [showTocModal, setShowTocModal] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);

  // Queries
  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  const reports = useQuery({
    queryKey: ['reports', projectId, 0, 20],
    queryFn: () => reportApi.reports(projectId, 0, 20),
    enabled: Boolean(projectId),
  });

  const capabilitiesQuery = useQuery({
    queryKey: ['section-capabilities', projectId],
    queryFn: () => reportApi.sectionCapabilities(projectId),
    enabled: Boolean(projectId),
  });

  const selectedReport = pageContent(reports.data).find((report) => report.id === selectedReportId) ?? pageContent(reports.data)[0];
  const reportId = String(selectedReport?.id ?? '');

  const chapters = useQuery({
    queryKey: ['report-chapters', reportId],
    queryFn: () => reportApi.chapters(reportId),
    enabled: Boolean(reportId),
  });

  const selectedChapter = (chapters.data ?? []).find((chapter) => chapter.id === selectedChapterId) ?? chapters.data?.[0];
  const chapterId = String(selectedChapter?.id ?? '');

  const sections = useQuery({
    queryKey: ['report-sections', chapterId],
    queryFn: () => reportApi.sections(chapterId),
    enabled: Boolean(chapterId),
  });

  const selectedSection = (sections.data ?? []).find((section) => section.id === selectedSectionId) ?? sections.data?.[0];

  // Set initial content when selected section changes
  useEffect(() => {
    if (selectedSection) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSectionContent(String(selectedSection.content ?? ''));
    }
  }, [selectedSection]);

  // Mutations
  const validation = useMutation({ mutationFn: () => reportApi.validate(reportId) });
  const assemble = useMutation({
    mutationFn: () => reportApi.assemble(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setSaveStatus('Report assembled from template!');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });
  const finalize = useMutation({ mutationFn: () => reportApi.finalize(reportId) });

  const saveSection = useMutation({
    mutationFn: () => reportApi.updateSection(String(selectedSection?.id), { content: sectionContent }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setSaveStatus('Section revision saved successfully.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const generateSection = useMutation({
    mutationFn: () => reportApi.generateSection(String(selectedSection?.id)),
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      if (data?.content) {
        setSectionContent(data.content);
      }
      setSaveStatus('AI draft generated for section.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Section generation failed.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  const tocMutation = useMutation({
    mutationFn: () => reportApi.tableOfContents(reportId),
    onSuccess: () => {
      setShowTocModal(true);
    },
  });

  const docxExport = useMutation({ mutationFn: () => reportApi.exports(reportId, { format: 'DOCX' }) });
  const pdfExport = useMutation({ mutationFn: () => reportApi.exports(reportId, { format: 'PDF' }) });
  const integrity = useMutation({ mutationFn: () => reportApi.integrity(reportId) });
  const writing = useMutation({ mutationFn: () => reportApi.writingReview(reportId) });
  const similarity = useMutation({ mutationFn: () => reportApi.similarity(projectId, { targetType: 'REPORT', targetId: reportId }) });
  const aiUsage = useQuery({ queryKey: ['ai-usage', projectId], queryFn: () => analysisApi.aiUsage(projectId), enabled: Boolean(projectId) });

  if (!projectId) return <EmptyState title="Select a project" />;

  // Find capability for selected section
  const sectionCap = capabilitiesQuery.data?.sections?.find((s) => s.sectionId === selectedSection?.id);
  const isEmpiricalBlocked = sectionCap && !sectionCap.canGenerate;

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectQuery.data?.title || projectId, 'Report']} />

      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <FileText className="text-primary" size={26} />
            Professional Report Builder
          </h1>
          <p className="muted" style={{ maxWidth: '750px' }}>
            Template-driven multi-chapter academic report workspace with deterministic Table of Contents, verified source citations, and empirical data safety.
          </p>
        </div>

        <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Button type="button" variant="secondary" onClick={() => tocMutation.mutate()} disabled={!reportId || tocMutation.isPending}>
            <ListOrdered size={15} style={{ marginRight: 6 }} />
            {tocMutation.isPending ? 'Generating TOC...' : 'Table of Contents'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => validation.mutate()} disabled={!reportId || validation.isPending}>
            Validate
          </Button>
          <Button type="button" variant="secondary" onClick={() => assemble.mutate()} disabled={!reportId || assemble.isPending}>
            <RefreshCw size={15} style={{ marginRight: 6 }} />
            Assemble from Template
          </Button>
          <Button type="button" variant="primary" onClick={() => finalize.mutate()} disabled={!reportId || finalize.isPending}>
            Finalize Report
          </Button>
        </div>
      </div>

      {saveStatus && (
        <div style={{
          padding: '0.75rem 1rem',
          borderRadius: '8px',
          backgroundColor: saveStatus.includes('failed') || saveStatus.includes('Failed') ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
          color: saveStatus.includes('failed') || saveStatus.includes('Failed') ? 'var(--color-danger, #ef4444)' : 'var(--color-success, #10b981)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          marginBottom: '1rem',
          fontSize: '0.9rem',
          border: '1px solid currentColor'
        }}>
          {saveStatus.includes('failed') || saveStatus.includes('Failed') ? <AlertTriangle size={16} /> : <CheckCircle2 size={16} />}
          {saveStatus}
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid cols-3" style={{ marginBottom: '1.5rem' }}>
        <Metric label="Active Report" value={selectedReport?.title ?? 'No report'} />
        <Metric label="Report Template" value={projectQuery.data?.reportTemplateName ?? 'TTU Computer Science Final Project Report'} />
        <Metric label="Citation Style" value={projectQuery.data?.citationStyle ?? selectedReport?.citationStyle ?? 'APA 7th Edition'} />
      </div>

      {/* Main Report Builder Layout */}
      <div className="report-builder" style={{ display: 'grid', gridTemplateColumns: '320px 1fr 340px', gap: '1.5rem', alignItems: 'flex-start' }}>
        {/* Left Column: Chapters & Sections Tree */}
        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.75rem' }}>Report Structure</h2>
          <Field label="Open Report">
            <Select value={reportId} onChange={(event) => setSelectedReportId(event.target.value)}>
              {pageContent(reports.data).map((report) => (
                <option key={String(report.id)} value={String(report.id)}>
                  {displayValue(report.title)}
                </option>
              ))}
            </Select>
          </Field>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1rem', maxHeight: '520px', overflowY: 'auto' }}>
            {(chapters.data ?? []).map((chapter) => {
              const isChapterSelected = chapter.id === chapterId;
              return (
                <div className="tree-node" key={String(chapter.id)} style={{ border: '1px solid var(--border)', borderRadius: '6px', padding: '6px 8px' }}>
                  <button
                    type="button"
                    className="tree-button"
                    onClick={() => setSelectedChapterId(String(chapter.id))}
                    style={{
                      width: '100%',
                      textAlign: 'left',
                      fontWeight: isChapterSelected ? 700 : 500,
                      background: 'none',
                      border: 'none',
                      cursor: 'pointer',
                      fontSize: '0.9rem',
                      padding: '4px 0',
                    }}
                  >
                    {displayValue(chapter.title ?? chapter.type)}
                  </button>

                  {isChapterSelected && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '6px', paddingLeft: '8px', borderLeft: '2px solid var(--primary)' }}>
                      {(sections.data ?? []).map((section) => {
                        const isSecSelected = section.id === selectedSection?.id;
                        const cap = capabilitiesQuery.data?.sections?.find((s) => s.sectionId === section.id);
                        return (
                          <button
                            key={String(section.id)}
                            type="button"
                            onClick={() => {
                              setSelectedSectionId(String(section.id));
                              setSectionContent(String(section.content ?? ''));
                            }}
                            style={{
                              textAlign: 'left',
                              padding: '6px 8px',
                              borderRadius: '4px',
                              background: isSecSelected ? 'var(--surface-hover)' : 'transparent',
                              border: isSecSelected ? '1px solid var(--primary)' : 'none',
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              fontSize: '0.82rem',
                            }}
                          >
                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '170px' }}>
                              {displayValue(section.heading ?? section.type)}
                            </span>
                            {cap?.status === 'DATA_REQUIRED' && <Badge tone="warning" style={{ fontSize: '0.68rem' }}>DATA REQ</Badge>}
                            {cap?.status === 'FINDINGS_REQUIRED' && <Badge tone="warning" style={{ fontSize: '0.68rem' }}>FINDINGS REQ</Badge>}
                            {cap?.status === 'READY' && <Badge tone="success" style={{ fontSize: '0.68rem' }}>READY</Badge>}
                          </button>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {!pageContent(reports.data).length && (
            <EmptyState title="No reports" description="Create or assemble a report from the backend workflow." />
          )}
        </Card>

        {/* Center Column: Section Editor & Workspace */}
        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.75rem' }}>Section Editor</h2>
          {selectedSection ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
                <Badge>{displayValue(selectedSection.origin, 'USER')}</Badge>
                <Badge tone={selectedSection.sourceOutOfDate ? 'warning' : 'success'}>
                  {selectedSection.sourceOutOfDate ? 'STALE SOURCE' : 'CURRENT SOURCE'}
                </Badge>
                <Badge>Revision {displayValue(selectedSection.revisionNumber)}</Badge>
                {sectionCap?.status && (
                  <Badge tone={sectionCap.status === 'READY' ? 'success' : 'warning'}>
                    {sectionCap.status}
                  </Badge>
                )}
              </div>

              {/* Empirical Safety Warning */}
              {isEmpiricalBlocked && (
                <div className="alert warning" style={{ fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertTriangle size={18} />
                  <div>
                    <strong>Empirical Data Safety:</strong> {sectionCap?.reason || 'This section requires uploaded dataset or analysis findings before AI generation.'}
                  </div>
                </div>
              )}

              <Field label="Section Heading">
                <Input value={String(selectedSection.heading ?? '')} readOnly />
              </Field>

              <Field label="Content">
                <Textarea
                  rows={14}
                  value={sectionContent}
                  onChange={(event) => setSectionContent(event.target.value)}
                  placeholder="Write manually or generate section draft with AI..."
                  style={{ fontFamily: 'inherit', lineHeight: 1.6, fontSize: '0.92rem' }}
                />
              </Field>

              <div className="toolbar" style={{ display: 'flex', gap: '0.75rem', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Button
                    type="button"
                    variant="primary"
                    onClick={() => saveSection.mutate()}
                    disabled={saveSection.isPending}
                    style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                  >
                    <Save size={15} /> Save Revision
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => generateSection.mutate()}
                    disabled={generateSection.isPending || Boolean(isEmpiricalBlocked)}
                    style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                  >
                    <Sparkles size={15} className="text-primary" />
                    {generateSection.isPending ? 'Generating...' : 'AI Draft'}
                  </Button>
                </div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>
                  {sectionContent.split(/\s+/).filter(Boolean).length} words • {sectionContent.length} chars
                </span>
              </div>

              {generateSection.data && <DraftPreview draft={generateSection.data} />}
            </div>
          ) : (
            <EmptyState title="Select a report section" description="Click on any chapter and section on the left to view or edit its contents." />
          )}
        </Card>

        {/* Right Column: Evidence & AI Usage */}
        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.5rem' }}>Evidence & Traceability</h2>
          <p className="muted" style={{ fontSize: '0.82rem', marginBottom: '1rem' }}>
            Section citations resolve to backend evidence and reference metadata where available.
          </p>
          <RecordRows rows={aiUsage.data ? [aiUsage.data] : []} />
        </Card>
      </div>

      {/* Review, Export, and Quality Row */}
      <div className="grid cols-2" style={{ marginTop: '1.5rem', gap: '1.5rem' }}>
        <ValidationPanel validation={validation.data} />

        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.75rem' }}>Export Publication Report</h2>
          <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
            <Button type="button" variant="primary" onClick={() => docxExport.mutate()} disabled={!reportId || docxExport.isPending}>
              <Download size={14} style={{ marginRight: 6 }} /> Export DOCX
            </Button>
            <Button type="button" variant="secondary" onClick={() => pdfExport.mutate()} disabled={!reportId || pdfExport.isPending}>
              <Download size={14} style={{ marginRight: 6 }} /> Export PDF
            </Button>
          </div>
          <ExportJob data={docxExport.data} />
          <ExportJob data={pdfExport.data} />
        </Card>

        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.5rem' }}>Similarity & Overlap Review</h2>
          <p className="muted" style={{ fontSize: '0.82rem', marginBottom: '0.75rem' }}>
            Verifies academic uniqueness against project sources.
          </p>
          <Button type="button" variant="secondary" onClick={() => similarity.mutate()} disabled={!reportId || similarity.isPending}>
            Run Similarity Check
          </Button>
          <RecordRows rows={similarity.data ? [similarity.data] : []} />
        </Card>

        <Card style={{ padding: '1.25rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.5rem' }}>Integrity & Writing Review</h2>
          <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <Button type="button" variant="secondary" onClick={() => integrity.mutate()} disabled={!reportId || integrity.isPending}>
              Integrity Review
            </Button>
            <Button type="button" variant="secondary" onClick={() => writing.mutate()} disabled={!reportId || writing.isPending}>
              Writing Review
            </Button>
          </div>
          <RecordRows rows={[integrity.data, writing.data].filter(Boolean) as Record<string, unknown>[]} />
        </Card>
      </div>

      {/* Table of Contents Modal */}
      <Modal title="Report Table of Contents" open={showTocModal} onClose={() => setShowTocModal(false)}>
        {tocMutation.data ? (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
              <span style={{ fontWeight: 600 }}>{tocMutation.data.title}</span>
              <Badge tone="success">Deterministic (0 Credits)</Badge>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '420px', overflowY: 'auto' }}>
              {tocMutation.data.items?.map((item) => (
                <div key={item.chapterId} style={{ marginBottom: '0.5rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '0.92rem' }}>
                    <span>{item.title}</span>
                    <span style={{ fontFamily: 'monospace' }}>p. {item.pageNumber}</span>
                  </div>
                  {item.sections?.map((sec) => (
                    <div key={sec.sectionId} style={{ display: 'flex', justifyContent: 'space-between', paddingLeft: '1.25rem', fontSize: '0.85rem', color: 'var(--color-muted)', marginTop: '3px' }}>
                      <span>{sec.title}</span>
                      <span style={{ fontFamily: 'monospace' }}>p. {sec.pageNumber}</span>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p>Loading table of contents...</p>
        )}
      </Modal>
    </section>
  );
}

export function ReferencesPage() {
  const projectId = useProjectId();
  const [q, setQ] = useState('');
  const [style, setStyle] = useState('APA_7');
  const [presentation, setPresentation] = useState<'PARENTHETICAL' | 'NARRATIVE'>('PARENTHETICAL');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const references = useQuery({
    queryKey: ['references', projectId, q],
    queryFn: () => reportApi.references(projectId, 0, 50, { title: q || undefined }),
    enabled: Boolean(projectId),
  });

  const format = useMutation({
    mutationFn: () =>
      reportApi.formatCitation({
        referenceId: pageContent(references.data)[0]?.id,
        style,
        presentation,
      }),
  });

  const duplicates = useMutation({
    mutationFn: () =>
      reportApi.duplicateReferences(projectId, {
        referenceIds: pageContent(references.data).map((reference) => reference.id),
      }),
  });

  const importRefs = useMutation({
    mutationFn: (formatName: string) => reportApi.importReferences(projectId, { format: formatName, content: '' }),
  });

  const refList = pageContent(references.data) as any[];

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'References']} />

      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <BookOpen className="text-primary" size={26} />
            Reference Library & Citation Formatter
          </h1>
          <p className="muted">
            Manage project scholarly references with strict APA 7th, IEEE, Harvard, Chicago, Vancouver, and MLA-9 formatting.
          </p>
        </div>

        <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Button type="button" variant="secondary" onClick={() => importRefs.mutate('RIS')}>
            Import RIS
          </Button>
          <Button type="button" variant="secondary" onClick={() => importRefs.mutate('BIBTEX')}>
            Import BibTeX
          </Button>
          <Button type="button" variant="secondary" onClick={() => importRefs.mutate('ENDNOTE_XML')}>
            Import EndNote XML
          </Button>
          <Button type="button" variant="secondary" onClick={() => duplicates.mutate()}>
            Check Duplicates
          </Button>
        </div>
      </div>

      {/* Citation Style Selector Card */}
      <Card style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '0.75rem' }}>Citation Style & Presentation</h2>
        <div className="toolbar" style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.88rem' }}>Style:</span>
            <Select value={style} onChange={(event) => setStyle(event.target.value)}>
              {[
                { id: 'APA_7', label: 'APA 7th Edition (Default)' },
                { id: 'IEEE', label: 'IEEE Numerical' },
                { id: 'HARVARD', label: 'Harvard Author-Date' },
                { id: 'CHICAGO_AUTHOR_DATE', label: 'Chicago Author-Date' },
                { id: 'VANCOUVER', label: 'Vancouver Numerical' },
                { id: 'MLA_9', label: 'MLA 9th Edition' },
              ].map((item) => (
                <option key={item.id} value={item.id}>{item.label}</option>
              ))}
            </Select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.88rem' }}>Presentation:</span>
            <Select value={presentation} onChange={(e) => setPresentation(e.target.value as any)}>
              <option value="PARENTHETICAL">Parenthetical e.g., (Smith, 2024)</option>
              <option value="NARRATIVE">Narrative e.g., Smith (2024)</option>
            </Select>
          </div>

          <Button type="button" variant="secondary" onClick={() => format.mutate()} disabled={format.isPending || refList.length === 0}>
            Preview Formatted Citation
          </Button>
        </div>

        {format.data && (
          <div className="panel" style={{ marginTop: '1rem', padding: '1rem', background: 'var(--surface-hover)', borderRadius: '6px' }}>
            <span className="muted" style={{ fontSize: '0.78rem' }}>FORMATTED PREVIEW ({style}):</span>
            <div style={{ fontSize: '0.95rem', fontWeight: 500, marginTop: '4px' }}>
              {displayValue(format.data.formattedCitation ?? format.data.citation)}
            </div>
          </div>
        )}
      </Card>

      {/* References Search and List */}
      <Card style={{ padding: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>
            Project Sources & References ({refList.length})
          </h2>
          <div style={{ width: '280px' }}>
            <Input
              value={q}
              onChange={(event) => setQ(event.target.value)}
              placeholder="Search references by title or author..."
            />
          </div>
        </div>

        {refList.length === 0 ? (
          <EmptyState
            title="No references found"
            description="Upload research papers in Documents/Sources to automatically populate the project reference library."
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {refList.map((ref: any, idx: number) => {
              const hasMissingMeta = !ref.authors || !ref.year;
              return (
                <div
                  key={ref.id || idx}
                  style={{
                    padding: '1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    gap: '1rem',
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '4px' }}>
                      <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.85rem' }}>
                        [{ref.referenceNumber ?? idx + 1}]
                      </span>
                      <strong style={{ fontSize: '0.95rem' }}>{ref.title || 'Untitled Reference'}</strong>
                      {hasMissingMeta && (
                        <Badge tone="warning" style={{ fontSize: '0.72rem' }}>
                          Missing Metadata
                        </Badge>
                      )}
                    </div>
                    <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>
                      {ref.authors ? ref.authors : 'Author(s) unparsed'} • {ref.year ? ref.year : 'n.d.'} • {ref.sourceTitle || ref.journal || ref.publisher || 'Source details pending'}
                    </p>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                      type="button"
                      variant="secondary"
                      className="btn-compact"
                      onClick={() => {
                        const cit = `${ref.authors || 'Author'} (${ref.year || 'n.d.'}). ${ref.title}.`;
                        navigator.clipboard.writeText(cit);
                        setCopiedId(ref.id);
                        setTimeout(() => setCopiedId(null), 2500);
                      }}
                    >
                      <Copy size={13} style={{ marginRight: 4 }} />
                      {copiedId === ref.id ? 'Copied!' : 'Copy'}
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {duplicates.data && (
        <Card style={{ marginTop: '1.5rem' }}>
          <h2>Duplicates</h2>
          <p className="muted">
            Exact duplicate and possible duplicate findings require user review; uncertain references are not auto-merged.
          </p>
          <RecordRows rows={[duplicates.data]} />
        </Card>
      )}
    </section>
  );
}

export { BillingPage } from './BillingPage';

export function NotificationsPage() {
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: notificationApi.list });
  const markAll = useMutation({ mutationFn: notificationApi.markAllRead });
  return <section className="page"><div className="page-header"><h1 className="page-title">Notifications</h1><Button type="button" variant="secondary" onClick={() => markAll.mutate()}>Mark all read</Button></div>{notifications.isError ? <ErrorState error={notifications.error} /> : <RecordRows rows={pageContent(notifications.data) as never} />}</section>;
}

export { ProfilePage } from './ProfilePage';
export { SecuritySettingsPage } from './SecuritySettingsPage';

export function NotificationSettingsPage() {
  return <section className="page"><h1 className="page-title">Notification settings</h1><div className="grid cols-2"><Card><h2>Channels</h2>{['In-app', 'Email', 'SMS', 'Push'].map((channel) => <label key={channel} className="field"><span className="label">{channel}</span><input type="checkbox" /></label>)}</Card><Card><h2>Types</h2><p className="muted">Preferences are shown for backend-supported notification types. Unsupported channels remain disabled by backend policy.</p></Card></div></section>;
}

export { AdminDashboardPage as AdminPage } from './admin/AdminDashboardPage';

function RecordRows({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No backend records returned" />;
  return <div className="grid">{rows.slice(0, 10).map((row, index) => <div className="panel" key={index}><Badge>{displayValue(row.status ?? row.type ?? row.id ?? 'Record')}</Badge><p>{displayValue(row.title ?? row.name ?? row.message ?? row.description ?? row.email ?? row.id)}</p></div>)}</div>;
}

function Metric({ label, value }: { label: string; value: unknown }) {
  return <Card><span className="muted">{label}</span><p className="metric">{displayValue(value)}</p></Card>;
}

function DraftPreview({ draft }: { draft: Record<string, unknown> }) {
  return <div className="panel"><Badge tone="info">AI draft</Badge><p>{displayValue(draft.draftText ?? draft.text)}</p><div className="toolbar"><Button type="button">Accept</Button><Button type="button" variant="secondary">Edit</Button><Button type="button" variant="danger">Reject</Button></div></div>;
}

function ValidationPanel({ validation }: { validation?: Record<string, unknown> }) {
  const errors = Array.isArray(validation?.errors) ? validation.errors as Record<string, unknown>[] : [];
  const warnings = Array.isArray(validation?.warnings) ? validation.warnings as Record<string, unknown>[] : [];
  const information = Array.isArray(validation?.information) ? validation.information as Record<string, unknown>[] : [];
  return (
    <Card>
      <h2>Report validation</h2>
      {[['ERROR', errors], ['WARNING', warnings], ['INFO', information]].map(([severity, rows]) => (
        <div key={String(severity)}>
          <h3>{String(severity)}</h3>
          {(rows as Record<string, unknown>[]).length ? (rows as Record<string, unknown>[]).map((issue, index) => <div className={`alert ${String(severity).toLowerCase()}`} key={String(issue.code ?? index)}>{displayValue(issue.message)} {issue.artifactId ? <a href={`#${issue.artifactId}`}>Open artifact</a> : null}</div>) : <p className="muted">No {String(severity).toLowerCase()} issues.</p>}
        </div>
      ))}
    </Card>
  );
}

function ExportJob({ data }: { data?: Record<string, unknown> }) {
  if (!data) return null;
  const id = String(data.id ?? '');
  const status = String(data.status ?? '');
  return <div className="panel"><Badge tone={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning'}>{displayValue(data.format)} {status}</Badge><p className="muted">Revision {displayValue(data.reportRevisionNumber)} | Size {displayValue(data.fileSizeBytes)}</p>{status === 'COMPLETED' && id ? <a className="button secondary" href={reportApi.exportDownloadUrl(id)}>Download</a> : null}</div>;
}
