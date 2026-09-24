import { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { reportApi } from '../../api/endpoints';
import { Modal, Button, Input, Badge } from '../ui';
import { Search, Check, AlertCircle } from 'lucide-react';

export interface InsertCitationModalProps {
  open: boolean;
  onClose: () => void;
  projectId: string;
  citationStyle?: string;
  onSelectReference: (reference: {
    id: string;
    citationKey: string;
    label: string;
    title: string;
  }) => void;
}

export function InsertCitationModal({
  open,
  onClose,
  projectId,
  citationStyle = 'APA_7',
  onSelectReference,
}: InsertCitationModalProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const referencesQuery = useQuery({
    queryKey: ['project-references', projectId],
    queryFn: () => reportApi.references(projectId, 0, 100),
    enabled: open && Boolean(projectId),
  });

  const referencesList = useMemo(() => {
    const raw = referencesQuery.data?.content ?? (referencesQuery.data as any) ?? [];
    return Array.isArray(raw) ? raw : [];
  }, [referencesQuery.data]);

  const filteredReferences = useMemo(() => {
    if (!searchTerm.trim()) return referencesList;
    const term = searchTerm.toLowerCase();
    return referencesList.filter((ref: any) => {
      const title = (ref.title || '').toLowerCase();
      const key = (ref.citationKey || '').toLowerCase();
      const authors = Array.isArray(ref.authors)
        ? ref.authors.map((a: any) => `${a.familyName || ''} ${a.givenName || ''} ${a.literalName || ''}`).join(' ').toLowerCase()
        : '';
      const year = String(ref.year || '');
      return title.includes(term) || key.includes(term) || authors.includes(term) || year.includes(term);
    });
  }, [referencesList, searchTerm]);

  const handleSelect = (ref: any) => {
    const authors = Array.isArray(ref.authors) ? ref.authors : [];
    const firstAuthor = authors[0]?.familyName || authors[0]?.literalName || 'Author';
    const year = ref.year || 'n.d.';

    let label: string;
    if (citationStyle === 'IEEE' || citationStyle === 'NUMERIC_APA' || citationStyle === 'VANCOUVER') {
      label = ref.citationKey || `[${ref.id.slice(0, 4)}]`;
    } else if (citationStyle === 'MLA_9') {
      label = `(${firstAuthor})`;
    } else {
      label = authors.length > 2 ? `(${firstAuthor} et al., ${year})` : `(${firstAuthor}, ${year})`;
    }

    onSelectReference({
      id: ref.id,
      citationKey: ref.citationKey,
      label,
      title: ref.title || 'Untitled Reference',
    });
    onClose();
  };

  return (
    <Modal title="Insert Academic Citation" open={open} onClose={onClose}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '480px', maxWidth: '640px' }}>
        <p className="muted" style={{ fontSize: '0.88rem' }}>
          Select a reference from your project library to insert a structured, dynamic citation into the document.
        </p>

        <div style={{ position: 'relative' }}>
          <Search size={16} style={{ position: 'absolute', left: 10, top: 12, color: 'var(--color-muted)' }} />
          <Input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by author, title, year, or citation key..."
            style={{ paddingLeft: '2.25rem' }}
            autoFocus
          />
        </div>

        <div
          style={{
            maxHeight: '340px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.5rem',
            border: '1px solid var(--border)',
            borderRadius: '6px',
            padding: '0.5rem',
          }}
        >
          {referencesQuery.isLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center' }} className="muted">
              Loading project references...
            </div>
          ) : filteredReferences.length === 0 ? (
            <div style={{ padding: '2rem', textAlign: 'center' }} className="muted">
              {referencesList.length === 0
                ? 'No references found in this project. Add or upload papers in the References library.'
                : 'No references match your search term.'}
            </div>
          ) : (
            filteredReferences.map((ref: any) => {
              const isSelected = selectedId === ref.id;
              const authors = Array.isArray(ref.authors)
                ? ref.authors.map((a: any) => a.familyName || a.literalName).filter(Boolean).join(', ')
                : '';
              const isAvailable = ref.availableForCitation !== false;
              const isComplete = ref.metadataStatus === 'VERIFIED' || (ref.title && ref.year && authors);

              return (
                <div
                  key={ref.id}
                  onClick={() => setSelectedId(ref.id)}
                  onDoubleClick={() => handleSelect(ref)}
                  style={{
                    padding: '0.65rem 0.85rem',
                    borderRadius: '6px',
                    border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border)',
                    backgroundColor: isSelected ? 'var(--surface-hover)' : 'transparent',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.25rem',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-main)' }}>
                      {ref.title || 'Untitled Reference'}
                    </div>
                    <div style={{ display: 'flex', gap: '0.25rem', flexShrink: 0 }}>
                      {ref.citationKey && <Badge style={{ fontFamily: 'monospace', fontSize: '0.72rem' }}>{ref.citationKey}</Badge>}
                      {isComplete ? (
                        <Badge tone="success" style={{ fontSize: '0.7rem' }}>Complete</Badge>
                      ) : (
                        <Badge tone="warning" style={{ fontSize: '0.7rem' }}>Incomplete</Badge>
                      )}
                    </div>
                  </div>

                  <div style={{ fontSize: '0.82rem', color: 'var(--color-muted)' }}>
                    {authors || 'Unknown authors'} {ref.year ? `(${ref.year})` : ''}
                    {ref.containerTitle ? ` • ${ref.containerTitle}` : ''}
                  </div>

                  {!isAvailable && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.75rem', color: 'var(--color-warning, #d97706)' }}>
                      <AlertCircle size={12} />
                      Marked unavailable for citation in project library
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="button"
            variant="primary"
            disabled={!selectedId}
            onClick={() => {
              const selected = filteredReferences.find((r: any) => r.id === selectedId);
              if (selected) handleSelect(selected);
            }}
          >
            <Check size={15} style={{ marginRight: 6 }} />
            Insert Citation
          </Button>
        </div>
      </div>
    </Modal>
  );
}
