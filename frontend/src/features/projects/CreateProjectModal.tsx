import { useEffect, useId, useRef, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useForm, type FieldErrors } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { projectApi, reportApi } from '../../api/endpoints';
import { Button, Field, Input, LoadingButton, Modal, Select, Textarea } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { useWorkspace } from '../workspaces/WorkspaceProvider';
import { paths } from '../../routes/paths';

const RESEARCH_TYPES = [
  { id: 'SOFTWARE_SYSTEM_PROJECT', label: 'Software / System Project' },
  { id: 'QUANTITATIVE_SURVEY', label: 'Quantitative Survey' },
  { id: 'QUALITATIVE_RESEARCH', label: 'Qualitative Research' },
  { id: 'MIXED_METHODS', label: 'Mixed Methods' },
  { id: 'EXPERIMENTAL_RESEARCH', label: 'Experimental Research' },
  { id: 'CASE_STUDY', label: 'Case Study' },
  { id: 'LITERATURE_BASED_RESEARCH', label: 'Literature-Based Research' },
  { id: 'GENERAL_ACADEMIC_RESEARCH', label: 'General Academic Research' },
];

const CITATION_STYLES = [
  { id: 'APA_7', label: 'APA 7th Edition (Default)' },
  { id: 'IEEE', label: 'IEEE Numerical' },
  { id: 'HARVARD', label: 'Harvard Author-Date' },
  { id: 'CHICAGO_AUTHOR_DATE', label: 'Chicago Author-Date' },
  { id: 'VANCOUVER', label: 'Vancouver Numerical' },
  { id: 'MLA_9', label: 'MLA 9th Edition' },
];

const projectFormSchema = z.object({
  workspaceId: z.string().min(1, 'Workspace is required'),
  title: z.string().trim().min(3, 'Title must be at least 3 characters').max(255, 'Title cannot exceed 255 characters'),
  description: z.string().max(2000, 'Description cannot exceed 2000 characters').optional(),
  researchAim: z.string().max(3000, 'Research aim cannot exceed 3000 characters').optional(),
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
  'title',
  'description',
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
}

export function CreateProjectModal({ open, onClose, defaultWorkspaceId }: CreateProjectModalProps) {
  const formId = useId();
  const formRef = useRef<HTMLFormElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { workspaces, selectedWorkspace, selectedWorkspaceId } = useWorkspace();

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
    defaultValues: {
      workspaceId: targetWorkspaceId,
      title: '',
      description: '',
      researchAim: '',
      studyArea: '',
      researchType: 'SOFTWARE_SYSTEM_PROJECT',
      reportTemplateId: '',
      citationStyle: 'APA_7',
      keywords: '',
    },
  });

  useEffect(() => {
    if (open) {
      const defaultTpl = templatesQuery.data?.[0]?.id || '';
      form.reset({
        workspaceId: targetWorkspaceId,
        title: '',
        description: '',
        researchAim: '',
        studyArea: '',
        researchType: 'SOFTWARE_SYSTEM_PROJECT',
        reportTemplateId: defaultTpl,
        citationStyle: 'APA_7',
        keywords: '',
      });
    }
  }, [open, targetWorkspaceId, form, templatesQuery.data]);

  const createMutation = useMutation({
    mutationFn: (values: ProjectFormValues) =>
      projectApi.create(values.workspaceId, {
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
        researchAim: values.researchAim?.trim() || undefined,
        studyArea: values.studyArea?.trim() || undefined,
        researchType: values.researchType?.trim() || undefined,
        reportTemplateId: values.reportTemplateId || undefined,
        citationStyle: values.citationStyle || undefined,
        keywords: values.keywords?.trim() || undefined,
      }),
    onSuccess: (newProject, variables) => {
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
      navigate(paths.project(newProject.id));
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

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    void form.handleSubmit(submitProject, focusFirstInvalidField)(event);
  };

  const err = createMutation.error as { status?: number; code?: string; response?: { status?: number } } | null;
  const isQuotaError = err?.status === 429 || err?.code === 'QUOTA_EXCEEDED' || err?.response?.status === 429;

  return (
    <Modal
      title="Create Research Project"
      open={open}
      onClose={handleClose}
      className="create-project-modal"
      closeDisabled={createMutation.isPending}
      footer={
        <>
          <Button type="button" variant="secondary" onClick={handleClose} disabled={createMutation.isPending}>
            Cancel
          </Button>
          <LoadingButton
            type="submit"
            form={formId}
            loading={createMutation.isPending}
            loadingLabel="Creating..."
            disabled={!targetWorkspaceId}
            aria-label="Create Project & Launch"
          >
            Create Project
          </LoadingButton>
        </>
      }
    >
      {isQuotaError ? (
        <div className="alert warning" style={{ marginBottom: 16 }}>
          <strong>Plan Quota Exceeded</strong>
          <p style={{ margin: '6px 0 12px' }}>
            You've reached the project limit on the Free plan.
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
          <ErrorState title="Failed to create project" error={createMutation.error} />
        </div>
      ) : null}

      {!targetWorkspaceId ? (
        <div className="alert warning" style={{ marginBottom: 16 }}>
          Select or create a workspace before creating a project.
        </div>
      ) : null}

      <form ref={formRef} id={formId} className="form create-project-form" onSubmit={onSubmit}>
        <div className="form-group" style={{ marginBottom: 16 }}>
          <span className="label" style={{ fontSize: '0.85rem', color: 'var(--muted)', display: 'block', marginBottom: 4 }}>
            Workspace:
          </span>
          <div className="readonly-field-display">
            <strong style={{ fontSize: '0.98rem' }}>{currentWorkspace?.name ?? (targetWorkspaceId ? 'Current Workspace' : 'No workspace selected')}</strong>
            {currentWorkspace?.type ? (
              <span className="muted" style={{ fontSize: '0.8rem', marginLeft: 8 }}>({currentWorkspace.type})</span>
            ) : null}
          </div>
          <input type="hidden" {...form.register('workspaceId')} value={targetWorkspaceId} />
        </div>

        <Field label="Research Project Title / Topic *" error={form.formState.errors.title?.message}>
          <Input
            placeholder="e.g., Deep Learning in Clinical Diagnostics"
            {...form.register('title')}
            autoFocus
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

        <Field label="Project Description (Optional)" error={form.formState.errors.description?.message}>
          <Textarea
            placeholder="Brief overview of research questions, objectives, or context..."
            rows={2}
            {...form.register('description')}
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

        <div className="create-project-field-grid">
          <Field label="Study Area / Domain Context" error={form.formState.errors.studyArea?.message}>
            <Input
              placeholder="e.g., Computer Science, Healthcare"
              {...form.register('studyArea')}
              disabled={createMutation.isPending || !targetWorkspaceId}
            />
          </Field>

          <Field label="Research Type" error={form.formState.errors.researchType?.message}>
            <Select
              {...form.register('researchType')}
              disabled={createMutation.isPending || !targetWorkspaceId}
            >
              {RESEARCH_TYPES.map((t) => (
                <option key={t.id} value={t.id}>{t.label}</option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="create-project-field-grid">
          <Field label="Report Template">
            <Select
              {...form.register('reportTemplateId')}
              disabled={createMutation.isPending || !targetWorkspaceId}
            >
              {templatesQuery.data?.map((tpl) => (
                <option key={tpl.id} value={tpl.id}>{tpl.name}</option>
              )) ?? <option value="">TTU Computer Science Final Project Report</option>}
            </Select>
          </Field>

          <Field label="Citation Style">
            <Select
              {...form.register('citationStyle')}
              disabled={createMutation.isPending || !targetWorkspaceId}
            >
              {CITATION_STYLES.map((c) => (
                <option key={c.id} value={c.id}>{c.label}</option>
              ))}
            </Select>
          </Field>
        </div>

        <Field label="Research Aim / Goal" error={form.formState.errors.researchAim?.message}>
          <Textarea
            placeholder="e.g., Synthesize recent breakthroughs in diagnostic transformer architectures and identify clinical deployment bottlenecks."
            rows={2}
            {...form.register('researchAim')}
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

        <Field label="Keywords (comma-separated)" error={form.formState.errors.keywords?.message}>
          <Input
            placeholder="e.g., neural networks, radiology, transformer models"
            {...form.register('keywords')}
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

      </form>
    </Modal>
  );
}
