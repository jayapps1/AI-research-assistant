import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  ChevronRight,
  Copy,
  Download,
  Layers,
  MessageSquare,
  Plus,
  RefreshCw,
  Save,
  Send,
  Sparkles,
  Table,
  Trash2,
} from 'lucide-react';
import {
  billingApi,
  datasetApi,
  documentApi,
  projectApi,
  ragApi,
  reportApi,
} from '../api/endpoints';
import {
  Breadcrumbs,
  Button,
  Card,
  Field,
  Input,
  Textarea,
  Badge,
  Drawer,
  Modal,
} from '../components/ui';
import { EmptyState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { pageContent } from '../utils/collections';
import type { Citation, DocumentItem, RagAnswer } from '../types/api';

const SECTION_OPTIONS = [
  { key: 'LITERATURE_REVIEW', label: 'Literature Review', desc: 'Thematic synthesis, comparative analysis, and research gaps across uploaded sources', recommended: true, requiresData: false },
  { key: 'PROBLEM_STATEMENT', label: 'Problem Statement', desc: 'Formulate the empirical gap, background context, and problem magnitude', recommended: false, requiresData: false },
  { key: 'BACKGROUND', label: 'Background of the Study', desc: 'Broader scholarly and contextual foundation for the investigation', recommended: false, requiresData: false },
  { key: 'RESEARCH_GAP', label: 'Research Gap', desc: 'Synthesized omissions, conflicting findings, and methodological limitations in existing literature', recommended: false, requiresData: false },
  { key: 'OBJECTIVES', label: 'General & Specific Objectives', desc: 'Hierarchical research aims derived from the study topic and problem', recommended: false, requiresData: false },
  { key: 'RESEARCH_QUESTIONS', label: 'Research Questions', desc: 'Empirically answerable research questions aligned with study aims', recommended: false, requiresData: false },
  { key: 'HYPOTHESES', label: 'Hypotheses', desc: 'Directional or null hypotheses grounded in theoretical literature', recommended: false, requiresData: false },
  { key: 'CONCEPTUAL_FRAMEWORK', label: 'Conceptual Framework', desc: 'Key constructs, variables, and hypothesized interrelationships', recommended: false, requiresData: false },
  { key: 'THEORETICAL_FRAMEWORK', label: 'Theoretical Framework', desc: 'Grounding theoretical paradigms and explanatory models', recommended: false, requiresData: false },
  { key: 'METHODOLOGY', label: 'Methodology Draft (Proposal)', desc: 'Research design, paradigm, population, and sampling strategy proposal for user review', recommended: false, requiresData: false },
  { key: 'POPULATION_SAMPLING', label: 'Population & Sampling Draft', desc: 'Sampling frame, sample size determination, and selection criteria proposal', recommended: false, requiresData: false },
  { key: 'DATA_COLLECTION_METHOD', label: 'Data Collection Method Draft', desc: 'Procedures for primary or secondary empirical data collection', recommended: false, requiresData: false },
  { key: 'RESEARCH_INSTRUMENT', label: 'Research Instrument Draft', desc: 'Structured questionnaire or interview guide draft based on literature', recommended: false, requiresData: false },
  { key: 'FINDINGS', label: 'Findings (Requires Dataset)', desc: 'Empirical results — cannot be synthesized without uploaded datasets or results data', recommended: false, requiresData: true, badge: 'DATA REQUIRED' },
  { key: 'DISCUSSION', label: 'Discussion Draft', desc: 'Interpretation of empirical findings contextualized against literature', recommended: false, requiresData: true, badge: 'FINDINGS REQUIRED' },
  { key: 'CONCLUSION', label: 'Conclusion Draft', desc: 'Synthesized conclusions directly addressing research objectives', recommended: false, requiresData: true, badge: 'FINDINGS REQUIRED' },
  { key: 'RECOMMENDATIONS', label: 'Recommendations Draft', desc: 'Actionable policy, practical, and future research recommendations', recommended: false, requiresData: true, badge: 'FINDINGS REQUIRED' },
  { key: 'CUSTOM', label: 'Custom Section', desc: 'Define your own section title and specific generation prompt', recommended: false, requiresData: false },
] as const;

const ANALYZE_OPTIONS = [
  { key: 'LITERATURE_MATRIX', label: 'Literature Matrix (Structured Table)', desc: 'Generate a comprehensive comparison matrix table across Author, Year, Title, Purpose, Theory, Methodology, Population, Sample, Data Collection, Analysis, Findings, Limitations, Gap, and Relevance.', isTable: true },
  { key: 'PAPER_SUMMARY', label: 'Paper Summary', desc: 'In-depth academic summary of individual or selected papers highlighting contributions and key methods.' },
  { key: 'COMPARE_PAPERS', label: 'Compare Papers', desc: 'Side-by-side comparison of objectives, theoretical foundations, methodologies, and findings.' },
  { key: 'RESEARCH_GAP', label: 'Research Gap Analysis', desc: 'Comprehensive identification of empirical, theoretical, and methodological gaps across selected sources.' },
  { key: 'METHODOLOGY_COMPARISON', label: 'Methodology Comparison', desc: 'Detailed comparative analysis of research designs, sampling strategies, instruments, and analytical tools.' },
  { key: 'FINDINGS_COMPARISON', label: 'Findings Comparison', desc: 'Synthesis of empirical results across studies highlighting consensus, divergence, and conflicting evidence.' },
  { key: 'THEORY_ANALYSIS', label: 'Theory & Conceptual Analysis', desc: 'Examination of theoretical frameworks, constructs, and underlying theoretical assumptions.' },
  { key: 'THEMES', label: 'Theme Extraction & Synthesis', desc: 'Identification of recurring patterns, core themes, and emergent categories across sources.' },
  { key: 'LIMITATIONS', label: 'Limitations & Validity Analysis', desc: 'Critical review of self-reported limitations, internal/external validity threats, and sample constraints.' },
  { key: 'POPULATION_SAMPLE', label: 'Population & Sample Comparison', desc: 'Comparative breakdown of target populations, sample sizes, sampling techniques, and demographic representation.' },
  { key: 'CUSTOM_ANALYSIS', label: 'Custom Analysis', desc: 'Specify custom analytical criteria or comparative questions to evaluate the selected literature.' },
] as const;

const SUGGESTED_QUESTIONS = [
  'What are the major findings across these uploaded papers?',
  'What research gaps do these studies identify?',
  'Which authors discuss social media addiction or usage patterns?',
  'Compare the methodologies used across these studies.',
  'Summarize the relationship between social media use and academic performance.',
];

function aiUserMessage(code: string | null | undefined, fallback: string) {
  if (code === 'AI_PROVIDER_REQUEST_INVALID') {
    return 'The AI provider rejected this request configuration. Please try again shortly or contact support if it continues.';
  }
  return fallback;
}

export function ResearchAssistantPage() {
  const projectId = useProjectId();
  const { selectedWorkspaceId: workspaceId } = useWorkspace();
  const [searchParams, setSearchParams] = useSearchParams();
  const client = useQueryClient();

  // Mode: 'ask' | 'generate' | 'analyze'
  const mode = (searchParams.get('mode') as 'ask' | 'generate' | 'analyze') || 'generate';
  const initialSection = searchParams.get('section') || 'LITERATURE_REVIEW';

  const [conversationId, setConversationId] = useState('');
  const [scopeType, setScopeType] = useState<'PROJECT_ALL_DOCUMENTS' | 'SELECTED_DOCUMENTS'>('PROJECT_ALL_DOCUMENTS');
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [activeCitation, setActiveCitation] = useState<Citation | null>(null);
  const [showBuyCredits, setShowBuyCredits] = useState(false);

  // Ask Mode State
  const [askAnswer, setAskAnswer] = useState<RagAnswer | null>(null);

  // Analyze Mode State
  const [selectedAnalysis, setSelectedAnalysis] = useState<string>('LITERATURE_MATRIX');
  const [customAnalysisPrompt, setCustomAnalysisPrompt] = useState('');
  const [analysisResult, setAnalysisResult] = useState<RagAnswer | null>(null);

  // Generate Mode State
  const [selectedSection, setSelectedSection] = useState<string>(initialSection);
  const [customSectionTitle, setCustomSectionTitle] = useState('');
  const [customInstructions, setCustomInstructions] = useState('');
  const [generatedDraft, setGeneratedDraft] = useState<string>('');
  const [draftCitations, setDraftCitations] = useState<Citation[]>([]);
  const [isEditingDraft, setIsEditingDraft] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);

  // Queries
  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  const documentsQuery = useQuery({
    queryKey: ['documents', projectId],
    queryFn: () => documentApi.list(projectId),
    enabled: Boolean(projectId),
    refetchInterval: (query) => {
      const docs = (pageContent(query.state.data) as DocumentItem[]) ?? [];
      const hasProcessing = docs.some((d) => d.status === 'PROCESSING' || d.status === 'UPLOADING');
      return hasProcessing ? 3000 : false;
    },
  });

  const datasetsQuery = useQuery({
    queryKey: ['datasets', projectId],
    queryFn: () => datasetApi.list(projectId),
    enabled: Boolean(projectId),
  });
  const hasDatasets = ((pageContent(datasetsQuery.data) as any[]) ?? []).length > 0;

  const conversationsQuery = useQuery({
    queryKey: ['rag-conversations', projectId],
    queryFn: () => ragApi.conversations(projectId),
    enabled: Boolean(projectId),
  });

  const creditsQuery = useQuery({
    queryKey: ['billing', workspaceId, 'ai-credits'],
    queryFn: () => billingApi.aiCredits(workspaceId),
    enabled: Boolean(workspaceId),
  });

  const activeConversationId = conversationId || conversationsQuery.data?.[0]?.id || '';

  const createConversation = useMutation({
    mutationFn: () => ragApi.createConversation(projectId, { title: 'Research Session' }),
    onSuccess: (data) => {
      setConversationId(data.id);
      client.invalidateQueries({ queryKey: ['rag-conversations', projectId] });
    },
  });

  // Ask Mutation
  const askMutation = useMutation({
    mutationFn: async (question: string) => {
      let targetConvId = activeConversationId;
      if (!targetConvId) {
        const newConv = await ragApi.createConversation(projectId, { title: 'Research Session' });
        targetConvId = newConv.id;
        setConversationId(newConv.id);
      }
      return ragApi.ask(targetConvId, {
        question,
        scopeType,
        documentIds: scopeType === 'SELECTED_DOCUMENTS' ? selectedDocuments : undefined,
        evidenceLimit: 8,
      });
    },
    onSuccess: (data) => {
      setAskAnswer(data);
      creditsQuery.refetch();
    },
  });

  // Analyze Mutation
  const analyzeMutation = useMutation({
    mutationFn: async () => {
      let targetConvId = activeConversationId;
      if (!targetConvId) {
        const newConv = await ragApi.createConversation(projectId, { title: `Analyze: ${selectedAnalysis}` });
        targetConvId = newConv.id;
        setConversationId(newConv.id);
      }

      const project = projectQuery.data;

      let prompt = `You are an expert academic research assistant analyzing research literature for the project "${project?.title ?? ''}".\n`;
      if (project?.researchAim) prompt += `Research Aim: ${project.researchAim}\n`;
      if (project?.researchType) prompt += `Research Type: ${project.researchType}\n`;

      if (selectedAnalysis === 'LITERATURE_MATRIX') {
        prompt += `\nTASK: Generate a comprehensive Literature Matrix table in Markdown format comparing the selected papers.\n`;
        prompt += `Use the following exact Markdown table headers:\n`;
        prompt += `| Source Code | Author(s) & Year | Title | Purpose / Objectives | Theoretical Framework | Methodology & Design | Population & Sample | Data Collection & Analysis | Major Findings | Limitations | Research Gap | Relevance to Project |\n`;
        prompt += `| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n`;
        prompt += `For each selected paper, fill out every column with verified details extracted directly from the text.\n`;
        prompt += `Follow the table with a 2-paragraph thematic synthesis highlighting cross-cutting patterns and critical gaps.\n`;
      } else if (selectedAnalysis === 'PAPER_SUMMARY') {
        prompt += `\nTASK: Provide a rigorous academic summary of the selected paper(s), covering: 1) Core research questions/aims, 2) Theoretical framework, 3) Methodology and sample, 4) Major empirical findings, 5) Limitations, and 6) Significance.\n`;
      } else if (selectedAnalysis === 'COMPARE_PAPERS') {
        prompt += `\nTASK: Compare the selected papers across: Aims, Conceptual frameworks, Methodologies, Empirical findings, and Scholarly conclusions. Highlight key areas of convergence and divergence.\n`;
      } else if (selectedAnalysis === 'RESEARCH_GAP') {
        prompt += `\nTASK: Synthesize the empirical, methodological, theoretical, and contextual research gaps identified across the selected literature. Formulate how the current research project addresses these gaps.\n`;
      } else if (selectedAnalysis === 'METHODOLOGY_COMPARISON') {
        prompt += `\nTASK: Conduct a comparative analysis of the methodologies employed across the selected studies (paradigms, research designs, sampling strategies, instruments, and statistical/thematic analysis techniques).\n`;
      } else if (selectedAnalysis === 'FINDINGS_COMPARISON') {
        prompt += `\nTASK: Compare and contrast the empirical findings across the selected studies. Identify points of consensus, contradictions, and unresolved questions in the scholarly debate.\n`;
      } else if (selectedAnalysis === 'THEORY_ANALYSIS') {
        prompt += `\nTASK: Analyze the theoretical foundations and conceptual frameworks present in the selected literature. Evaluate how constructs are defined and operationalized.\n`;
      } else if (selectedAnalysis === 'THEMES') {
        prompt += `\nTASK: Extract and synthesize the overarching themes and emergent categories across the selected papers, providing evidence citations for each theme.\n`;
      } else if (selectedAnalysis === 'LIMITATIONS') {
        prompt += `\nTASK: Critically review the methodological, sampling, and analytical limitations acknowledged across the selected studies, and explain how future research or the current study can mitigate them.\n`;
      } else if (selectedAnalysis === 'POPULATION_SAMPLE') {
        prompt += `\nTASK: Compare the target populations, sampling frames, sample sizes, and sampling strategies across the selected papers. Note any demographic gaps or generalization limits.\n`;
      } else if (selectedAnalysis === 'CUSTOM_ANALYSIS') {
        prompt += `\nTASK: ${customAnalysisPrompt || 'Analyze the selected literature based on the project research objectives.'}\n`;
      }

      if (customAnalysisPrompt.trim() && selectedAnalysis !== 'CUSTOM_ANALYSIS') {
        prompt += `\nADDITIONAL INSTRUCTIONS: ${customAnalysisPrompt.trim()}\n`;
      }

      return ragApi.ask(targetConvId, {
        question: prompt,
        scopeType,
        documentIds: scopeType === 'SELECTED_DOCUMENTS' ? selectedDocuments : undefined,
        evidenceLimit: 15,
      });
    },
    onSuccess: (data) => {
      setAnalysisResult(data);
      creditsQuery.refetch();
    },
  });

  // Generate Section Mutation
  const generateMutation = useMutation({
    mutationFn: async () => {
      const sectionMeta = SECTION_OPTIONS.find((s) => s.key === selectedSection);
      if (sectionMeta?.requiresData && !hasDatasets) {
        throw new Error(
          selectedSection === 'FINDINGS'
            ? 'Dataset or analysis results required. Please upload or import a dataset in Data Analysis first.'
            : 'Findings and empirical results required. Cannot synthesize section without real research results.'
        );
      }

      let activeConvId = activeConversationId;
      if (!activeConvId) {
        const newConv = await ragApi.createConversation(projectId, {
          title: `Generate ${selectedSection}`,
        });
        activeConvId = newConv.id;
        setConversationId(newConv.id);
      }

      const project = projectQuery.data;
      const sectionName = selectedSection === 'CUSTOM' ? (customSectionTitle || 'Custom Section') : sectionMeta?.label;

      let prompt = `Generate a structured, evidence-grounded academic draft for the section: "${sectionName}".\n`;
      prompt += `Project Research Topic: "${project?.title ?? ''}"\n`;
      if (project?.description) {
        prompt += `Project Description: "${project.description}"\n`;
      }
      if (project?.researchAim) {
        prompt += `Research Aim: "${project.researchAim}"\n`;
      }
      if (project?.studyArea) {
        prompt += `Study Area: "${project.studyArea}"\n`;
      }
      if (project?.researchType) {
        prompt += `Research Type: "${project.researchType}"\n`;
      }

      if (selectedSection === 'LITERATURE_REVIEW') {
        prompt += `\nLITERATURE REVIEW SPECIFIC INSTRUCTIONS:\n`;
        prompt += `- Synthesize the uploaded research sources into thematic subsections (e.g. Introduction, Thematic Findings, Comparative Evidence, Methodological Patterns, Research Gaps, and Summary).\n`;
        prompt += `- Adapt the thematic structure to the specific topic rather than using generic headings.\n`;
        prompt += `- Compare and contrast studies, showing areas of scholarly agreement and disagreement.\n`;
        prompt += `- Do NOT write an article-by-article summary ("Author A said... Author B said...").\n`;
        prompt += `- Cite every source-dependent claim with evidence references.\n`;
      } else if (selectedSection === 'PROBLEM_STATEMENT') {
        prompt += `\nPROBLEM STATEMENT INSTRUCTIONS:\n`;
        prompt += `- Articulate the background context, the identified empirical/theoretical gap from the uploaded literature, the magnitude/relevance of the problem, and why research is needed.\n`;
        prompt += `- Do not invent unsupported statistics.\n`;
      } else if (selectedSection === 'BACKGROUND') {
        prompt += `\nBACKGROUND OF THE STUDY INSTRUCTIONS:\n`;
        prompt += `- Establish the scholarly and contextual foundation using the uploaded sources.\n`;
        prompt += `- Cite all source-derived factual assertions. Do not invent historical or statistical claims.\n`;
      } else if (selectedSection === 'RESEARCH_GAP') {
        prompt += `\nRESEARCH GAP INSTRUCTIONS:\n`;
        prompt += `- Identify only research gaps reasonably supported by the uploaded literature (empirical, methodological, population, geographic, theoretical, contextual, or contradictory findings).\n`;
        prompt += `- Do not manufacture gaps not supported by the literature.\n`;
      } else if (selectedSection === 'OBJECTIVES') {
        prompt += `\nOBJECTIVES INSTRUCTIONS:\n`;
        prompt += `- Formulate one primary General Objective and 3-5 Specific Objectives derived from the study topic, problem statement, and literature context.\n`;
        prompt += `- Do not fabricate evidence.\n`;
      } else if (selectedSection === 'RESEARCH_QUESTIONS') {
        prompt += `\nRESEARCH QUESTIONS INSTRUCTIONS:\n`;
        prompt += `- Derive empirically answerable research questions directly from the topic, problem, and objectives.\n`;
        prompt += `- Ensure clear one-to-one alignment between each specific objective and research question.\n`;
      } else if (selectedSection === 'HYPOTHESES') {
        prompt += `\nHYPOTHESES INSTRUCTIONS:\n`;
        prompt += `- Only generate hypotheses where the research context and uploaded literature support testable variables and relationships.\n`;
        prompt += `- If the study is qualitative or exploratory, state clearly that hypotheses are Not Applicable.\n`;
      } else if (selectedSection === 'CONCEPTUAL_FRAMEWORK') {
        prompt += `\nCONCEPTUAL FRAMEWORK INSTRUCTIONS:\n`;
        prompt += `- Map key constructs, variables (independent, dependent, moderating/mediating), and hypothesized relationships grounded in the literature.\n`;
        prompt += `- Do not invent validated causal relationships.\n`;
      } else if (selectedSection === 'THEORETICAL_FRAMEWORK') {
        prompt += `\nTHEORETICAL FRAMEWORK INSTRUCTIONS:\n`;
        prompt += `- Synthesize theoretical paradigms and explanatory models explicitly found in the uploaded literature.\n`;
        prompt += `- Do not invent theory citations. If literature is insufficient, state that more theoretical sources are required.\n`;
      } else if (selectedSection === 'METHODOLOGY') {
        prompt += `\nMETHODOLOGY PROPOSAL INSTRUCTIONS:\n`;
        prompt += `- Classify this draft clearly as a 'PROPOSED METHODOLOGY DRAFT'.\n`;
        prompt += `- Propose research design, paradigm, target population, sampling technique, and data collection approach based on study aims and literature patterns.\n`;
        prompt += `- State clearly that this proposal requires researcher review; never present proposed methods as completed fieldwork.\n`;
      } else if (selectedSection === 'POPULATION_SAMPLING') {
        prompt += `\nPOPULATION & SAMPLING INSTRUCTIONS:\n`;
        prompt += `- Propose target population considerations, sampling techniques, and sample-size determination approaches based on literature conventions.\n`;
        prompt += `- Do NOT fabricate actual population numbers, sample sizes, or participant counts.\n`;
      } else if (selectedSection === 'DATA_COLLECTION_METHOD') {
        prompt += `\nDATA COLLECTION METHODS INSTRUCTIONS:\n`;
        prompt += `- Propose primary/secondary data collection procedures based on objectives and research design.\n`;
        prompt += `- Clearly distinguish proposed methods from completed fieldwork.\n`;
      } else if (selectedSection === 'RESEARCH_INSTRUMENT') {
        prompt += `\nRESEARCH INSTRUMENT INSTRUCTIONS:\n`;
        prompt += `- Draft sample questionnaire items, interview guides, or observation protocols aligned with research questions.\n`;
        prompt += `- Do not claim instrument validation or pilot testing has occurred.\n`;
      } else if (selectedSection === 'CUSTOM') {
        prompt += `\nCUSTOM SECTION INSTRUCTIONS:\n`;
        prompt += `- Generate the section "${customSectionTitle || 'Custom Section'}" following the user's specific instructions below.\n`;
      }

      if (customInstructions.trim()) {
        prompt += `\nADDITIONAL RESEARCHER INSTRUCTIONS:\n${customInstructions.trim()}\n`;
      }

      prompt += `\nProvide an academic, publication-grade draft with comprehensive coverage and verified citations to the provided sources.`;

      const result = await ragApi.ask(activeConvId, {
        question: prompt,
        scopeType,
        documentIds: scopeType === 'SELECTED_DOCUMENTS' ? selectedDocuments : undefined,
        evidenceLimit: 12,
      });

      return result;
    },
    onSuccess: (data) => {
      setGeneratedDraft(data.answer ?? '');
      setDraftCitations(data.citations ?? data.evidence ?? []);
      setIsEditingDraft(true);
      setSaveStatus(null);
      creditsQuery.refetch();
    },
  });

  // Save draft to project report sections / writing workspace
  const saveDraftMutation = useMutation({
    mutationFn: async () => {
      // Find or create default report
      const reports = await reportApi.reports(projectId);
      let reportId: string;
      const reportList = pageContent(reports);
      if (reportList.length > 0) {
        reportId = reportList[0].id as string;
      } else {
        const createdReport = await reportApi.createReport(projectId, {
          title: projectQuery.data?.title ?? 'Research Report',
          type: 'RESEARCH_REPORT',
          citationStyle: 'APA_7',
        });
        reportId = createdReport.id as string;
      }

      // Find chapters and appropriate section
      const chapters = await reportApi.chapters(reportId);
      const sectionMeta = SECTION_OPTIONS.find((s) => s.key === selectedSection);
      const heading = selectedSection === 'CUSTOM' ? (customSectionTitle || 'Custom Section') : sectionMeta?.label;

      let targetChapter = chapters.find((ch: any) => {
        if (selectedSection === 'LITERATURE_REVIEW') return ch.type === 'LITERATURE_REVIEW';
        if (selectedSection === 'PROBLEM_STATEMENT' || selectedSection === 'BACKGROUND' || selectedSection === 'OBJECTIVES') return ch.type === 'INTRODUCTION';
        if (selectedSection === 'METHODOLOGY' || selectedSection === 'POPULATION_SAMPLING') return ch.type === 'METHODOLOGY';
        if (selectedSection === 'FINDINGS') return ch.type === 'RESULTS';
        if (selectedSection === 'DISCUSSION' || selectedSection === 'CONCLUSION' || selectedSection === 'RECOMMENDATIONS') return ch.type === 'DISCUSSION';
        return false;
      }) ?? chapters[0];

      if (!targetChapter) {
        // Create chapter if none exists
        targetChapter = await reportApi.createChapter(reportId, {
          title: 'Research Sections',
          type: 'CUSTOM',
          displayOrder: 1,
        });
      }

      // Check existing sections in chapter
      const sections = await reportApi.sections(targetChapter.id as string);
      const existingSection = sections.find((s: any) => s.heading === heading || s.type === selectedSection);

      if (existingSection) {
        await reportApi.updateSection(existingSection.id as string, {
          content: generatedDraft,
          origin: 'AI_GENERATED',
        });
      } else {
        await reportApi.createSection(targetChapter.id as string, {
          heading,
          type: selectedSection === 'CUSTOM' ? 'CUSTOM' : selectedSection,
          content: generatedDraft,
          displayOrder: sections.length + 1,
          origin: 'AI_GENERATED',
        });
      }

      // Also update project description/aim if problem statement was generated
      if (selectedSection === 'PROBLEM_STATEMENT' && projectQuery.data) {
        await projectApi.update(projectId, {
          description: generatedDraft.slice(0, 4900),
        });
      }

      return true;
    },
    onSuccess: () => {
      setSaveStatus('Saved to Project Writing Workspace!');
      client.invalidateQueries({ queryKey: ['project-dashboard', projectId] });
      client.invalidateQueries({ queryKey: ['reports', projectId] });
    },
  });

  const allDocuments = (pageContent(documentsQuery.data) as DocumentItem[]) ?? [];
  const readyDocuments = allDocuments.filter((d) => d.status === 'READY');
  const notReadyDocuments = allDocuments.filter((d) => d.status !== 'READY');

  const totalCredits = creditsQuery.data ? Number(creditsQuery.data.totalAvailable) : 0;
  const hasCredits = totalCredits > 0;
  const isGenerating = generateMutation.isPending;

  // Evaluate blockers for generation
  let generateBlocker: string | null = null;
  const currentSectionMeta = SECTION_OPTIONS.find((s) => s.key === selectedSection);
  if (!projectId || !projectQuery.data) {
    generateBlocker = 'Please open an active research project.';
  } else if (currentSectionMeta?.requiresData && !hasDatasets) {
    generateBlocker = selectedSection === 'FINDINGS'
      ? 'Dataset or analysis results required. Please upload or import a dataset in Data Analysis.'
      : 'Findings and empirical results required. Cannot synthesize section without real research results.';
  } else if (allDocuments.length === 0) {
    generateBlocker = 'No research sources uploaded yet. Please upload research papers in Sources.';
  } else if (readyDocuments.length === 0) {
    const hasProcessing = allDocuments.some((d) => d.status === 'PROCESSING' || d.status === 'UPLOADING');
    if (hasProcessing) {
      generateBlocker = 'Uploaded sources are currently being processed. Please wait for indexing to finish.';
    } else {
      generateBlocker = 'No ready sources available. Please retry document processing in Sources.';
    }
  } else if (!hasCredits) {
    generateBlocker = 'Workspace has 0 AI Credits. Please top up or request an allocation to generate.';
  }

  const canGenerate = !generateBlocker && !isGenerating;


  const handleModeChange = (newMode: 'ask' | 'generate' | 'analyze') => {
    setSearchParams({ mode: newMode, section: selectedSection });
  };

  const handleSectionChange = (sectionKey: string) => {
    setSelectedSection(sectionKey);
    setSearchParams({ mode: 'generate', section: sectionKey });
    setSaveStatus(null);
  };

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', 'Research Assistant']} />

      {/* Header Banner */}
      <div className="page-header" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <h1 className="page-title" style={{ fontSize: '1.65rem', fontWeight: 700, margin: 0 }}>
              AI Research Assistant
            </h1>
            <Badge tone="info">Source-Grounded</Badge>
          </div>
          <p className="muted" style={{ fontSize: '0.92rem', marginTop: 4 }}>
            Ask questions about your uploaded research sources or generate grounded sections of your study with verified citations.
          </p>
        </div>

        {/* AI Credits & Top Up */}
        {workspaceId ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--surface-hover)', padding: '6px 14px', borderRadius: 8, border: '1px solid var(--border)' }}>
            <span className="muted" style={{ fontSize: '0.82rem' }}>Credits:</span>
            <strong style={{ fontSize: '0.95rem' }}>
              {creditsQuery.data ? Number(creditsQuery.data.totalAvailable).toLocaleString(undefined, { maximumFractionDigits: 1 }) : '—'}
            </strong>
            <Button
              type="button"
              variant="secondary"
              style={{ fontSize: '0.78rem', padding: '2px 8px' }}
              onClick={() => setShowBuyCredits(true)}
            >
              Top Up
            </Button>
          </div>
        ) : null}
      </div>

      {/* Source Scope Selector Card */}
      <Card style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12, marginBottom: 12 }}>
          <strong style={{ fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Layers size={16} /> Source Evidence Scope
          </strong>
          <span className="muted" style={{ fontSize: '0.8rem' }}>
            {readyDocuments.length} ready sources available
          </span>
        </div>

        <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginBottom: 12 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.9rem' }}>
            <input
              type="radio"
              name="scopeType"
              value="PROJECT_ALL_DOCUMENTS"
              checked={scopeType === 'PROJECT_ALL_DOCUMENTS'}
              onChange={() => setScopeType('PROJECT_ALL_DOCUMENTS')}
            />
            <span>All ready sources ({readyDocuments.length} papers)</span>
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.9rem' }}>
            <input
              type="radio"
              name="scopeType"
              value="SELECTED_DOCUMENTS"
              checked={scopeType === 'SELECTED_DOCUMENTS'}
              onChange={() => setScopeType('SELECTED_DOCUMENTS')}
            />
            <span>Selected sources only</span>
          </label>
        </div>

        {scopeType === 'SELECTED_DOCUMENTS' && (
          <div style={{ marginTop: 12, padding: 12, borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface-hover)' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: 8 }}>
              Choose specific sources to include in evidence:
            </div>
            {readyDocuments.length === 0 ? (
              <p className="muted" style={{ fontSize: '0.85rem' }}>
                No ready sources available. Please upload sources and wait for processing.
              </p>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 8 }}>
                {readyDocuments.map((doc) => {
                  const code = doc.documentCode ?? doc.docCode ?? `DOC-${doc.documentNumber}`;
                  const isChecked = selectedDocuments.includes(doc.id);
                  return (
                    <label key={doc.id} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.85rem' }}>
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setSelectedDocuments((prev) => [...prev, doc.id]);
                          } else {
                            setSelectedDocuments((prev) => prev.filter((id) => id !== doc.id));
                          }
                        }}
                      />
                      <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{code}</span>
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {doc.title || doc.currentVersion?.originalFilename || 'Untitled'}
                      </span>
                    </label>
                  );
                })}
              </div>
            )}

            {notReadyDocuments.length > 0 && (
              <div style={{ marginTop: 12, borderTop: '1px solid var(--border)', paddingTop: 8 }}>
                <span className="muted" style={{ fontSize: '0.78rem' }}>Unprocessed sources (not selectable):</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 4 }}>
                  {notReadyDocuments.map((doc) => (
                    <span key={doc.id} className="muted" style={{ fontSize: '0.78rem' }}>
                      • {doc.documentCode ?? doc.docCode ?? 'DOC'} · {doc.title || doc.filename} —{' '}
                      <em>{doc.status === 'PROCESSING' ? 'Processing — not ready for AI yet.' : doc.status}</em>
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </Card>

      {/* Mode Switcher Tabs */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 20, borderBottom: '1px solid var(--border)', paddingBottom: 8 }}>
        <button
          type="button"
          className={`button ${mode === 'generate' ? 'primary' : 'secondary'}`}
          onClick={() => handleModeChange('generate')}
          style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}
        >
          <Sparkles size={16} /> GENERATE MODE
        </button>
        <button
          type="button"
          className={`button ${mode === 'ask' ? 'primary' : 'secondary'}`}
          onClick={() => handleModeChange('ask')}
          style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}
        >
          <MessageSquare size={16} /> ASK MODE
        </button>
        <button
          type="button"
          className={`button ${mode === 'analyze' ? 'primary' : 'secondary'}`}
          onClick={() => handleModeChange('analyze')}
          style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600 }}
        >
          <Table size={16} /> ANALYZE MODE
        </button>
      </div>

      {/* ========================================================================= */}
      {/* 1. GENERATE MODE */}
      {/* ========================================================================= */}
      {mode === 'generate' && (
        <div className="grid cols-2" style={{ gap: 24, alignItems: 'flex-start' }}>
          {/* Left Column: Section Selector & Settings */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: 12 }}>
                Choose What to Generate
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 360, overflowY: 'auto' }}>
                {SECTION_OPTIONS.map((section) => {
                  const isSelected = selectedSection === section.key;
                  return (
                    <div
                      key={section.key}
                      onClick={() => handleSectionChange(section.key)}
                      style={{
                        padding: '10px 14px',
                        borderRadius: 8,
                        border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border)',
                        background: isSelected ? 'var(--surface-hover)' : 'transparent',
                        cursor: 'pointer',
                        transition: 'all 150ms ease',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <strong style={{ fontSize: '0.92rem' }}>{section.label}</strong>
                        {section.recommended && <Badge tone="success">Recommended</Badge>}
                        {'badge' in section && section.badge && (
                          <Badge tone={hasDatasets ? 'info' : 'warning'}>{section.badge}</Badge>
                        )}
                      </div>
                      <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.8rem', lineHeight: 1.35 }}>
                        {section.desc}
                      </p>
                    </div>
                  );
                })}
              </div>

              {selectedSection === 'CUSTOM' && (
                <div style={{ marginTop: 14 }}>
                  <Field label="Custom Section Title">
                    <Input
                      placeholder="e.g. Theoretical Implications"
                      value={customSectionTitle}
                      onChange={(e) => setCustomSectionTitle(e.target.value)}
                    />
                  </Field>
                </div>
              )}

              {/* Domain Safety Callout */}
              {selectedSection === 'FINDINGS' && (
                <div className="alert warning" style={{ marginTop: 14, fontSize: '0.82rem' }}>
                  <AlertTriangle size={15} style={{ marginRight: 6 }} />
                  <strong>Domain Safety Rule:</strong> AI cannot invent empirical findings. Findings require an uploaded dataset, analysis run, or empirical results tables.
                </div>
              )}

              {selectedSection === 'LITERATURE_REVIEW' && (
                <div className="alert info" style={{ marginTop: 14, fontSize: '0.82rem' }}>
                  <BookOpen size={15} style={{ marginRight: 6 }} />
                  <strong>Thematic Synthesis:</strong> Generates a structured academic literature review comparing methodologies, agreements, and research gaps across your uploaded sources.
                </div>
              )}

              {/* Custom Instructions */}
              <div style={{ marginTop: 16 }}>
                <Field label="Custom Instructions (Optional)">
                  <Textarea
                    rows={3}
                    placeholder="e.g., Focus on Ghanaian university studies; Use thematic organization; Compare quantitative vs qualitative findings; Write approx. 1,500 words."
                    value={customInstructions}
                    onChange={(e) => setCustomInstructions(e.target.value)}
                  />
                </Field>
              </div>

              {/* Generate Action Button */}
              <div style={{ marginTop: 16 }}>
                <Button
                  type="button"
                  variant="primary"
                  disabled={!canGenerate}
                  onClick={() => generateMutation.mutate()}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, padding: '10px' }}
                >
                  {isGenerating ? (
                    <>
                      <RefreshCw size={16} className="spin" />
                      Retrieving evidence & generating {SECTION_OPTIONS.find((s) => s.key === selectedSection)?.label}...
                    </>
                  ) : (
                    <>
                      <Sparkles size={16} />
                      Generate {SECTION_OPTIONS.find((s) => s.key === selectedSection)?.label} Draft
                    </>
                  )}
                </Button>
                {generateBlocker && (
                  <div
                    className="alert warning"
                    style={{
                      marginTop: 8,
                      fontSize: '0.8rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 6,
                      padding: '8px 12px',
                    }}
                  >
                    <AlertTriangle size={14} style={{ flexShrink: 0 }} />
                    <span>{generateBlocker}</span>
                  </div>
                )}
              </div>

            </Card>
          </div>

          {/* Right Column: AI Draft Workspace */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <div>
                  <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
                    AI Draft Workspace
                  </h2>
                  <span className="muted" style={{ fontSize: '0.82rem' }}>
                    {SECTION_OPTIONS.find((s) => s.key === selectedSection)?.label} • {draftCitations.length} citations verified
                  </span>
                </div>
                {saveStatus ? (
                  <Badge tone="success">{saveStatus}</Badge>
                ) : isEditingDraft ? (
                  <Badge tone="info">Editable Draft</Badge>
                ) : null}
              </div>

              {generateMutation.isError && (() => {
                const err = generateMutation.error as any;
                const code = err?.response?.data?.errorCode || err?.response?.data?.code || 'AI_GENERATION_FAILED';
                const msg = err?.response?.data?.message || err?.message || 'Generation failed.';
                const displayMessage = aiUserMessage(code, msg);
                return (
                  <div className="alert danger" style={{ marginBottom: 14, display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Badge tone="danger">{code}</Badge>
                      <Button
                        type="button"
                        variant="secondary"
                        style={{ fontSize: '0.78rem', padding: '2px 8px' }}
                        onClick={() => generateMutation.mutate()}
                      >
                        <RefreshCw size={12} style={{ marginRight: 4 }} /> Retry
                      </Button>
                    </div>
                    <span style={{ fontSize: '0.88rem' }}>{displayMessage}</span>
                  </div>
                );
              })()}

              {generateMutation.isPending ? (
                <div style={{ padding: '40px 20px', textAlign: 'center' }}>
                  <RefreshCw size={28} className="spin text-brand" style={{ margin: '0 auto 12px' }} />
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 600 }}>Synthesizing Sources & Verifying Citations...</h3>
                  <p className="muted" style={{ fontSize: '0.85rem', maxWidth: 400, margin: '6px auto 0' }}>
                    The backend is performing hybrid retrieval across authorized documents, assembling an evidence bundle, and generating a grounded draft with verified citations.
                  </p>
                </div>
              ) : generatedDraft ? (
                <div>
                  <div style={{ marginBottom: 14 }}>
                    <Textarea
                      rows={16}
                      value={generatedDraft}
                      onChange={(e) => setGeneratedDraft(e.target.value)}
                      style={{ fontFamily: 'var(--font-sans)', lineHeight: 1.6, fontSize: '0.92rem' }}
                    />
                  </div>

                  {/* Citations List */}
                  {draftCitations.length > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                        <CheckCircle2 size={14} style={{ color: 'var(--success-text, #10b981)' }} />
                        Verified Source Citations ({draftCitations.length})
                      </h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 160, overflowY: 'auto' }}>
                        {draftCitations.map((citation, index) => (
                          <button
                            key={citation.id ?? index}
                            type="button"
                            className="citation"
                            onClick={() => setActiveCitation(citation)}
                            style={{ textAlign: 'left', cursor: 'pointer' }}
                          >
                            <strong>[{citation.number ?? index + 1}] {citation.documentCode ?? citation.docCode ?? 'DOC'}</strong> · Version {citation.versionNumber ?? 1} · Page {citation.pageNumber ?? citation.page ?? 'n/a'}
                            <p className="muted" style={{ margin: '2px 0 0', fontSize: '0.78rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {citation.supportingExcerpt ?? citation.snippet ?? citation.quote}
                            </p>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Draft Actions */}
                  <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', borderTop: '1px solid var(--border)', paddingTop: 14 }}>
                    <Button
                      type="button"
                      variant="primary"
                      disabled={saveDraftMutation.isPending}
                      onClick={() => saveDraftMutation.mutate()}
                      style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                    >
                      <Save size={14} />
                      {saveDraftMutation.isPending ? 'Saving...' : `Save as ${SECTION_OPTIONS.find((s) => s.key === selectedSection)?.label}`}
                    </Button>
                    <Button
                      type="button"
                      variant="secondary"
                      disabled={generateMutation.isPending}
                      onClick={() => generateMutation.mutate()}
                      style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                    >
                      <RefreshCw size={14} /> Regenerate
                    </Button>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => {
                        setGeneratedDraft('');
                        setDraftCitations([]);
                        setSaveStatus(null);
                      }}
                      style={{ marginLeft: 'auto', color: 'var(--danger-text, #ef4444)' }}
                    >
                      <Trash2 size={14} /> Discard
                    </Button>
                  </div>
                </div>
              ) : (
                <EmptyState
                  title="No draft generated yet"
                  description="Choose a section on the left, customize instructions if desired, and click Generate Draft to synthesize your uploaded sources."
                />
              )}
            </Card>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 2. ASK MODE */}
      {/* ========================================================================= */}
      {mode === 'ask' && (
        <div className="grid cols-2" style={{ gap: 24, alignItems: 'flex-start' }}>
          {/* Left Column: Conversations & Suggested Questions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                <h2 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0 }}>
                  Conversations
                </h2>
                <Button
                  type="button"
                  variant="secondary"
                  style={{ fontSize: '0.8rem', padding: '3px 8px' }}
                  onClick={() => createConversation.mutate()}
                  disabled={createConversation.isPending}
                >
                  <Plus size={13} style={{ marginRight: 4 }} /> New
                </Button>
              </div>

              {conversationsQuery.data && conversationsQuery.data.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 180, overflowY: 'auto' }}>
                  {conversationsQuery.data.map((c) => (
                    <button
                      key={c.id}
                      type="button"
                      className={`button ${activeConversationId === c.id ? 'primary' : 'secondary'}`}
                      style={{ textAlign: 'left', justifyContent: 'flex-start', fontSize: '0.82rem', padding: '6px 10px' }}
                      onClick={() => setConversationId(c.id)}
                    >
                      <MessageSquare size={13} style={{ marginRight: 6 }} />
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {c.title || 'Research Session'}
                      </span>
                    </button>
                  ))}
                </div>
              ) : (
                <p className="muted" style={{ fontSize: '0.82rem' }}>No conversations yet. Start asking questions below.</p>
              )}
            </Card>

            <Card>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 8 }}>
                Suggested Research Inquiries
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {SUGGESTED_QUESTIONS.map((q, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="button secondary"
                    style={{ textAlign: 'left', fontSize: '0.82rem', padding: '8px 10px', lineHeight: 1.3 }}
                    onClick={() => askMutation.mutate(q)}
                    disabled={askMutation.isPending || readyDocuments.length === 0}
                  >
                    <ChevronRight size={13} style={{ marginRight: 4, flexShrink: 0 }} />
                    {q}
                  </button>
                ))}
              </div>
            </Card>
          </div>

          {/* Right Column: Q&A Output & Input */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600, marginBottom: 12 }}>
                Question & Grounded Answer
              </h2>

              {askMutation.isPending && (
                <div className="alert info" style={{ marginBottom: 14 }}>
                  <RefreshCw size={14} className="spin" style={{ marginRight: 6 }} />
                  Retrieving project evidence and generating grounded response...
                </div>
              )}

              {askMutation.isError && (() => {
                const err = askMutation.error as any;
                const code = err?.response?.data?.errorCode || err?.response?.data?.code || 'AI_QUERY_FAILED';
                const msg = err?.response?.data?.message || err?.message || 'Query failed.';
                const displayMessage = aiUserMessage(code, msg);
                return (
                  <div className="alert danger" style={{ marginBottom: 14, display: 'flex', flexDirection: 'column', gap: 6 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Badge tone="danger">{code}</Badge>
                    </div>
                    <span style={{ fontSize: '0.88rem' }}>{displayMessage}</span>
                  </div>
                );
              })()}

              {askAnswer ? (
                <div>
                  <div style={{ padding: '14px', borderRadius: 8, background: 'var(--surface-hover)', border: '1px solid var(--border)', marginBottom: 16, lineHeight: 1.6 }}>
                    <p style={{ margin: 0, fontSize: '0.92rem' }}>{askAnswer.answer}</p>
                  </div>

                  {/* Citations */}
                  {(askAnswer.citations?.length ?? 0) > 0 && (
                    <div style={{ marginBottom: 16 }}>
                      <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 8 }}>
                        Evidence & Citations ({askAnswer.citations?.length})
                      </h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                        {askAnswer.citations?.map((citation, index) => (
                          <button
                            key={citation.id ?? index}
                            type="button"
                            className="citation"
                            onClick={() => setActiveCitation(citation)}
                            style={{ textAlign: 'left', cursor: 'pointer' }}
                          >
                            <strong>[{citation.number ?? index + 1}] {citation.documentCode ?? citation.docCode ?? 'DOC'}</strong> · Version {citation.versionNumber ?? 1} · Page {citation.pageNumber ?? citation.page ?? 'n/a'}
                            <p className="muted" style={{ margin: '2px 0 0', fontSize: '0.78rem' }}>
                              {citation.supportingExcerpt ?? citation.snippet ?? citation.quote}
                            </p>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <EmptyState
                  title="Ask your research sources"
                  description="Enter a question below or select one of the suggested research inquiries to retrieve verified evidence from your uploaded papers."
                />
              )}

              {/* Question Input Form */}
              <AskForm
                disabled={askMutation.isPending || readyDocuments.length === 0 || !hasCredits}
                onAsk={(q) => askMutation.mutate(q)}
                blockerMessage={
                  !hasCredits
                    ? 'Workspace has 0 AI Credits. Please top up or request an allocation to ask questions.'
                    : readyDocuments.length === 0
                    ? (allDocuments.some((d) => d.status === 'PROCESSING')
                        ? 'Uploaded sources are currently being processed. Please wait for indexing to finish.'
                        : 'Upload and index research sources before asking questions.')
                    : undefined
                }
              />
            </Card>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* 3. ANALYZE MODE */}
      {/* ========================================================================= */}
      {mode === 'analyze' && (
        <div className="grid cols-2" style={{ gap: 24, alignItems: 'flex-start' }}>
          {/* Left Column: Analysis Selector & Settings */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: 12 }}>
                Choose Literature Analysis Type
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 380, overflowY: 'auto' }}>
                {ANALYZE_OPTIONS.map((opt) => {
                  const isSelected = selectedAnalysis === opt.key;
                  return (
                    <div
                      key={opt.key}
                      onClick={() => setSelectedAnalysis(opt.key)}
                      style={{
                        padding: '10px 14px',
                        borderRadius: 8,
                        border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border)',
                        background: isSelected ? 'var(--surface-hover)' : 'transparent',
                        cursor: 'pointer',
                        transition: 'all 150ms ease',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <strong style={{ fontSize: '0.92rem' }}>{opt.label}</strong>
                        {opt.key === 'LITERATURE_MATRIX' && <Badge tone="info">Structured Matrix</Badge>}
                      </div>
                      <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.8rem', lineHeight: 1.35 }}>
                        {opt.desc}
                      </p>
                    </div>
                  );
                })}
              </div>

              {/* Custom Analysis prompt */}
              <div style={{ marginTop: 16 }}>
                <Field label={selectedAnalysis === 'CUSTOM_ANALYSIS' ? 'Custom Analysis Criteria *' : 'Additional Focus / Criteria (Optional)'}>
                  <Textarea
                    rows={3}
                    placeholder={
                      selectedAnalysis === 'CUSTOM_ANALYSIS'
                        ? 'e.g., Analyze how each study handles cybersecurity threats in IoT devices...'
                        : 'e.g., Focus on sample size and geographical distribution across studies.'
                    }
                    value={customAnalysisPrompt}
                    onChange={(e) => setCustomAnalysisPrompt(e.target.value)}
                  />
                </Field>
              </div>

              {/* Action Button */}
              <div style={{ marginTop: 16 }}>
                <Button
                  type="button"
                  variant="primary"
                  disabled={
                    analyzeMutation.isPending ||
                    readyDocuments.length === 0 ||
                    !hasCredits ||
                    (scopeType === 'SELECTED_DOCUMENTS' && selectedDocuments.length === 0)
                  }
                  onClick={() => analyzeMutation.mutate()}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
                >
                  <Sparkles size={16} />
                  {analyzeMutation.isPending
                    ? 'Analyzing Literature...'
                    : selectedAnalysis === 'LITERATURE_MATRIX'
                    ? 'Generate Literature Matrix'
                    : 'Run Literature Analysis'}
                </Button>
              </div>
            </Card>
          </div>

          {/* Right Column: Analysis Output */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Card>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>
                  {ANALYZE_OPTIONS.find((o) => o.key === selectedAnalysis)?.label ?? 'Analysis Output'}
                </h3>
                {analysisResult?.answer && (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <Button
                      variant="secondary"
                      className="btn-compact"
                      onClick={() => {
                        navigator.clipboard.writeText(analysisResult.answer ?? '');
                        setSaveStatus('Copied to clipboard!');
                        setTimeout(() => setSaveStatus(null), 3000);
                      }}
                      style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      <Copy size={14} /> Copy
                    </Button>
                    <Button
                      variant="secondary"
                      className="btn-compact"
                      onClick={() => {
                        setGeneratedDraft(analysisResult.answer ?? '');
                        setDraftCitations(analysisResult.citations ?? []);
                        handleModeChange('generate');
                      }}
                      style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      <Save size={14} /> Send to Draft
                    </Button>
                  </div>
                )}
              </div>

              {analyzeMutation.isPending && (
                <div style={{ padding: '3rem 1rem', textAlign: 'center' }}>
                  <RefreshCw size={28} className="animate-spin text-primary" style={{ margin: '0 auto 1rem auto' }} />
                  <p style={{ fontWeight: 600, margin: 0 }}>Analyzing literature across ready sources...</p>
                  <p className="muted" style={{ fontSize: '0.85rem', marginTop: 4 }}>
                    Extracting methodology, evidence, and structured findings from indexed documents.
                  </p>
                </div>
              )}

              {analyzeMutation.isError && (
                <div className="alert danger" style={{ marginBottom: 14 }}>
                  {(analyzeMutation.error as any)?.message || 'Analysis failed. Please ensure sources are indexed.'}
                </div>
              )}

              {analysisResult?.answer ? (
                <div style={{ overflowX: 'auto' }}>
                  <div
                    style={{
                      padding: 16,
                      borderRadius: 8,
                      background: 'var(--surface-hover)',
                      border: '1px solid var(--border)',
                      fontSize: '0.9rem',
                      lineHeight: 1.6,
                      whiteSpace: 'pre-wrap',
                      fontFamily: selectedAnalysis === 'LITERATURE_MATRIX' ? 'var(--font-mono, monospace)' : 'inherit',
                    }}
                  >
                    {analysisResult.answer}
                  </div>

                  {/* Evidence citations */}
                  {(analysisResult.citations?.length ?? 0) > 0 && (
                    <div style={{ marginTop: 16 }}>
                      <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 8 }}>
                        Referenced Sources ({analysisResult.citations?.length})
                      </h4>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                        {analysisResult.citations?.map((cit, idx) => (
                          <button
                            key={cit.id ?? idx}
                            type="button"
                            className="citation"
                            onClick={() => setActiveCitation(cit)}
                            style={{ textAlign: 'left', cursor: 'pointer' }}
                          >
                            <strong>[{cit.number ?? idx + 1}] {cit.documentCode ?? cit.docCode ?? 'DOC'}</strong> · {cit.documentTitle || 'Source'} (Page {cit.pageNumber ?? cit.page ?? 'N/A'})
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ) : !analyzeMutation.isPending && (
                <EmptyState
                  title="Run Literature Analysis"
                  description="Select an analysis type on the left, such as Literature Matrix, Research Gap Analysis, or Methodology Comparison, and click Generate to extract structured insights."
                />
              )}
            </Card>
          </div>
        </div>
      )}

      {/* Citation Detail Drawer */}
      <Drawer
        title="Source Evidence Detail"
        open={Boolean(activeCitation)}
        onClose={() => setActiveCitation(null)}
      >
        {activeCitation ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div>
              <Badge tone="info">Citation Reference</Badge>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: '6px 0 0' }}>
                [{activeCitation.number ?? 1}] {activeCitation.documentCode ?? activeCitation.docCode ?? 'DOC'}
              </h3>
            </div>
            <div>
              <span className="muted" style={{ fontSize: '0.8rem' }}>DOCUMENT TITLE</span>
              <div style={{ fontSize: '0.95rem', fontWeight: 600 }}>
                {activeCitation.documentTitle ?? 'Untitled Document'}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 16 }}>
              <div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>VERSION</span>
                <div>v{activeCitation.versionNumber ?? 1}</div>
              </div>
              <div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>PAGE</span>
                <div>{activeCitation.pageNumber ?? activeCitation.page ?? 'N/A'}</div>
              </div>
            </div>
            <div>
              <span className="muted" style={{ fontSize: '0.8rem' }}>SUPPORTING EXCERPT</span>
              <div style={{ padding: 12, borderRadius: 6, background: 'var(--surface-hover)', border: '1px solid var(--border)', fontSize: '0.85rem', lineHeight: 1.5, marginTop: 4 }}>
                {activeCitation.supportingExcerpt ?? activeCitation.snippet ?? activeCitation.quote ?? 'No excerpt returned.'}
              </div>
            </div>
            {activeCitation.documentId && (
              <Button asChild variant="secondary" style={{ marginTop: 10 }}>
                <a href={documentApi.downloadUrl(activeCitation.documentId)} target="_blank" rel="noopener noreferrer">
                  <Download size={14} style={{ marginRight: 6 }} /> Download Source PDF
                </a>
              </Button>
            )}
          </div>
        ) : null}
      </Drawer>

      {/* Top Up Credits Modal */}
      {workspaceId ? (
        <BuyAiCreditsModal
          open={showBuyCredits}
          onClose={() => setShowBuyCredits(false)}
          workspaceId={workspaceId}
        />
      ) : null}
    </section>
  );
}

function AskForm({
  disabled,
  onAsk,
  blockerMessage,
}: {
  disabled?: boolean;
  onAsk: (q: string) => void;
  blockerMessage?: string;
}) {
  const [question, setQuestion] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || disabled) return;
    onAsk(question.trim());
    setQuestion('');
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: 16 }}>
      <Field label="Your Question">
        <Textarea
          rows={3}
          placeholder="Ask a question grounded in your uploaded research sources..."
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        />
      </Field>
      <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        {blockerMessage ? (
          <span className="muted" style={{ fontSize: '0.8rem', color: 'var(--warning-text, #f59e0b)' }}>
            <AlertTriangle size={13} style={{ display: 'inline', marginRight: 4 }} />
            {blockerMessage}
          </span>
        ) : <span />}
        <Button type="submit" variant="primary" disabled={disabled || !question.trim()} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Send size={14} /> Ask Your Sources
        </Button>
      </div>
    </form>
  );
}

function BuyAiCreditsModal({ open, onClose, workspaceId }: { open: boolean; onClose: () => void; workspaceId: string }) {
  const packsQuery = useQuery({
    queryKey: ['billing', workspaceId, 'ai-credit-packs'],
    queryFn: () => billingApi.aiCreditPacks(workspaceId),
    enabled: open && Boolean(workspaceId),
  });

  const purchaseMutation = useMutation({
    mutationFn: (packId: string) => billingApi.buyAiCreditPack(workspaceId, packId),
    onSuccess: (data) => {
      if (data?.authorizationUrl && /^https:\/\/(checkout|standard)\.paystack\.(com|co)\//.test(data.authorizationUrl)) {
        window.location.href = data.authorizationUrl;
      }
    },
  });

  return (
    <Modal title="Top Up AI Credits" open={open} onClose={onClose}>
      <p className="muted" style={{ fontSize: '0.875rem', marginBottom: '16px' }}>
        AI credits power grounded research queries and document analysis. Credit packs never expire and are consumed after your plan's monthly allowance.
      </p>
      {purchaseMutation.isError && (
        <div className="alert danger" style={{ marginBottom: '12px' }}>
          {(purchaseMutation.error as any)?.response?.data?.message || 'Failed to initialize payment attempt.'}
        </div>
      )}
      {packsQuery.isLoading ? (
        <p className="muted">Loading credit packs...</p>
      ) : packsQuery.data?.length === 0 ? (
        <p className="muted">No credit packs available at this time.</p>
      ) : (
        <div className="grid" style={{ gap: '12px' }}>
          {packsQuery.data?.map((pack) => (
            <div
              key={pack.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '14px',
                borderRadius: '8px',
                border: '1px solid var(--border)',
                background: 'var(--surface-hover)',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div>
                <strong style={{ fontSize: '1rem' }}>{pack.name}</strong>
                <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>
                  {pack.description || `${Number(pack.creditAmount || pack.credits).toLocaleString()} AI Credits`}
                </p>
                <Badge tone="info" style={{ marginTop: '6px' }}>
                  {Number(pack.creditAmount || pack.credits).toLocaleString()} credits
                </Badge>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: '8px' }}>
                  {pack.currency} {Number(pack.priceAmount || pack.price).toFixed(2)}
                </div>
                <Button
                  type="button"
                  variant="primary"
                  disabled={purchaseMutation.isPending}
                  onClick={() => purchaseMutation.mutate(pack.id)}
                >
                  {purchaseMutation.isPending ? 'Processing...' : 'Buy with Paystack'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}
