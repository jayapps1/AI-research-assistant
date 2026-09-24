import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  BookOpen,
  Sparkles,
  Save,
  CheckCircle2,
  AlertCircle,
  Cpu,
  Layers,
  FileQuestion,
  Users,
  Compass,
  Database,
  ListOrdered,
  Plus,
  Trash2,
  ClipboardList,
  Download
} from 'lucide-react';
import { projectApi, researchApi, datasetApi, researchDesignApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge } from '../components/ui';
import { EmptyState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { paths } from '../routes/paths';
import { pageContent } from '../utils/collections';

const RESEARCH_TYPES: { id: string; label: string; description: string; icon: any }[] = [
  { id: 'SOFTWARE_SYSTEM_PROJECT', label: 'Software / System Project', description: 'System architecture, requirements, tech stack, testing, and deployment', icon: Cpu },
  { id: 'QUANTITATIVE_SURVEY', label: 'Quantitative Survey', description: 'Statistical hypotheses, sampling plan, survey instrument, and data analysis', icon: Compass },
  { id: 'QUALITATIVE_RESEARCH', label: 'Qualitative Research', description: 'Phenomenological or grounded inquiry, interviews, and thematic coding', icon: Users },
  { id: 'MIXED_METHODS', label: 'Mixed Methods', description: 'Integrated quantitative and qualitative strands with triangulation', icon: Layers },
  { id: 'EXPERIMENTAL_RESEARCH', label: 'Experimental Research', description: 'Hypothesis testing, control/treatment groups, and measurement protocols', icon: Database },
  { id: 'CASE_STUDY', label: 'Case Study', description: 'In-depth bounded case examination with multi-source evidence', icon: BookOpen },
  { id: 'LITERATURE_BASED_RESEARCH', label: 'Literature-Based Research', description: 'Systematic literature review, inclusion criteria, and synthesis matrix', icon: ClipboardList },
  { id: 'GENERAL_ACADEMIC_RESEARCH', label: 'General Academic Research', description: 'Standard academic inquiry with problem statement, objectives, and methods', icon: BookOpen },
];

export function ResearchPage() {
  const projectId = useProjectId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'setup' | 'design' | 'instruments' | 'data'>('setup');
  const [saveStatus, setSaveStatus] = useState<string | null>(null);

  // Form states
  const [topic, setTopic] = useState('');
  const [researchAim, setResearchAim] = useState('');
  const [problemStatement, setProblemStatement] = useState('');
  const [studyArea, setStudyArea] = useState('');
  const [researchType, setResearchType] = useState('SOFTWARE_SYSTEM_PROJECT');
  const [objectives, setObjectives] = useState<string[]>(['']);
  const [questions, setQuestions] = useState<string[]>(['']);
  const [hypotheses, setHypotheses] = useState<string[]>(['']);

  // Design form states
  const [approach, setApproach] = useState('');
  const [designType, setDesignType] = useState('');
  const [targetPopulation, setTargetPopulation] = useState('');
  const [samplingTechnique, setSamplingTechnique] = useState('');
  const [sampleSize, setSampleSize] = useState('');
  const [dataCollectionStrategy, setDataCollectionStrategy] = useState('');

  // Software specific
  const [systemArchitecture, setSystemArchitecture] = useState('');
  const [functionalRequirements, setFunctionalRequirements] = useState('');
  const [techStack, setTechStack] = useState('');
  const [testingStrategy, setTestingStrategy] = useState('');

  // Project query
  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  // Methodologies query
  const methodologiesQuery = useQuery({
    queryKey: ['methodologies', projectId],
    queryFn: () => researchApi.methodologies(projectId),
    enabled: Boolean(projectId),
  });

  // Validation query
  const validationQuery = useQuery({
    queryKey: ['research-design-validation', projectId],
    queryFn: () => researchApi.validateResearchDesign(projectId),
    enabled: Boolean(projectId),
  });

  const researchDesignQuery = useQuery({
    queryKey: ['research-design', projectId],
    queryFn: () => researchDesignApi.get(projectId),
    enabled: Boolean(projectId),
  });

  // Datasets query
  const datasetsQuery = useQuery({
    queryKey: ['datasets', projectId],
    queryFn: () => datasetApi.list(projectId),
    enabled: Boolean(projectId),
  });

  // References query for literature grounding
  const referencesQuery = useQuery({
    queryKey: ['references', projectId],
    queryFn: () => reportApi.references(projectId, 0, 100),
    enabled: Boolean(projectId),
  });

  const updateScopeMutation = useMutation({
    mutationFn: ({ referenceId, enabled, citationEnabled }: { referenceId: string; enabled: boolean; citationEnabled: boolean }) =>
      reportApi.setUsageScope(referenceId, { availableForResearchAi: enabled, availableForCitation: citationEnabled }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['references', projectId] });
      setSaveStatus('Literature scope for research updated.');
      setTimeout(() => setSaveStatus(null), 3000);
    },
  });

  // Populate data when project loads
  useEffect(() => {
    if (projectQuery.data) {
      const p = projectQuery.data;
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTopic(p.title || '');
      setResearchAim(p.researchAim || '');
      setProblemStatement(p.description || '');
      setStudyArea(p.studyArea || '');
      if (p.researchType) {
        setResearchType(p.researchType);
      }
    }
  }, [projectQuery.data]);

  useEffect(() => {
    const design = researchDesignQuery.data as any;
    if (!design) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (design.problemStatement) setProblemStatement(String(design.problemStatement));
    const objectiveTexts = asTextList(design.objectives);
    const questionTexts = asTextList(design.questions);
    const hypothesisTexts = asTextList(design.hypotheses);
    setObjectives(objectiveTexts.length ? objectiveTexts : ['']);
    setQuestions(questionTexts.length ? questionTexts : ['']);
    setHypotheses(hypothesisTexts.length ? hypothesisTexts : ['']);
  }, [researchDesignQuery.data]);

  // Populate methodology data when available
  useEffect(() => {
    if (methodologiesQuery.data && methodologiesQuery.data.length > 0) {
      const m = methodologiesQuery.data[0] as any;
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (m.approach) setApproach(m.approach);
      if (m.designType) setDesignType(m.designType);
      if (m.targetPopulation) setTargetPopulation(m.targetPopulation);
      if (m.samplingTechnique) setSamplingTechnique(m.samplingTechnique);
      if (m.sampleSize) setSampleSize(String(m.sampleSize));
      if (m.dataCollectionStrategy) setDataCollectionStrategy(m.dataCollectionStrategy);
      if (m.systemArchitecture) setSystemArchitecture(m.systemArchitecture);
      if (m.functionalRequirements) setFunctionalRequirements(m.functionalRequirements);
      if (m.techStack) setTechStack(m.techStack);
      if (m.testingStrategy) setTestingStrategy(m.testingStrategy);
    }
  }, [methodologiesQuery.data]);

  // Project update mutation
  const updateProjectMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => projectApi.update(projectId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setSaveStatus('Research project saved successfully.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.message || 'Failed to save project.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  // Methodology update/create mutation
  const saveMethodologyMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => {
      return researchApi.createMethodology(projectId, body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['methodologies', projectId] });
      queryClient.invalidateQueries({ queryKey: ['research-design-validation', projectId] });
      setSaveStatus('Research design saved.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.message || 'Failed to save research design.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  const saveResearchDesignMutation = useMutation({
    mutationFn: () => researchDesignApi.save(projectId, {
      problemStatement,
      objectives,
      questions,
      hypotheses,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['research-design', projectId] });
      queryClient.invalidateQueries({ queryKey: ['research-design-validation', projectId] });
      setSaveStatus('Research setup saved successfully.');
      setTimeout(() => setSaveStatus(null), 3500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.message || 'Failed to save research setup.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  const [exportingFormat, setExportingFormat] = useState<string | null>(null);

  const exportResearchProtocol = async (format: 'docx' | 'markdown') => {
    try {
      setExportingFormat(format);
      const res = await researchDesignApi.exportProtocol(projectId, format);
      reportApi.saveBlob(res.blob, res.filename);
      setSaveStatus(`Research protocol exported as ${format.toUpperCase()} with complete references.`);
      setTimeout(() => setSaveStatus(null), 3500);
    } catch (err: any) {
      setSaveStatus(err?.message || 'Export failed.');
      setTimeout(() => setSaveStatus(null), 4000);
    } finally {
      setExportingFormat(null);
    }
  };

  // Generate Setup with AI mutation
  const generateResearchSetupMutation = useMutation({
    mutationFn: () => researchDesignApi.generate(projectId),
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['research-design', projectId] });
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      if (data) {
        if (data.problemStatement) setProblemStatement(data.problemStatement);
        if (data.researchAim) setResearchAim(data.researchAim);
        if (Array.isArray(data.objectives) && data.objectives.length > 0) setObjectives(data.objectives);
        if (Array.isArray(data.questions) && data.questions.length > 0) setQuestions(data.questions);
        if (Array.isArray(data.hypotheses) && data.hypotheses.length > 0) setHypotheses(data.hypotheses);
      }
      setSaveStatus('AI synthesized research problem, aim, objectives, and questions from uploaded papers.');
      setTimeout(() => setSaveStatus(null), 4500);
    },
    onError: (err: any) => {
      setSaveStatus(err?.message || 'Failed to synthesize research setup.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  // Generate with AI mutation
  const generateMethodologyMutation = useMutation({
    mutationFn: () => researchApi.generateMethodology(projectId),
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['methodologies', projectId] });
      queryClient.invalidateQueries({ queryKey: ['research-design-validation', projectId] });
      if (data) {
        if (data.approach) setApproach(data.approach);
        if (data.designType) setDesignType(data.designType);
        if (data.targetPopulation) setTargetPopulation(data.targetPopulation);
        if (data.samplingTechnique) setSamplingTechnique(data.samplingTechnique);
        if (data.sampleSize) setSampleSize(String(data.sampleSize));
        if (data.dataCollectionStrategy) setDataCollectionStrategy(data.dataCollectionStrategy);
        if (data.systemArchitecture) setSystemArchitecture(data.systemArchitecture);
        if (data.functionalRequirements) setFunctionalRequirements(data.functionalRequirements);
        if (data.techStack) setTechStack(data.techStack);
        if (data.testingStrategy) setTestingStrategy(data.testingStrategy);
      }
      setSaveStatus('AI generated research design based on your project topic and sources.');
      setTimeout(() => setSaveStatus(null), 4000);
    },
    onError: (err: any) => {
      setSaveStatus(err?.message || 'AI generation failed. Ensure ready sources are uploaded.');
      setTimeout(() => setSaveStatus(null), 5000);
    },
  });

  const handleSaveSetup = () => {
    updateProjectMutation.mutate({
      title: topic,
      researchAim,
      description: problemStatement,
      studyArea,
      researchType,
    });
    saveResearchDesignMutation.mutate();
  };

  const handleSaveDesign = () => {
    saveMethodologyMutation.mutate({
      approach: approach || (researchType === 'SOFTWARE_SYSTEM_PROJECT' ? 'APPLIED_COMPUTATIONAL' : 'EMPIRICAL'),
      designType: designType || researchType,
      targetPopulation,
      samplingTechnique,
      sampleSize: sampleSize ? parseInt(sampleSize, 10) : undefined,
      dataCollectionStrategy,
      systemArchitecture,
      functionalRequirements,
      techStack,
      testingStrategy,
    });
  };

  const addObjective = () => setObjectives([...objectives, '']);
  const removeObjective = (index: number) => setObjectives(objectives.filter((_, i) => i !== index));
  const updateObjective = (index: number, val: string) => {
    const copy = [...objectives];
    copy[index] = val;
    setObjectives(copy);
  };

  const addQuestion = () => setQuestions([...questions, '']);
  const removeQuestion = (index: number) => setQuestions(questions.filter((_, i) => i !== index));
  const updateQuestion = (index: number, val: string) => {
    const copy = [...questions];
    copy[index] = val;
    setQuestions(copy);
  };

  const addHypothesis = () => setHypotheses([...hypotheses, '']);
  const removeHypothesis = (index: number) => setHypotheses(hypotheses.filter((_, i) => i !== index));
  const updateHypothesis = (index: number, val: string) => {
    const copy = [...hypotheses];
    copy[index] = val;
    setHypotheses(copy);
  };

  if (!projectId) {
    return (
      <main className="page">
        <EmptyState title="Select a Project" description="Please open a research project to view its research design." />
      </main>
    );
  }

  if (projectQuery.isLoading) {
    return <PageLoading label="Loading research workspace..." />;
  }

  const currentTypeConfig = RESEARCH_TYPES.find((t) => t.id === researchType) ?? RESEARCH_TYPES[0];
  const isSoftware = researchType === 'SOFTWARE_SYSTEM_PROJECT';
  const isQuantitative = researchType === 'QUANTITATIVE_SURVEY' || researchType === 'EXPERIMENTAL_RESEARCH';
  const allProjectRefs = pageContent(referencesQuery.data) as any[];
  const activeResearchRefs = allProjectRefs.filter((r) => r.availableForResearchAi !== false);

  return (
    <main className="page">
      <Breadcrumbs
        items={['Projects', projectQuery.data?.title || 'Project', 'Research Design']}
      />

      {/* Top Banner / Hero */}
      <div className="section-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '1.75rem', fontWeight: 700 }}>
            <currentTypeConfig.icon className="text-primary" size={28} />
            Research Design & Methodology
          </h1>
          <p className="section-subtitle" style={{ maxWidth: '750px', color: 'var(--color-muted)' }}>
            Define the conceptual, methodological, and empirical framework for your research. Every field is connected to AI generation and report assembly.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <Button
            variant="secondary"
            onClick={() => exportResearchProtocol('docx')}
            disabled={exportingFormat !== null}
            title="Download full research protocol dossier as formatted Word document with numbered references"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Download size={16} />
            {exportingFormat === 'docx' ? 'Exporting DOCX...' : 'Export Protocol (DOCX)'}
          </Button>

          <Button
            variant="secondary"
            onClick={() => exportResearchProtocol('markdown')}
            disabled={exportingFormat !== null}
            title="Download research protocol as academic Markdown file"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Download size={16} />
            {exportingFormat === 'markdown' ? 'Exporting MD...' : 'Export Protocol (MD)'}
          </Button>

          <Button
            variant="secondary"
            onClick={() => navigate(paths.projectAdvanced(projectId))}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <ListOrdered size={16} />
            Advanced 18-Stage Workflow
          </Button>

          <Button
            variant="primary"
            onClick={activeTab === 'setup' ? handleSaveSetup : handleSaveDesign}
            disabled={updateProjectMutation.isPending || saveMethodologyMutation.isPending || saveResearchDesignMutation.isPending}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Save size={16} />
            Save Changes
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
          {saveStatus.includes('failed') || saveStatus.includes('Failed') ? <AlertCircle size={16} /> : <CheckCircle2 size={16} />}
          {saveStatus}
        </div>
      )}

      {/* Research Type Selection Bar */}
      <Card style={{ padding: '1rem 1.25rem', marginBottom: '1.5rem', background: 'var(--color-surface-subtle)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', width: '100%' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span style={{ fontWeight: 600, fontSize: '0.95rem' }}>Research Type:</span>
            <select
              className="select-input"
              value={researchType}
              onChange={(e) => {
                setResearchType(e.target.value);
                updateProjectMutation.mutate({ researchType: e.target.value });
              }}
              style={{ padding: '0.4rem 0.8rem', borderRadius: '6px', fontWeight: 500 }}
            >
              {RESEARCH_TYPES.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </select>
            <Badge tone="info">{currentTypeConfig.description}</Badge>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {validationQuery.data && (
              <Badge tone={(validationQuery.data as any).isValid ? 'success' : 'warning'}>
                {(validationQuery.data as any).isValid ? 'Design Validated' : 'Design Needs Attention'}
              </Badge>
            )}
          </div>
        </div>
      </Card>

      {/* Navigation Tabs */}
      <div className="tab-bar" style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--color-border)', marginBottom: '1.5rem' }}>
        <button
          className={`tab-btn ${activeTab === 'setup' ? 'active' : ''}`}
          onClick={() => setActiveTab('setup')}
          style={{
            padding: '0.6rem 1.25rem',
            borderBottom: activeTab === 'setup' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontWeight: activeTab === 'setup' ? 600 : 400,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <BookOpen size={16} />
          1. Setup & Objectives
        </button>

        <button
          className={`tab-btn ${activeTab === 'design' ? 'active' : ''}`}
          onClick={() => setActiveTab('design')}
          style={{
            padding: '0.6rem 1.25rem',
            borderBottom: activeTab === 'design' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontWeight: activeTab === 'design' ? 600 : 400,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <Compass size={16} />
          2. {isSoftware ? 'System Architecture & Design' : 'Methodology & Sampling'}
        </button>

        <button
          className={`tab-btn ${activeTab === 'instruments' ? 'active' : ''}`}
          onClick={() => setActiveTab('instruments')}
          style={{
            padding: '0.6rem 1.25rem',
            borderBottom: activeTab === 'instruments' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontWeight: activeTab === 'instruments' ? 600 : 400,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <FileQuestion size={16} />
          3. Instruments & Protocols
        </button>

        <button
          className={`tab-btn ${activeTab === 'data' ? 'active' : ''}`}
          onClick={() => setActiveTab('data')}
          style={{
            padding: '0.6rem 1.25rem',
            borderBottom: activeTab === 'data' ? '2px solid var(--color-primary)' : '2px solid transparent',
            background: 'none',
            fontWeight: activeTab === 'data' ? 600 : 400,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <Database size={16} />
          4. Datasets & Participants
        </button>
      </div>

      {/* TAB 1: SETUP & OBJECTIVES */}
      {activeTab === 'setup' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* AI Grounded Literature Synthesis Banner */}
          <Card style={{
            padding: '1.25rem 1.5rem',
            background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.06) 0%, rgba(79, 70, 229, 0.06) 100%)',
            border: '1px solid rgba(37, 99, 235, 0.25)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem'
          }}>
            <div style={{ maxWidth: '650px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '4px' }}>
                <Sparkles size={18} className="text-primary" />
                <strong style={{ fontSize: '1rem' }}>AI Research Synthesis Grounded in Uploaded Literature</strong>
              </div>
              <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>
                Leverage RAG over your uploaded papers to automatically formulate a rigorous Problem Statement, Research Aim, Specific Objectives, Research Questions, and Hypotheses.
              </p>
            </div>
            <Button
              variant="primary"
              onClick={() => generateResearchSetupMutation.mutate()}
              disabled={generateResearchSetupMutation.isPending}
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              <Sparkles size={16} />
              {generateResearchSetupMutation.isPending ? 'Synthesizing with AI...' : 'Synthesize from Uploaded Literature (AI)'}
            </Button>
          </Card>

          {/* Literature Active for Research Design Partition */}
          <Card style={{ padding: '1.25rem 1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.75rem' }}>
              <div>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <BookOpen size={18} className="text-primary" />
                  Literature Partitioned for Research Design ({activeResearchRefs.length} of {allProjectRefs.length} Papers Active)
                </h3>
                <p className="muted" style={{ margin: '2px 0 0', fontSize: '0.82rem' }}>
                  Select which papers specifically ground this study's Problem Statement, Objectives, and Research Protocol export.
                </p>
              </div>

              {allProjectRefs.length > 0 && (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <Button
                    type="button"
                    variant="secondary"
                    className="btn-compact"
                    onClick={() => {
                      allProjectRefs.forEach((r: any) => {
                        updateScopeMutation.mutate({ referenceId: r.id, enabled: true, citationEnabled: r.availableForCitation !== false });
                      });
                    }}
                  >
                    Select All
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    className="btn-compact"
                    onClick={() => {
                      allProjectRefs.forEach((r: any) => {
                        updateScopeMutation.mutate({ referenceId: r.id, enabled: false, citationEnabled: r.availableForCitation !== false });
                      });
                    }}
                  >
                    Deselect All
                  </Button>
                </div>
              )}
            </div>

            {allProjectRefs.length === 0 ? (
              <p className="muted" style={{ fontSize: '0.85rem', fontStyle: 'italic', margin: '0.5rem 0 0' }}>
                No project documents or references found. Upload PDFs in the Documents page to partition literature for research.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '220px', overflowY: 'auto', marginTop: '0.5rem' }}>
                {allProjectRefs.map((ref: any, idx: number) => {
                  const isResearchActive = ref.availableForResearchAi !== false;
                  return (
                    <div
                      key={ref.id || idx}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 10px',
                        borderRadius: '6px',
                        border: '1px solid var(--border)',
                        background: isResearchActive ? 'var(--surface-hover)' : 'transparent',
                        opacity: isResearchActive ? 1 : 0.7,
                        gap: '0.75rem'
                      }}
                    >
                      <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', flex: 1, overflow: 'hidden' }}>
                        <input
                          type="checkbox"
                          checked={isResearchActive}
                          onChange={(e) => updateScopeMutation.mutate({
                            referenceId: ref.id,
                            enabled: e.target.checked,
                            citationEnabled: ref.availableForCitation !== false
                          })}
                        />
                        <span style={{ fontSize: '0.85rem', fontWeight: isResearchActive ? 600 : 400, textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                          [{idx + 1}] {ref.title || 'Untitled Reference'} {ref.year ? `(${ref.year})` : ''}
                        </span>
                      </label>
                      <Badge tone={isResearchActive ? 'success' : undefined} style={{ fontSize: '0.72rem', flexShrink: 0 }}>
                        {isResearchActive ? 'Grounding Research' : 'Report Only'}
                      </Badge>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>

          <Card style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <BookOpen size={20} className="text-primary" />
              Research Topic & Aim
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.25rem' }}>
              <Field label="Research Topic / Working Title">
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                  The formal title of your research study or software project.
                </span>
                <Input
                  value={topic}
                  onChange={(e) => setTopic(e.target.value)}
                  placeholder="e.g. Design and Implementation of an Automated Research Assistant System"
                />
              </Field>

              <Field label="Research Aim / General Objective">
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                  The overarching purpose and ultimate outcome of this study.
                </span>
                <Textarea
                  value={researchAim}
                  onChange={(e) => setResearchAim(e.target.value)}
                  rows={2}
                  placeholder="The main aim of this project is to develop and evaluate..."
                />
              </Field>

              <Field label="Problem Statement">
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                  What real-world, theoretical, or empirical problem does this work address?
                </span>
                <Textarea
                  value={problemStatement}
                  onChange={(e) => setProblemStatement(e.target.value)}
                  rows={4}
                  placeholder="Clearly articulate the context, the specific gap or deficit, and why solving it is urgent or valuable..."
                />
              </Field>

              <Field label="Study Area / Domain Context">
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                  Geographical, organizational, or domain setting of the study.
                </span>
                <Input
                  value={studyArea}
                  onChange={(e) => setStudyArea(e.target.value)}
                  placeholder="e.g. Higher Education Institutions in Ghana, or Distributed Cloud Computing Environments"
                />
              </Field>
            </div>
          </Card>

          {/* Objectives, Questions, Hypotheses */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '1.5rem' }}>
            {/* Specific Objectives */}
            <Card style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Specific Objectives</h3>
                <Button variant="secondary" onClick={addObjective}>
                  <Plus size={14} /> Add Objective
                </Button>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', marginBottom: '1rem' }}>
                Concrete, measurable steps to achieve the overall research aim.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {objectives.map((obj, i) => (
                  <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span style={{ fontWeight: 600, fontSize: '0.85rem', minWidth: '1.5rem' }}>{i + 1}.</span>
                    <Input
                      value={obj}
                      onChange={(e) => updateObjective(i, e.target.value)}
                      placeholder={isSoftware ? `e.g. To design the microservice architecture for data ingestion` : `e.g. To determine the effect of X on Y`}
                      style={{ flex: 1 }}
                    />
                    {objectives.length > 1 && (
                      <Button variant="secondary" onClick={() => removeObjective(i)}>
                        <Trash2 size={14} />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </Card>

            {/* Research Questions */}
            <Card style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>Research Questions</h3>
                <Button variant="secondary" onClick={addQuestion}>
                  <Plus size={14} /> Add Question
                </Button>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', marginBottom: '1rem' }}>
                Key questions that the empirical investigation or system evaluation must answer.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {questions.map((q, i) => (
                  <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span style={{ fontWeight: 600, fontSize: '0.85rem', minWidth: '1.5rem' }}>RQ{i + 1}.</span>
                    <Input
                      value={q}
                      onChange={(e) => updateQuestion(i, e.target.value)}
                      placeholder={`e.g. How does the proposed system compare against existing baselines?`}
                      style={{ flex: 1 }}
                    />
                    {questions.length > 1 && (
                      <Button variant="secondary" onClick={() => removeQuestion(i)}>
                        <Trash2 size={14} />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </Card>

            {/* Hypotheses (if quantitative/experimental) */}
            {(isQuantitative || isSoftware) && (
              <Card style={{ padding: '1.5rem', gridColumn: '1 / -1' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: 0 }}>
                    {isSoftware ? 'System Performance Hypotheses / Success Criteria' : 'Research Hypotheses (H0 / H1)'}
                  </h3>
                  <Button variant="secondary" onClick={addHypothesis}>
                    <Plus size={14} /> Add Hypothesis
                  </Button>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', marginBottom: '1rem' }}>
                  Testable propositions or measurable quantitative success benchmarks.
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {hypotheses.map((h, i) => (
                    <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.85rem', minWidth: '2rem' }}>H{i + 1}.</span>
                      <Input
                        value={h}
                        onChange={(e) => updateHypothesis(i, e.target.value)}
                        placeholder={isSoftware ? `e.g. The automated pipeline reduces manual analysis latency by at least 50%` : `e.g. There is a statistically significant positive relationship between X and Y`}
                        style={{ flex: 1 }}
                      />
                      {hypotheses.length > 1 && (
                        <Button variant="secondary" onClick={() => removeHypothesis(i)}>
                          <Trash2 size={14} />
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: METHODOLOGY & DESIGN */}
      {activeTab === 'design' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
            <Button
              variant="secondary"
              onClick={() => generateMethodologyMutation.mutate()}
              disabled={generateMethodologyMutation.isPending}
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            >
              <Sparkles size={16} className="text-primary" />
              {generateMethodologyMutation.isPending ? 'Generating with AI...' : 'Generate Design with AI'}
            </Button>
          </div>

          {isSoftware ? (
            /* Software System Project Specific Fields */
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <Card style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Cpu size={18} className="text-primary" />
                  System Architecture & Paradigm
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <Field label="Architectural Pattern">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      e.g. Client-Server, Microservices, Event-Driven, MVC, Layered
                    </span>
                    <Input
                      value={approach}
                      onChange={(e) => setApproach(e.target.value)}
                      placeholder="e.g. Three-Tier Microservices with Spring Boot and React"
                    />
                  </Field>

                  <Field label="Detailed Architecture Description">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Explain components, database tier, communication protocols, and security boundaries.
                    </span>
                    <Textarea
                      value={systemArchitecture}
                      onChange={(e) => setSystemArchitecture(e.target.value)}
                      rows={5}
                      placeholder="The application is structured into a presentation tier (React SPA), an application service tier (Spring Boot 3.4 REST API), and a persistence tier (PostgreSQL with pgvector)..."
                    />
                  </Field>

                  <Field label="Technology Stack">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Programming languages, frameworks, libraries, DBMS, cloud services.
                    </span>
                    <Textarea
                      value={techStack}
                      onChange={(e) => setTechStack(e.target.value)}
                      rows={3}
                      placeholder="Backend: Java 21, Spring Boot 3.4, Spring Security, Hibernate\nFrontend: TypeScript, React 19, Vite\nDatabase: PostgreSQL 16\nAI: OpenAI API / Local RAG"
                    />
                  </Field>
                </div>
              </Card>

              <Card style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <ClipboardList size={18} className="text-primary" />
                  Requirements & Evaluation Strategy
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <Field label="Functional & Non-Functional Requirements">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Key capabilities and quality attributes (performance, security, usability).
                    </span>
                    <Textarea
                      value={functionalRequirements}
                      onChange={(e) => setFunctionalRequirements(e.target.value)}
                      rows={5}
                      placeholder="FR1: User authentication with MFA\nFR2: PDF document ingestion and chunking\nNFR1: Search response time under 500ms\nNFR2: Role-based authorization"
                    />
                  </Field>

                  <Field label="Testing & Evaluation Strategy">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Unit testing, integration testing, system testing, user acceptance testing (UAT).
                    </span>
                    <Textarea
                      value={testingStrategy}
                      onChange={(e) => setTestingStrategy(e.target.value)}
                      rows={4}
                      placeholder="Testing encompasses JUnit 5 unit tests, MockMvc API integration tests, Playwright end-to-end browser automation, and SUS (System Usability Scale) user evaluation."
                    />
                  </Field>
                </div>
              </Card>
            </div>
          ) : (
            /* Empirical / Academic Research Design Fields */
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <Card style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Compass size={18} className="text-primary" />
                  Research Paradigm & Approach
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <Field label="Research Paradigm / Philosophy">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      e.g. Positivism, Interpretivism, Pragmatism, Critical Realism
                    </span>
                    <Input
                      value={approach}
                      onChange={(e) => setApproach(e.target.value)}
                      placeholder="e.g. Positivism / Quantitative or Pragmatism / Mixed Methods"
                    />
                  </Field>

                  <Field label="Research Design Type">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      e.g. Descriptive Cross-Sectional, Quasi-Experimental, Phenomenological, Case Study
                    </span>
                    <Input
                      value={designType}
                      onChange={(e) => setDesignType(e.target.value)}
                      placeholder="e.g. Descriptive Cross-Sectional Survey Design"
                    />
                  </Field>

                  <Field label="Data Collection Strategy">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Instruments, procedure, timing, and ethical clearances.
                    </span>
                    <Textarea
                      value={dataCollectionStrategy}
                      onChange={(e) => setDataCollectionStrategy(e.target.value)}
                      rows={4}
                      placeholder="Structured online questionnaire administered via institutional portals..."
                    />
                  </Field>
                </div>
              </Card>

              <Card style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Users size={18} className="text-primary" />
                  Population & Sampling Plan
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <Field label="Target Population">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      The entire group of individuals or instances to which findings apply.
                    </span>
                    <Textarea
                      value={targetPopulation}
                      onChange={(e) => setTargetPopulation(e.target.value)}
                      rows={2}
                      placeholder="e.g. All final-year undergraduate computer science students across accredited universities in Region X (N ≈ 1,200)"
                    />
                  </Field>

                  <Field label="Sampling Technique">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      e.g. Stratified Random Sampling, Purposive Sampling, Convenience Sampling
                    </span>
                    <Input
                      value={samplingTechnique}
                      onChange={(e) => setSamplingTechnique(e.target.value)}
                      placeholder="e.g. Proportionate Stratified Random Sampling"
                    />
                  </Field>

                  <Field label="Sample Size & Justification">
                    <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: 4 }}>
                      Calculated sample size (e.g. Yamane formula, Cochran, G*Power) or participant count.
                    </span>
                    <Input
                      value={sampleSize}
                      onChange={(e) => setSampleSize(e.target.value)}
                      placeholder="e.g. 291 (determined via Yamane formula at 95% confidence and 5% margin of error)"
                    />
                  </Field>
                </div>
              </Card>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: INSTRUMENTS & PROTOCOLS */}
      {activeTab === 'instruments' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <Card style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>Research Instruments</h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', margin: '0.25rem 0 0 0' }}>
                  {isSoftware
                    ? 'Evaluation rubrics, usability questionnaires (SUS), benchmark suites, and test case matrices.'
                    : 'Questionnaires, interview protocols, observation guides, or measurement scales.'}
                </p>
              </div>
              <Button variant="secondary" onClick={() => navigate(paths.projectAssistant(projectId))}>
                <Sparkles size={14} className="text-primary" /> Generate Instrument in AI Assistant
              </Button>
            </div>

            <div style={{ padding: '1.5rem', background: 'var(--color-surface-subtle)', borderRadius: '8px', border: '1px dashed var(--color-border)' }}>
              <div style={{ textAlign: 'center', padding: '1rem' }}>
                <FileQuestion size={36} style={{ color: 'var(--color-muted)', margin: '0 auto 0.75rem auto' }} />
                <h4 style={{ fontWeight: 600, margin: '0 0 0.5rem 0' }}>
                  {isSoftware ? 'System Usability & Performance Instrument' : 'Structured Research Questionnaire / Interview Guide'}
                </h4>
                <p style={{ color: 'var(--color-muted)', fontSize: '0.9rem', maxWidth: '550px', margin: '0 auto 1rem auto' }}>
                  You can design and review your instrument sections here, or draft them with AI using your uploaded literature and research objectives.
                </p>
                <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
                  <Button variant="primary" onClick={() => navigate(paths.projectAssistant(projectId))}>
                    Open AI Assistant to Generate Instrument
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* TAB 4: DATASETS & PARTICIPANTS */}
      {activeTab === 'data' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <Card style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>Empirical Datasets & Data Files</h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', margin: '0.25rem 0 0 0' }}>
                  Uploaded tabular or structured data used for analysis and empirical report sections.
                </p>
              </div>
              <Button variant="primary" onClick={() => navigate(`/app/projects/${projectId}/data`)}>
                <Database size={14} /> Open Dataset Workbench
              </Button>
            </div>

            {datasetsQuery.isLoading ? (
              <p>Loading datasets...</p>
            ) : !datasetsQuery.data || (datasetsQuery.data as any).content?.length === 0 ? (
              <div style={{ padding: '2rem', textAlign: 'center', background: 'var(--color-surface-subtle)', borderRadius: '8px' }}>
                <Database size={32} style={{ color: 'var(--color-muted)', margin: '0 auto 0.5rem auto' }} />
                <p style={{ fontWeight: 500, margin: '0 0 0.5rem 0' }}>No datasets uploaded yet</p>
                <p style={{ fontSize: '0.85rem', color: 'var(--color-muted)', marginBottom: '1rem' }}>
                  Empirical sections (Findings, Results) require real datasets or analysis results before AI can generate them.
                </p>
                <Button variant="secondary" onClick={() => navigate(`/app/projects/${projectId}/data`)}>
                  Upload CSV / Excel Dataset
                </Button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {(datasetsQuery.data as any).content?.map((ds: any) => (
                  <div
                    key={ds.id}
                    style={{
                      padding: '1rem',
                      borderRadius: '8px',
                      border: '1px solid var(--border)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <div>
                      <span style={{ fontWeight: 600 }}>{ds.name}</span>
                      <p style={{ fontSize: '0.8rem', color: 'var(--color-muted)', margin: '0.2rem 0 0 0' }}>
                        {ds.description || 'No description'} • {ds.sourceType || 'UPLOADED_FILE'}
                      </p>
                    </div>
                    <Badge tone="success">READY FOR ANALYSIS</Badge>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}
    </main>
  );
}

function asTextList(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (typeof item === 'string') return item;
      if (item && typeof item === 'object' && 'text' in item) return String((item as { text?: unknown }).text ?? '');
      return '';
    })
    .map((item) => item.trim())
    .filter(Boolean);
}
