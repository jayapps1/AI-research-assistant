import { useEffect, useId, useMemo, useRef, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useForm, type FieldErrors } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { BookOpen, CheckCircle2, ClipboardList, Eye, FileText, GraduationCap, Sparkles, UploadCloud } from 'lucide-react';
import { academicTemplateApi, projectApi, reportApi } from '../../api/endpoints';
import { Badge, Button, Field, Input, LoadingButton, Modal, Select, Textarea } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { useWorkspace } from '../workspaces/WorkspaceProvider';
import { paths } from '../../routes/paths';
import type {
  AcademicDocumentGuidelineResponse,
  AcademicProjectType,
  AcademicWorkspaceType,
  ExtractedAcademicTemplate,
  ResearchProject,
} from '../../types/api';
import { TemplateReviewModal } from '../templates/TemplateReviewModal';
import {
  FINAL_DOCUMENT_LABELS,
  PROJECT_TYPE_LABELS,
  WORKSPACE_TYPE_LABELS,
  finalDocumentLabel,
  templateSupportsWorkspace,
} from './workspaceMeta';

const RESEARCH_TYPES = [
  { id: 'QUANTITATIVE_SURVEY', label: 'Quantitative Survey' },
  { id: 'QUALITATIVE_RESEARCH', label: 'Qualitative Research' },
  { id: 'MIXED_METHODS', label: 'Mixed Methods' },
  { id: 'EXPERIMENTAL_RESEARCH', label: 'Experimental Research' },
  { id: 'CASE_STUDY', label: 'Case Study' },
  { id: 'LITERATURE_BASED_RESEARCH', label: 'Literature-Based Research' },
  { id: 'GENERAL_ACADEMIC_RESEARCH', label: 'General Academic Research' },
];

const PROJECT_TYPES = Object.entries(PROJECT_TYPE_LABELS).map(([id, label]) => ({ id, label }));

const CITATION_STYLES = [
  { id: 'APA_7', label: 'APA 7th Edition (Default)' },
  { id: 'IEEE', label: 'IEEE Numerical' },
  { id: 'HARVARD', label: 'Harvard Author-Date' },
  { id: 'CHICAGO_AUTHOR_DATE', label: 'Chicago Author-Date' },
  { id: 'VANCOUVER', label: 'Vancouver Numerical' },
  { id: 'MLA_9', label: 'MLA 9th Edition' },
];

const WORKSPACE_TYPE_CARDS: Array<{
  type: AcademicWorkspaceType;
  title: string;
  description: string;
  icon: typeof BookOpen;
}> = [
  {
    type: 'ACADEMIC_RESEARCH',
    title: 'Academic Research',
    description: 'Conduct a formal academic study and produce a research report.',
    icon: BookOpen,
  },
  {
    type: 'ACADEMIC_PROJECT',
    title: 'Academic Project',
    description: 'Final-year project, capstone or institution-required academic project.',
    icon: GraduationCap,
  },
  {
    type: 'COURSEWORK',
    title: 'Coursework',
    description: 'Assignment, term paper, essay, case study or class exercise.',
    icon: ClipboardList,
  },
];

const projectFormSchema = z.object({
  workspaceId: z.string().min(1, 'Workspace is required'),
  workspaceType: z.enum(['ACADEMIC_RESEARCH', 'ACADEMIC_PROJECT', 'COURSEWORK']),
  title: z.string().trim().min(3, 'Title must be at least 3 characters').max(255, 'Title cannot exceed 255 characters'),
  description: z.string().max(5000, 'Description cannot exceed 5000 characters').optional(),
  institution: z.string().max(255, 'Institution cannot exceed 255 characters').optional(),
  department: z.string().max(255, 'Department cannot exceed 255 characters').optional(),
  programme: z.string().max(255, 'Programme cannot exceed 255 characters').optional(),
  academicYear: z.string().max(40, 'Academic year cannot exceed 40 characters').optional(),
  supervisor: z.string().max(255, 'Supervisor cannot exceed 255 characters').optional(),
  courseName: z.string().max(255, 'Course name cannot exceed 255 characters').optional(),
  courseCode: z.string().max(80, 'Course code cannot exceed 80 characters').optional(),
  lecturer: z.string().max(255, 'Lecturer cannot exceed 255 characters').optional(),
  deadline: z.string().optional(),
  projectType: z.string().optional(),
  researchAim: z.string().max(5000, 'Research aim cannot exceed 5000 characters').optional(),
  studyArea: z.string().max(255, 'Study area cannot exceed 255 characters').optional(),
  researchType: z.string().max(100, 'Research type cannot exceed 100 characters').optional(),
  reportTemplateId: z.string().optional(),
  citationStyle: z.string().optional(),
  keywords: z.string().max(500, 'Keywords cannot exceed 500 characters').optional(),
});

type ProjectFormValues = z.infer<typeof projectFormSchema>;
type ProjectFormFieldName = keyof ProjectFormValues;

const PROJECT_FORM_FIELD_ORDER: ProjectFormFieldName[] = [
  'workspaceId',
  'workspaceType',
  'title',
  'description',
  'institution',
  'department',
  'programme',
  'academicYear',
  'supervisor',
  'courseName',
  'courseCode',
  'lecturer',
  'deadline',
  'projectType',
  'studyArea',
  'researchType',
  'reportTemplateId',
  'citationStyle',
  'researchAim',
  'keywords',
];

export interface CreateProjectModalProps {
  open: boolean;
  onClose: () => void;
  defaultWorkspaceId?: string;
  onCreated?: (project: ResearchProject) => void | Promise<void>;
}

function defaultValues(workspaceId: string): ProjectFormValues {
  return {
    workspaceId,
    workspaceType: 'ACADEMIC_RESEARCH',
    title: '',
    description: '',
    institution: '',
    department: '',
    programme: '',
    academicYear: '',
    supervisor: '',
    courseName: '',
    courseCode: '',
    lecturer: '',
    deadline: '',
    projectType: 'GENERAL_ACADEMIC_PROJECT',
    researchAim: '',
    studyArea: '',
    researchType: 'GENERAL_ACADEMIC_RESEARCH',
    reportTemplateId: '',
    citationStyle: 'APA_7',
    keywords: '',
  };
}

function clean(value?: string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

export function CreateProjectModal({ open, onClose, defaultWorkspaceId, onCreated }: CreateProjectModalProps) {
  const formId = useId();
  const formRef = useRef<HTMLFormElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { workspaces, selectedWorkspace, selectedWorkspaceId } = useWorkspace();
  const [step, setStep] = useState<1 | 2 | 3>(1);

  const templatesQuery = useQuery({
    queryKey: ['report-templates'],
    queryFn: () => reportApi.templates(),
    enabled: open,
  });

  const currentWorkspace =
    (defaultWorkspaceId ? workspaces.find((w) => w.id === defaultWorkspaceId) : null) ??
    selectedWorkspace ??
    (selectedWorkspaceId ? workspaces.find((w) => w.id === selectedWorkspaceId) : null) ??
    workspaces[0] ??
    null;

  const targetWorkspaceId = defaultWorkspaceId || currentWorkspace?.id || selectedWorkspaceId || '';

  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    defaultValues: defaultValues(targetWorkspaceId),
  });

  const workspaceType = form.watch('workspaceType');

  const compatibleTemplates = useMemo(
    () => (templatesQuery.data ?? []).filter((tpl) => templateSupportsWorkspace(tpl, workspaceType)),
    [templatesQuery.data, workspaceType],
  );

  useEffect(() => {
    if (open) {
      setStep(1);
      form.reset(defaultValues(targetWorkspaceId));
    }
  }, [open, targetWorkspaceId, form]);

  useEffect(() => {
    if (!open) return;
    const selected = form.getValues('reportTemplateId');
    const stillValid = selected && compatibleTemplates.some((tpl) => tpl.id === selected);
    if (!stillValid) {
      form.setValue('reportTemplateId', '');
    }
  }, [compatibleTemplates, form, open, workspaceType]);

  const [templateSource, setTemplateSource] = useState<'BUILTIN' | 'UPLOAD_GUIDELINE' | 'BLANK'>('BUILTIN');
  const [uploadedGuideline, setUploadedGuideline] = useState<AcademicDocumentGuidelineResponse | null>(null);
  const [approvedTemplate, setApprovedTemplate] = useState<ExtractedAcademicTemplate | null>(null);
  const [isUploadingGuideline, setIsUploadingGuideline] = useState(false);
  const [guidelineError, setGuidelineError] = useState<string | null>(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleGuidelineUpload = async (file: File) => {
    if (!targetWorkspaceId) return;
    setIsUploadingGuideline(true);
    setGuidelineError(null);
    try {
      const res = await academicTemplateApi.uploadGuideline(targetWorkspaceId, file, null);
      setUploadedGuideline(res);
      setApprovedTemplate(res.extractedTemplate);
      if (res.extractedTemplate.institution !== 'NOT_SPECIFIED' && !form.getValues('institution')) {
        form.setValue('institution', res.extractedTemplate.institution);
      }
      if (res.extractedTemplate.department !== 'NOT_SPECIFIED' && !form.getValues('department')) {
        form.setValue('department', res.extractedTemplate.department);
      }
      if (res.extractedTemplate.citationStyle !== 'NOT_SPECIFIED') {
        form.setValue('citationStyle', res.extractedTemplate.citationStyle);
      }
      setIsReviewOpen(true);
    } catch (err: any) {
      setGuidelineError(err?.message || 'Failed to upload and extract guideline.');
    } finally {
      setIsUploadingGuideline(false);
    }
  };

  const createMutation = useMutation({
    mutationFn: (values: ProjectFormValues) =>
      projectApi.create(values.workspaceId, {
        title: values.title.trim(),
        description: clean(values.description),
        workspaceType: values.workspaceType,
        projectType: values.workspaceType === 'ACADEMIC_PROJECT'
          ? (clean(values.projectType) as AcademicProjectType | undefined)
          : undefined,
        institution: clean(values.institution),
        department: clean(values.department),
        programme: clean(values.programme),
        academicYear: clean(values.academicYear),
        supervisor: clean(values.supervisor),
        courseName: values.workspaceType === 'COURSEWORK' ? clean(values.courseName) : undefined,
        courseCode: values.workspaceType === 'COURSEWORK' ? clean(values.courseCode) : undefined,
        lecturer: values.workspaceType === 'COURSEWORK' ? clean(values.lecturer) : undefined,
        deadline: values.workspaceType === 'COURSEWORK' ? clean(values.deadline) : undefined,
        researchAim: values.workspaceType === 'ACADEMIC_RESEARCH' ? clean(values.researchAim) : undefined,
        studyArea: values.workspaceType === 'ACADEMIC_RESEARCH' ? clean(values.studyArea) : undefined,
        researchType: values.workspaceType === 'ACADEMIC_RESEARCH' ? clean(values.researchType) : undefined,
        reportTemplateId: values.reportTemplateId || undefined,
        citationStyle: values.citationStyle || undefined,
        keywords: values.workspaceType === 'ACADEMIC_RESEARCH' ? clean(values.keywords) : undefined,
      }),
    onSuccess: async (newProject, variables) => {
      if (uploadedGuideline && approvedTemplate) {
        try {
          await academicTemplateApi.approveAndApply(uploadedGuideline.id, {
            targetProjectId: newProject.id,
            applyToProject: true,
            preserveExistingContent: false,
          });
        } catch (err) {
          console.warn('Failed to apply approved guideline structure:', err);
        }
      }
      queryClient.invalidateQueries({ queryKey: ['projects', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['projects', 'mine'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['workspace-dashboard', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['usage', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['billing', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['billing', variables.workspaceId, 'usage'] });
      onClose();
      form.reset();
      setUploadedGuideline(null);
      setApprovedTemplate(null);
      setTemplateSource('BUILTIN');
      if (onCreated) {
        void onCreated(newProject);
      } else {
        navigate(paths.project(newProject.id));
      }
    },
  });

  const handleClose = () => {
    if (!createMutation.isPending) {
      onClose();
    }
  };

  const focusFirstInvalidField = (errors: FieldErrors<ProjectFormValues>) => {
    const firstInvalidField = PROJECT_FORM_FIELD_ORDER.find((field) => Boolean(errors[field]));
    if (!firstInvalidField) return;

    const invalidControl = formRef.current?.querySelector<HTMLElement>(`[name="${firstInvalidField}"]`);
    if (!invalidControl) return;

    invalidControl.focus({ preventScroll: true });
    invalidControl.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  const submitProject = (values: ProjectFormValues) => {
    const finalWorkspaceId = values.workspaceId || targetWorkspaceId;
    if (!finalWorkspaceId) return;
    createMutation.mutate({ ...values, workspaceId: finalWorkspaceId });
  };

  const goNext = async () => {
    if (step === 1) {
      setStep(2);
      return;
    }
    const valid = await form.trigger(['workspaceId', 'workspaceType', 'title']);
    if (valid) setStep(3);
    else focusFirstInvalidField(form.formState.errors);
  };

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    if (step < 3) {
      event.preventDefault();
      void goNext();
      return;
    }
    void form.handleSubmit(submitProject, focusFirstInvalidField)(event);
  };

  const err = createMutation.error as { status?: number; code?: string; response?: { status?: number } } | null;
  const isQuotaError = err?.status === 429 || err?.code === 'QUOTA_EXCEEDED' || err?.response?.status === 429;

  const selectedCard = WORKSPACE_TYPE_CARDS.find((card) => card.type === workspaceType) ?? WORKSPACE_TYPE_CARDS[0];

  return (
    <Modal
      title="Create Workspace"
      open={open}
      onClose={handleClose}
      className="create-project-modal"
      closeDisabled={createMutation.isPending}
      footer={
        <>
          <Button type="button" variant="secondary" onClick={step === 1 ? handleClose : () => setStep((value) => (value === 3 ? 2 : 1))} disabled={createMutation.isPending}>
            {step === 1 ? 'Cancel' : 'Back'}
          </Button>
          {step < 3 ? (
            <Button type="button" onClick={() => void goNext()} disabled={!targetWorkspaceId}>
              Continue
            </Button>
          ) : (
            <LoadingButton
              type="submit"
              form={formId}
              loading={createMutation.isPending}
              loadingLabel="Creating..."
              disabled={!targetWorkspaceId}
              aria-label="Create Workspace & Launch"
            >
              Create Workspace
            </LoadingButton>
          )}
        </>
      }
    >
      {isQuotaError ? (
        <div className="alert warning" style={{ marginBottom: 16 }}>
          <strong>Plan Quota Exceeded</strong>
          <p style={{ margin: '6px 0 12px' }}>
            You've reached the workspace project limit on the Free plan.
          </p>
          <Button
            type="button"
            variant="primary"
            className="btn-compact"
            onClick={() => {
              onClose();
              navigate(paths.billing);
            }}
          >
            View Plans
          </Button>
        </div>
      ) : null}

      {createMutation.error && !isQuotaError ? (
        <div style={{ marginBottom: 16 }}>
          <ErrorState title="Failed to create workspace" error={createMutation.error} />
        </div>
      ) : null}

      {!targetWorkspaceId ? (
        <div className="alert warning" style={{ marginBottom: 16 }}>
          Select or create an account workspace before creating an academic workspace.
        </div>
      ) : null}

      <div className="workspace-modal-steps" aria-label="Workspace creation progress">
        {['Type', 'Details', 'Template'].map((label, index) => (
          <span key={label} className={index + 1 === step ? 'active' : index + 1 < step ? 'complete' : undefined}>
            {label}
          </span>
        ))}
      </div>

      <form ref={formRef} id={formId} className="form create-project-form" onSubmit={onSubmit}>
        <div className="form-group" style={{ marginBottom: 16 }}>
          <span className="label" style={{ fontSize: '0.85rem', color: 'var(--muted)', display: 'block', marginBottom: 4 }}>
            Account Workspace
          </span>
          <div className="readonly-field-display">
            <strong style={{ fontSize: '0.98rem' }}>{currentWorkspace?.name ?? (targetWorkspaceId ? 'Current Workspace' : 'No workspace selected')}</strong>
            {currentWorkspace?.type ? (
              <span className="muted" style={{ fontSize: '0.8rem', marginLeft: 8 }}>({currentWorkspace.type})</span>
            ) : null}
          </div>
          <input type="hidden" {...form.register('workspaceId')} value={targetWorkspaceId} />
          <input type="hidden" {...form.register('workspaceType')} />
        </div>

        {step === 1 ? (
          <section>
            <h4 className="workspace-modal-section-title">What are you working on?</h4>
            <div className="workspace-type-grid">
              {WORKSPACE_TYPE_CARDS.map((card) => {
                const Icon = card.icon;
                const active = workspaceType === card.type;
                return (
                  <button
                    key={card.type}
                    type="button"
                    className={`workspace-type-card ${active ? 'active' : ''}`}
                    onClick={() => {
                      form.setValue('workspaceType', card.type, { shouldDirty: true, shouldValidate: true });
                      if (card.type === 'ACADEMIC_RESEARCH') {
                        form.setValue('researchType', 'GENERAL_ACADEMIC_RESEARCH');
                      }
                      if (card.type === 'ACADEMIC_PROJECT') {
                        form.setValue('projectType', 'GENERAL_ACADEMIC_PROJECT');
                      }
                    }}
                  >
                    <Icon size={22} />
                    <strong>{card.title}</strong>
                    <span>{card.description}</span>
                  </button>
                );
              })}
            </div>
          </section>
        ) : null}

        {step === 2 ? (
          <section>
            <h4 className="workspace-modal-section-title">{selectedCard.title} Details</h4>
            <Field label={`${workspaceType === 'COURSEWORK' ? 'Assignment / Coursework Title' : workspaceType === 'ACADEMIC_PROJECT' ? 'Project Title' : 'Title / Research Topic'} *`} error={form.formState.errors.title?.message}>
              <Input
                placeholder={workspaceType === 'COURSEWORK' ? 'Software Development Methodologies Assignment' : workspaceType === 'ACADEMIC_PROJECT' ? 'Farmer-to-Buyer Agricultural Marketplace' : 'Digital Agriculture Adoption Among Smallholder Farmers'}
                {...form.register('title')}
                autoFocus
                disabled={createMutation.isPending || !targetWorkspaceId}
              />
            </Field>

            <Field label={workspaceType === 'COURSEWORK' ? 'Description / Instructions' : 'Description'} error={form.formState.errors.description?.message}>
              <Textarea
                placeholder={workspaceType === 'COURSEWORK' ? 'Brief instructions, scope, grading notes, or assignment context.' : 'Brief overview, context, or expected output.'}
                rows={2}
                {...form.register('description')}
                disabled={createMutation.isPending || !targetWorkspaceId}
              />
            </Field>

            {workspaceType === 'ACADEMIC_RESEARCH' ? (
              <>
                <div className="create-project-field-grid">
                  <Field label="Study Area / Domain" error={form.formState.errors.studyArea?.message}>
                    <Input placeholder="Agriculture, public health, education..." {...form.register('studyArea')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Research Type" error={form.formState.errors.researchType?.message}>
                    <Select {...form.register('researchType')} disabled={createMutation.isPending || !targetWorkspaceId}>
                      {RESEARCH_TYPES.map((t) => (
                        <option key={t.id} value={t.id}>{t.label}</option>
                      ))}
                    </Select>
                  </Field>
                </div>
                <Field label="Research Aim" error={form.formState.errors.researchAim?.message}>
                  <Textarea placeholder="State the main aim of the study." rows={2} {...form.register('researchAim')} disabled={createMutation.isPending || !targetWorkspaceId} />
                </Field>
                <Field label="Keywords" error={form.formState.errors.keywords?.message}>
                  <Input placeholder="adoption, smallholder farmers, digital agriculture" {...form.register('keywords')} disabled={createMutation.isPending || !targetWorkspaceId} />
                </Field>
              </>
            ) : null}

            {workspaceType === 'ACADEMIC_PROJECT' ? (
              <>
                <div className="create-project-field-grid">
                  <Field label="Project Type" error={form.formState.errors.projectType?.message}>
                    <Select {...form.register('projectType')} disabled={createMutation.isPending || !targetWorkspaceId}>
                      {PROJECT_TYPES.map((t) => (
                        <option key={t.id} value={t.id}>{t.label}</option>
                      ))}
                    </Select>
                  </Field>
                  <Field label="Institution" error={form.formState.errors.institution?.message}>
                    <Input {...form.register('institution')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
                <div className="create-project-field-grid">
                  <Field label="Department" error={form.formState.errors.department?.message}>
                    <Input {...form.register('department')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Programme" error={form.formState.errors.programme?.message}>
                    <Input {...form.register('programme')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
                <div className="create-project-field-grid">
                  <Field label="Academic Year" error={form.formState.errors.academicYear?.message}>
                    <Input placeholder="2026/2027" {...form.register('academicYear')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Supervisor" error={form.formState.errors.supervisor?.message}>
                    <Input {...form.register('supervisor')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
              </>
            ) : null}

            {workspaceType === 'COURSEWORK' ? (
              <>
                <div className="create-project-field-grid">
                  <Field label="Course Name" error={form.formState.errors.courseName?.message}>
                    <Input placeholder="Software Engineering" {...form.register('courseName')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Course Code" error={form.formState.errors.courseCode?.message}>
                    <Input placeholder="SENG 401" {...form.register('courseCode')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
                <div className="create-project-field-grid">
                  <Field label="Lecturer" error={form.formState.errors.lecturer?.message}>
                    <Input {...form.register('lecturer')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Deadline" error={form.formState.errors.deadline?.message}>
                    <Input type="date" {...form.register('deadline')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
                <div className="create-project-field-grid">
                  <Field label="Institution" error={form.formState.errors.institution?.message}>
                    <Input {...form.register('institution')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                  <Field label="Department" error={form.formState.errors.department?.message}>
                    <Input {...form.register('department')} disabled={createMutation.isPending || !targetWorkspaceId} />
                  </Field>
                </div>
              </>
            ) : null}
          </section>
        ) : null}

        {step === 3 ? (
          <section>
            <h4 className="workspace-modal-section-title">
              <FileText size={18} /> {FINAL_DOCUMENT_LABELS[workspaceType]} Setup
            </h4>

            {/* 3 Template Choices */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', marginBottom: '16px' }}>
              <button
                type="button"
                className={`button ${templateSource === 'BUILTIN' ? 'primary' : 'secondary'}`}
                style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 8px', gap: '6px' }}
                onClick={() => {
                  setTemplateSource('BUILTIN');
                  form.setValue('reportTemplateId', compatibleTemplates[0]?.id || '');
                }}
              >
                <Sparkles size={20} />
                <strong style={{ fontSize: '0.85rem' }}>Skilite Template</strong>
                <span className="muted" style={{ fontSize: '0.72rem', textAlign: 'center' }}>Recommended platform structures</span>
              </button>

              <button
                type="button"
                className={`button ${templateSource === 'UPLOAD_GUIDELINE' ? 'primary' : 'secondary'}`}
                style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 8px', gap: '6px' }}
                onClick={() => {
                  setTemplateSource('UPLOAD_GUIDELINE');
                  form.setValue('reportTemplateId', '');
                }}
              >
                <UploadCloud size={20} />
                <strong style={{ fontSize: '0.85rem' }}>Upload Guideline</strong>
                <span className="muted" style={{ fontSize: '0.72rem', textAlign: 'center' }}>PDF / DOCX official guideline</span>
              </button>

              <button
                type="button"
                className={`button ${templateSource === 'BLANK' ? 'primary' : 'secondary'}`}
                style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 8px', gap: '6px' }}
                onClick={() => {
                  setTemplateSource('BLANK');
                  form.setValue('reportTemplateId', '');
                }}
              >
                <FileText size={20} />
                <strong style={{ fontSize: '0.85rem' }}>Start Blank</strong>
                <span className="muted" style={{ fontSize: '0.72rem', textAlign: 'center' }}>Build custom document tree</span>
              </button>
            </div>

            {/* Upload Institution Template / Guideline */}
            {templateSource === 'UPLOAD_GUIDELINE' && (
              <div style={{ marginBottom: '16px' }}>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,.docx"
                  style={{ display: 'none' }}
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) void handleGuidelineUpload(file);
                  }}
                />

                {!uploadedGuideline ? (
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    style={{
                      border: '2px dashed var(--border-subtle, #cbd5e1)',
                      borderRadius: '8px',
                      padding: '24px 16px',
                      textAlign: 'center',
                      cursor: 'pointer',
                      background: 'var(--surface-sunken, rgba(0,0,0,0.02))'
                    }}
                  >
                    <UploadCloud size={32} style={{ margin: '0 auto 8px auto', color: 'var(--primary)' }} />
                    <strong>{isUploadingGuideline ? 'Analyzing Guideline Structure...' : 'Upload University / Department Guideline'}</strong>
                    <p className="muted" style={{ fontSize: '0.82rem', margin: '4px 0 0 0' }}>
                      Supports official PDF or DOCX instructions. The AI extracts required chapters and formatting rules.
                    </p>
                  </div>
                ) : (
                  <div
                    style={{
                      border: '1px solid var(--border-subtle, #cbd5e1)',
                      borderRadius: '8px',
                      padding: '12px 16px',
                      background: 'var(--surface-sunken, rgba(0,0,0,0.02))',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <CheckCircle2 size={16} color="var(--success, #16a34a)" />
                        <strong style={{ fontSize: '0.9rem' }}>{uploadedGuideline.originalFileName}</strong>
                        <Badge tone="success">Extracted</Badge>
                      </div>
                      <p className="muted" style={{ fontSize: '0.8rem', margin: '2px 0 0 0' }}>
                        {approvedTemplate?.institution !== 'NOT_SPECIFIED' ? approvedTemplate?.institution : 'Institution not specified'} • {approvedTemplate?.chapters.length} chapters detected
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <Button
                        type="button"
                        variant="secondary"
                        style={{ fontSize: '0.82rem' }}
                        onClick={() => setIsReviewOpen(true)}
                      >
                        <Eye size={14} style={{ marginRight: '4px' }} />
                        Review Structure
                      </Button>
                      <Button
                        type="button"
                        variant="secondary"
                        style={{ fontSize: '0.82rem' }}
                        onClick={() => fileInputRef.current?.click()}
                      >
                        Replace
                      </Button>
                    </div>
                  </div>
                )}

                {guidelineError && (
                  <div className="badge danger" style={{ marginTop: '8px', display: 'block' }}>
                    {guidelineError}
                  </div>
                )}
              </div>
            )}

            {/* Use Skilite Scholar Template */}
            {templateSource === 'BUILTIN' && (
              <div className="create-project-field-grid">
                <Field label={`${finalDocumentLabel(workspaceType)} Template`}>
                  <Select {...form.register('reportTemplateId')} disabled={createMutation.isPending || !targetWorkspaceId}>
                    <option value="">Default {finalDocumentLabel(workspaceType)}</option>
                    {compatibleTemplates.map((tpl) => (
                      <option key={tpl.id} value={tpl.id}>{tpl.name}</option>
                    ))}
                  </Select>
                </Field>

                <Field label="Citation Style">
                  <Select {...form.register('citationStyle')} disabled={createMutation.isPending || !targetWorkspaceId}>
                    {CITATION_STYLES.map((c) => (
                      <option key={c.id} value={c.id}>{c.label}</option>
                    ))}
                  </Select>
                </Field>
              </div>
            )}

            {/* Start Blank */}
            {templateSource === 'BLANK' && (
              <div style={{ marginBottom: '16px' }}>
                <Field label="Citation Style">
                  <Select {...form.register('citationStyle')} disabled={createMutation.isPending || !targetWorkspaceId}>
                    {CITATION_STYLES.map((c) => (
                      <option key={c.id} value={c.id}>{c.label}</option>
                    ))}
                  </Select>
                </Field>
                <div className="muted" style={{ fontSize: '0.85rem', marginTop: '6px' }}>
                  Starting blank creates a clean report. You can attach an institutional guideline or define custom chapters anytime later.
                </div>
              </div>
            )}

            <div className="workspace-create-summary">
              <strong>{WORKSPACE_TYPE_LABELS[workspaceType]}</strong>
              <span>
                {templateSource === 'UPLOAD_GUIDELINE' && uploadedGuideline
                  ? `Custom Guideline: ${uploadedGuideline.originalFileName}`
                  : templateSource === 'BLANK'
                  ? 'Blank Report Structure'
                  : finalDocumentLabel(workspaceType)}
              </span>
            </div>
          </section>
        ) : null}
      </form>

      {uploadedGuideline && (
        <TemplateReviewModal
          isOpen={isReviewOpen}
          guideline={uploadedGuideline}
          onClose={() => setIsReviewOpen(false)}
          onApproveAndApply={async (updated) => {
            setApprovedTemplate(updated);
            setIsReviewOpen(false);
          }}
        />
      )}
    </Modal>
  );
}
