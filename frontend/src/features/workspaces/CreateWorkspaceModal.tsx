import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { workspaceApi } from '../../api/endpoints';
import { Button, Field, Input, LoadingButton, Modal, Select } from '../../components/ui';
import { ErrorState } from '../../components/states';
import { useWorkspace } from './WorkspaceProvider';
import { paths } from '../../routes/paths';
import type { Workspace } from '../../types/api';

const workspaceFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Workspace name must be at least 2 characters')
    .max(255, 'Workspace name cannot exceed 255 characters'),
  type: z.enum(['ORGANIZATION', 'PERSONAL']),
});

type WorkspaceFormValues = z.infer<typeof workspaceFormSchema>;

export interface CreateWorkspaceModalProps {
  open: boolean;
  onClose: () => void;
  onCreated?: (workspaceId: string) => void;
}

export function CreateWorkspaceModal({ open, onClose, onCreated }: CreateWorkspaceModalProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { setSelectedWorkspace } = useWorkspace();

  const form = useForm<WorkspaceFormValues>({
    resolver: zodResolver(workspaceFormSchema),
    defaultValues: {
      name: '',
      type: 'ORGANIZATION',
    },
  });

  useEffect(() => {
    if (open) {
      form.reset({
        name: '',
        type: 'ORGANIZATION',
      });
    }
  }, [open, form]);

  const createMutation = useMutation({
    mutationFn: (values: WorkspaceFormValues) =>
      workspaceApi.create({
        name: values.name.trim(),
        type: values.type,
      }),
    onSuccess: (newWorkspace) => {
      queryClient.setQueryData<Workspace[]>(['workspaces'], (old = []) => {
        const filtered = old.filter((w) => w.id !== newWorkspace.id);
        return [newWorkspace, ...filtered];
      });
      setSelectedWorkspace(newWorkspace);
      queryClient.invalidateQueries({ queryKey: ['workspaces'] });
      queryClient.invalidateQueries({ queryKey: ['billing', newWorkspace.id] });
      queryClient.invalidateQueries({ queryKey: ['projects', newWorkspace.id] });
      queryClient.invalidateQueries({ queryKey: ['workspace-dashboard', newWorkspace.id] });
      queryClient.invalidateQueries({ queryKey: ['usage', newWorkspace.id] });
      onClose();
      form.reset();
      if (onCreated) {
        onCreated(newWorkspace.id);
      } else {
        navigate(paths.dashboard);
      }
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    createMutation.mutate(values);
  });

  return (
    <Modal title="Create New Workspace" open={open} onClose={onClose}>
      {createMutation.error ? (
        <div style={{ marginBottom: 16 }}>
          <ErrorState title="Failed to create workspace" error={createMutation.error} />
        </div>
      ) : null}

      <form className="form" onSubmit={onSubmit}>
        <Field label="Workspace Name *" error={form.formState.errors.name?.message}>
          <Input
            placeholder="e.g., Clinical Trials Research Group"
            {...form.register('name')}
            autoFocus
            disabled={createMutation.isPending}
          />
        </Field>

        <Field label="Workspace Type" error={form.formState.errors.type?.message}>
          <Select {...form.register('type')} disabled={createMutation.isPending}>
            <option value="ORGANIZATION">Organization (Collaborative Team / Lab)</option>
          </Select>
        </Field>

        <p className="muted" style={{ fontSize: '0.85rem', marginTop: 4 }}>
          New workspaces are provisioned with the <strong>FREE</strong> plan by default.
          You can immediately add members, create research projects, and access core empirical tools.
        </p>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 24 }}>
          <Button type="button" variant="secondary" onClick={onClose} disabled={createMutation.isPending}>
            Cancel
          </Button>
          <LoadingButton type="submit" loading={createMutation.isPending}>
            Create Workspace
          </LoadingButton>
        </div>
      </form>
    </Modal>
  );
}
