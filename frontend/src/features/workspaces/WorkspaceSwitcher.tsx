import { useState, useRef, useEffect } from 'react';
import { Check, ChevronDown, Plus } from 'lucide-react';
import { Badge, Button } from '../../components/ui';
import { useWorkspace } from './WorkspaceProvider';
import { CreateWorkspaceModal } from './CreateWorkspaceModal';

export function WorkspaceSwitcher() {
  const [isOpen, setIsOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { workspaces, selectedWorkspace, selectedWorkspaceId, setSelectedWorkspaceId, isLoading } = useWorkspace();

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // Close on Escape key
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  // LOADING STATE
  if (isLoading && workspaces.length === 0) {
    return (
      <div className="workspace-switcher-loading" aria-label="Loading workspace">
        <span className="workspace-switcher-muted">Loading workspace...</span>
      </div>
    );
  }

  // ZERO WORKSPACES: Show "No workspace" and a Create Workspace button
  if (workspaces.length === 0) {
    return (
      <div className="workspace-switcher-empty">
        <span className="workspace-switcher-muted">No workspace</span>
        <Button
          type="button"
          variant="primary"
          className="btn-compact"
          onClick={() => setCreateModalOpen(true)}
        >
          <Plus size={14} /> Create Workspace
        </Button>
        <CreateWorkspaceModal open={createModalOpen} onClose={() => setCreateModalOpen(false)} />
      </div>
    );
  }

  // ONE OR MORE WORKSPACES: Compact trigger with dropdown
  return (
    <div className="workspace-switcher-wrapper" ref={dropdownRef}>
      <button
        type="button"
        className="workspace-switcher-btn"
        aria-haspopup="true"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label="Switch workspace"
        title="Switch workspace"
      >
        <div className="workspace-switcher-text">
          <span className="workspace-switcher-kicker">Workspace</span>
          <span className="workspace-switcher-name">
            {selectedWorkspace?.name ?? 'Select Workspace'}
          </span>
        </div>
        <ChevronDown size={14} className={`workspace-switcher-chevron ${isOpen ? 'open' : ''}`} />
      </button>

      {isOpen ? (
        <div className="workspace-switcher-menu" role="menu">
          <div className="workspace-switcher-header">
            <span>Workspaces</span>
            <Badge tone="info" className="badge-sm">
              {workspaces.length}
            </Badge>
          </div>

          <div className="workspace-switcher-items">
            {workspaces.map((ws) => {
              const isSelected = ws.id === selectedWorkspaceId;
              return (
                <button
                  key={ws.id}
                  type="button"
                  className={`workspace-switcher-item ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => {
                    setSelectedWorkspaceId(ws.id);
                    setIsOpen(false);
                  }}
                >
                  <div className="workspace-item-info">
                    <span className="workspace-item-name">{ws.name}</span>
                    <span className="workspace-item-sub">
                      {ws.type ?? 'PERSONAL'} • {ws.currentUserRole ?? ws.role ?? 'MEMBER'}
                    </span>
                  </div>
                  {isSelected ? <Check size={14} className="text-brand" /> : null}
                </button>
              );
            })}
          </div>

          <hr className="dropdown-divider" />

          <button
            type="button"
            className="workspace-switcher-create-action"
            onClick={() => {
              setIsOpen(false);
              setCreateModalOpen(true);
            }}
          >
            <Plus size={15} /> Create Workspace
          </button>
        </div>
      ) : null}

      <CreateWorkspaceModal open={createModalOpen} onClose={() => setCreateModalOpen(false)} />
    </div>
  );
}
