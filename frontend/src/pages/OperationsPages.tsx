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
  Sparkles,
  Layers,
  Eye,
  Edit3,
  FileDown,
  RotateCw,
  ShieldAlert,
  Bookmark,
  Check,
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  Table,
  FilePlus,
  CornerDownRight,
  Edit2,
} from 'lucide-react';
import { analysisApi, notificationApi, projectApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Input, Badge, Select, Modal } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';
import { ReportRichEditor } from '../components/editor/ReportRichEditor';
import type { LiteratureMatrixInclusion, ReorderStructureRequest } from '../types/api';

const CITATION_STYLES = [
  { id: 'APA_7', label: 'APA 7th Edition (Author, Year)' },
  { id: 'IEEE', label: 'IEEE [1]' },
  { id: 'NUMERIC_APA', label: 'Numeric + APA Bibliography [1]' },
  { id: 'HARVARD', label: 'Harvard (Author, Year)' },
  { id: 'CHICAGO_AUTHOR_DATE', label: 'Chicago Author-Date (Author, Year)' },
  { id: 'MLA_9', label: 'MLA 9th Edition (Author)' },
  { id: 'VANCOUVER', label: 'Vancouver [1]' },
];

export function ReportPage() {
  const projectId = useProjectId();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'sections' | 'final-doc' | 'preview'>('sections');
  const [selectedReportId, setSelectedReportId] = useState('');
  const [selectedChapterId, setSelectedChapterId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [showTocModal, setShowTocModal] = useState(false);
  const [tocViewMode, setTocViewMode] = useState<'dotleaders' | 'outline' | 'markdown'>('dotleaders');
  const [tocCopied, setTocCopied] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [validationModalOpen, setValidationModalOpen] = useState(false);
  const [validationErrors, setValidationErrors] = useState<any[]>([]);

  // Structure & Modal States
  const [showAddChapterModal, setShowAddChapterModal] = useState(false);
  const [newChapterTitle, setNewChapterTitle] = useState('');
  const [showAddSectionModal, setShowAddSectionModal] = useState<{ chapterId: string; parentSectionId?: string | null; parentTitle?: string } | null>(null);
  const [newSectionTitle, setNewSectionTitle] = useState('');
  const [showRenameModal, setShowRenameModal] = useState<{ type: 'chapter' | 'section'; id: string; currentTitle: string } | null>(null);
  const [renameTitle, setRenameTitle] = useState('');
  const [showLitMatrixModal, setShowLitMatrixModal] = useState(false);
  const [deleteConfirmModal, setDeleteConfirmModal] = useState<{ type: 'chapter' | 'section'; id: string; title: string; required: boolean } | null>(null);

  // Queries
  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  // Lazy Initialization on Mount: Guarantee report and template sections exist
  const ensureReport = useMutation({
    mutationFn: () => reportApi.ensure(projectId),
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      if (data?.id && !selectedReportId) {
        setSelectedReportId(String(data.id));
      }
    },
  });

  useEffect(() => {
    if (projectId) {
      ensureReport.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId]);

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

  const structureQuery = useQuery({
    queryKey: ['report-structure', reportId],
    queryFn: () => reportApi.structure(reportId),
    enabled: Boolean(reportId),
  });

  const litMatrixQuery = useQuery({
    queryKey: ['literature-matrix', projectId],
    queryFn: () => reportApi.literatureMatrix(projectId),
    enabled: Boolean(projectId),
  });

  const currentChapterId = selectedChapterId || (structureQuery.data?.chapters?.[0] ? String(structureQuery.data.chapters[0].id) : '');

  const chapters = useQuery({
    queryKey: ['report-chapters', reportId],
    queryFn: () => reportApi.chapters(reportId),
    enabled: Boolean(reportId),
  });

  const selectedChapter = (chapters.data ?? []).find((chapter) => chapter.id === currentChapterId) ??
    (structureQuery.data?.chapters ?? []).find((ch) => ch.id === currentChapterId) ?? chapters.data?.[0];
  const chapterId = String(selectedChapter?.id ?? currentChapterId);

  const sections = useQuery({
    queryKey: ['report-sections', chapterId],
    queryFn: () => reportApi.sections(chapterId),
    enabled: Boolean(chapterId),
  });

  const currentSectionId = selectedSectionId ||
    (selectedChapter && 'sections' in selectedChapter && (selectedChapter as any).sections?.[0] ? String((selectedChapter as any).sections[0].id) : '') ||
    (sections.data?.[0] ? String(sections.data[0].id) : '');

  const selectedSection = (sections.data ?? []).find((section) => String(section.id) === currentSectionId) ?? sections.data?.[0];

  // Final document query
  const finalDocQuery = useQuery({
    queryKey: ['report-final-document', reportId],
    queryFn: () => reportApi.finalDocument(reportId),
    enabled: Boolean(reportId) && activeTab === 'final-doc',
  });

  // Validation query for preview panel
  const validationQuery = useQuery({
    queryKey: ['report-validation', reportId],
    queryFn: () => reportApi.validate(reportId),
    enabled: Boolean(reportId) && activeTab === 'preview',
  });

  const handleSelectSection = (chapId: string, secId: string) => {
    setSelectedChapterId(chapId);
    setSelectedSectionId(secId);
  };

  // Mutations
  const reorderStructure = useMutation({
    mutationFn: (body: ReorderStructureRequest) => reportApi.reorderStructure(reportId, body),
    onSuccess: (updated) => {
      queryClient.setQueryData(['report-structure', reportId], updated);
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setSaveStatus('Report structure reordered.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const createChapter = useMutation({
    mutationFn: (title: string) => reportApi.createChapter(reportId, { title, description: '' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      setShowAddChapterModal(false);
      setNewChapterTitle('');
      setSaveStatus('New chapter added.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const updateChapter = useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) => reportApi.updateChapter(id, { title }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      setShowRenameModal(null);
      setSaveStatus('Chapter renamed.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const deleteChapter = useMutation({
    mutationFn: (chapId: string) => reportApi.deleteChapter(chapId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      setDeleteConfirmModal(null);
      setSaveStatus('Chapter deleted.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Failed to delete chapter.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const createSection = useMutation({
    mutationFn: ({ chapterId: chapId, title, parentSectionId }: { chapterId: string; title: string; parentSectionId?: string | null }) =>
      reportApi.createSection(chapId, { title, heading: title, sectionType: 'CUSTOM', parentSectionId }),
    onSuccess: (newSec: any) => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setShowAddSectionModal(null);
      setNewSectionTitle('');
      if (newSec?.id) {
        setSelectedSectionId(String(newSec.id));
      }
      setSaveStatus('New section added.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const updateSectionTitle = useMutation({
    mutationFn: ({ id, heading }: { id: string; heading: string }) => reportApi.updateSection(id, { heading }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setShowRenameModal(null);
      setSaveStatus('Section renamed.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const deleteSection = useMutation({
    mutationFn: (secId: string) => reportApi.deleteSection(secId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setDeleteConfirmModal(null);
      setSaveStatus('Section deleted.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Failed to delete section.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const refreshReferences = useMutation({
    mutationFn: () => reportApi.refreshReferences(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Deterministic academic bibliography compiled (0 AI credits used).');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const updateReportSettings = useMutation({
    mutationFn: (settings: { citationStyle?: string; includeUncitedReferences?: boolean; literatureMatrixInclusion?: LiteratureMatrixInclusion }) =>
      reportApi.updateSettings(reportId, settings),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Report settings updated.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const moveChapter = (idx: number, direction: 'up' | 'down') => {
    const list = structureQuery.data?.chapters;
    if (!list) return;
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= list.length) return;
    const copy = [...list];
    const temp = copy[idx];
    copy[idx] = copy[targetIdx];
    copy[targetIdx] = temp;
    const payload = copy.map((ch, i) => ({ id: ch.id, displayOrder: i + 1 }));
    reorderStructure.mutate({ chapters: payload });
  };

  const moveSection = (chapId: string, sectionsList: any[], idx: number, direction: 'up' | 'down', parentSecId?: string | null) => {
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= sectionsList.length) return;
    const copy = [...sectionsList];
    const temp = copy[idx];
    copy[idx] = copy[targetIdx];
    copy[targetIdx] = temp;
    const payload = copy.map((s, i) => ({ id: s.id, parentId: parentSecId || chapId, displayOrder: i + 1 }));
    reorderStructure.mutate({ sections: payload });
  };
  const validation = useMutation({
    mutationFn: () => reportApi.validate(reportId),
    onSuccess: (data: any) => {
      queryClient.setQueryData(['report-validation', reportId], data);
      if (data?.errors?.length > 0) {
        setValidationErrors(data.errors);
        setValidationModalOpen(true);
      } else {
        setSaveStatus('Validation passed! Report meets publication requirements.');
        setTimeout(() => setSaveStatus(null), 3500);
      }
    },
  });

  const assemble = useMutation({
    mutationFn: () => reportApi.assemble(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setSaveStatus('Report assembled from template.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });

  const finalize = useMutation({
    mutationFn: () => reportApi.finalize(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      setSaveStatus('Report finalized successfully! Ready for verified export.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
    onError: (err: any) => {
      const resp = err?.response?.data;
      if (resp?.code === 'REPORT_VALIDATION_FAILED' || resp?.status === 422 || resp?.errors) {
        setValidationErrors(resp?.errors || []);
        setValidationModalOpen(true);
      } else {
        setSaveStatus(resp?.message || err?.message || 'Finalization failed.');
        setTimeout(() => setSaveStatus(null), 4000);
      }
    },
  });

  const saveSection = useMutation({
    mutationFn: (data: { content?: string; contentJson?: string; plainText?: string }) =>
      reportApi.updateSection(String(selectedSection?.id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      setSaveStatus('Section revision saved.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const updateCitationStyle = useMutation({
    mutationFn: (style: string) => reportApi.updateReport(reportId, { citationStyle: style }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Citation style updated deterministically (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const prepareFinalDoc = useMutation({
    mutationFn: () => reportApi.prepareFinalDocument(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Final document prepared and snapshot compiled.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const updateFinalDoc = useMutation({
    mutationFn: (data: { contentJson?: string; plainText?: string; title?: string }) =>
      reportApi.updateFinalDocument(reportId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
    },
  });

  const generateSection = useMutation({
    mutationFn: () => reportApi.generateSection(String(selectedSection?.id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
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

  // Draft exports (Always allowed even with validation errors)
  const draftDocxExport = useMutation({
    mutationFn: () => reportApi.exports(reportId, { format: 'DOCX', draft: true }),
    onSuccess: () => {
      setSaveStatus('Draft DOCX generated (for academic review).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });
  const draftPdfExport = useMutation({
    mutationFn: () => reportApi.exports(reportId, { format: 'PDF', draft: true }),
    onSuccess: () => {
      setSaveStatus('Draft PDF generated (for academic review).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });

  // Final exports (Enforce validation)
  const finalDocxExport = useMutation({
    mutationFn: () => reportApi.exports(reportId, { format: 'DOCX', draft: false }),
    onError: (err: any) => {
      const resp = err?.response?.data;
      if (resp?.code === 'REPORT_VALIDATION_FAILED' || resp?.status === 422) {
        setValidationErrors(resp?.errors || []);
        setValidationModalOpen(true);
      }
    },
  });
  const finalPdfExport = useMutation({
    mutationFn: () => reportApi.exports(reportId, { format: 'PDF', draft: false }),
    onError: (err: any) => {
      const resp = err?.response?.data;
      if (resp?.code === 'REPORT_VALIDATION_FAILED' || resp?.status === 422) {
        setValidationErrors(resp?.errors || []);
        setValidationModalOpen(true);
      }
    },
  });

  const aiUsage = useQuery({ queryKey: ['ai-usage', projectId], queryFn: () => analysisApi.aiUsage(projectId), enabled: Boolean(projectId) });

  if (!projectId) return <EmptyState title="Select a project" />;

  const currentCitationStyle = ((selectedReport as any)?.citationStyle || structureQuery.data?.citationStyle || projectQuery.data?.citationStyle || 'APA_7') as string;
  const isFinal = (selectedReport as any)?.status === 'FINAL';
  const sectionCap = capabilitiesQuery.data?.sections?.find((s) => s.sectionId === selectedSection?.id);
  const isEmpiricalBlocked = sectionCap && !sectionCap.canGenerate;
  const reportData = selectedReport as any;
  const finalDoc = finalDocQuery.data as any;
  const valData = validationQuery.data as any;
  const isReferencesSection = selectedSection?.type === 'REFERENCES' || (selectedSection as any)?.sectionType === 'REFERENCES' || String(selectedChapter?.title || '').toLowerCase() === 'references';
  const isLiteratureReviewSection = selectedSection?.type === 'LITERATURE_REVIEW' || (selectedSection as any)?.sectionType === 'LITERATURE_REVIEW' || String(selectedChapter?.title || '').toLowerCase().includes('literature review');

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectQuery.data?.title || projectId, 'Report']} />

      {/* Top Header */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', margin: 0 }}>
              <FileText className="text-primary" size={26} />
              Academic Report Writing & Publication Studio
            </h1>
            {isFinal ? (
              <Badge tone="success" style={{ fontSize: '0.8rem' }}>FINALIZED</Badge>
            ) : (
              <Badge tone="warning" style={{ fontSize: '0.8rem' }}>DRAFT / EDITING</Badge>
            )}
          </div>
          <p className="muted" style={{ maxWidth: '750px', marginTop: '0.25rem' }}>
            Publication-grade Word-like editor with deterministic citations, lazy project migration, live A4 formatting, and validation governance.
          </p>
        </div>

        {/* Global Action Toolbar */}
        <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Citation Style Switcher Dropdown */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', background: 'var(--surface-hover)', padding: '2px 8px', borderRadius: '6px' }}>
            <Bookmark size={14} className="text-primary" />
            <span style={{ fontSize: '0.82rem', fontWeight: 600 }}>Style:</span>
            <Select
              value={currentCitationStyle}
              onChange={(e) => updateCitationStyle.mutate(e.target.value)}
              style={{ fontSize: '0.82rem', padding: '2px 6px', height: '28px' }}
            >
              {CITATION_STYLES.map((st) => (
                <option key={st.id} value={st.id}>{st.label}</option>
              ))}
            </Select>
          </div>

          <Button type="button" variant="secondary" onClick={() => tocMutation.mutate()} disabled={!reportId || tocMutation.isPending}>
            <ListOrdered size={14} style={{ marginRight: 4 }} />
            TOC
          </Button>

          <Button type="button" variant="secondary" onClick={() => validation.mutate()} disabled={!reportId || validation.isPending}>
            <ShieldAlert size={14} style={{ marginRight: 4 }} />
            Validate
          </Button>

          <Button type="button" variant="secondary" onClick={() => assemble.mutate()} disabled={!reportId || assemble.isPending}>
            <RefreshCw size={14} style={{ marginRight: 4 }} />
            Re-sync Template
          </Button>

          <Button
            type="button"
            variant="primary"
            onClick={() => finalize.mutate()}
            disabled={!reportId || finalize.isPending || isFinal}
          >
            <Check size={15} style={{ marginRight: 4 }} />
            {isFinal ? 'Report Finalized' : 'Finalize Report'}
          </Button>
        </div>
      </div>

      {/* Save / Status Toast Alert */}
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

      {/* Tab Navigation Navigation */}
      <div
        className="editor-tabs"
        style={{
          display: 'flex',
          gap: '0.5rem',
          borderBottom: '2px solid var(--border)',
          marginBottom: '1.25rem',
          paddingBottom: '2px',
        }}
      >
        <button
          type="button"
          onClick={() => setActiveTab('sections')}
          style={{
            padding: '8px 16px',
            fontSize: '0.92rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            borderBottom: activeTab === 'sections' ? '3px solid var(--primary)' : '3px solid transparent',
            color: activeTab === 'sections' ? 'var(--primary)' : 'var(--color-muted)',
            marginBottom: '-5px',
          }}
        >
          <Layers size={16} /> Structure & Section Editor
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('final-doc')}
          style={{
            padding: '8px 16px',
            fontSize: '0.92rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            borderBottom: activeTab === 'final-doc' ? '3px solid var(--primary)' : '3px solid transparent',
            color: activeTab === 'final-doc' ? 'var(--primary)' : 'var(--color-muted)',
            marginBottom: '-5px',
          }}
        >
          <Edit3 size={16} /> Final Document Editor
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('preview')}
          style={{
            padding: '8px 16px',
            fontSize: '0.92rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            border: 'none',
            background: 'none',
            cursor: 'pointer',
            borderBottom: activeTab === 'preview' ? '3px solid var(--primary)' : '3px solid transparent',
            color: activeTab === 'preview' ? 'var(--primary)' : 'var(--color-muted)',
            marginBottom: '-5px',
          }}
        >
          <Eye size={16} /> A4 Preview & Exports
        </button>
      </div>

      {/* TAB 1: STRUCTURE & SECTION EDITOR */}
      {activeTab === 'sections' && (
        <div className="report-builder" style={{ display: 'grid', gridTemplateColumns: '320px 1fr 280px', gap: '1.25rem', alignItems: 'flex-start' }}>
          {/* Left Column: Chapters & Sections Tree */}
          <Card style={{ padding: '0.75rem', maxHeight: '740px', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Layers size={16} className="text-primary" />
                <h2 style={{ fontSize: '0.95rem', fontWeight: 600, margin: 0 }}>Report Hierarchy</h2>
              </div>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setNewChapterTitle('');
                  setShowAddChapterModal(true);
                }}
                style={{ fontSize: '0.75rem', padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}
                title="Add custom chapter"
              >
                <Plus size={13} /> Chapter
              </Button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem', overflowY: 'auto', flex: 1, paddingRight: '2px' }}>
              {(structureQuery.data?.chapters ?? []).map((chapter, cIdx, cArr) => {
                const isChapterSelected = chapter.id === selectedChapterId;
                return (
                  <div
                    key={String(chapter.id)}
                    style={{
                      border: isChapterSelected ? '1px solid var(--primary)' : '1px solid var(--border)',
                      borderRadius: '6px',
                      background: isChapterSelected ? 'rgba(var(--primary-rgb, 59, 130, 246), 0.03)' : 'transparent',
                      overflow: 'hidden',
                    }}
                  >
                    {/* Chapter Header */}
                    <div
                      style={{
                        padding: '6px 8px',
                        background: isChapterSelected ? 'var(--surface-hover)' : 'rgba(0,0,0,0.02)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '4px',
                      }}
                    >
                      <button
                        type="button"
                        onClick={() => setSelectedChapterId(String(chapter.id))}
                        style={{
                          flex: 1,
                          textAlign: 'left',
                          fontWeight: isChapterSelected ? 700 : 600,
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          fontSize: '0.84rem',
                          color: 'var(--foreground)',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          padding: 0,
                        }}
                        title={chapter.title}
                      >
                        {chapter.chapterNumber ? `Ch ${chapter.chapterNumber}: ` : ''}{chapter.title}
                      </button>

                      {/* Chapter Actions */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '2px' }}>
                        <button
                          type="button"
                          disabled={cIdx === 0 || reorderStructure.isPending}
                          onClick={() => moveChapter(cIdx, 'up')}
                          style={{ background: 'none', border: 'none', cursor: cIdx === 0 ? 'default' : 'pointer', padding: '1px', opacity: cIdx === 0 ? 0.3 : 0.8 }}
                          title="Move Chapter Up"
                        >
                          <ChevronUp size={13} />
                        </button>
                        <button
                          type="button"
                          disabled={cIdx === cArr.length - 1 || reorderStructure.isPending}
                          onClick={() => moveChapter(cIdx, 'down')}
                          style={{ background: 'none', border: 'none', cursor: cIdx === cArr.length - 1 ? 'default' : 'pointer', padding: '1px', opacity: cIdx === cArr.length - 1 ? 0.3 : 0.8 }}
                          title="Move Chapter Down"
                        >
                          <ChevronDown size={13} />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setRenameTitle(chapter.title);
                            setShowRenameModal({ type: 'chapter', id: String(chapter.id), currentTitle: chapter.title });
                          }}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.8 }}
                          title="Rename Chapter"
                        >
                          <Edit2 size={12} />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setNewSectionTitle('');
                            setShowAddSectionModal({ chapterId: String(chapter.id) });
                          }}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.8, color: 'var(--primary)' }}
                          title="Add Section to this Chapter"
                        >
                          <FilePlus size={13} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteConfirmModal({ type: 'chapter', id: String(chapter.id), title: chapter.title, required: chapter.required })}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: chapter.required ? 0.3 : 0.8, color: chapter.required ? 'var(--color-muted)' : 'var(--color-danger, #ef4444)' }}
                          title={chapter.required ? 'Required template chapter (cannot delete)' : 'Delete Chapter'}
                        >
                          <Trash2 size={12} />
                        </button>
                      </div>
                    </div>

                    {/* Sections inside this Chapter */}
                    {isChapterSelected && (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', padding: '4px 6px' }}>
                        {(chapter.sections ?? []).map((sec, sIdx, sArr) => {
                          const isSecSelected = String(sec.id) === selectedSectionId;
                          return (
                            <div key={String(sec.id)} style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                              {/* Section Row */}
                              <div
                                style={{
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'space-between',
                                  padding: '4px 6px',
                                  borderRadius: '4px',
                                  background: isSecSelected ? 'var(--surface-hover)' : 'transparent',
                                  border: isSecSelected ? '1px solid var(--primary)' : '1px solid transparent',
                                  fontSize: '0.8rem',
                                  gap: '4px',
                                }}
                              >
                                {(() => {
                                  const secHeading = sec.heading || (sec as any).title || 'Untitled Section';
                                  return (
                                    <>
                                      <button
                                        type="button"
                                        onClick={() => handleSelectSection(String(chapter.id), String(sec.id))}
                                        style={{
                                          flex: 1,
                                          textAlign: 'left',
                                          background: 'none',
                                          border: 'none',
                                          cursor: 'pointer',
                                          color: isSecSelected ? 'var(--primary)' : 'inherit',
                                          fontWeight: isSecSelected ? 600 : 400,
                                          overflow: 'hidden',
                                          textOverflow: 'ellipsis',
                                          whiteSpace: 'nowrap',
                                          padding: 0,
                                        }}
                                        title={secHeading}
                                      >
                                        <span style={{ fontWeight: 600, marginRight: '4px' }}>{sec.sectionNumber || ''}</span>
                                        {secHeading}
                                      </button>

                                      {/* Section Action buttons */}
                                      <div style={{ display: 'flex', alignItems: 'center', gap: '2px' }}>
                                        <button
                                          type="button"
                                          disabled={sIdx === 0 || reorderStructure.isPending}
                                          onClick={() => moveSection(String(chapter.id), chapter.sections, sIdx, 'up')}
                                          style={{ background: 'none', border: 'none', cursor: sIdx === 0 ? 'default' : 'pointer', padding: '1px', opacity: sIdx === 0 ? 0.3 : 0.7 }}
                                          title="Move Section Up"
                                        >
                                          <ChevronUp size={12} />
                                        </button>
                                        <button
                                          type="button"
                                          disabled={sIdx === sArr.length - 1 || reorderStructure.isPending}
                                          onClick={() => moveSection(String(chapter.id), chapter.sections, sIdx, 'down')}
                                          style={{ background: 'none', border: 'none', cursor: sIdx === sArr.length - 1 ? 'default' : 'pointer', padding: '1px', opacity: sIdx === sArr.length - 1 ? 0.3 : 0.7 }}
                                          title="Move Section Down"
                                        >
                                          <ChevronDown size={12} />
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => {
                                            setRenameTitle(secHeading);
                                            setShowRenameModal({ type: 'section', id: String(sec.id), currentTitle: secHeading });
                                          }}
                                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7 }}
                                          title="Rename Section"
                                        >
                                          <Edit2 size={11} />
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => {
                                            setNewSectionTitle('');
                                            setShowAddSectionModal({ chapterId: String(chapter.id), parentSectionId: String(sec.id), parentTitle: secHeading });
                                          }}
                                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7, color: 'var(--primary)' }}
                                          title="Add Subsection"
                                        >
                                          <CornerDownRight size={12} />
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => setDeleteConfirmModal({ type: 'section', id: String(sec.id), title: secHeading, required: sec.required })}
                                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: sec.required ? 0.3 : 0.7, color: sec.required ? 'var(--color-muted)' : 'var(--color-danger, #ef4444)' }}
                                          title={sec.required ? 'Required template section (cannot delete)' : 'Delete Section'}
                                        >
                                          <Trash2 size={11} />
                                        </button>
                                      </div>
                                    </>
                                  );
                                })()}
                              </div>

                              {/* Nested Subsections (Indented) */}
                              {(sec.subsections ?? []).length > 0 && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', paddingLeft: '14px', borderLeft: '1px solid var(--border)' }}>
                                  {sec.subsections.map((sub, subIdx, subArr) => {
                                    const isSubSelected = String(sub.id) === selectedSectionId;
                                    const subHeading = sub.heading || (sub as any).title || 'Untitled Subsection';
                                    return (
                                      <div
                                        key={String(sub.id)}
                                        style={{
                                          display: 'flex',
                                          alignItems: 'center',
                                          justifyContent: 'space-between',
                                          padding: '3px 6px',
                                          borderRadius: '3px',
                                          background: isSubSelected ? 'var(--surface-hover)' : 'transparent',
                                          border: isSubSelected ? '1px solid var(--primary)' : '1px solid transparent',
                                          fontSize: '0.78rem',
                                          gap: '4px',
                                        }}
                                      >
                                        <button
                                          type="button"
                                          onClick={() => handleSelectSection(String(chapter.id), String(sub.id))}
                                          style={{
                                            flex: 1,
                                            textAlign: 'left',
                                            background: 'none',
                                            border: 'none',
                                            cursor: 'pointer',
                                            color: isSubSelected ? 'var(--primary)' : 'inherit',
                                            fontWeight: isSubSelected ? 600 : 400,
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                            padding: 0,
                                          }}
                                          title={subHeading}
                                        >
                                          <span style={{ fontWeight: 600, marginRight: '4px' }}>{sub.sectionNumber || ''}</span>
                                          {subHeading}
                                        </button>

                                        {/* Subsection Actions */}
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '2px' }}>
                                          <button
                                            type="button"
                                            disabled={subIdx === 0 || reorderStructure.isPending}
                                            onClick={() => moveSection(String(chapter.id), sec.subsections, subIdx, 'up', String(sec.id))}
                                            style={{ background: 'none', border: 'none', cursor: subIdx === 0 ? 'default' : 'pointer', padding: '1px', opacity: subIdx === 0 ? 0.3 : 0.7 }}
                                            title="Move Subsection Up"
                                          >
                                            <ChevronUp size={11} />
                                          </button>
                                          <button
                                            type="button"
                                            disabled={subIdx === subArr.length - 1 || reorderStructure.isPending}
                                            onClick={() => moveSection(String(chapter.id), sec.subsections, subIdx, 'down', String(sec.id))}
                                            style={{ background: 'none', border: 'none', cursor: subIdx === subArr.length - 1 ? 'default' : 'pointer', padding: '1px', opacity: subIdx === subArr.length - 1 ? 0.3 : 0.7 }}
                                            title="Move Subsection Down"
                                          >
                                            <ChevronDown size={11} />
                                          </button>
                                          <button
                                            type="button"
                                            onClick={() => {
                                              setRenameTitle(subHeading);
                                              setShowRenameModal({ type: 'section', id: String(sub.id), currentTitle: subHeading });
                                            }}
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7 }}
                                            title="Rename Subsection"
                                          >
                                            <Edit2 size={10} />
                                          </button>
                                          <button
                                            type="button"
                                            onClick={() => setDeleteConfirmModal({ type: 'section', id: String(sub.id), title: subHeading, required: sub.required })}
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: sub.required ? 0.3 : 0.7, color: sub.required ? 'var(--color-muted)' : 'var(--color-danger, #ef4444)' }}
                                            title={sub.required ? 'Required template section' : 'Delete Subsection'}
                                          >
                                            <Trash2 size={10} />
                                          </button>
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </Card>

          {/* Center Column: Rich Section Editor */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {selectedSection ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
                      {selectedSection.sectionNumber ? `${selectedSection.sectionNumber} ` : ''}
                      {displayValue(selectedSection.heading ?? selectedSection.type)}
                    </h2>
                    <span className="muted" style={{ fontSize: '0.8rem' }}>
                      Chapter: {displayValue(selectedChapter?.title)} • Revision {displayValue(selectedSection.revisionNumber)}
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    {isReferencesSection ? (
                      <Button
                        type="button"
                        variant="primary"
                        onClick={() => refreshReferences.mutate()}
                        disabled={refreshReferences.isPending}
                        style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                        title="Recompile references deterministically from citations and project references"
                      >
                        <RotateCw size={14} className={refreshReferences.isPending ? 'animate-spin' : ''} />
                        {refreshReferences.isPending ? 'Compiling References...' : 'Refresh References'}
                      </Button>
                    ) : (
                      <>
                        {isLiteratureReviewSection && (
                          <Button
                            type="button"
                            variant="secondary"
                            onClick={() => setShowLitMatrixModal(true)}
                            style={{ fontSize: '0.82rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 5 }}
                          >
                            <Table size={14} className="text-primary" />
                            Evidence Matrix
                          </Button>
                        )}
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => generateSection.mutate()}
                          disabled={generateSection.isPending || Boolean(isEmpiricalBlocked)}
                          style={{ fontSize: '0.82rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 4 }}
                        >
                          <Sparkles size={14} className="text-primary" />
                          {generateSection.isPending ? 'Generating...' : 'AI Draft'}
                        </Button>
                      </>
                    )}
                  </div>
                </div>

                {/* References Specific Banner */}
                {isReferencesSection && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10, padding: '8px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <BookOpen size={16} className="text-primary" style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Deterministic Academic Bibliography:</strong> Compiled directly from citations embedded in your report and project references formatted in {currentCitationStyle}. (0 AI credits consumed)
                      </div>
                    </div>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                      <input
                        type="checkbox"
                        checked={Boolean(structureQuery.data?.includeUncitedReferences)}
                        onChange={(e) => {
                          updateReportSettings.mutate({ includeUncitedReferences: e.target.checked });
                          refreshReferences.mutate();
                        }}
                      />
                      Include uncited project references
                    </label>
                  </div>
                )}

                {/* Empirical Safety Alert */}
                {isEmpiricalBlocked && !isReferencesSection && (
                  <div className="alert warning" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle size={16} />
                    <div>
                      <strong>Empirical Data Safety:</strong> {sectionCap?.reason || 'This section requires uploaded dataset or analysis findings before AI generation.'}
                    </div>
                  </div>
                )}

                {/* Rich Editor */}
                <ReportRichEditor
                  key={String(selectedSection.id)}
                  content={String(selectedSection.content || '')}
                  contentJson={String(selectedSection.contentJson || '')}
                  projectId={projectId}
                  citationStyle={currentCitationStyle}
                  minHeight="540px"
                  onSave={({ contentJson, plainText, markdown }) => {
                    saveSection.mutate({
                      content: markdown,
                      contentJson,
                      plainText,
                    });
                  }}
                />

                {generateSection.data && <DraftPreview draft={generateSection.data} />}
              </div>
            ) : (
              <Card style={{ padding: '2rem', textAlign: 'center' }}>
                <EmptyState title="Select a section" description="Choose a chapter and section on the left to start editing with the rich-text editor." />
              </Card>
            )}
          </div>

          {/* Right Column: Evidence, Citations, & Section Meta */}
          <Card style={{ padding: '1rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.5rem' }}>Section Properties</h2>
            {selectedSection ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', fontSize: '0.84rem' }}>
                {(selectedSection as any)?.sectionNumber && (
                  <div>
                    <span className="muted">Section No:</span> <strong>{String((selectedSection as any).sectionNumber)}</strong>
                  </div>
                )}
                <div>
                  <span className="muted">Status:</span> <strong>{displayValue(selectedSection.status)}</strong>
                </div>
                <div>
                  <span className="muted">Origin:</span> <Badge>{displayValue(selectedSection.origin, 'USER')}</Badge>
                </div>
                <div>
                  <span className="muted">Word Count:</span> {Number((selectedSection as any)?.wordCount || 0)}
                </div>

                {isLiteratureReviewSection && (
                  <div style={{ padding: '8px 10px', background: 'var(--surface-hover)', borderRadius: '6px', marginTop: '0.25rem', border: '1px solid var(--border)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <strong style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Table size={13} className="text-primary" /> Evidence Matrix
                      </strong>
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => setShowLitMatrixModal(true)}
                        style={{ fontSize: '0.72rem', padding: '2px 6px' }}
                      >
                        View
                      </Button>
                    </div>
                    <label style={{ fontSize: '0.76rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                      Report Placement:
                    </label>
                    <Select
                      value={structureQuery.data?.literatureMatrixInclusion || 'EXCLUDED'}
                      onChange={(e) => updateReportSettings.mutate({ literatureMatrixInclusion: e.target.value as LiteratureMatrixInclusion })}
                      style={{ fontSize: '0.76rem', width: '100%', padding: '2px 4px' }}
                    >
                      <option value="EXCLUDED">Analyze Mode Only (Clean Prose)</option>
                      <option value="CHAPTER_TWO">Append to Chapter 2</option>
                      <option value="APPENDIX">Include in Appendix</option>
                    </Select>
                  </div>
                )}

                {isReferencesSection && (
                  <div style={{ padding: '8px 10px', background: 'var(--surface-hover)', borderRadius: '6px', marginTop: '0.25rem', border: '1px solid var(--border)' }}>
                    <strong style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 4, marginBottom: '6px' }}>
                      <Bookmark size={13} className="text-primary" /> Bibliography Settings
                    </strong>
                    <div style={{ fontSize: '0.78rem', color: 'var(--color-muted)', marginBottom: '6px' }}>
                      Active Style: <strong>{currentCitationStyle}</strong>
                    </div>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => refreshReferences.mutate()}
                      disabled={refreshReferences.isPending}
                      style={{ fontSize: '0.75rem', width: '100%', padding: '3px' }}
                    >
                      <RotateCw size={12} style={{ marginRight: 4 }} /> Re-compile Bibliography
                    </Button>
                  </div>
                )}

                <hr style={{ borderColor: 'var(--border)', margin: '0.5rem 0' }} />
                <h3 style={{ fontSize: '0.9rem', fontWeight: 600 }}>Evidence & Citations</h3>
                <p className="muted" style={{ fontSize: '0.78rem' }}>
                  Citations inserted using the [ Insert Citation ] tool link directly to project references and update automatically.
                </p>
                <div style={{ marginTop: '0.5rem' }}>
                  <RecordRows rows={aiUsage.data ? [aiUsage.data] : []} />
                </div>
              </div>
            ) : (
              <p className="muted" style={{ fontSize: '0.85rem' }}>No section selected.</p>
            )}
          </Card>
        </div>
      )}

      {/* TAB 2: FINAL DOCUMENT EDITOR */}
      {activeTab === 'final-doc' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {finalDocQuery.isLoading ? (
            <Card style={{ padding: '2rem', textAlign: 'center' }}>Loading assembled final document...</Card>
          ) : !finalDoc ? (
            <Card style={{ padding: '2.5rem', textAlign: 'center' }}>
              <FileText size={40} className="text-primary" style={{ margin: '0 auto 1rem' }} />
              <h2 style={{ fontSize: '1.2rem', fontWeight: 600 }}>No Final Document Snapshot Prepared Yet</h2>
              <p className="muted" style={{ maxWidth: '540px', margin: '0.5rem auto 1.5rem' }}>
                Prepare a snapshot of the full document version compiling all chapters, sections, and references into a single cohesive document for full editing.
              </p>
              <Button
                type="button"
                variant="primary"
                onClick={() => prepareFinalDoc.mutate()}
                disabled={prepareFinalDoc.isPending}
              >
                <RefreshCw size={15} style={{ marginRight: 6 }} />
                {prepareFinalDoc.isPending ? 'Preparing Snapshot...' : 'Prepare Final Document Snapshot'}
              </Button>
            </Card>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {/* Stale Warning Header */}
              {Boolean(finalDoc.stale) && (
                <div
                  className="alert warning"
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '0.75rem 1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle size={18} />
                    <span>
                      <strong>Document Stale:</strong> Individual report sections have been updated since this snapshot was created.
                    </span>
                  </div>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => prepareFinalDoc.mutate()}
                    disabled={prepareFinalDoc.isPending}
                    style={{ fontSize: '0.82rem', padding: '3px 8px' }}
                  >
                    <RotateCw size={13} style={{ marginRight: 4 }} />
                    {prepareFinalDoc.isPending ? 'Updating...' : 'Update Final Snapshot'}
                  </Button>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
                    {String(finalDoc.title || 'Complete Report Document Version')}
                  </h2>
                  <span className="muted" style={{ fontSize: '0.82rem' }}>
                    Version {String(finalDoc.versionNumber || 1)} • Status: {String(finalDoc.status || '')} • Style: {String(finalDoc.citationStyle || '')}
                  </span>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => prepareFinalDoc.mutate()}
                  disabled={prepareFinalDoc.isPending}
                >
                  <RefreshCw size={14} style={{ marginRight: 4 }} /> Re-compile From Sections
                </Button>
              </div>

              {/* Full Document Rich Editor */}
              <ReportRichEditor
                key={`final-doc-${finalDoc.id}-${finalDoc.updatedAt}`}
                content={String(finalDoc.plainText || '')}
                contentJson={String(finalDoc.contentJson || '')}
                projectId={projectId}
                citationStyle={currentCitationStyle}
                minHeight="750px"
                onSave={({ contentJson, plainText }) => {
                  updateFinalDoc.mutate({
                    contentJson,
                    plainText,
                    title: String(finalDoc.title || ''),
                  });
                }}
              />
            </div>
          )}
        </div>
      )}

      {/* TAB 3: A4 PREVIEW & EXPORTS */}
      {activeTab === 'preview' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.5rem', alignItems: 'flex-start' }}>
          {/* Main A4 Document Preview */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <Card style={{ padding: '0', overflow: 'hidden' }}>
              <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0 }}>A4 Paginated Report Preview</h2>
                  <span className="muted" style={{ fontSize: '0.8rem' }}>Formatted with institutional margins, cover page, and auto-generated references.</span>
                </div>
                <Button type="button" variant="secondary" onClick={() => setActiveTab('final-doc')}>
                  <Edit3 size={14} style={{ marginRight: 4 }} /> Edit Document
                </Button>
              </div>

              {/* A4 Paper Mockup Preview */}
              <div
                style={{
                  background: 'var(--surface-bg, #f1f5f9)',
                  padding: '2rem',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '2rem',
                  maxHeight: '800px',
                  overflowY: 'auto',
                }}
              >
                {/* Title Page */}
                <div
                  className="preview-page"
                  style={{
                    width: '100%',
                    maxWidth: '750px',
                    minHeight: '600px',
                    background: '#fff',
                    color: '#1e293b',
                    padding: '4rem 3.5rem',
                    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    textAlign: 'center',
                    fontFamily: '"Times New Roman", Times, serif',
                  }}
                >
                  <h1 style={{ fontSize: '1.8rem', fontWeight: 'bold', textTransform: 'uppercase', marginBottom: '2rem', maxWidth: '600px' }}>
                    {String(reportData?.title || 'Academic Research Report')}
                  </h1>
                  <p style={{ fontSize: '1.1rem', margin: '0.5rem 0' }}>By</p>
                  <p style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>{String(reportData?.authorName || projectQuery.data?.title || 'Researcher')}</p>
                  <div style={{ marginTop: '4rem' }}>
                    <p style={{ margin: '0.25rem 0' }}>{String(reportData?.institutionName || 'Department of Computer Science')}</p>
                    <p style={{ margin: '0.25rem 0' }}>{String(reportData?.departmentName || 'Faculty of Computing & Informatics')}</p>
                    <p style={{ margin: '0.25rem 0', fontWeight: 'bold' }}>{String(reportData?.degreeProgram || 'Undergraduate Degree Project')}</p>
                    <p style={{ margin: '1rem 0 0' }}>{String(reportData?.submissionYear || new Date().getFullYear())}</p>
                  </div>
                </div>

                {/* Table of Contents Mock */}
                <div
                  className="preview-page"
                  style={{
                    width: '100%',
                    maxWidth: '750px',
                    minHeight: '400px',
                    background: '#fff',
                    color: '#1e293b',
                    padding: '3rem',
                    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
                    fontFamily: '"Times New Roman", Times, serif',
                  }}
                >
                  <h2 style={{ textAlign: 'center', fontWeight: 'bold', marginBottom: '1.5rem', textTransform: 'uppercase' }}>Table of Contents</h2>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {((chapters.data ?? []) as any[]).map((ch, idx) => (
                      <div key={String(ch.id)} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px dotted #94a3b8', paddingBottom: '3px' }}>
                        <span>{ch.chapterNumber ? `Chapter ${ch.chapterNumber}: ` : ''}{String(ch.title || '')}</span>
                        <span>{idx * 4 + 1}</span>
                      </div>
                    ))}
                    <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px dotted #94a3b8', paddingBottom: '3px' }}>
                      <span>References</span>
                      <span>{((chapters.data ?? []).length) * 4 + 1}</span>
                    </div>
                  </div>
                </div>

                {/* Body Content Preview */}
                <div
                  className="preview-page"
                  style={{
                    width: '100%',
                    maxWidth: '750px',
                    minHeight: '600px',
                    background: '#fff',
                    color: '#1e293b',
                    padding: '3rem',
                    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
                    fontFamily: '"Times New Roman", Times, serif',
                    lineHeight: 1.7,
                  }}
                >
                  {((chapters.data ?? []) as any[]).map((ch) => (
                    <div key={String(ch.id)} style={{ marginBottom: '2rem' }}>
                      <h2 style={{ fontSize: '1.3rem', fontWeight: 'bold', marginBottom: '1rem' }}>
                        {String(ch.title || '')}
                      </h2>
                      <p className="muted" style={{ fontStyle: 'italic', fontSize: '0.9rem' }}>
                        [Chapter content compiled with verified scholarly citations in {currentCitationStyle} style]
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </Card>
          </div>

          {/* Right Column: Validation Status & Downloads */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Draft Export Card (ALWAYS Available!) */}
            <Card style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '0.5rem' }}>
                <FileDown size={18} className="text-primary" />
                <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Draft Export (Non-Blocking)</h2>
              </div>
              <p className="muted" style={{ fontSize: '0.82rem', marginBottom: '1rem' }}>
                Download working drafts at any time for supervisor review, editing, or reference checking. Bypasses finalization validation.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => draftDocxExport.mutate()}
                  disabled={!reportId || draftDocxExport.isPending}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> Download Draft DOCX
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => draftPdfExport.mutate()}
                  disabled={!reportId || draftPdfExport.isPending}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> Download Draft PDF
                </Button>
              </div>
              <div style={{ marginTop: '0.75rem' }}>
                <ExportJob data={draftDocxExport.data} />
                <ExportJob data={draftPdfExport.data} />
              </div>
            </Card>

            {/* Validation & Final Publication Export Card */}
            <Card style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '0.5rem' }}>
                <ShieldAlert size={18} className="text-primary" />
                <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Final Publication Export</h2>
              </div>
              <p className="muted" style={{ fontSize: '0.82rem', marginBottom: '1rem' }}>
                Final export enforces full academic validation: no missing sections, no incomplete metadata, and clean citations.
              </p>

              {/* Validation Summary */}
              {valData && (
                <div style={{ marginBottom: '1rem', padding: '0.75rem', background: 'var(--surface-hover)', borderRadius: '6px', fontSize: '0.82rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <span>Errors:</span>
                    <strong style={{ color: valData.errors?.length ? '#ef4444' : '#10b981' }}>
                      {valData.errors?.length || 0}
                    </strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                    <span>Warnings:</span>
                    <strong>{valData.warnings?.length || 0}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Ready for Publication:</span>
                    <strong>{isFinal ? 'YES' : 'PENDING FINALIZATION'}</strong>
                  </div>
                </div>
              )}

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <Button
                  type="button"
                  variant="primary"
                  onClick={() => finalDocxExport.mutate()}
                  disabled={!reportId || finalDocxExport.isPending}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> Final Publication DOCX
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => finalPdfExport.mutate()}
                  disabled={!reportId || finalPdfExport.isPending}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> Final Publication PDF
                </Button>
              </div>
              <div style={{ marginTop: '0.75rem' }}>
                <ExportJob data={finalDocxExport.data} />
                <ExportJob data={finalPdfExport.data} />
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* Validation Issue Modal (When finalize or final export fails validation) */}
      <Modal
        title="Report Finalization Issues"
        open={validationModalOpen}
        onClose={() => setValidationModalOpen(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: '560px' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }} className="alert warning">
            <ShieldAlert size={20} style={{ flexShrink: 0, marginTop: 2 }} />
            <div>
              <strong>Action Required Before Finalization:</strong>
              <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>
                The report cannot be marked final while validation errors exist. However, you can freely continue editing, saving drafts, or review references.
              </p>
            </div>
          </div>

          <div style={{ maxHeight: '280px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {validationErrors.map((err: any, idx: number) => (
              <div
                key={idx}
                style={{
                  padding: '8px 12px',
                  borderRadius: '6px',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  background: 'rgba(239, 68, 68, 0.05)',
                  fontSize: '0.85rem',
                }}
              >
                <div style={{ fontWeight: 600, color: '#ef4444' }}>{err.code || 'VALIDATION_ERROR'}</div>
                <div>{err.message}</div>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setValidationModalOpen(false);
                draftDocxExport.mutate();
              }}
            >
              Download Draft Anyway
            </Button>
            <Button
              type="button"
              variant="primary"
              onClick={() => {
                setValidationModalOpen(false);
                setActiveTab('sections');
              }}
            >
              Continue Editing
            </Button>
          </div>
        </div>
      </Modal>

      {/* Academic Table of Contents Modal */}
      <Modal title="Academic Table of Contents" open={showTocModal} onClose={() => setShowTocModal(false)}>
        {tocMutation.data ? (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.6rem' }}>
              <div>
                <span style={{ fontWeight: 700, fontSize: '1rem' }}>
                  {tocMutation.data.reportTitle || tocMutation.data.title || 'Table of Contents'}
                </span>
                <span className="muted" style={{ display: 'block', fontSize: '0.78rem' }}>
                  Deterministic academic structure compiled from chapter & section headings
                </span>
              </div>
              <Badge tone="success">0 AI Credits (Deterministic)</Badge>
            </div>

            {/* View Mode Switcher */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
              <button
                type="button"
                className={`btn-compact ${tocViewMode === 'dotleaders' ? 'active' : ''}`}
                onClick={() => setTocViewMode('dotleaders')}
                style={{
                  padding: '4px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: tocViewMode === 'dotleaders' ? 600 : 400,
                  background: tocViewMode === 'dotleaders' ? 'var(--primary)' : 'transparent',
                  color: tocViewMode === 'dotleaders' ? '#fff' : 'inherit',
                  border: '1px solid var(--border)',
                  cursor: 'pointer'
                }}
              >
                Dot Leaders View
              </button>
              <button
                type="button"
                className={`btn-compact ${tocViewMode === 'outline' ? 'active' : ''}`}
                onClick={() => setTocViewMode('outline')}
                style={{
                  padding: '4px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: tocViewMode === 'outline' ? 600 : 400,
                  background: tocViewMode === 'outline' ? 'var(--primary)' : 'transparent',
                  color: tocViewMode === 'outline' ? '#fff' : 'inherit',
                  border: '1px solid var(--border)',
                  cursor: 'pointer'
                }}
              >
                Hierarchical Outline
              </button>
              <button
                type="button"
                className={`btn-compact ${tocViewMode === 'markdown' ? 'active' : ''}`}
                onClick={() => setTocViewMode('markdown')}
                style={{
                  padding: '4px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: tocViewMode === 'markdown' ? 600 : 400,
                  background: tocViewMode === 'markdown' ? 'var(--primary)' : 'transparent',
                  color: tocViewMode === 'markdown' ? '#fff' : 'inherit',
                  border: '1px solid var(--border)',
                  cursor: 'pointer'
                }}
              >
                Markdown Source
              </button>
            </div>

            {/* DOT LEADERS VIEW */}
            {tocViewMode === 'dotleaders' && (
              <div style={{
                fontFamily: '"Courier New", Courier, monospace',
                fontSize: '0.85rem',
                lineHeight: '1.7',
                maxHeight: '400px',
                overflowY: 'auto',
                padding: '1rem',
                background: 'var(--surface-hover)',
                borderRadius: '6px',
                border: '1px solid var(--border)'
              }}>
                {(tocMutation.data.chapters || tocMutation.data.items || []).map((ch: any, cIdx: number) => {
                  const chNumber = ch.chapterNumber ?? (cIdx + 1);
                  const chTitle = ch.title || `Chapter ${chNumber}`;
                  return (
                    <div key={ch.chapterId || cIdx} style={{ marginBottom: '0.75rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700 }}>
                        <span style={{ textTransform: 'uppercase' }}>
                          CHAPTER {chNumber}: {chTitle}
                        </span>
                        <span style={{ flexGrow: 1, borderBottom: '1px dotted var(--text-muted, #888)', margin: '0 8px 4px' }} />
                        <span>Page {cIdx * 5 + 1}</span>
                      </div>
                      {(ch.sections || []).map((sec: any, sIdx: number) => {
                        const secHeading = sec.heading || sec.title || `Section ${sIdx + 1}`;
                        const secNum = sec.sectionNumber || `${chNumber}.${sIdx + 1}`;
                        return (
                          <div key={sec.sectionId || sIdx} style={{ display: 'flex', justifyContent: 'space-between', paddingLeft: '1.5rem', color: 'var(--text-muted)' }}>
                            <span>{secNum} {secHeading}</span>
                            <span style={{ flexGrow: 1, borderBottom: '1px dotted var(--border)', margin: '0 8px 4px' }} />
                            <span>Page {cIdx * 5 + sIdx + 1}</span>
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            )}

            {/* HIERARCHICAL OUTLINE VIEW */}
            {tocViewMode === 'outline' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '400px', overflowY: 'auto', padding: '0.5rem' }}>
                {(tocMutation.data.chapters || tocMutation.data.items || []).map((ch: any, cIdx: number) => (
                  <div key={ch.chapterId || cIdx} style={{ border: '1px solid var(--border)', borderRadius: '6px', padding: '0.75rem', background: 'var(--surface)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontWeight: 700 }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Badge tone="info">Ch {ch.chapterNumber ?? (cIdx + 1)}</Badge>
                        {ch.title}
                      </span>
                      <span className="muted" style={{ fontSize: '0.78rem' }}>{(ch.sections || []).length} Sections</span>
                    </div>
                    {(ch.sections || []).length > 0 && (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '0.5rem', paddingLeft: '1rem', borderLeft: '2px solid var(--border)' }}>
                        {ch.sections.map((sec: any, sIdx: number) => (
                          <div key={sec.sectionId || sIdx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem', padding: '2px 0' }}>
                            <span>
                              <span style={{ fontWeight: 600, marginRight: '6px', color: 'var(--primary)' }}>
                                {sec.sectionNumber || `${ch.chapterNumber ?? (cIdx + 1)}.${sIdx + 1}`}
                              </span>
                              {sec.heading || sec.title}
                            </span>
                            {sec.type && <Badge style={{ fontSize: '0.7rem' }}>{sec.type}</Badge>}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* MARKDOWN SOURCE VIEW */}
            {tocViewMode === 'markdown' && (
              <pre style={{
                fontFamily: 'monospace',
                fontSize: '0.82rem',
                maxHeight: '400px',
                overflowY: 'auto',
                padding: '1rem',
                background: 'var(--surface-hover)',
                borderRadius: '6px',
                whiteSpace: 'pre-wrap'
              }}>
                {tocMutation.data.formattedMarkdown || 'No markdown generated'}
              </pre>
            )}

            {/* Modal Actions Footer */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border)' }}>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  const text = tocMutation.data.formattedMarkdown || 'Table of Contents';
                  navigator.clipboard.writeText(text);
                  setTocCopied(true);
                  setTimeout(() => setTocCopied(false), 2500);
                }}
              >
                <Copy size={13} style={{ marginRight: 4 }} />
                {tocCopied ? 'TOC Copied!' : 'Copy Formatted Markdown'}
              </Button>
              <Button type="button" variant="primary" onClick={() => setShowTocModal(false)}>
                Close
              </Button>
            </div>
          </div>
        ) : (
          <p>Loading table of contents...</p>
        )}
      </Modal>

      {/* Add Chapter Modal */}
      <Modal
        title="Add Report Chapter"
        open={showAddChapterModal}
        onClose={() => setShowAddChapterModal(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '380px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>Chapter Title</label>
            <Input
              value={newChapterTitle}
              onChange={(e) => setNewChapterTitle(e.target.value)}
              placeholder="e.g. Chapter 6: Future Directions & Recommendations"
              autoFocus
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowAddChapterModal(false)}>Cancel</Button>
            <Button
              type="button"
              variant="primary"
              disabled={!newChapterTitle.trim() || createChapter.isPending}
              onClick={() => createChapter.mutate(newChapterTitle.trim())}
            >
              {createChapter.isPending ? 'Creating...' : 'Create Chapter'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Add Section / Subsection Modal */}
      <Modal
        title={showAddSectionModal?.parentSectionId ? `Add Subsection to "${showAddSectionModal.parentTitle || 'Section'}"` : 'Add Section to Chapter'}
        open={Boolean(showAddSectionModal)}
        onClose={() => setShowAddSectionModal(null)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '380px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>
              {showAddSectionModal?.parentSectionId ? 'Subsection Heading' : 'Section Heading'}
            </label>
            <Input
              value={newSectionTitle}
              onChange={(e) => setNewSectionTitle(e.target.value)}
              placeholder={showAddSectionModal?.parentSectionId ? 'e.g. System Architecture Overview' : 'e.g. Theoretical Framework'}
              autoFocus
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowAddSectionModal(null)}>Cancel</Button>
            <Button
              type="button"
              variant="primary"
              disabled={!newSectionTitle.trim() || createSection.isPending}
              onClick={() => {
                if (showAddSectionModal) {
                  createSection.mutate({
                    chapterId: showAddSectionModal.chapterId,
                    title: newSectionTitle.trim(),
                    parentSectionId: showAddSectionModal.parentSectionId,
                  });
                }
              }}
            >
              {createSection.isPending ? 'Adding...' : 'Add Section'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Rename Modal */}
      <Modal
        title={`Rename ${showRenameModal?.type === 'chapter' ? 'Chapter' : 'Section'}`}
        open={Boolean(showRenameModal)}
        onClose={() => setShowRenameModal(null)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '380px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>Title / Heading</label>
            <Input
              value={renameTitle}
              onChange={(e) => setRenameTitle(e.target.value)}
              autoFocus
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowRenameModal(null)}>Cancel</Button>
            <Button
              type="button"
              variant="primary"
              disabled={!renameTitle.trim() || updateChapter.isPending || updateSectionTitle.isPending}
              onClick={() => {
                if (showRenameModal?.type === 'chapter') {
                  updateChapter.mutate({ id: showRenameModal.id, title: renameTitle.trim() });
                } else if (showRenameModal?.type === 'section') {
                  updateSectionTitle.mutate({ id: showRenameModal.id, heading: renameTitle.trim() });
                }
              }}
            >
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        title={`Delete ${deleteConfirmModal?.type === 'chapter' ? 'Chapter' : 'Section'}`}
        open={Boolean(deleteConfirmModal)}
        onClose={() => setDeleteConfirmModal(null)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '380px' }}>
          {deleteConfirmModal?.required ? (
            <div className="alert warning" style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
              <AlertTriangle size={18} style={{ flexShrink: 0, marginTop: 2 }} />
              <div>
                <strong>Required Template {deleteConfirmModal.type === 'chapter' ? 'Chapter' : 'Section'}:</strong>
                <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>
                  This {deleteConfirmModal.type} is required by institutional academic guidelines and cannot be deleted. You can edit its content freely.
                </p>
              </div>
            </div>
          ) : (
            <div>
              <p style={{ margin: 0, fontSize: '0.9rem' }}>
                Are you sure you want to delete <strong>{deleteConfirmModal?.title}</strong>?
                {deleteConfirmModal?.type === 'chapter' ? ' All sections and subsections within this chapter will also be deleted.' : ' Any nested subsections will also be deleted.'}
              </p>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setDeleteConfirmModal(null)}>
              {deleteConfirmModal?.required ? 'Close' : 'Cancel'}
            </Button>
            {!deleteConfirmModal?.required && (
              <Button
                type="button"
                variant="danger"
                disabled={deleteChapter.isPending || deleteSection.isPending}
                onClick={() => {
                  if (deleteConfirmModal?.type === 'chapter') {
                    deleteChapter.mutate(deleteConfirmModal.id);
                  } else if (deleteConfirmModal?.type === 'section') {
                    deleteSection.mutate(deleteConfirmModal.id);
                  }
                }}
              >
                {deleteChapter.isPending || deleteSection.isPending ? 'Deleting...' : 'Delete'}
              </Button>
            )}
          </div>
        </div>
      </Modal>

      {/* Literature Evidence Matrix Modal */}
      <Modal
        title="Source-Level Literature Evidence Matrix"
        open={showLitMatrixModal}
        onClose={() => setShowLitMatrixModal(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: '850px', minWidth: '600px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div>
              <p className="muted" style={{ fontSize: '0.85rem', margin: 0 }}>
                Structured comparative assessment across indexed project sources. Kept separate from literature narrative.
              </p>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.82rem', fontWeight: 600 }}>Inclusion:</span>
              <Select
                value={structureQuery.data?.literatureMatrixInclusion || 'EXCLUDED'}
                onChange={(e) => updateReportSettings.mutate({ literatureMatrixInclusion: e.target.value as LiteratureMatrixInclusion })}
                style={{ fontSize: '0.82rem' }}
              >
                <option value="EXCLUDED">Analyze Mode Only (Clean Chapter 2)</option>
                <option value="CHAPTER_TWO">Append to Chapter 2</option>
                <option value="APPENDIX">Include in Appendix</option>
              </Select>
            </div>
          </div>

          {litMatrixQuery.isLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }}>Loading literature matrix...</div>
          ) : litMatrixQuery.data?.markdownTable ? (
            <div style={{ maxHeight: '500px', overflow: 'auto', border: '1px solid var(--border)', borderRadius: '6px', padding: '1rem', background: 'var(--surface-hover)' }}>
              <pre style={{ margin: 0, fontSize: '0.82rem', whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                {litMatrixQuery.data.markdownTable}
              </pre>
            </div>
          ) : (
            <div style={{ padding: '2rem', textAlign: 'center' }}>
              <Table size={32} className="muted" style={{ margin: '0 auto 0.5rem' }} />
              <p className="muted">No literature matrix generated yet for this project.</p>
              <p className="muted" style={{ fontSize: '0.82rem' }}>
                Run Literature Matrix analysis in Analyze Mode or generate Chapter 2 to extract source comparisons.
              </p>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
            {litMatrixQuery.data?.markdownTable && (
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  navigator.clipboard.writeText(litMatrixQuery.data?.markdownTable || '');
                  setSaveStatus('Matrix markdown copied to clipboard.');
                  setTimeout(() => setSaveStatus(null), 2500);
                }}
              >
                <Copy size={14} style={{ marginRight: 4 }} /> Copy Markdown
              </Button>
            )}
            <Button type="button" variant="primary" onClick={() => setShowLitMatrixModal(false)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>
    </section>
  );
}


/**
 * Formats bibliographic metadata into strict academic citation style entries.
 * Supports APA 7th, IEEE, Numeric APA, Harvard, Chicago Author-Date, Vancouver, and MLA 9th.
 */
export function formatScholarlyReference(
  ref: {
    authors?: any;
    title?: string;
    year?: number | string;
    publicationYear?: number | string;
    containerTitle?: string;
    journal?: string;
    volume?: string | number;
    issue?: string | number;
    pages?: string;
    publisher?: string;
    doi?: string;
    url?: string;
  },
  style: string = 'APA_7',
  index: number = 1
): string {
  const yearVal = ref.year || ref.publicationYear || 'n.d.';
  const titleVal = (ref.title || 'Untitled Reference').trim();
  const containerVal = (ref.containerTitle || ref.journal || ref.publisher || '').trim();
  const volumeVal = ref.volume ? String(ref.volume).trim() : '';
  const issueVal = ref.issue ? String(ref.issue).trim() : '';
  const pagesVal = ref.pages ? String(ref.pages).trim() : '';
  const doiVal = ref.doi ? ref.doi.trim() : '';
  const doiUrl = doiVal ? (doiVal.startsWith('http') ? doiVal : `https://doi.org/${doiVal.replace(/^doi:\s*/i, '')}`) : '';

  // Extract author names
  let authorList: string[] = [];
  if (Array.isArray(ref.authors)) {
    authorList = ref.authors.map((a: any) => {
      if (typeof a === 'string') return a.trim();
      if (a.literalName) return a.literalName.trim();
      const family = a.familyName?.trim();
      const given = a.givenName?.trim();
      if (family && given) return `${family}, ${given.charAt(0)}.`;
      return family || given || '';
    }).filter(Boolean);
  } else if (typeof ref.authors === 'string' && ref.authors.trim()) {
    authorList = ref.authors.split(/[,;&]/).map((s: string) => s.trim()).filter(Boolean);
  }

  const rawAuthor = authorList.length > 0 ? authorList : ['Author Unknown'];

  switch (style) {
    case 'IEEE': {
      // [1] J. K. Author, "Title of paper," Abbrev. Container, vol. x, no. y, pp. xxx-xxx, Year.
      const ieeeAuthors = rawAuthor.map((a: string) => {
        if (a.includes(',')) {
          const [f, g] = a.split(',');
          return `${g.trim().charAt(0)}. ${f.trim()}`;
        }
        return a;
      }).join(', ');
      let s = `[${index}] ${ieeeAuthors}, "${titleVal},"`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += `, vol. ${volumeVal}`;
      if (issueVal) s += `, no. ${issueVal}`;
      if (pagesVal) s += `, pp. ${pagesVal}`;
      s += `, ${yearVal}.`;
      if (doiUrl) s += ` DOI: ${doiVal}.`;
      return s;
    }

    case 'NUMERIC_APA': {
      // [1] Author, A. (Year). Title. Container, Volume(Issue), pages.
      let apaAuthors = '';
      if (rawAuthor.length === 1) apaAuthors = rawAuthor[0];
      else if (rawAuthor.length === 2) apaAuthors = `${rawAuthor[0]} & ${rawAuthor[1]}`;
      else apaAuthors = `${rawAuthor.slice(0, -1).join(', ')}, & ${rawAuthor[rawAuthor.length - 1]}`;
      let s = `[${index}] ${apaAuthors} (${yearVal}). ${titleVal}.`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += `, ${volumeVal}`;
      if (issueVal) s += `(${issueVal})`;
      if (pagesVal) s += `, ${pagesVal}.`;
      else if (volumeVal || containerVal) s += '.';
      if (doiUrl) s += ` ${doiUrl}`;
      return s;
    }

    case 'HARVARD': {
      // Author, A., Year. Title. Container, Volume(Issue), pp.pages.
      const hAuthors = rawAuthor.join(' and ');
      let s = `${hAuthors}, ${yearVal}. ${titleVal}.`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += `, ${volumeVal}`;
      if (issueVal) s += `(${issueVal})`;
      if (pagesVal) s += `, pp.${pagesVal}.`;
      else if (volumeVal || containerVal) s += '.';
      if (doiUrl) s += ` Available at: <${doiUrl}>.`;
      return s;
    }

    case 'CHICAGO_AUTHOR_DATE': {
      // Author, First, and Second Author. Year. "Title." Container Volume (Issue): pages.
      const cAuthors = rawAuthor.join(' and ');
      let s = `${cAuthors}. ${yearVal}. "${titleVal}."`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += ` ${volumeVal}`;
      if (issueVal) s += ` (${issueVal})`;
      if (pagesVal) s += `: ${pagesVal}.`;
      else if (volumeVal || containerVal) s += '.';
      if (doiUrl) s += ` ${doiUrl}.`;
      return s;
    }

    case 'VANCOUVER': {
      // 1. Author A, Author B. Title. Container. Year;Volume(Issue):pages.
      const vAuthors = rawAuthor.map((a: string) => a.replace(/[,.]/g, '')).join(', ');
      let s = `${index}. ${vAuthors}. ${titleVal}.`;
      if (containerVal) s += ` ${containerVal}.`;
      s += ` ${yearVal}`;
      if (volumeVal) s += `;${volumeVal}`;
      if (issueVal) s += `(${issueVal})`;
      if (pagesVal) s += `:${pagesVal}.`;
      else s += '.';
      if (doiUrl) s += ` doi:${doiVal}`;
      return s;
    }

    case 'MLA_9': {
      // Author. "Title." Container, vol. X, no. Y, Year, pp. pages.
      const mlaAuthor = rawAuthor.length > 2 ? `${rawAuthor[0]}, et al.` : rawAuthor.join(', and ');
      let s = `${mlaAuthor}. "${titleVal}."`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += `, vol. ${volumeVal}`;
      if (issueVal) s += `, no. ${issueVal}`;
      s += `, ${yearVal}`;
      if (pagesVal) s += `, pp. ${pagesVal}.`;
      else s += '.';
      if (doiUrl) s += ` ${doiUrl}.`;
      return s;
    }

    case 'APA_7':
    default: {
      // Author, A., & Author, B. (Year). Title of work. Container, Volume(Issue), pages. https://doi.org/...
      let apaAuthors = '';
      if (rawAuthor.length === 1) apaAuthors = rawAuthor[0];
      else if (rawAuthor.length === 2) apaAuthors = `${rawAuthor[0]} & ${rawAuthor[1]}`;
      else apaAuthors = `${rawAuthor.slice(0, -1).join(', ')}, & ${rawAuthor[rawAuthor.length - 1]}`;
      let s = `${apaAuthors} (${yearVal}). ${titleVal}.`;
      if (containerVal) s += ` ${containerVal}`;
      if (volumeVal) s += `, ${volumeVal}`;
      if (issueVal) s += `(${issueVal})`;
      if (pagesVal) s += `, ${pagesVal}.`;
      else if (volumeVal || containerVal) s += '.';
      if (doiUrl) s += ` ${doiUrl}`;
      return s;
    }
  }
}

export function ReferencesPage() {
  const projectId = useProjectId();
  const queryClient = useQueryClient();
  const [q, setQ] = useState('');
  const [style, setStyle] = useState('APA_7');
  const [presentation, setPresentation] = useState<'PARENTHETICAL' | 'NARRATIVE'>('PARENTHETICAL');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'RESEARCH' | 'REPORT' | 'INCOMPLETE'>('ALL');
  const [rescanStatus, setRescanStatus] = useState<string | null>(null);

  const [editingRef, setEditingRef] = useState<any | null>(null);
  const [editForm, setEditForm] = useState({
    title: '',
    authors: '',
    year: '',
    containerTitle: '',
    volume: '',
    issue: '',
    pages: '',
    publisher: '',
    doi: '',
    url: '',
    citationKey: '',
    availableForResearchAi: true,
    availableForCitation: true,
  });

  const openEditModal = (ref: any) => {
    setEditingRef(ref);
    const authorStr = formatReferenceAuthors(ref.authors);
    setEditForm({
      title: ref.title || '',
      authors: authorStr || '',
      year: ref.year ? String(ref.year) : '',
      containerTitle: ref.containerTitle || ref.journal || '',
      volume: ref.volume || '',
      issue: ref.issue || '',
      pages: ref.pages || '',
      publisher: ref.publisher || '',
      doi: ref.doi || '',
      url: ref.url || '',
      citationKey: ref.citationKey || '',
      availableForResearchAi: ref.availableForResearchAi !== false,
      availableForCitation: ref.availableForCitation !== false,
    });
  };

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (projectQuery.data?.citationStyle) setStyle(projectQuery.data.citationStyle);
    if (projectQuery.data?.citationPresentation) setPresentation(projectQuery.data.citationPresentation as 'PARENTHETICAL' | 'NARRATIVE');
  }, [projectQuery.data?.citationPresentation, projectQuery.data?.citationStyle]);

  const updateCitationSettings = useMutation({
    mutationFn: (body: Record<string, unknown>) => projectApi.update(projectId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['references', projectId] });
      queryClient.invalidateQueries({ queryKey: ['rag-conversations', projectId] });
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
    },
  });

  const handleStyleChange = (next: string) => {
    setStyle(next);
    updateCitationSettings.mutate({ citationStyle: next });
    // Also synchronize all reports in the project so final printing/exports immediately reflect chosen citation style
    reportApi.reports(projectId).then((res) => {
      (res?.content || []).forEach((r: any) => {
        if (r?.id) {
          reportApi.updateReport(String(r.id), { citationStyle: next }).catch(() => {});
        }
      });
    }).catch(() => {});
  };

  const references = useQuery({
    queryKey: ['references', projectId, q],
    queryFn: () => reportApi.references(projectId, 0, 100, { title: q || undefined }),
    enabled: Boolean(projectId),
  });

  const updateUsageScope = useMutation({
    mutationFn: ({ referenceId, availableForResearchAi, availableForCitation }: { referenceId: string; availableForResearchAi?: boolean; availableForCitation?: boolean }) =>
      reportApi.setUsageScope(referenceId, { availableForResearchAi, availableForCitation }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['references', projectId] });
    },
  });

  const updateReferenceMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => {
      if (!editingRef?.id) throw new Error('No reference selected');
      return reportApi.updateReference(editingRef.id, body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['references', projectId] });
      setEditingRef(null);
      setRescanStatus('Reference metadata updated successfully.');
      setTimeout(() => setRescanStatus(null), 3500);
    },
    onError: (err: any) => {
      setRescanStatus(err?.message || 'Failed to update reference.');
      setTimeout(() => setRescanStatus(null), 4000);
    },
  });

  const handleSaveReferenceEdit = () => {
    const authorRequests = editForm.authors
      .split(/[,;&]/)
      .map((a) => a.trim())
      .filter(Boolean)
      .map((name, idx) => {
        let familyName = '';
        let givenName = '';
        if (name.includes(',')) {
          const parts = name.split(',');
          familyName = parts[0].trim();
          givenName = parts.slice(1).join(' ').trim();
        } else {
          const parts = name.split(/\s+/);
          if (parts.length > 1) {
            familyName = parts[parts.length - 1];
            givenName = parts.slice(0, parts.length - 1).join(' ');
          } else {
            familyName = parts[0];
          }
        }
        return {
          literalName: name,
          familyName,
          givenName,
          displayOrder: idx + 1,
          role: 'AUTHOR',
        };
      });

    updateReferenceMutation.mutate({
      title: editForm.title.trim(),
      containerTitle: editForm.containerTitle.trim() || undefined,
      publicationYear: editForm.year ? parseInt(editForm.year, 10) : undefined,
      volume: editForm.volume.trim() || undefined,
      issue: editForm.issue.trim() || undefined,
      pages: editForm.pages.trim() || undefined,
      publisher: editForm.publisher.trim() || undefined,
      doi: editForm.doi.trim() || undefined,
      url: editForm.url.trim() || undefined,
      citationKey: editForm.citationKey.trim() || undefined,
      authors: authorRequests.length > 0 ? authorRequests : undefined,
      availableForResearchAi: editForm.availableForResearchAi,
      availableForCitation: editForm.availableForCitation,
    });
  };

  const rescanMetadata = useMutation({
    mutationFn: () => reportApi.rescanProjectMetadata(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['references', projectId] });
      setRescanStatus('Scholarly reference metadata re-scanned successfully.');
      setTimeout(() => setRescanStatus(null), 3500);
    },
    onError: (err: any) => {
      setRescanStatus(err?.message || 'Metadata re-scan failed.');
      setTimeout(() => setRescanStatus(null), 4000);
    },
  });

  const exportReferences = useMutation({
    mutationFn: async (format: string) => {
      const result = await reportApi.exportReferences(projectId, format);
      reportApi.saveBlob(result.blob, result.filename);
    },
  });

  const format = useMutation({
    mutationFn: () =>
      reportApi.formatCitation({
        referenceId: pageContent(references.data)[0]?.id,
        style,
        context: style === 'IEEE' || style === 'VANCOUVER' || style === 'NUMERIC_APA'
          ? 'NUMERIC'
          : presentation === 'NARRATIVE'
          ? 'IN_TEXT_NARRATIVE'
          : 'IN_TEXT_PARENTHETICAL',
        citationNumber: 1,
      }),
  });

  const duplicates = useMutation({
    mutationFn: () =>
      reportApi.duplicateReferences(projectId, {
        referenceIds: pageContent(references.data).map((reference) => reference.id),
      }),
  });

  const allRefs = pageContent(references.data) as any[];
  const researchCount = allRefs.filter((r) => r.availableForResearchAi !== false).length;
  const reportCount = allRefs.filter((r) => r.availableForCitation !== false).length;
  const incompleteCount = allRefs.filter((r) => {
    const authorText = formatReferenceAuthors(r.authors);
    return !authorText || !r.year || !r.title || r.metadataStatus === 'INCOMPLETE';
  }).length;

  const filteredRefs = allRefs.filter((ref) => {
    if (activeFilter === 'RESEARCH') return ref.availableForResearchAi !== false;
    if (activeFilter === 'REPORT') return ref.availableForCitation !== false;
    if (activeFilter === 'INCOMPLETE') {
      const authorText = formatReferenceAuthors(ref.authors);
      return !authorText || !ref.year || !ref.title || ref.metadataStatus === 'INCOMPLETE';
    }
    return true;
  });

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectQuery.data?.title || projectId, 'References']} />

      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <BookOpen className="text-primary" size={26} />
            Reference Library & Scholarly Citations
          </h1>
          <p className="muted">
            Manage project references with strict APA 7th, IEEE, Harvard, Chicago, Vancouver, and MLA-9 formatting. Select partition scopes for Research Design vs Report Writing.
          </p>
        </div>

        <div className="toolbar" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Button
            type="button"
            variant="secondary"
            onClick={() => rescanMetadata.mutate()}
            disabled={rescanMetadata.isPending}
            title="Re-extract and verify bibliographic metadata from uploaded PDFs"
          >
            <RotateCw size={14} style={{ marginRight: 4 }} />
            {rescanMetadata.isPending ? 'Re-scanning...' : 'Re-scan Metadata'}
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => exportReferences.mutate('RIS')}
            disabled={exportReferences.isPending || allRefs.length === 0}
          >
            <Download size={14} style={{ marginRight: 4 }} /> Export RIS
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => exportReferences.mutate('BIBTEX')}
            disabled={exportReferences.isPending || allRefs.length === 0}
          >
            <Download size={14} style={{ marginRight: 4 }} /> Export BibTeX
          </Button>
          <Button
            type="button"
            variant="secondary"
            onClick={() => exportReferences.mutate('ENDNOTE_XML')}
            disabled={exportReferences.isPending || allRefs.length === 0}
          >
            <Download size={14} style={{ marginRight: 4 }} /> Export EndNote
          </Button>
          <Button type="button" variant="secondary" onClick={() => duplicates.mutate()}>
            Check Duplicates
          </Button>
        </div>
      </div>

      {rescanStatus && (
        <div className="alert info" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: 8 }}>
          <CheckCircle2 size={16} /> {rescanStatus}
        </div>
      )}

      {/* Citation Style Selector Card */}
      <Card style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Citation Style & Final Printing Engine</h2>
          <Badge tone="info">Active Style: {CITATION_STYLES.find(c => c.id === style)?.label || style}</Badge>
        </div>
        <div className="toolbar" style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.88rem' }}>Citation Style:</span>
            <Select value={style} onChange={(event) => handleStyleChange(event.target.value)}>
              {CITATION_STYLES.map((item) => (
                <option key={item.id} value={item.id}>{item.label}</option>
              ))}
            </Select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.88rem' }}>In-text Format:</span>
            <Select value={presentation} onChange={(e) => {
              const next = e.target.value as 'PARENTHETICAL' | 'NARRATIVE';
              setPresentation(next);
              updateCitationSettings.mutate({ citationPresentation: next });
            }}>
              <option value="PARENTHETICAL">Parenthetical e.g., (Smith, 2024) / [1]</option>
              <option value="NARRATIVE">Narrative e.g., Smith (2024)</option>
            </Select>
          </div>

          <Button type="button" variant="secondary" onClick={() => format.mutate()} disabled={format.isPending || allRefs.length === 0}>
            Preview In-Text Citation
          </Button>
        </div>

        {format.data && (
          <div className="panel" style={{ marginTop: '1rem', padding: '1rem', background: 'var(--surface-hover)', borderRadius: '6px' }}>
            <span className="muted" style={{ fontSize: '0.78rem' }}>FORMATTED IN-TEXT PREVIEW ({style}):</span>
            <div style={{ fontSize: '0.95rem', fontWeight: 500, marginTop: '4px' }}>
              {displayValue((format.data as any).text ?? (format.data as any).formattedCitation ?? (format.data as any).citation)}
            </div>
            {Array.isArray((format.data as any).warnings) && (format.data as any).warnings.length > 0 && (
              <div style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: '#d97706' }}>
                {((format.data as any).warnings as string[]).map((w: string, idx: number) => (
                  <div key={idx}>⚠️ {w}</div>
                ))}
              </div>
            )}
          </div>
        )}
      </Card>

      {/* References Search and List */}
      <Card style={{ padding: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
            <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>
              Project References ({filteredRefs.length} of {allRefs.length})
            </h2>
            <div style={{ display: 'flex', gap: '4px', marginLeft: '0.5rem', flexWrap: 'wrap' }}>
              <button
                type="button"
                onClick={() => setActiveFilter('ALL')}
                style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: activeFilter === 'ALL' ? 600 : 400,
                  border: '1px solid var(--border)',
                  background: activeFilter === 'ALL' ? 'var(--primary)' : 'transparent',
                  color: activeFilter === 'ALL' ? '#fff' : 'inherit',
                  cursor: 'pointer',
                }}
              >
                All ({allRefs.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter('RESEARCH')}
                style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: activeFilter === 'RESEARCH' ? 600 : 400,
                  border: '1px solid var(--border)',
                  background: activeFilter === 'RESEARCH' ? 'var(--primary)' : 'transparent',
                  color: activeFilter === 'RESEARCH' ? '#fff' : 'inherit',
                  cursor: 'pointer',
                }}
              >
                Active for Research ({researchCount})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter('REPORT')}
                style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: activeFilter === 'REPORT' ? 600 : 400,
                  border: '1px solid var(--border)',
                  background: activeFilter === 'REPORT' ? 'var(--primary)' : 'transparent',
                  color: activeFilter === 'REPORT' ? '#fff' : 'inherit',
                  cursor: 'pointer',
                }}
              >
                Active for Report ({reportCount})
              </button>
              <button
                type="button"
                onClick={() => setActiveFilter('INCOMPLETE')}
                style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  fontWeight: activeFilter === 'INCOMPLETE' ? 600 : 400,
                  border: '1px solid var(--border)',
                  background: activeFilter === 'INCOMPLETE' ? 'var(--primary)' : 'transparent',
                  color: activeFilter === 'INCOMPLETE' ? '#fff' : 'inherit',
                  cursor: 'pointer',
                }}
              >
                Needs Review ({incompleteCount})
              </button>
            </div>
          </div>

          <div style={{ width: '280px' }}>
            <Input
              value={q}
              onChange={(event) => setQ(event.target.value)}
              placeholder="Search references by title or author..."
            />
          </div>
        </div>

        {filteredRefs.length === 0 ? (
          <EmptyState
            title="No references found"
            description="Upload research papers in Documents/Sources to automatically populate the project reference library."
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            {filteredRefs.map((ref: any, idx: number) => {
              const authorText = formatReferenceAuthors(ref.authors);
              const hasMissingMeta = !authorText || !ref.year || !ref.title || ref.metadataStatus === 'INCOMPLETE';
              const isResearchActive = ref.availableForResearchAi !== false;
              const isReportActive = ref.availableForCitation !== false;
              const formattedCitation = formatScholarlyReference(ref, style, idx + 1);

              return (
                <div
                  key={ref.id || idx}
                  style={{
                    padding: '1.1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    gap: '1.25rem',
                    background: (isResearchActive || isReportActive) ? 'transparent' : 'var(--surface-hover)',
                    opacity: (isResearchActive || isReportActive) ? 1 : 0.7,
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '6px', flexWrap: 'wrap' }}>
                      <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '0.95rem', color: 'var(--color-primary)' }}>
                        [{idx + 1}]
                      </span>
                      <Badge style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>
                        {ref.citationKey ?? `ref${idx + 1}`}
                      </Badge>
                      <strong style={{ fontSize: '0.95rem' }}>{ref.title || 'Untitled Reference'}</strong>
                      {hasMissingMeta && (
                        <Badge tone="warning" style={{ fontSize: '0.72rem' }}>
                          Incomplete Metadata
                        </Badge>
                      )}
                      {isResearchActive && isReportActive ? (
                        <Badge tone="success" style={{ fontSize: '0.72rem' }}>Research & Report Active</Badge>
                      ) : isResearchActive ? (
                        <Badge tone="info" style={{ fontSize: '0.72rem' }}>Research Only</Badge>
                      ) : isReportActive ? (
                        <Badge tone="info" style={{ fontSize: '0.72rem' }}>Report Only</Badge>
                      ) : (
                        <Badge style={{ fontSize: '0.72rem', opacity: 0.7 }}>Excluded from All</Badge>
                      )}
                    </div>

                    {/* Styled Reference Display matching chosen citation style */}
                    <div style={{
                      fontSize: '0.88rem',
                      lineHeight: '1.5',
                      color: 'var(--text)',
                      padding: '8px 12px',
                      background: 'var(--surface-hover)',
                      borderRadius: '6px',
                      borderLeft: '3px solid var(--primary)',
                      margin: '6px 0 8px'
                    }}>
                      <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--primary)', display: 'block', textTransform: 'uppercase', marginBottom: '3px' }}>
                        {CITATION_STYLES.find(c => c.id === style)?.label || style} Bibliography Entry:
                      </span>
                      {formattedCitation}
                    </div>

                    <p className="muted" style={{ margin: '2px 0 0', fontSize: '0.82rem' }}>
                      {authorText || 'Author(s) unparsed'} • {ref.year ? ref.year : 'n.d.'} • {ref.containerTitle || ref.journal || ref.publisher || 'Source details pending'}
                      {ref.doi ? ` • DOI: ${ref.doi}` : ''}
                    </p>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.6rem', flexShrink: 0 }}>
                    {/* Dual Scope Selection: Research vs Report */}
                    <div style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px',
                      background: 'var(--surface-hover)',
                      padding: '6px 10px',
                      borderRadius: '6px',
                      border: '1px solid var(--border)'
                    }}>
                      <span style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Scope Selection</span>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', cursor: 'pointer', userSelect: 'none' }}>
                        <input
                          type="checkbox"
                          checked={isResearchActive}
                          onChange={(e) => updateUsageScope.mutate({
                            referenceId: ref.id,
                            availableForResearchAi: e.target.checked,
                            availableForCitation: isReportActive
                          })}
                        />
                        <span style={{ fontWeight: isResearchActive ? 600 : 400 }}>Research Design</span>
                      </label>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', cursor: 'pointer', userSelect: 'none' }}>
                        <input
                          type="checkbox"
                          checked={isReportActive}
                          onChange={(e) => updateUsageScope.mutate({
                            referenceId: ref.id,
                            availableForResearchAi: isResearchActive,
                            availableForCitation: e.target.checked
                          })}
                        />
                        <span style={{ fontWeight: isReportActive ? 600 : 400 }}>Report Writing</span>
                      </label>
                    </div>

                    <div style={{ display: 'flex', gap: '4px' }}>
                      <Button
                        type="button"
                        variant="secondary"
                        className="btn-compact"
                        onClick={() => openEditModal(ref)}
                        title="Edit bibliographic metadata and citation key"
                      >
                        <Edit3 size={13} style={{ marginRight: 4 }} />
                        Edit
                      </Button>

                      <Button
                        type="button"
                        variant="secondary"
                        className="btn-compact"
                        onClick={() => {
                          navigator.clipboard.writeText(formattedCitation);
                          setCopiedId(ref.id);
                          setTimeout(() => setCopiedId(null), 2500);
                        }}
                      >
                        <Copy size={13} style={{ marginRight: 4 }} />
                        {copiedId === ref.id ? 'Copied!' : 'Copy'}
                      </Button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* Edit Reference Metadata Modal */}
      {editingRef && (
        <Modal
          open={Boolean(editingRef)}
          onClose={() => setEditingRef(null)}
          title="Edit Reference Metadata & Scholarly Details"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '75vh', overflowY: 'auto', padding: '0.5rem 0' }}>
            {/* Live Formatted Citation Preview in Chosen Style */}
            <div style={{
              padding: '0.75rem 1rem',
              background: 'var(--surface-hover)',
              borderRadius: '6px',
              border: '1px solid var(--border)',
              borderLeft: '4px solid var(--primary)'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary)', textTransform: 'uppercase' }}>
                  Live Formatted Preview ({CITATION_STYLES.find(c => c.id === style)?.label || style}):
                </span>
                <Badge tone="success" style={{ fontSize: '0.7rem' }}>Live Preview</Badge>
              </div>
              <p style={{ margin: 0, fontSize: '0.85rem', fontStyle: 'italic', lineHeight: '1.4' }}>
                {formatScholarlyReference({
                  title: editForm.title,
                  authors: editForm.authors,
                  year: editForm.year,
                  containerTitle: editForm.containerTitle,
                  volume: editForm.volume,
                  issue: editForm.issue,
                  pages: editForm.pages,
                  doi: editForm.doi,
                  publisher: editForm.publisher,
                }, style, 1)}
              </p>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                Paper Title
              </label>
              <Input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                placeholder="Full scholarly title of the paper or book"
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                Author(s) (comma-separated, e.g. "Abate, G. T., Abay, K. A., Chamberlin, J.")
              </label>
              <Input
                value={editForm.authors}
                onChange={(e) => setEditForm({ ...editForm, authors: e.target.value })}
                placeholder="Authors (e.g. Smith, J., Doe, A.)"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Publication Year
                </label>
                <Input
                  type="number"
                  value={editForm.year}
                  onChange={(e) => setEditForm({ ...editForm, year: e.target.value })}
                  placeholder="e.g. 2023"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Citation Key (In-Text Identifier)
                </label>
                <Input
                  value={editForm.citationKey}
                  onChange={(e) => setEditForm({ ...editForm, citationKey: e.target.value })}
                  placeholder="e.g. abate2023"
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                Journal / Conference / Container Title
              </label>
              <Input
                value={editForm.containerTitle}
                onChange={(e) => setEditForm({ ...editForm, containerTitle: e.target.value })}
                placeholder="e.g. Food Policy, Heliyon, Sustainability"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Volume
                </label>
                <Input
                  value={editForm.volume}
                  onChange={(e) => setEditForm({ ...editForm, volume: e.target.value })}
                  placeholder="e.g. 116"
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Issue
                </label>
                <Input
                  value={editForm.issue}
                  onChange={(e) => setEditForm({ ...editForm, issue: e.target.value })}
                  placeholder="e.g. 2"
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Pages / Article No.
                </label>
                <Input
                  value={editForm.pages}
                  onChange={(e) => setEditForm({ ...editForm, pages: e.target.value })}
                  placeholder="e.g. 102439 or 45-60"
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  Publisher
                </label>
                <Input
                  value={editForm.publisher}
                  onChange={(e) => setEditForm({ ...editForm, publisher: e.target.value })}
                  placeholder="e.g. Elsevier, Springer, MDPI"
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: 4 }}>
                  DOI
                </label>
                <Input
                  value={editForm.doi}
                  onChange={(e) => setEditForm({ ...editForm, doi: e.target.value })}
                  placeholder="e.g. 10.1016/j.foodpol.2023.102439"
                />
              </div>
            </div>

            {/* Scope Selection in Edit Modal */}
            <div style={{
              display: 'flex',
              gap: '1.5rem',
              background: 'var(--surface-hover)',
              padding: '0.75rem 1rem',
              borderRadius: '6px',
              border: '1px solid var(--border)'
            }}>
              <span style={{ fontSize: '0.82rem', fontWeight: 600 }}>Active Scopes:</span>
              <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={editForm.availableForResearchAi}
                  onChange={(e) => setEditForm({ ...editForm, availableForResearchAi: e.target.checked })}
                />
                <span>Grounds Research Design</span>
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={editForm.availableForCitation}
                  onChange={(e) => setEditForm({ ...editForm, availableForCitation: e.target.checked })}
                />
                <span>Active in Report Citations</span>
              </label>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
              <Button type="button" variant="secondary" onClick={() => setEditingRef(null)}>
                Cancel
              </Button>
              <Button
                type="button"
                variant="primary"
                onClick={handleSaveReferenceEdit}
                disabled={updateReferenceMutation.isPending || !editForm.title.trim()}
              >
                {updateReferenceMutation.isPending ? 'Saving...' : 'Save Reference'}
              </Button>
            </div>
          </div>
        </Modal>
      )}

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

function formatReferenceAuthors(authors: any): string {
  if (!authors) return '';
  if (typeof authors === 'string') return authors;
  if (!Array.isArray(authors)) return '';
  return authors
    .map((author) => author.literalName || [author.givenName, author.familyName].filter(Boolean).join(' '))
    .filter(Boolean)
    .join('; ');
}

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

export function Metric({ label, value }: { label: string; value: unknown }) {
  return <Card><span className="muted">{label}</span><p className="metric">{displayValue(value)}</p></Card>;
}

function DraftPreview({ draft }: { draft: Record<string, unknown> }) {
  return <div className="panel"><Badge tone="info">AI draft</Badge><p>{displayValue(draft.draftText ?? draft.text)}</p><div className="toolbar"><Button type="button">Accept</Button><Button type="button" variant="secondary">Edit</Button><Button type="button" variant="danger">Reject</Button></div></div>;
}

export function ValidationPanel({ validation }: { validation?: Record<string, unknown> }) {
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
  const [error, setError] = useState<string | null>(null);
  const download = useMutation({
    mutationFn: async (exportId: string) => {
      const result = await reportApi.downloadExport(exportId);
      reportApi.saveBlob(result.blob, result.filename);
    },
    onError: (err: any) => {
      setError(err?.message ?? 'Export download failed.');
    },
  });
  if (!data) return null;
  const id = String(data.id ?? '');
  const status = String(data.status ?? '');
  return (
    <div className="panel">
      <Badge tone={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning'}>
        {displayValue(data.format)} {status}
      </Badge>
      <p className="muted">Revision {displayValue(data.reportRevisionNumber)} | Size {displayValue(data.fileSizeBytes)}</p>
      {status === 'COMPLETED' && id ? (
        <Button type="button" variant="secondary" onClick={() => download.mutate(id)} disabled={download.isPending}>
          <Download size={14} style={{ marginRight: 6 }} /> {download.isPending ? 'Downloading...' : 'Download'}
        </Button>
      ) : null}
      {error ? <div className="alert danger">{error}</div> : null}
    </div>
  );
}
