import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { projectApi } from '../../api/endpoints';
import { Button, Field, Input, LoadingButton, Modal, Textarea } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { useWorkspace } from '../workspaces/WorkspaceProvider';
import { paths } from '../../routes/paths';

const projectFormSchema = z.object({
  workspaceId: z.string().min(1, 'Workspace is required'),
  title: z.string().trim().min(3, 'Title must be at least 3 characters').max(255, 'Title cannot exceed 255 characters'),
  description: z.string().max(2000, 'Description cannot exceed 2000 characters').optional(),
});

type ProjectFormValues = z.infer<typeof projectFormSchema>;

export interface CreateProjectModalProps {
  open: boolean;
  onClose: () => void;
  defaultWorkspaceId?: string;
}

export function CreateProjectModal({ open, onClose, defaultWorkspaceId }: CreateProjectModalProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { workspaces, selectedWorkspace, selectedWorkspaceId } = useWorkspace();

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
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        workspaceId: targetWorkspaceId,
        title: '',
        description: '',
      });
    }
  }, [open, targetWorkspaceId, form]);

  const createMutation = useMutation({
    mutationFn: (values: ProjectFormValues) =>
      projectApi.create(values.workspaceId, {
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
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

  const onSubmit = form.handleSubmit((values) => {
    const finalWorkspaceId = values.workspaceId || targetWorkspaceId;
    if (!finalWorkspaceId) return;
    createMutation.mutate({ ...values, workspaceId: finalWorkspaceId });
  });

  const err = createMutation.error as { status?: number; code?: string; response?: { status?: number } } | null;
  const isQuotaError = err?.status === 429 || err?.code === 'QUOTA_EXCEEDED' || err?.response?.status === 429;

  return (
    <Modal title="Create Research Project" open={open} onClose={onClose}>
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

      <form className="form" onSubmit={onSubmit}>
        <div className="form-group" style={{ marginBottom: 16 }}>
          <span className="label" style={{ fontSize: '0.85rem', color: 'var(--muted)', display: 'block', marginBottom: 4 }}>
            Workspace:
          </span>
          <div style={{ padding: '8px 12px', background: 'var(--surface-subtle, #f8fafc)', borderRadius: 6, border: '1px solid var(--border)' }}>
            <strong style={{ fontSize: '0.98rem' }}>{currentWorkspace?.name ?? (targetWorkspaceId ? 'Current Workspace' : 'No workspace selected')}</strong>
            {currentWorkspace?.type ? (
              <span className="muted" style={{ fontSize: '0.8rem', marginLeft: 8 }}>({currentWorkspace.type})</span>
            ) : null}
          </div>
          <input type="hidden" {...form.register('workspaceId')} value={targetWorkspaceId} />
        </div>

        <Field label="Research Project Title *" error={form.formState.errors.title?.message}>
          <Input
            placeholder="e.g., Deep Learning in Clinical Diagnostics"
            {...form.register('title')}
            autoFocus
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

        <Field label="Description (Optional)" error={form.formState.errors.description?.message}>
          <Textarea
            placeholder="Brief overview of research questions, objectives, or methodology scope..."
            rows={3}
            {...form.register('description')}
            disabled={createMutation.isPending || !targetWorkspaceId}
          />
        </Field>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 20 }}>
          <Button type="button" variant="secondary" onClick={onClose} disabled={createMutation.isPending}>
            Cancel
          </Button>
          <LoadingButton
            type="submit"
            loading={createMutation.isPending}
            disabled={!targetWorkspaceId}
            aria-label="Create Project & Launch"
          >
            Create Project
          </LoadingButton>
        </div>
      </form>
    </Modal>
  );
}
