import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect, useMemo } from 'react';
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
  ChevronRight,
  Table,
  FilePlus,
  CornerDownRight,
  Edit2,
  Image as ImageIcon,
} from 'lucide-react';
import { analysisApi, documentApi, notificationApi, projectApi, reportApi } from '../api/endpoints';
import { AssistantResponse } from '../components/AssistantResponse';
import { Breadcrumbs, Button, Card, Input, Badge, Select, Modal } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';
import { ReportRichEditor } from '../components/editor/ReportRichEditor';
import { ProjectEvidencePanel } from '../features/evidence/ProjectEvidencePanel';
import { finalDocumentLabel, formatChapterTitle, workspaceTypeOf } from '../features/projects/workspaceMeta';
import type { GenerateSectionRequest, LiteratureMatrixInclusion, ReorderStructureRequest, UpdateTitlePageDetailsRequest } from '../types/api';

const CITATION_STYLES = [
  { id: 'APA_7', label: 'APA 7th Edition (Author, Year)' },
  { id: 'IEEE', label: 'IEEE [1]' },
  { id: 'NUMERIC_APA', label: 'Numeric + APA Bibliography [1]' },
  { id: 'HARVARD', label: 'Harvard (Author, Year)' },
  { id: 'CHICAGO_AUTHOR_DATE', label: 'Chicago Author-Date (Author, Year)' },
  { id: 'MLA_9', label: 'MLA 9th Edition (Author)' },
  { id: 'VANCOUVER', label: 'Vancouver [1]' },
];

export type DocumentNodeCategory =
  | 'TITLE_PAGE'
  | 'TABLE_OF_CONTENTS'
  | 'LIST_OF_FIGURES'
  | 'LIST_OF_TABLES'
  | 'REFERENCES'
  | 'DECLARATION'
  | 'CERTIFICATION'
  | 'DEDICATION'
  | 'ACKNOWLEDGEMENTS'
  | 'LITERATURE_REVIEW'
  | 'BACKGROUND'
  | 'PROBLEM_STATEMENT'
  | 'AIM_AND_OBJECTIVES'
  | 'RESEARCH_QUESTIONS'
  | 'METHODOLOGY'
  | 'SYSTEM_REQUIREMENTS'
  | 'SYSTEM_DESIGN'
  | 'IMPLEMENTATION'
  | 'TESTING'
  | 'FINDINGS'
  | 'DISCUSSION'
  | 'CONCLUSIONS'
  | 'CUSTOM_CONTENT'
  | 'CONTENT_SECTION';

export function getDocumentNodeCategory(section?: any, chapter?: any): DocumentNodeCategory {
  if (!section) return 'CONTENT_SECTION';
  const purpose = String(section.semanticPurpose || '').toUpperCase();
  const type = String(section.type || section.sectionType || '').toUpperCase();
  const heading = String(section.heading || section.title || '').trim().toLowerCase();
  const chTitle = String(chapter?.title || '').trim().toLowerCase();

  if (purpose === 'TITLE_PAGE' || type === 'TITLE_PAGE' || heading === 'title page' || heading === 'title' || (chTitle.includes('preliminary') && heading.includes('title'))) {
    return 'TITLE_PAGE';
  }
  if (purpose === 'TABLE_OF_CONTENTS' || type === 'TABLE_OF_CONTENTS' || heading === 'table of contents' || heading === 'contents' || heading === 'toc') {
    return 'TABLE_OF_CONTENTS';
  }
  if (purpose === 'LIST_OF_FIGURES' || type === 'LIST_OF_FIGURES' || heading.includes('list of figures') || heading === 'figures') {
    return 'LIST_OF_FIGURES';
  }
  if (purpose === 'LIST_OF_TABLES' || type === 'LIST_OF_TABLES' || heading.includes('list of tables') || heading === 'tables') {
    return 'LIST_OF_TABLES';
  }
  if (purpose === 'REFERENCES' || type === 'REFERENCES' || heading === 'references' || heading === 'bibliography' || chTitle === 'references') {
    return 'REFERENCES';
  }
  if (purpose === 'DECLARATION' || type === 'DECLARATION' || heading.includes('declaration')) {
    return 'DECLARATION';
  }
  if (purpose === 'CERTIFICATION' || type === 'CERTIFICATION' || heading.includes('certification')) {
    return 'CERTIFICATION';
  }
  if (purpose === 'DEDICATION' || type === 'DEDICATION' || heading.includes('dedication')) {
    return 'DEDICATION';
  }
  if (purpose === 'ACKNOWLEDGEMENTS' || type === 'ACKNOWLEDGEMENTS' || heading.includes('acknowledgement') || heading.includes('acknowledgment')) {
    return 'ACKNOWLEDGEMENTS';
  }
  if (purpose === 'LITERATURE_REVIEW' || type === 'LITERATURE_REVIEW' || heading.includes('literature review') || chTitle.includes('literature review')) {
    return 'LITERATURE_REVIEW';
  }
  if (purpose === 'BACKGROUND' || type === 'BACKGROUND' || heading.includes('background')) {
    return 'BACKGROUND';
  }
  if (purpose === 'PROBLEM_STATEMENT' || type === 'PROBLEM_STATEMENT' || heading.includes('problem statement')) {
    return 'PROBLEM_STATEMENT';
  }
  if (purpose === 'AIM_AND_OBJECTIVES' || type === 'AIM_AND_OBJECTIVES' || heading.includes('objective') || heading.includes('aim')) {
    return 'AIM_AND_OBJECTIVES';
  }
  if (purpose === 'RESEARCH_QUESTIONS' || type === 'RESEARCH_QUESTIONS' || heading.includes('research question')) {
    return 'RESEARCH_QUESTIONS';
  }
  if (purpose === 'METHODOLOGY' || type === 'METHODOLOGY' || heading.includes('methodology') || chTitle.includes('methodology')) {
    return 'METHODOLOGY';
  }
  if (purpose === 'SYSTEM_REQUIREMENTS' || type === 'SYSTEM_REQUIREMENTS' || heading.includes('requirement')) {
    return 'SYSTEM_REQUIREMENTS';
  }
  if (purpose === 'SYSTEM_DESIGN' || type === 'SYSTEM_DESIGN' || heading.includes('system design') || heading.includes('architecture') || heading.includes('database design')) {
    return 'SYSTEM_DESIGN';
  }
  if (purpose === 'IMPLEMENTATION' || type === 'IMPLEMENTATION' || heading.includes('implementation')) {
    return 'IMPLEMENTATION';
  }
  if (purpose === 'TESTING' || type === 'TESTING' || heading.includes('testing') || heading.includes('test results') || heading.includes('verification and validation')) {
    return 'TESTING';
  }
  if (purpose === 'FINDINGS' || type === 'FINDINGS' || heading.includes('finding') || (heading.includes('result') && !heading.includes('test'))) {
    return 'FINDINGS';
  }
  if (purpose === 'DISCUSSION' || type === 'DISCUSSION' || heading.includes('discussion')) {
    return 'DISCUSSION';
  }
  if (purpose === 'CONCLUSIONS' || type === 'CONCLUSIONS' || heading.includes('conclusion')) {
    return 'CONCLUSIONS';
  }
  if (purpose === 'CUSTOM' || type === 'CUSTOM') {
    return 'CUSTOM_CONTENT';
  }
  return 'CONTENT_SECTION';
}

export function isDeterministicNode(category: DocumentNodeCategory): boolean {
  return [
    'TITLE_PAGE',
    'TABLE_OF_CONTENTS',
    'LIST_OF_FIGURES',
    'LIST_OF_TABLES',
    'REFERENCES',
  ].includes(category);
}

export function isCorruptedDeterministicContent(content?: string, category?: DocumentNodeCategory): boolean {
  if (!content) return false;
  if (!category || !isDeterministicNode(category)) return false;
  const lower = content.toLowerCase();
  return (
    lower.includes('overview and thematic context') ||
    lower.includes('synthesis of grounded empirical literature') ||
    lower.includes('scholarly discourse on') ||
    lower.includes('grounded empirical literature') ||
    lower.includes('theoretical foundations')
  );
}

export function ReportPage() {
  const projectId = useProjectId();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'sections' | 'final-doc' | 'preview' | 'evidence'>('sections');
  const [selectedReportId, setSelectedReportId] = useState('');
  const [selectedChapterId, setSelectedChapterId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [showTocModal, setShowTocModal] = useState(false);
  const [tocViewMode, setTocViewMode] = useState<'dotleaders' | 'outline' | 'markdown'>('dotleaders');
  const [tocCopied, setTocCopied] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [validationModalOpen, setValidationModalOpen] = useState(false);
  const [validationErrors, setValidationErrors] = useState<any[]>([]);
  const [expandedChapters, setExpandedChapters] = useState<Record<string, boolean>>({});
  const [draggedItem, setDraggedItem] = useState<{
    type: 'chapter' | 'section';
    index: number;
    chapterId?: string;
    parentId?: string | null;
  } | null>(null);

  // Structure & Modal States
  const [showAddChapterModal, setShowAddChapterModal] = useState(false);
  const [newChapterTitle, setNewChapterTitle] = useState('');
  const [newChapterNumber, setNewChapterNumber] = useState('');
  const [showAddSectionModal, setShowAddSectionModal] = useState<{ chapterId: string; parentSectionId?: string | null; parentTitle?: string } | null>(null);
  const [newSectionTitle, setNewSectionTitle] = useState('');
  const [newSectionAiEnabled, setNewSectionAiEnabled] = useState(true);
  const [showRenameModal, setShowRenameModal] = useState<{ type: 'chapter' | 'section'; id: string; currentTitle: string } | null>(null);
  const [renameTitle, setRenameTitle] = useState('');
  const [showLitMatrixModal, setShowLitMatrixModal] = useState(false);
  const [deleteConfirmModal, setDeleteConfirmModal] = useState<{ type: 'chapter' | 'section'; id: string; title: string; required: boolean } | null>(null);

  // AI Draft Modal State
  const [showAiDraftModal, setShowAiDraftModal] = useState(false);
  const [aiDraftSourceScope, setAiDraftSourceScope] = useState<'ALL_PROJECT_DOCUMENTS' | 'SELECTED_DOCUMENTS' | 'NONE'>('ALL_PROJECT_DOCUMENTS');
  const [aiDraftSelectedDocIds, setAiDraftSelectedDocIds] = useState<string[]>([]);
  const [aiDraftInstructions, setAiDraftInstructions] = useState('');
  const [aiDraftApplyMode, setAiDraftApplyMode] = useState<'PREVIEW' | 'APPEND' | 'REPLACE'>('REPLACE');
  const [aiDraftConfirmReplace, setAiDraftConfirmReplace] = useState(false);

  // Edit Title Page Modal State
  const [showEditTitlePageModal, setShowEditTitlePageModal] = useState(false);
  const [exportScopeMode, setExportScopeMode] = useState<'FULL' | 'SELECTED'>('FULL');
  const [selectedExportNodeIds, setSelectedExportNodeIds] = useState<string[]>([]);
  const [includeExportCoverPage, setIncludeExportCoverPage] = useState(false);
  const [includeExportToc, setIncludeExportToc] = useState(false);
  const [includeExportListOfFigures, setIncludeExportListOfFigures] = useState(false);
  const [includeExportListOfTables, setIncludeExportListOfTables] = useState(false);
  const [includeExportReferences, setIncludeExportReferences] = useState(true);
  const [includeExportAppendices, setIncludeExportAppendices] = useState(false);
  const [exportReferenceMode, setExportReferenceMode] = useState<'CITED_IN_SELECTION' | 'ALL_PROJECT_REFERENCES' | 'NONE'>('CITED_IN_SELECTION');
  const [titlePageForm, setTitlePageForm] = useState<UpdateTitlePageDetailsRequest>({
    title: '',
    authorName: '',
    studentId: '',
    institutionName: '',
    departmentName: '',
    degreeProgram: '',
    supervisorName: '',
    academicYear: '',
    submissionYear: new Date().getFullYear(),
  });

  // Queries
  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  const projectDocumentsQuery = useQuery({
    queryKey: ['project-documents', projectId],
    queryFn: () => documentApi.list(projectId, 0, 50),
    enabled: Boolean(projectId) && showAiDraftModal,
  });

  const openEditTitlePageModal = () => {
    if (projectQuery.data) {
      const p = projectQuery.data;
      setTitlePageForm((prev) => ({
        title: prev.title || p.title || '',
        authorName: prev.authorName || '',
        studentId: prev.studentId || '',
        institutionName: prev.institutionName || p.institution || 'Takoradi Technical University',
        departmentName: prev.departmentName || p.department || 'Computer Science Department',
        degreeProgram: prev.degreeProgram || p.programme || '',
        supervisorName: prev.supervisorName || p.supervisor || '',
        academicYear: prev.academicYear || p.academicYear || '2024 / 2025',
        submissionYear: prev.submissionYear || new Date().getFullYear(),
      }));
    }
    setShowEditTitlePageModal(true);
  };

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
    enabled: Boolean(reportId) && (activeTab === 'final-doc' || activeTab === 'preview'),
  });

  const exportSelectionPayload = useMemo(() => ({
    selectionMode: exportScopeMode,
    selectedNodeIds: exportScopeMode === 'SELECTED' ? selectedExportNodeIds : [],
    includeCoverPage: exportScopeMode === 'FULL' ? true : includeExportCoverPage,
    includeFrontMatter: exportScopeMode === 'FULL',
    includeToc: exportScopeMode === 'FULL' ? true : includeExportToc,
    includeListOfFigures: exportScopeMode === 'FULL' ? true : includeExportListOfFigures,
    includeListOfTables: exportScopeMode === 'FULL' ? true : includeExportListOfTables,
    includeReferences: exportScopeMode === 'FULL' ? true : includeExportReferences,
    includeAppendices: exportScopeMode === 'FULL' ? true : includeExportAppendices,
    referenceMode: exportScopeMode === 'FULL' ? 'ALL_PROJECT_REFERENCES' : exportReferenceMode,
  }), [exportScopeMode, selectedExportNodeIds, includeExportCoverPage, includeExportToc, includeExportListOfFigures, includeExportListOfTables, includeExportReferences, includeExportAppendices, exportReferenceMode]);

  const previewQuery = useQuery({
    queryKey: ['report-preview', reportId, exportSelectionPayload],
    queryFn: () => reportApi.preview(reportId, { selection: exportSelectionPayload }),
    enabled: Boolean(reportId) && activeTab === 'preview' && (exportScopeMode === 'FULL' || selectedExportNodeIds.length > 0),
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
      queryClient.invalidateQueries({ queryKey: ['report-sections'] });
      setSaveStatus('Report structure reordered.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
  });

  const createChapter = useMutation({
    mutationFn: (payload: { title: string; chapterNumber?: number }) =>
      reportApi.createChapter(reportId, {
        title: payload.title,
        chapterNumber: payload.chapterNumber,
        type: 'CUSTOM',
        required: false,
        systemDefined: false,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      setShowAddChapterModal(false);
      setNewChapterTitle('');
      setNewChapterNumber('');
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
    mutationFn: ({ chapterId: chapId, title, parentSectionId, aiEnabled }: { chapterId: string; title: string; parentSectionId?: string | null; aiEnabled: boolean }) =>
      reportApi.createSection(chapId, {
        heading: title,
        type: 'CUSTOM',
        parentSectionId: parentSectionId || undefined,
        aiEnabled,
        required: false,
        systemDefined: false,
      }),
    onSuccess: (newSec: any) => {
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections'] });
      setShowAddSectionModal(null);
      setNewSectionTitle('');
      setNewSectionAiEnabled(true);
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

  const exportProjectReferences = useMutation({
    mutationFn: async (format: string) => {
      const result = await reportApi.exportReferences(projectId, format);
      reportApi.saveBlob(result.blob, result.filename);
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

  const moveSection = (_chapId: string, sectionsList: any[], idx: number, direction: 'up' | 'down', parentSecId?: string | null) => {
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= sectionsList.length) return;
    const copy = [...sectionsList];
    const temp = copy[idx];
    copy[idx] = copy[targetIdx];
    copy[targetIdx] = temp;
    const payload = copy.map((s, i) => ({ id: s.id, parentId: parentSecId || null, displayOrder: i + 1 }));
    reorderStructure.mutate({ sections: payload });
  };

  const toggleChapter = (chapId: string) => {
    setExpandedChapters((prev) => ({
      ...prev,
      [chapId]: prev[chapId] !== undefined ? !prev[chapId] : false,
    }));
  };

  const reorderChaptersToIndex = (sourceIdx: number, targetIdx: number) => {
    const list = structureQuery.data?.chapters;
    if (!list || sourceIdx === targetIdx) return;
    const copy = [...list];
    const [removed] = copy.splice(sourceIdx, 1);
    copy.splice(targetIdx, 0, removed);
    const payload = copy.map((ch, i) => ({ id: ch.id, displayOrder: i + 1 }));
    reorderStructure.mutate({ chapters: payload });
  };

  const reorderSectionsToIndex = (sectionsList: any[], sourceIdx: number, targetIdx: number, parentSecId?: string | null) => {
    if (!sectionsList || sourceIdx === targetIdx) return;
    const copy = [...sectionsList];
    const [removed] = copy.splice(sourceIdx, 1);
    copy.splice(targetIdx, 0, removed);
    const payload = copy.map((s, i) => ({ id: s.id, parentId: parentSecId || null, displayOrder: i + 1 }));
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
    mutationFn: () => reportApi.applyTemplateFormatting(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('Template formatting applied without removing custom sections.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });

  const finalize = useMutation({
    mutationFn: () => reportApi.finalize(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
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
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('Section revision saved.');
      setTimeout(() => setSaveStatus(null), 2500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Save failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const updateCitationStyle = useMutation({
    mutationFn: (style: string) => reportApi.updateSettings(reportId, { citationStyle: style }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Citation style updated deterministically (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const prepareFinalDoc = useMutation({
    mutationFn: () => reportApi.prepareFinalDocument(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
      setSaveStatus('Final document updated from latest saved sections. Previous versions remain available.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  const updateFinalDoc = useMutation({
    mutationFn: (data: { contentJson?: string; plainText?: string; title?: string }) =>
      reportApi.updateFinalDocument(reportId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-final-document', reportId] });
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Final document save failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const refreshTitlePageMutation = useMutation({
    mutationFn: () => reportApi.refreshTitlePage(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('Title page refreshed from project metadata (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Title page refresh failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const updateTitlePageDetailsMutation = useMutation({
    mutationFn: (details: UpdateTitlePageDetailsRequest) => reportApi.updateTitlePageDetails(reportId, details),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setShowEditTitlePageModal(false);
      setSaveStatus('Title page details updated and formatted (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Failed to update title page.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const refreshTocMutation = useMutation({
    mutationFn: () => reportApi.refreshTableOfContents(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('Table of Contents compiled from document hierarchy (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'TOC refresh failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const refreshFiguresMutation = useMutation({
    mutationFn: () => reportApi.refreshListOfFigures(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('List of Figures compiled from structured figures (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'List of Figures refresh failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const refreshTablesMutation = useMutation({
    mutationFn: () => reportApi.refreshListOfTables(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus('List of Tables compiled from structured tables (0 AI credits consumed).');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'List of Tables refresh failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const repairDeterministicMutation = useMutation({
    mutationFn: () => reportApi.repairDeterministicNodes(reportId),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setSaveStatus(`Repaired ${res.repairedSectionsCount} deterministic nodes from template/metadata (0 AI credits consumed).`);
      setTimeout(() => setSaveStatus(null), 4000);
    },
    onError: (err: any) => {
      setSaveStatus(err?.response?.data?.message || err?.message || 'Repair failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
  });

  const generateSection = useMutation({
    mutationFn: (req?: GenerateSectionRequest) => reportApi.generateSection(String(selectedSection?.id), req),
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['report-sections', chapterId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      setShowAiDraftModal(false);
      setSaveStatus(data?.content ? 'AI draft generated for section.' : 'Section updated.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      const resp = err?.response?.data;
      if (resp?.code === 'INSUFFICIENT_PROJECT_EVIDENCE' || resp?.status === 422) {
        setSaveStatus(resp?.message || 'Generation blocked: Required empirical/project evidence is missing.');
      } else {
        setSaveStatus(resp?.message || err?.message || 'Section generation failed.');
      }
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
  const createAndDownloadExport = async (format: 'DOCX' | 'PDF', draft: boolean, selection?: Record<string, unknown>) => {
    const job = await reportApi.exports(reportId, { format, draft, selection });
    if (String(job.status) === 'COMPLETED' && job.id) {
      const result = await reportApi.downloadExport(String(job.id));
      reportApi.saveBlob(result.blob, result.filename);
    }
    return job;
  };

  const draftDocxExport = useMutation({
    mutationFn: () => createAndDownloadExport('DOCX', true, exportSelectionPayload),
    onSuccess: () => {
      setSaveStatus('Draft DOCX generated and downloaded with authenticated request.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });
  const draftPdfExport = useMutation({
    mutationFn: () => createAndDownloadExport('PDF', true, exportSelectionPayload),
    onSuccess: () => {
      setSaveStatus('Draft PDF generated and downloaded with authenticated request.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
  });

  // Final exports (Enforce validation)
  const finalDocxExport = useMutation({
    mutationFn: () => createAndDownloadExport('DOCX', false),
    onSuccess: () => {
      setSaveStatus('Final DOCX downloaded from saved final document snapshot. Export used 0 AI credits.');
      setTimeout(() => setSaveStatus(null), 4500);
    },
    onError: (err: any) => {
      const resp = err?.response?.data;
      if (resp?.code === 'REPORT_VALIDATION_FAILED' || resp?.status === 422) {
        setValidationErrors(resp?.errors || []);
        setValidationModalOpen(true);
      }
    },
  });
  const finalPdfExport = useMutation({
    mutationFn: () => createAndDownloadExport('PDF', false),
    onSuccess: () => {
      setSaveStatus('Final PDF downloaded from saved final document snapshot. Export used 0 AI credits.');
      setTimeout(() => setSaveStatus(null), 4500);
    },
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
  const workspaceType = workspaceTypeOf(projectQuery.data as any);
  const documentLabel = finalDocumentLabel(workspaceType);
  const isCourseworkDocument = workspaceType === 'COURSEWORK';
  const contentChapter = (structureQuery.data?.chapters ?? []).find((chapter: any) =>
    !['PRELIMINARY', 'REFERENCES', 'APPENDICES'].includes(String(chapter.type))
  ) ?? structureQuery.data?.chapters?.[0];
  const isFinal = (selectedReport as any)?.status === 'FINAL';
  const sectionCap = capabilitiesQuery.data?.sections?.find((s) => s.sectionId === selectedSection?.id);
  const isEmpiricalBlocked = sectionCap && !sectionCap.canGenerate;
  const reportData = selectedReport as any;
  const finalDoc = finalDocQuery.data as any;
  const previewDoc = previewQuery.data as any;
  const exportableChapters = ((structureQuery.data?.chapters ?? []) as any[]).filter((chapter) =>
    !['PRELIMINARY', 'REFERENCES', 'APPENDICES'].includes(String(chapter.type))
  );
  const toggleExportNode = (nodeId: string) => {
    setSelectedExportNodeIds((prev) =>
      prev.includes(nodeId) ? prev.filter((id) => id !== nodeId) : [...prev, nodeId]
    );
  };
  const selectAllExportNodes = () => setSelectedExportNodeIds(exportableChapters.map((chapter) => String(chapter.id)));
  const clearExportNodes = () => setSelectedExportNodeIds([]);
  const valData = validationQuery.data as any;
  const selectedSectionContent = typeof selectedSection?.content === 'string' ? selectedSection.content : '';
  const hasSectionContent = selectedSectionContent.trim().length > 0;
  const nodeCategory = getDocumentNodeCategory(selectedSection, selectedChapter);
  const isNodeDeterministic = isDeterministicNode(nodeCategory);
  const isCorruptedContent = isCorruptedDeterministicContent(selectedSectionContent, nodeCategory);
  const isReferencesSection = nodeCategory === 'REFERENCES';
  const isLiteratureReviewSection = nodeCategory === 'LITERATURE_REVIEW';
  const isTitlePage = nodeCategory === 'TITLE_PAGE';
  const isTableOfContents = nodeCategory === 'TABLE_OF_CONTENTS';
  const isListOfFigures = nodeCategory === 'LIST_OF_FIGURES';
  const isListOfTables = nodeCategory === 'LIST_OF_TABLES';

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectQuery.data?.title || projectId, documentLabel]} />

      {/* Top Header */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <h1 className="page-title" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', margin: 0 }}>
              <FileText className="text-primary" size={26} />
              {documentLabel}
            </h1>
            {isFinal ? (
              <Badge tone="success" style={{ fontSize: '0.8rem' }}>FINALIZED</Badge>
            ) : (
              <Badge tone="warning" style={{ fontSize: '0.8rem' }}>DRAFT / EDITING</Badge>
            )}
          </div>
          <p className="muted" style={{ maxWidth: '750px', marginTop: '0.25rem' }}>
            Shared academic document engine with editable structure, deterministic citations, final snapshots, live A4 preview, and validation governance.
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
            Apply Template Formatting
          </Button>

          <Button
            type="button"
            variant="primary"
            onClick={() => finalize.mutate()}
            disabled={!reportId || finalize.isPending}
          >
            <Check size={15} style={{ marginRight: 4 }} />
            {isFinal && (finalDocQuery.data as any)?.stale ? `Update & Finalize ${documentLabel}` : isFinal ? 'Finalize New Version' : `Finalize ${documentLabel}`}
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

        <button
          type="button"
          onClick={() => setActiveTab('evidence')}
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
            borderBottom: activeTab === 'evidence' ? '3px solid var(--primary)' : '3px solid transparent',
            color: activeTab === 'evidence' ? 'var(--primary)' : 'var(--color-muted)',
            marginBottom: '-5px',
          }}
        >
          <ImageIcon size={16} /> Evidence & Figures
        </button>
      </div>

      {/* TAB 1: STRUCTURE & SECTION EDITOR */}
      {activeTab === 'sections' && (
        <div className="report-builder" style={{ display: 'grid', gridTemplateColumns: '320px 1fr 280px', gap: '1.25rem', alignItems: 'flex-start' }}>
          {/* Left Column: Chapters & Sections Tree */}
          <Card style={{ padding: '0.75rem', height: 'calc(100vh - 220px)', minHeight: '520px', maxHeight: 'calc(100vh - 220px)', display: 'flex', flexDirection: 'column', position: 'sticky', top: '1rem', overflow: 'hidden' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.65rem', paddingBottom: '0.4rem', borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Layers size={16} className="text-primary" />
                <h2 style={{ fontSize: '0.95rem', fontWeight: 600, margin: 0 }}>{documentLabel} Structure</h2>
              </div>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  if (isCourseworkDocument && contentChapter?.id) {
                    setNewSectionTitle('');
                    setNewSectionAiEnabled(true);
                    setShowAddSectionModal({ chapterId: String(contentChapter.id) });
                  } else {
                    setNewChapterTitle('');
                    setNewChapterNumber('');
                    setShowAddChapterModal(true);
                  }
                }}
                style={{ fontSize: '0.75rem', padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}
                title={isCourseworkDocument ? 'Add heading' : 'Add custom chapter'}
                disabled={isCourseworkDocument && !contentChapter?.id}
              >
                <Plus size={13} /> {isCourseworkDocument ? 'Heading' : 'Chapter'}
              </Button>
            </div>

            <div className="report-chapter-tree" style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem', overflowY: 'auto', flex: 1, minHeight: 0, paddingRight: '4px' }}>
              {(structureQuery.data?.chapters ?? []).map((chapter, cIdx, cArr) => {
                const isChapterSelected = chapter.id === selectedChapterId;
                const isExpanded = expandedChapters[String(chapter.id)] ?? true;
                const isDraggingThis = draggedItem?.type === 'chapter' && draggedItem.index === cIdx;
                return (
                  <div
                    key={String(chapter.id)}
                    className="report-chapter-node"
                    draggable={!reorderStructure.isPending}
                    onDragStart={(e) => {
                      e.dataTransfer.setData('text/plain', JSON.stringify({ type: 'chapter', index: cIdx }));
                      setDraggedItem({ type: 'chapter', index: cIdx });
                    }}
                    onDragEnd={() => setDraggedItem(null)}
                    onDragOver={(e) => {
                      if (draggedItem?.type === 'chapter') {
                        e.preventDefault();
                      }
                    }}
                    onDrop={(e) => {
                      if (draggedItem?.type === 'chapter') {
                        e.preventDefault();
                        reorderChaptersToIndex(draggedItem.index, cIdx);
                        setDraggedItem(null);
                      }
                    }}
                    style={{
                      border: isChapterSelected ? '1px solid var(--primary)' : '1px solid var(--border)',
                      borderRadius: '6px',
                      background: isChapterSelected ? 'rgba(var(--primary-rgb, 59, 130, 246), 0.03)' : 'var(--surface)',
                      overflow: 'hidden',
                      opacity: isDraggingThis ? 0.5 : 1,
                      flexShrink: 0,
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
                        onClick={() => toggleChapter(String(chapter.id))}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7, display: 'flex', alignItems: 'center' }}
                        title={isExpanded ? 'Collapse' : 'Expand'}
                      >
                        {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setSelectedChapterId(String(chapter.id));
                          if (!isExpanded) toggleChapter(String(chapter.id));
                        }}
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
                        {isCourseworkDocument
                          ? chapter.title
                          : formatChapterTitle(chapter.chapterNumber, chapter.title)}
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
                            setNewSectionAiEnabled(true);
                            setShowAddSectionModal({ chapterId: String(chapter.id) });
                          }}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.8, color: 'var(--primary)' }}
                          title={isCourseworkDocument ? 'Add Heading' : 'Add Section to this Chapter'}
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
                    {isExpanded && (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', padding: '4px 6px' }}>
                        {(chapter.sections ?? []).map((sec, sIdx, sArr) => {
                          const isSecSelected = String(sec.id) === selectedSectionId;
                          const isDraggingSec = draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === null && draggedItem.index === sIdx;
                          const secHeading = sec.heading || (sec as any).title || 'Untitled Section';
                          const secNumberDisplay = isCourseworkDocument && sec.sectionNumber
                            ? `${sec.sectionNumber}. `
                            : sec.sectionNumber
                              ? `${sec.sectionNumber} `
                              : '';
                          return (
                            <div key={String(sec.id)} style={{ display: 'flex', flexDirection: 'column', gap: '2px', opacity: isDraggingSec ? 0.5 : 1, flexShrink: 0 }}>
                              {/* Section Row */}
                              <div
                                draggable={!reorderStructure.isPending}
                                onDragStart={(e) => {
                                  e.stopPropagation();
                                  e.dataTransfer.setData('text/plain', JSON.stringify({ type: 'section', index: sIdx, chapterId: String(chapter.id), parentId: null }));
                                  setDraggedItem({ type: 'section', index: sIdx, chapterId: String(chapter.id), parentId: null });
                                }}
                                onDragEnd={() => setDraggedItem(null)}
                                onDragOver={(e) => {
                                  if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === null) {
                                    e.preventDefault();
                                    e.stopPropagation();
                                  }
                                }}
                                onDrop={(e) => {
                                  if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === null) {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    reorderSectionsToIndex(chapter.sections, draggedItem.index, sIdx, null);
                                    setDraggedItem(null);
                                  }
                                }}
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
                                  cursor: 'grab',
                                  flexShrink: 0,
                                }}
                              >
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
                                  <span style={{ fontWeight: 600, marginRight: '4px' }}>{secNumberDisplay}</span>
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
                                      setNewSectionAiEnabled(true);
                                      setShowAddSectionModal({ chapterId: String(chapter.id), parentSectionId: String(sec.id), parentTitle: secHeading });
                                    }}
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7, color: 'var(--primary)' }}
                                    title={isCourseworkDocument ? 'Add Subheading' : 'Add Subsection'}
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
                              </div>

                              {/* Nested Subsections (Indented) */}
                              {(sec.subsections ?? []).length > 0 && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', paddingLeft: '14px', borderLeft: '1px solid var(--border)' }}>
                                  {sec.subsections.map((sub, subIdx, subArr) => {
                                    const isSubSelected = String(sub.id) === selectedSectionId;
                                    const subHeading = sub.heading || (sub as any).title || 'Untitled Subsection';
                                    const isDraggingSub = draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sec.id) && draggedItem.index === subIdx;
                                    return (
                                      <div key={String(sub.id)} style={{ opacity: isDraggingSub ? 0.5 : 1 }}>
                                        <div
                                          draggable={!reorderStructure.isPending}
                                          onDragStart={(e) => {
                                            e.stopPropagation();
                                            e.dataTransfer.setData('text/plain', JSON.stringify({ type: 'section', index: subIdx, chapterId: String(chapter.id), parentId: String(sec.id) }));
                                            setDraggedItem({ type: 'section', index: subIdx, chapterId: String(chapter.id), parentId: String(sec.id) });
                                          }}
                                          onDragEnd={() => setDraggedItem(null)}
                                          onDragOver={(e) => {
                                            if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sec.id)) {
                                              e.preventDefault();
                                              e.stopPropagation();
                                            }
                                          }}
                                          onDrop={(e) => {
                                            if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sec.id)) {
                                              e.preventDefault();
                                              e.stopPropagation();
                                              reorderSectionsToIndex(sec.subsections, draggedItem.index, subIdx, String(sec.id));
                                              setDraggedItem(null);
                                            }
                                          }}
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
                                            cursor: 'grab',
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
                                            <span style={{ fontWeight: 600, marginRight: '4px' }}>{sub.sectionNumber ? `${sub.sectionNumber} ` : ''}</span>
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
                                              onClick={() => {
                                                setNewSectionTitle('');
                                                setNewSectionAiEnabled(true);
                                                setShowAddSectionModal({ chapterId: String(chapter.id), parentSectionId: String(sub.id), parentTitle: subHeading });
                                              }}
                                              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7, color: 'var(--primary)' }}
                                              title="Add Nested Subheading"
                                            >
                                              <CornerDownRight size={11} />
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

                                        {(sub.subsections ?? []).length > 0 && (
                                          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', paddingLeft: '14px', borderLeft: '1px solid var(--border)', marginTop: '2px' }}>
                                            {sub.subsections.map((nested: any, nestedIdx: number, nestedArr: any[]) => {
                                              const isNestedSelected = String(nested.id) === selectedSectionId;
                                              const nestedHeading = nested.heading || nested.title || 'Untitled Subheading';
                                              const isDraggingNested = draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sub.id) && draggedItem.index === nestedIdx;
                                              return (
                                                <div
                                                  key={String(nested.id)}
                                                  draggable={!reorderStructure.isPending}
                                                  onDragStart={(e) => {
                                                    e.stopPropagation();
                                                    e.dataTransfer.setData('text/plain', JSON.stringify({ type: 'section', index: nestedIdx, chapterId: String(chapter.id), parentId: String(sub.id) }));
                                                    setDraggedItem({ type: 'section', index: nestedIdx, chapterId: String(chapter.id), parentId: String(sub.id) });
                                                  }}
                                                  onDragEnd={() => setDraggedItem(null)}
                                                  onDragOver={(e) => {
                                                    if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sub.id)) {
                                                      e.preventDefault();
                                                      e.stopPropagation();
                                                    }
                                                  }}
                                                  onDrop={(e) => {
                                                    if (draggedItem?.type === 'section' && draggedItem.chapterId === String(chapter.id) && draggedItem.parentId === String(sub.id)) {
                                                      e.preventDefault();
                                                      e.stopPropagation();
                                                      reorderSectionsToIndex(sub.subsections, draggedItem.index, nestedIdx, String(sub.id));
                                                      setDraggedItem(null);
                                                    }
                                                  }}
                                                  style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    padding: '3px 6px',
                                                    borderRadius: '3px',
                                                    background: isNestedSelected ? 'var(--surface-hover)' : 'transparent',
                                                    border: isNestedSelected ? '1px solid var(--primary)' : '1px solid transparent',
                                                    fontSize: '0.76rem',
                                                    gap: '4px',
                                                    opacity: isDraggingNested ? 0.5 : 1,
                                                    cursor: 'grab',
                                                  }}
                                                >
                                                  <button
                                                    type="button"
                                                    onClick={() => handleSelectSection(String(chapter.id), String(nested.id))}
                                                    style={{
                                                      flex: 1,
                                                      textAlign: 'left',
                                                      background: 'none',
                                                      border: 'none',
                                                      cursor: 'pointer',
                                                      color: isNestedSelected ? 'var(--primary)' : 'inherit',
                                                      fontWeight: isNestedSelected ? 600 : 400,
                                                      overflow: 'hidden',
                                                      textOverflow: 'ellipsis',
                                                      whiteSpace: 'nowrap',
                                                      padding: 0,
                                                    }}
                                                    title={nestedHeading}
                                                  >
                                                    <span style={{ fontWeight: 600, marginRight: '4px' }}>{nested.sectionNumber ? `${nested.sectionNumber} ` : ''}</span>
                                                    {nestedHeading}
                                                  </button>
                                                  <div style={{ display: 'flex', alignItems: 'center', gap: '2px' }}>
                                                    <button
                                                      type="button"
                                                      disabled={nestedIdx === 0 || reorderStructure.isPending}
                                                      onClick={() => moveSection(String(chapter.id), sub.subsections, nestedIdx, 'up', String(sub.id))}
                                                      style={{ background: 'none', border: 'none', cursor: nestedIdx === 0 ? 'default' : 'pointer', padding: '1px', opacity: nestedIdx === 0 ? 0.3 : 0.7 }}
                                                      title="Move Nested Subheading Up"
                                                    >
                                                      <ChevronUp size={10} />
                                                    </button>
                                                    <button
                                                      type="button"
                                                      disabled={nestedIdx === nestedArr.length - 1 || reorderStructure.isPending}
                                                      onClick={() => moveSection(String(chapter.id), sub.subsections, nestedIdx, 'down', String(sub.id))}
                                                      style={{ background: 'none', border: 'none', cursor: nestedIdx === nestedArr.length - 1 ? 'default' : 'pointer', padding: '1px', opacity: nestedIdx === nestedArr.length - 1 ? 0.3 : 0.7 }}
                                                      title="Move Nested Subheading Down"
                                                    >
                                                      <ChevronDown size={10} />
                                                    </button>
                                                    <button
                                                      type="button"
                                                      onClick={() => {
                                                        setRenameTitle(nestedHeading);
                                                        setShowRenameModal({ type: 'section', id: String(nested.id), currentTitle: nestedHeading });
                                                      }}
                                                      style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: 0.7 }}
                                                      title="Rename Nested Subheading"
                                                    >
                                                      <Edit2 size={10} />
                                                    </button>
                                                    <button
                                                      type="button"
                                                      onClick={() => setDeleteConfirmModal({ type: 'section', id: String(nested.id), title: nestedHeading, required: nested.required })}
                                                      style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '1px', opacity: nested.required ? 0.3 : 0.7, color: nested.required ? 'var(--color-muted)' : 'var(--color-danger, #ef4444)' }}
                                                      title={nested.required ? 'Required template section' : 'Delete Nested Subheading'}
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
                      {selectedSection.sectionNumber ? (isCourseworkDocument && !String(selectedSection.sectionNumber).includes('.') ? `${selectedSection.sectionNumber}. ` : `${selectedSection.sectionNumber} `) : ''}
                      {displayValue(selectedSection.heading ?? selectedSection.type)}
                    </h2>
                    <span className="muted" style={{ fontSize: '0.8rem' }}>
                      Chapter: {displayValue(selectedChapter?.title)} • Revision {displayValue(selectedSection.revisionNumber)}
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                    {isTitlePage ? (
                      <>
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={openEditTitlePageModal}
                          style={{ fontSize: '0.82rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 5 }}
                          title="Edit student and project metadata for Title Page"
                        >
                          <Edit2 size={13} />
                          Edit Details
                        </Button>
                        <Button
                          type="button"
                          variant="primary"
                          onClick={() => refreshTitlePageMutation.mutate()}
                          disabled={refreshTitlePageMutation.isPending}
                          style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                          title="Refresh Title Page deterministically from project metadata (0 AI credits)"
                        >
                          <RotateCw size={14} className={refreshTitlePageMutation.isPending ? 'animate-spin' : ''} />
                          {refreshTitlePageMutation.isPending ? 'Formatting...' : 'Refresh Title Page'}
                        </Button>
                      </>
                    ) : isTableOfContents ? (
                      <>
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => {
                            tocMutation.mutate();
                            setShowTocModal(true);
                          }}
                          style={{ fontSize: '0.82rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 5 }}
                          title="View compiled Table of Contents hierarchy"
                        >
                          <ListOrdered size={14} />
                          View Hierarchy
                        </Button>
                        <Button
                          type="button"
                          variant="primary"
                          onClick={() => refreshTocMutation.mutate()}
                          disabled={refreshTocMutation.isPending}
                          style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                          title="Recompile Table of Contents deterministically from document hierarchy (0 AI credits)"
                        >
                          <RotateCw size={14} className={refreshTocMutation.isPending ? 'animate-spin' : ''} />
                          {refreshTocMutation.isPending ? 'Compiling TOC...' : 'Refresh TOC'}
                        </Button>
                      </>
                    ) : isListOfFigures ? (
                      <Button
                        type="button"
                        variant="primary"
                        onClick={() => refreshFiguresMutation.mutate()}
                        disabled={refreshFiguresMutation.isPending}
                        style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                        title="Recompile List of Figures deterministically from embedded figures (0 AI credits)"
                      >
                        <RotateCw size={14} className={refreshFiguresMutation.isPending ? 'animate-spin' : ''} />
                        {refreshFiguresMutation.isPending ? 'Compiling Figures...' : 'Refresh Figures'}
                      </Button>
                    ) : isListOfTables ? (
                      <Button
                        type="button"
                        variant="primary"
                        onClick={() => refreshTablesMutation.mutate()}
                        disabled={refreshTablesMutation.isPending}
                        style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                        title="Recompile List of Tables deterministically from embedded tables (0 AI credits)"
                      >
                        <RotateCw size={14} className={refreshTablesMutation.isPending ? 'animate-spin' : ''} />
                        {refreshTablesMutation.isPending ? 'Compiling Tables...' : 'Refresh Tables'}
                      </Button>
                    ) : isReferencesSection ? (
                      <Button
                        type="button"
                        variant="primary"
                        onClick={() => refreshReferences.mutate()}
                        disabled={refreshReferences.isPending}
                        style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                        title="Recompile references deterministically from citations and project references (0 AI credits)"
                      >
                        <RotateCw size={14} className={refreshReferences.isPending ? 'animate-spin' : ''} />
                        {refreshReferences.isPending ? 'Compiling References...' : 'Refresh References'}
                      </Button>
                    ) : (nodeCategory === 'DECLARATION' || nodeCategory === 'CERTIFICATION') ? (
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => generateSection.mutate({
                          targetNodeId: String(selectedSection.id),
                          targetNodeTitle: selectedSection.heading ? String(selectedSection.heading) : selectedSection.type ? String(selectedSection.type) : undefined,
                          applyMode: 'REPLACE',
                        })}
                        disabled={generateSection.isPending}
                        style={{ fontSize: '0.82rem', padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
                        title="Reset standard declaration wording from institutional template (0 AI credits)"
                      >
                        <RotateCw size={14} className={generateSection.isPending ? 'animate-spin' : ''} />
                        {generateSection.isPending ? 'Restoring...' : 'Reset from Template'}
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
                          onClick={() => {
                            setAiDraftConfirmReplace(false);
                            setAiDraftInstructions('');
                            setAiDraftApplyMode(hasSectionContent ? 'APPEND' : 'REPLACE');
                            setShowAiDraftModal(true);
                          }}
                          disabled={generateSection.isPending || Boolean(isEmpiricalBlocked)}
                          style={{ fontSize: '0.82rem', padding: '4px 10px', display: 'flex', alignItems: 'center', gap: 4 }}
                          title="Generate section draft tailored to section purpose and context"
                        >
                          <Sparkles size={14} className="text-primary" />
                          {generateSection.isPending ? 'Generating...' : 'AI Draft'}
                        </Button>
                      </>
                    )}
                  </div>
                </div>

                {/* Corrupted Legacy AI Content Repair Banner */}
                {isCorruptedContent && (
                  <div className="alert danger" style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10, padding: '10px 14px', border: '1px solid #ef4444', borderRadius: '6px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <AlertTriangle size={18} className="text-danger" style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Legacy AI Content Detected:</strong> This deterministic section contains inappropriate literature-synthesis prose generated by a legacy routing bug. Restore it to clean deterministic template formatting now. (0 AI credits consumed, user edits preserved in history)
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="primary"
                      onClick={() => {
                        if (isTitlePage) refreshTitlePageMutation.mutate();
                        else if (isTableOfContents) refreshTocMutation.mutate();
                        else if (isListOfFigures) refreshFiguresMutation.mutate();
                        else if (isListOfTables) refreshTablesMutation.mutate();
                        else if (isReferencesSection) refreshReferences.mutate();
                        else repairDeterministicMutation.mutate();
                      }}
                      disabled={refreshTitlePageMutation.isPending || refreshTocMutation.isPending || refreshFiguresMutation.isPending || refreshTablesMutation.isPending || refreshReferences.isPending || repairDeterministicMutation.isPending}
                      style={{ fontSize: '0.8rem', whiteSpace: 'nowrap' }}
                    >
                      Repair & Restore
                    </Button>
                  </div>
                )}

                {/* Deterministic Section Info Banners */}
                {isTitlePage && !isCorruptedContent && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px' }}>
                    <FileText size={16} className="text-primary" style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Deterministic Title Page:</strong> Formatted according to institutional template standards from your project metadata. (0 AI credits consumed)
                    </div>
                  </div>
                )}

                {isTableOfContents && !isCorruptedContent && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px' }}>
                    <ListOrdered size={16} className="text-primary" style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Deterministic Table of Contents:</strong> Automatically structured from your document's chapter, section, and subsection hierarchy. (0 AI credits consumed)
                    </div>
                  </div>
                )}

                {isListOfFigures && !isCorruptedContent && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px' }}>
                    <Layers size={16} className="text-primary" style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Deterministic List of Figures:</strong> Automatically extracted from embedded figures and captions across all chapters. (0 AI credits consumed)
                    </div>
                  </div>
                )}

                {isListOfTables && !isCorruptedContent && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px' }}>
                    <Table size={16} className="text-primary" style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Deterministic List of Tables:</strong> Automatically extracted from embedded Markdown tables across all chapters. (0 AI credits consumed)
                    </div>
                  </div>
                )}

                {/* References Specific Banner */}
                {isReferencesSection && (
                  <div className="alert info" style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10, padding: '8px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <BookOpen size={16} className="text-primary" style={{ flexShrink: 0 }} />
                      <div>
                        <strong>Deterministic Academic Bibliography:</strong> Compiled directly from citations embedded in your report and project references formatted in {currentCitationStyle}. (0 AI credits consumed)
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                      <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                        <input
                          type="checkbox"
                          checked={Boolean(structureQuery.data?.includeUncitedReferences)}
                          onChange={(e) => {
                            updateReportSettings.mutate({ includeUncitedReferences: e.target.checked });
                          }}
                        />
                        Include uncited project references
                      </label>
                      <Button type="button" variant="secondary" className="btn-compact" onClick={() => window.location.assign(`/app/projects/${projectId}/references`)}>
                        Review Metadata
                      </Button>
                    </div>
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
                  sectionId={String(selectedSection.id)}
                  citationStyle={currentCitationStyle}
                  readOnly={isReferencesSection}
                  minHeight="540px"
                  onSave={({ contentJson, plainText, markdown }) => {
                    if (isReferencesSection) return;
                    return saveSection.mutateAsync({
                      content: markdown,
                      contentJson,
                      plainText,
                    }).then(() => undefined);
                  }}
                />

                {generateSection.data && (
                  <DraftPreview
                    draft={generateSection.data}
                    onAccept={(text) => {
                      saveSection.mutateAsync({ content: text }).then(() => {
                        generateSection.reset();
                      });
                    }}
                    onAppend={(text) => {
                      const existing = String(selectedSection?.content || '');
                      const separator = existing.trim() ? '\n\n' : '';
                      saveSection.mutateAsync({ content: existing + separator + text }).then(() => {
                        generateSection.reset();
                      });
                    }}
                    onDismiss={() => generateSection.reset()}
                  />
                )}
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
                  <span className="muted">Purpose:</span>{' '}
                  <Badge tone={isNodeDeterministic ? 'success' : 'info'}>
                    {displayValue(selectedSection.semanticPurpose || nodeCategory)}
                  </Badge>
                </div>
                <div>
                  <span className="muted">Policy:</span>{' '}
                  <Badge tone={selectedSection.generationPolicy === 'DETERMINISTIC' || isNodeDeterministic ? 'success' : undefined}>
                    {displayValue(selectedSection.generationPolicy || (isNodeDeterministic ? 'DETERMINISTIC' : 'CONTEXTUAL_AI'))}
                  </Badge>
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
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '4px', marginTop: '6px' }}>
                      <Button type="button" variant="secondary" onClick={() => window.location.assign(`/app/projects/${projectId}/references`)} style={{ fontSize: '0.75rem', width: '100%', padding: '3px' }}>
                        Review Metadata
                      </Button>
                      <Button type="button" variant="secondary" onClick={() => exportProjectReferences.mutate('RIS')} disabled={exportProjectReferences.isPending} style={{ fontSize: '0.75rem', width: '100%', padding: '3px' }}>
                        Export RIS
                      </Button>
                      <Button type="button" variant="secondary" onClick={() => exportProjectReferences.mutate('BIBTEX')} disabled={exportProjectReferences.isPending} style={{ fontSize: '0.75rem', width: '100%', padding: '3px' }}>
                        Export BibTeX
                      </Button>
                      <Button type="button" variant="secondary" onClick={() => exportProjectReferences.mutate('ENDNOTE_XML')} disabled={exportProjectReferences.isPending} style={{ fontSize: '0.75rem', width: '100%', padding: '3px' }}>
                        Export EndNote XML
                      </Button>
                    </div>
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
                Compile the full document from saved chapters, sections, figures, tables, citations, references, and appendices into a single editable version.
              </p>
              <Button
                type="button"
                variant="primary"
                onClick={() => prepareFinalDoc.mutate()}
                disabled={prepareFinalDoc.isPending}
              >
                <RefreshCw size={15} style={{ marginRight: 6 }} />
                {prepareFinalDoc.isPending ? 'Preparing...' : 'Prepare Final Document'}
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
                      <strong>Final document is out of date:</strong> one or more report sections have changed. Your previous final version will be preserved.
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
                    {prepareFinalDoc.isPending ? 'Updating...' : 'Update Final Document'}
                  </Button>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
                    {String(finalDoc.title || `Complete ${documentLabel} Version`)}
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
                  <RefreshCw size={14} style={{ marginRight: 4 }} /> Update Final Document
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
                  return updateFinalDoc.mutateAsync({
                    contentJson,
                    plainText,
                    title: String(finalDoc.title || ''),
                  }).then(() => undefined);
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
                  <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0 }}>A4 Paginated {documentLabel} Preview</h2>
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
                    {String(reportData?.title || documentLabel)}
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

                {/* Dynamic Academic Table of Contents */}
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
                  <h2 style={{ textAlign: 'center', fontWeight: 'bold', marginBottom: '1.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '1.25rem' }}>
                    Table of Contents
                  </h2>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                    {((structureQuery.data?.chapters ?? chapters.data ?? []) as any[]).map((ch) => {
                      const isPreliminary = String(ch.type) === 'PRELIMINARY';
                      const isReferences = String(ch.type) === 'REFERENCES';
                      const isAppendices = String(ch.type) === 'APPENDICES';
                      const chapterTitle = ch.chapterNumber && !isCourseworkDocument && !isPreliminary && !isReferences && !isAppendices
                        ? `Chapter ${ch.chapterNumber}: ${ch.title}`
                        : ch.title;
                      return (
                        <div key={String(ch.id)} style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <div style={{ display: 'flex', alignItems: 'baseline', fontWeight: 'bold', fontSize: '0.98rem' }}>
                            <span style={{ textTransform: isCourseworkDocument ? 'none' : 'uppercase' }}>{chapterTitle}</span>
                            <span style={{ flexGrow: 1, borderBottom: '1px dotted #64748b', margin: '0 8px 4px' }} />
                            <span style={{ fontSize: '0.88rem', color: '#475569' }}>
                              {ch.chapterNumber ? `Ch. ${ch.chapterNumber}` : ''}
                            </span>
                          </div>
                          {(ch.sections ?? []).map((sec: any) => (
                            <div key={String(sec.id)} style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                              <div style={{ display: 'flex', alignItems: 'baseline', paddingLeft: '1.25rem', fontSize: '0.92rem' }}>
                                <span>{sec.sectionNumber ? `${sec.sectionNumber} ` : ''}{sec.heading || sec.title}</span>
                                <span style={{ flexGrow: 1, borderBottom: '1px dotted #94a3b8', margin: '0 8px 4px' }} />
                                <span style={{ fontSize: '0.82rem', color: '#64748b' }}>
                                  {sec.sectionNumber ? `§ ${sec.sectionNumber}` : ''}
                                </span>
                              </div>
                              {(sec.subsections ?? []).map((sub: any) => (
                                <div key={String(sub.id)} style={{ display: 'flex', flexDirection: 'column', gap: '0.15rem' }}>
                                  <div style={{ display: 'flex', alignItems: 'baseline', paddingLeft: '2.5rem', fontSize: '0.86rem', color: '#334155' }}>
                                    <span>{sub.sectionNumber ? `${sub.sectionNumber} ` : ''}{sub.heading || sub.title}</span>
                                    <span style={{ flexGrow: 1, borderBottom: '1px dotted #cbd5e1', margin: '0 8px 4px' }} />
                                    <span style={{ fontSize: '0.78rem', color: '#94a3b8' }}>
                                      {sub.sectionNumber ? `§ ${sub.sectionNumber}` : ''}
                                    </span>
                                  </div>
                                  {(sub.subsections ?? []).map((nested: any) => (
                                    <div key={String(nested.id)} style={{ display: 'flex', alignItems: 'baseline', paddingLeft: '3.75rem', fontSize: '0.82rem', color: '#475569' }}>
                                      <span>{nested.sectionNumber ? `${nested.sectionNumber} ` : ''}{nested.heading || nested.title}</span>
                                      <span style={{ flexGrow: 1, borderBottom: '1px dotted #e2e8f0', margin: '0 8px 4px' }} />
                                      <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                                        {nested.sectionNumber ? `§ ${nested.sectionNumber}` : ''}
                                      </span>
                                    </div>
                                  ))}
                                </div>
                              ))}
                            </div>
                          ))}
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Body Content Preview */}
                {previewDoc ? (
                  <ReportRichEditor
                    key={`preview-${exportScopeMode}-${selectedExportNodeIds.join('-')}-${previewDoc.sourceSectionCount || 0}`}
                    content={String(previewDoc.plainText || '')}
                    contentJson={String(previewDoc.contentJson || '')}
                    projectId={projectId}
                    citationStyle={currentCitationStyle}
                    readOnly
                    minHeight="680px"
                  />
                ) : (
                  <div
                    className="preview-page"
                    style={{
                      width: '100%',
                      maxWidth: '750px',
                      minHeight: '320px',
                      background: '#fff',
                      color: '#1e293b',
                      padding: '3rem',
                      boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
                      fontFamily: '"Times New Roman", Times, serif',
                      lineHeight: 1.7,
                      textAlign: 'center',
                    }}
                  >
                    <h2 style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>Select content to preview</h2>
                    <p className="muted">Selected preview compiles from the latest saved sections and does not alter the master report.</p>
                  </div>
                )}
              </div>
            </Card>
          </div>

          {/* Right Column: Validation Status & Downloads */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <Card style={{ padding: '1.25rem' }}>
              <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 0.75rem' }}>Document Scope</h2>
              <label style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
                <input type="radio" checked={exportScopeMode === 'FULL'} onChange={() => setExportScopeMode('FULL')} />
                <span>Entire Project Report</span>
              </label>
              <label style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 10 }}>
                <input type="radio" checked={exportScopeMode === 'SELECTED'} onChange={() => setExportScopeMode('SELECTED')} />
                <span>Selected Content</span>
              </label>

              {exportScopeMode === 'SELECTED' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button type="button" variant="secondary" onClick={selectAllExportNodes} style={{ fontSize: '0.78rem', padding: '3px 8px' }}>Select All</Button>
                    <Button type="button" variant="secondary" onClick={clearExportNodes} style={{ fontSize: '0.78rem', padding: '3px 8px' }}>Clear</Button>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 220, overflowY: 'auto' }}>
                    {exportableChapters.map((chapter) => {
                      const id = String(chapter.id);
                      const label = chapter.chapterNumber ? `Chapter ${chapter.chapterNumber} - ${chapter.title}` : String(chapter.title);
                      return (
                        <label key={id} style={{ display: 'flex', gap: 8, alignItems: 'flex-start', fontSize: '0.86rem' }}>
                          <input type="checkbox" checked={selectedExportNodeIds.includes(id)} onChange={() => toggleExportNode(id)} />
                          <span>{label}</span>
                        </label>
                      );
                    })}
                  </div>
                  <div style={{ borderTop: '1px solid var(--border)', paddingTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportCoverPage} onChange={(e) => setIncludeExportCoverPage(e.target.checked)} /> Cover Page
                    </label>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportToc} onChange={(e) => setIncludeExportToc(e.target.checked)} /> Table of Contents
                    </label>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportListOfFigures} onChange={(e) => setIncludeExportListOfFigures(e.target.checked)} /> List of Figures
                    </label>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportListOfTables} onChange={(e) => setIncludeExportListOfTables(e.target.checked)} /> List of Tables
                    </label>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportAppendices} onChange={(e) => setIncludeExportAppendices(e.target.checked)} /> Appendices
                    </label>
                    <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: '0.84rem' }}>
                      <input type="checkbox" checked={includeExportReferences} onChange={(e) => setIncludeExportReferences(e.target.checked)} /> References
                    </label>
                    <Select value={exportReferenceMode} onChange={(e) => setExportReferenceMode(e.target.value as any)} disabled={!includeExportReferences}>
                      <option value="CITED_IN_SELECTION">Cited in selection</option>
                      <option value="ALL_PROJECT_REFERENCES">All project references</option>
                      <option value="NONE">None</option>
                    </Select>
                  </div>
                  <Button type="button" variant="primary" onClick={() => previewQuery.refetch()} disabled={selectedExportNodeIds.length === 0 || previewQuery.isFetching}>
                    <Eye size={14} style={{ marginRight: 4 }} /> Preview Selected
                  </Button>
                </div>
              )}
            </Card>
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
                  disabled={!reportId || draftDocxExport.isPending || (exportScopeMode === 'SELECTED' && selectedExportNodeIds.length === 0)}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> {exportScopeMode === 'SELECTED' ? 'Download Selected DOCX' : 'Download Draft DOCX'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => draftPdfExport.mutate()}
                  disabled={!reportId || draftPdfExport.isPending || (exportScopeMode === 'SELECTED' && selectedExportNodeIds.length === 0)}
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                >
                  <Download size={14} /> {exportScopeMode === 'SELECTED' ? 'Download Selected PDF' : 'Download Draft PDF'}
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

      {/* TAB 4: EVIDENCE & FIGURES */}
      {activeTab === 'evidence' && (
        <div style={{ maxWidth: '1280px', margin: '0 auto', width: '100%' }}>
          <ProjectEvidencePanel
            projectId={projectId}
            reportId={reportId}
            onInsertToEditor={() => {
              setActiveTab('sections');
            }}
          />
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
                      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', fontWeight: 700 }}>
                        <span style={{ textTransform: 'uppercase' }}>
                          CHAPTER {chNumber}: {chTitle}
                        </span>
                        <span style={{ flexGrow: 1, borderBottom: '1px dotted var(--text-muted, #888)', margin: '0 8px 4px' }} />
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Ch. {chNumber}</span>
                      </div>
                      {(ch.sections || []).map((sec: any, sIdx: number) => {
                        const secHeading = sec.heading || sec.title || `Section ${sIdx + 1}`;
                        const secNum = sec.sectionNumber || `${chNumber}.${sIdx + 1}`;
                        return (
                          <div key={sec.sectionId || sIdx} style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', paddingLeft: '1.25rem', color: 'var(--foreground)' }}>
                              <span>{secNum} {secHeading}</span>
                              <span style={{ flexGrow: 1, borderBottom: '1px dotted var(--border)', margin: '0 8px 4px' }} />
                              <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>§ {secNum}</span>
                            </div>
                            {(sec.subsections || sec.children || []).map((sub: any, subIdx: number) => {
                              const subHeading = sub.heading || sub.title || `Subsection ${subIdx + 1}`;
                              const subNum = sub.sectionNumber || `${secNum}.${subIdx + 1}`;
                              return (
                                <div key={sub.sectionId || subIdx} style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', paddingLeft: '2.5rem', color: 'var(--text-muted)', fontSize: '0.82rem' }}>
                                  <span>{subNum} {subHeading}</span>
                                  <span style={{ flexGrow: 1, borderBottom: '1px dotted var(--border)', margin: '0 8px 4px' }} />
                                  <span style={{ fontSize: '0.75rem' }}>§ {subNum}</span>
                                </div>
                              );
                            })}
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
              placeholder="e.g. System Deployment"
              autoFocus
            />
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>Chapter Number (Optional)</label>
            <Input
              type="number"
              min={1}
              value={newChapterNumber}
              onChange={(e) => setNewChapterNumber(e.target.value)}
              placeholder="Auto"
            />
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowAddChapterModal(false)}>Cancel</Button>
            <Button
              type="button"
              variant="primary"
              disabled={!newChapterTitle.trim() || createChapter.isPending}
              onClick={() => createChapter.mutate({
                title: newChapterTitle.trim(),
                chapterNumber: newChapterNumber ? Number(newChapterNumber) : undefined,
              })}
            >
              {createChapter.isPending ? 'Creating...' : 'Create Chapter'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* Add Section / Subsection Modal */}
      <Modal
        title={showAddSectionModal?.parentSectionId
          ? `Add Subheading to "${showAddSectionModal.parentTitle || 'Heading'}"`
          : isCourseworkDocument ? 'Add Heading' : 'Add Section to Chapter'}
        open={Boolean(showAddSectionModal)}
        onClose={() => setShowAddSectionModal(null)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '380px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>
              {showAddSectionModal?.parentSectionId ? 'Subheading' : isCourseworkDocument ? 'Heading' : 'Section Heading'}
            </label>
            <Input
              value={newSectionTitle}
              onChange={(e) => setNewSectionTitle(e.target.value)}
              placeholder={showAddSectionModal?.parentSectionId ? 'e.g. Concept of Digital Agriculture' : isCourseworkDocument ? 'e.g. Literature Review' : 'e.g. Theoretical Framework'}
              autoFocus
            />
          </div>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.85rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={newSectionAiEnabled}
              onChange={(e) => setNewSectionAiEnabled(e.target.checked)}
            />
            AI assistance enabled for this custom section
          </label>
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
                    aiEnabled: newSectionAiEnabled,
                  });
                }
              }}
            >
              {createSection.isPending ? 'Adding...' : showAddSectionModal?.parentSectionId ? 'Add Subheading' : isCourseworkDocument ? 'Add Heading' : 'Add Section'}
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
            <AssistantResponse content={litMatrixQuery.data.markdownTable} ariaLabel="Literature evidence matrix" />
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

      {/* AI Section Draft Assistant Modal */}
      <Modal
        title="AI Section Draft Assistant"
        open={showAiDraftModal}
        onClose={() => setShowAiDraftModal(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '450px', maxWidth: '600px' }}>
          {/* Target Section Information Card */}
          <div style={{ padding: '0.75rem', background: 'var(--surface-hover)', borderRadius: '6px', border: '1px solid var(--border)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-muted)' }}>TARGET DESTINATION</span>
              <div style={{ display: 'flex', gap: '4px' }}>
                <Badge tone="info">{displayValue(selectedSection?.semanticPurpose || nodeCategory)}</Badge>
                <Badge>{displayValue(selectedSection?.generationPolicy || 'CONTEXTUAL_AI')}</Badge>
              </div>
            </div>
            <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--foreground)' }}>
              {selectedChapter?.title ? `${selectedChapter.title} → ` : ''}
              {selectedSection?.sectionNumber ? `${selectedSection.sectionNumber} ` : ''}
              {displayValue(selectedSection?.heading || selectedSection?.type)}
            </div>
            <p className="muted" style={{ fontSize: '0.78rem', margin: '4px 0 0' }}>
              {selectedSection?.generationPolicy === 'SOURCE_GROUNDED_AI'
                ? 'Comprehensive academic literature synthesis grounded in indexed project sources.'
                : selectedSection?.generationPolicy === 'PROJECT_EVIDENCE_REQUIRED'
                  ? 'Grounded in verified project datasets and analysis runs. Results are never fabricated.'
                  : selectedSection?.generationPolicy === 'USER_AUTHORED_ASSISTED'
                    ? 'Assists with phrasing and tone using your supplied personal context and project metadata.'
                    : 'Context-aware generation adhering strictly to chapter hierarchy and project design.'}
            </p>
          </div>

          {/* Source Scope Selection (only if not user-authored or non-source) */}
          {selectedSection?.generationPolicy !== 'USER_AUTHORED_ASSISTED' && (
            <div>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>Source Retrieval Scope</label>
              <Select
                value={aiDraftSourceScope}
                onChange={(e) => setAiDraftSourceScope(e.target.value as any)}
                style={{ fontSize: '0.85rem', width: '100%' }}
              >
                <option value="ALL_PROJECT_DOCUMENTS">All Indexed Project Sources</option>
                <option value="SELECTED_DOCUMENTS">Selected Project Sources Only</option>
                <option value="NONE">No Literature Sources (Project Metadata Only)</option>
              </Select>
            </div>
          )}

          {/* Document Picker if SELECTED_DOCUMENTS is chosen */}
          {aiDraftSourceScope === 'SELECTED_DOCUMENTS' && (
            <div style={{ maxHeight: '160px', overflowY: 'auto', border: '1px solid var(--border)', borderRadius: '6px', padding: '6px' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-muted)', display: 'block', marginBottom: '4px' }}>Select Sources to Synthesize:</span>
              {(pageContent(projectDocumentsQuery.data) ?? []).map((doc: any) => {
                const isChecked = aiDraftSelectedDocIds.includes(String(doc.id));
                return (
                  <label key={String(doc.id)} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8rem', padding: '3px 0', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setAiDraftSelectedDocIds((prev) => [...prev, String(doc.id)]);
                        } else {
                          setAiDraftSelectedDocIds((prev) => prev.filter((id) => id !== String(doc.id)));
                        }
                      }}
                    />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {doc.filename || doc.title || 'Untitled Document'}
                    </span>
                  </label>
                );
              })}
              {(!pageContent(projectDocumentsQuery.data) || pageContent(projectDocumentsQuery.data).length === 0) && (
                <span className="muted" style={{ fontSize: '0.78rem' }}>No documents uploaded yet.</span>
              )}
            </div>
          )}

          {/* Custom Instructions */}
          <div>
            <label style={{ fontSize: '0.85rem', fontWeight: 600, display: 'block', marginBottom: '6px' }}>
              Researcher Instructions (Optional)
            </label>
            <textarea
              value={aiDraftInstructions}
              onChange={(e) => setAiDraftInstructions(e.target.value)}
              placeholder="e.g. Focus on modern deep learning architectures, emphasize methodology limitations, or specify tone..."
              rows={3}
              style={{
                width: '100%',
                padding: '8px',
                borderRadius: '6px',
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--foreground)',
                fontSize: '0.85rem',
                fontFamily: 'inherit',
                resize: 'vertical',
              }}
            />
          </div>

          {/* Existing Content Notice */}
          {hasSectionContent && (
            <div className="alert warning" style={{ fontSize: '0.8rem', padding: '8px 10px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertTriangle size={15} style={{ flexShrink: 0 }} />
              <div>
                This section already contains content (~{Number((selectedSection as any)?.wordCount) || selectedSectionContent.split(/\s+/).length} words). Choose whether to preview first, append to the end, or replace.
              </div>
            </div>
          )}

          {/* Replace Confirmation Prompt if user selected REPLACE on existing content */}
          {aiDraftApplyMode === 'REPLACE' && hasSectionContent && !aiDraftConfirmReplace && (
            <div style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid var(--color-danger, #ef4444)', borderRadius: '6px', padding: '8px 10px', fontSize: '0.82rem' }}>
              <span style={{ fontWeight: 600, color: 'var(--color-danger, #ef4444)' }}>Confirmation Required:</span>
              <p style={{ margin: '2px 0 6px' }}>Replacing existing content will overwrite the editor text (prior revisions remain saved in history).</p>
              <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontWeight: 600 }}>
                <input
                  type="checkbox"
                  checked={aiDraftConfirmReplace}
                  onChange={(e) => setAiDraftConfirmReplace(e.target.checked)}
                />
                I confirm I want to replace existing section content
              </label>
            </div>
          )}

          {/* Modal Actions */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowAiDraftModal(false)}>
              Cancel
            </Button>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Button
                type="button"
                variant="secondary"
                disabled={generateSection.isPending}
                onClick={() => {
                  generateSection.mutate({
                    targetNodeId: String(selectedSection?.id),
                    targetNodeTitle: selectedSection?.heading ? String(selectedSection.heading) : selectedSection?.type ? String(selectedSection.type) : undefined,
                    sourceScope: aiDraftSourceScope,
                    documentIds: aiDraftSourceScope === 'SELECTED_DOCUMENTS' ? aiDraftSelectedDocIds : undefined,
                    instructions: aiDraftInstructions.trim() || undefined,
                    applyMode: 'PREVIEW',
                  });
                }}
              >
                Generate Preview
              </Button>
              {hasSectionContent && (
                <Button
                  type="button"
                  variant="secondary"
                  disabled={generateSection.isPending}
                  onClick={() => {
                    generateSection.mutate({
                      targetNodeId: String(selectedSection?.id),
                      targetNodeTitle: selectedSection?.heading ? String(selectedSection.heading) : selectedSection?.type ? String(selectedSection.type) : undefined,
                      sourceScope: aiDraftSourceScope,
                      documentIds: aiDraftSourceScope === 'SELECTED_DOCUMENTS' ? aiDraftSelectedDocIds : undefined,
                      instructions: aiDraftInstructions.trim() || undefined,
                      applyMode: 'APPEND',
                    });
                  }}
                >
                  Append Draft
                </Button>
              )}
              <Button
                type="button"
                variant="primary"
                disabled={generateSection.isPending || (hasSectionContent && aiDraftApplyMode === 'REPLACE' && !aiDraftConfirmReplace)}
                onClick={() => {
                  if (hasSectionContent && !aiDraftConfirmReplace) {
                    setAiDraftApplyMode('REPLACE');
                    return;
                  }
                  generateSection.mutate({
                    targetNodeId: String(selectedSection?.id),
                    targetNodeTitle: selectedSection?.heading ? String(selectedSection.heading) : selectedSection?.type ? String(selectedSection.type) : undefined,
                    sourceScope: aiDraftSourceScope,
                    documentIds: aiDraftSourceScope === 'SELECTED_DOCUMENTS' ? aiDraftSelectedDocIds : undefined,
                    instructions: aiDraftInstructions.trim() || undefined,
                    applyMode: 'REPLACE',
                  });
                }}
              >
                {generateSection.isPending ? 'Generating...' : hasSectionContent ? 'Replace & Save' : 'Generate & Save'}
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      {/* Edit Title Page Details Modal */}
      <Modal
        title="Edit Title Page Metadata"
        open={showEditTitlePageModal}
        onClose={() => setShowEditTitlePageModal(false)}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', minWidth: '450px', maxWidth: '600px' }}>
          <p className="muted" style={{ fontSize: '0.82rem', margin: 0 }}>
            Updates the project metadata and re-renders the Title Page deterministically using institutional template formatting. (0 AI credits consumed)
          </p>

          <div>
            <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Project Title</label>
            <Input
              value={titlePageForm.title || ''}
              onChange={(e) => setTitlePageForm((p) => ({ ...p, title: e.target.value }))}
              placeholder="e.g. Design and Implementation of an AI Research Assistant"
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Student / Author Name</label>
              <Input
                value={titlePageForm.authorName || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, authorName: e.target.value }))}
                placeholder="e.g. Jane Doe"
              />
            </div>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Student / Index ID</label>
              <Input
                value={titlePageForm.studentId || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, studentId: e.target.value }))}
                placeholder="e.g. 07210001"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Institution</label>
              <Input
                value={titlePageForm.institutionName || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, institutionName: e.target.value }))}
                placeholder="e.g. Takoradi Technical University"
              />
            </div>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Department</label>
              <Input
                value={titlePageForm.departmentName || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, departmentName: e.target.value }))}
                placeholder="e.g. Computer Science Department"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Degree / Programme</label>
              <Input
                value={titlePageForm.degreeProgram || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, degreeProgram: e.target.value }))}
                placeholder="e.g. B.Tech Computer Science"
              />
            </div>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Supervisor</label>
              <Input
                value={titlePageForm.supervisorName || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, supervisorName: e.target.value }))}
                placeholder="e.g. Dr. K. Mensah"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Academic Year</label>
              <Input
                value={titlePageForm.academicYear || ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, academicYear: e.target.value }))}
                placeholder="e.g. 2024 / 2025"
              />
            </div>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>Submission Year (Number)</label>
              <Input
                type="number"
                value={titlePageForm.submissionYear ? String(titlePageForm.submissionYear) : ''}
                onChange={(e) => setTitlePageForm((p) => ({ ...p, submissionYear: Number(e.target.value) || new Date().getFullYear() }))}
                placeholder="e.g. 2025"
              />
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
            <Button type="button" variant="secondary" onClick={() => setShowEditTitlePageModal(false)}>
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              disabled={updateTitlePageDetailsMutation.isPending}
              onClick={() => updateTitlePageDetailsMutation.mutate(titlePageForm)}
            >
              {updateTitlePageDetailsMutation.isPending ? 'Updating...' : 'Save & Re-render Title Page'}
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
      const apaAuthors = rawAuthor.length === 1
        ? rawAuthor[0]
        : rawAuthor.length === 2
          ? `${rawAuthor[0]} & ${rawAuthor[1]}`
          : `${rawAuthor.slice(0, -1).join(', ')}, & ${rawAuthor[rawAuthor.length - 1]}`;
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
      const apaAuthors = rawAuthor.length === 1
        ? rawAuthor[0]
        : rawAuthor.length === 2
          ? `${rawAuthor[0]} & ${rawAuthor[1]}`
          : `${rawAuthor.slice(0, -1).join(', ')}, & ${rawAuthor[rawAuthor.length - 1]}`;
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
        let familyName: string;
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

function DraftPreview({
  draft,
  onAccept,
  onAppend,
  onDismiss,
}: {
  draft: Record<string, unknown>;
  onAccept?: (text: string) => void;
  onAppend?: (text: string) => void;
  onDismiss?: () => void;
}) {
  const content = String(draft.content ?? draft.draftText ?? draft.text ?? '');
  return (
    <div className="panel" style={{ marginTop: '1rem', border: '1px solid var(--border)', borderRadius: '8px', padding: '1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <Badge tone="info">AI Generated Preview Draft</Badge>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {onAppend && (
            <Button type="button" variant="secondary" onClick={() => onAppend(content)} style={{ fontSize: '0.8rem', padding: '4px 10px' }}>
              Append to Section
            </Button>
          )}
          {onAccept && (
            <Button type="button" variant="primary" onClick={() => onAccept(content)} style={{ fontSize: '0.8rem', padding: '4px 10px' }}>
              Accept & Replace Section
            </Button>
          )}
          {onDismiss && (
            <Button type="button" variant="secondary" onClick={onDismiss} style={{ fontSize: '0.8rem', padding: '4px 10px' }}>
              Dismiss
            </Button>
          )}
        </div>
      </div>
      <AssistantResponse content={content} />
    </div>
  );
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
