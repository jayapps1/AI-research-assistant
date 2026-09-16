import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { projectApi } from '../../api/endpoints';
import { Button, Field, Input, LoadingButton, Modal, Select, Textarea } from '../../components/ui';
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
  const { workspaces, selectedWorkspaceId } = useWorkspace();

  const effectiveWorkspaceId = defaultWorkspaceId || selectedWorkspaceId || (workspaces[0]?.id ?? '');

  const form = useForm<ProjectFormValues>({
    resolver: zodResolver(projectFormSchema),
    defaultValues: {
      workspaceId: effectiveWorkspaceId,
      title: '',
      description: '',
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        workspaceId: effectiveWorkspaceId,
        title: '',
        description: '',
      });
    }
  }, [open, effectiveWorkspaceId, form]);

  const createMutation = useMutation({
    mutationFn: (values: ProjectFormValues) =>
      projectApi.create(values.workspaceId, {
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
      }),
    onSuccess: (newProject, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects', variables.workspaceId] });
      queryClient.invalidateQueries({ queryKey: ['projects', 'mine'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['billing', variables.workspaceId, 'usage'] });
      onClose();
      form.reset();
      navigate(paths.project(newProject.id));
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    createMutation.mutate(values);
  });

  const isQuotaError = (createMutation.error as { status?: number })?.status === 429;

  return (
    <Modal title="Create Research Project" open={open} onClose={onClose}>
      {isQuotaError ? (
        <div className="alert warning" style={{ marginBottom: 16 }}>
          <strong>Plan Quota Exceeded</strong>
          <p>
            You have reached the maximum number of research projects allowed for your current workspace plan.
            Please visit the Billing section to upgrade or free up project capacity.
          </p>
        </div>
      ) : null}

      {createMutation.error && !isQuotaError ? (
        <div style={{ marginBottom: 16 }}>
          <ErrorState title="Failed to create project" error={createMutation.error} />
        </div>
      ) : null}

      <form className="form" onSubmit={onSubmit}>
        <Field label="Workspace" error={form.formState.errors.workspaceId?.message}>
          <Select {...form.register('workspaceId')} disabled={createMutation.isPending || workspaces.length <= 1}>
            {workspaces.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name} {w.type ? `(${w.type})` : ''}
              </option>
            ))}
          </Select>
        </Field>

        <Field label="Research Project Title *" error={form.formState.errors.title?.message}>
          <Input
            placeholder="e.g., Deep Learning in Clinical Diagnostics"
            {...form.register('title')}
            autoFocus
            disabled={createMutation.isPending}
          />
        </Field>

        <Field label="Description (Optional)" error={form.formState.errors.description?.message}>
          <Textarea
            placeholder="Brief overview of research questions, objectives, or methodology scope..."
            rows={3}
            {...form.register('description')}
            disabled={createMutation.isPending}
          />
        </Field>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 20 }}>
          <Button type="button" variant="secondary" onClick={onClose} disabled={createMutation.isPending}>
            Cancel
          </Button>
          <LoadingButton type="submit" loading={createMutation.isPending} disabled={!effectiveWorkspaceId}>
            Create Project & Launch
          </LoadingButton>
        </div>
      </form>
    </Modal>
  );
}
