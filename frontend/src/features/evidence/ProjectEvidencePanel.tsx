import React, { useState, useEffect } from 'react';
import {
  Upload,
  Image as ImageIcon,
  Sparkles,
  Trash2,
  Edit2,
  ArrowUp,
  ArrowDown,
  ExternalLink,
  Plus,
  Filter,
  Search,
  Check,
  AlertCircle,
  Loader2,
  FileText,
  Table as TableIcon,
  ChevronRight,
  Eye,
} from 'lucide-react';
import { Button, Input, Select, Badge, Card, Modal } from '../../components/ui';
import { projectEvidenceApi, reportApi } from '../../api/endpoints';
import type {
  ProjectEvidenceItem,
  EvidenceType,
  ProjectImageAnalysisResult,
  ReportStructureResponse,
} from '../../types/api';
import { InsertFigureModal } from './InsertFigureModal';

export interface ProjectEvidencePanelProps {
  projectId: string;
  reportId?: string;
  onInsertToEditor?: (evidence: ProjectEvidenceItem) => void;
}

export function ProjectEvidencePanel({
  projectId,
  reportId,
  onInsertToEditor,
}: ProjectEvidencePanelProps) {
  const [evidenceList, setEvidenceList] = useState<ProjectEvidenceItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Filters
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSectionFilter, setSelectedSectionFilter] = useState<string>('ALL');

  // Modals & Active state
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [previewItem, setPreviewItem] = useState<ProjectEvidenceItem | null>(null);
  const [editingItem, setEditingItem] = useState<ProjectEvidenceItem | null>(null);
  const [editCaption, setEditCaption] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editSectionId, setEditSectionId] = useState<string>('');
  const [editEvidenceType, setEditEvidenceType] = useState<EvidenceType>('SCREENSHOT');
  const [savingEdit, setSavingEdit] = useState(false);

  // Section list from report
  const [sections, setSections] = useState<{ id: string; title: string; chapter: string }[]>([]);

  // AI Analysis state per item
  const [analyzingId, setAnalyzingId] = useState<string | null>(null);
  const [analysisResultModal, setAnalysisResultModal] = useState<{
    item: ProjectEvidenceItem;
    result: ProjectImageAnalysisResult;
  } | null>(null);

  useEffect(() => {
    if (projectId) {
      loadEvidence();
      loadReportSections();
    }
  }, [projectId, reportId]);

  const loadEvidence = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await projectEvidenceApi.list(projectId);
      setEvidenceList(data);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to load project evidence');
    } finally {
      setLoading(false);
    }
  };

  const loadReportSections = async () => {
    if (!reportId) return;
    try {
      const struct = await reportApi.structure(reportId);
      const flat: { id: string; title: string; chapter: string }[] = [];
      struct.chapters?.forEach((chap) => {
        chap.sections?.forEach((sec) => {
          flat.push({
            id: sec.id,
            title: `${sec.sectionNumber ? sec.sectionNumber + ' ' : ''}${sec.heading || sec.type}`,
            chapter: chap.title,
          });
        });
      });
      setSections(flat);
    } catch (err) {
      console.warn('Could not load report sections for evidence panel', err);
    }
  };

  const handleEditOpen = (item: ProjectEvidenceItem) => {
    setEditingItem(item);
    setEditCaption(item.caption || '');
    setEditDescription(item.description || '');
    setEditSectionId(item.sectionId || '');
    setEditEvidenceType(item.evidenceType);
  };

  const handleEditSave = async () => {
    if (!editingItem) return;
    setSavingEdit(true);
    setError(null);
    try {
      const updated = await projectEvidenceApi.update(projectId, editingItem.id, {
        caption: editCaption.trim() || undefined,
        description: editDescription.trim() || undefined,
        sectionId: editSectionId ? editSectionId : null,
        evidenceType: editEvidenceType,
      });
      setEvidenceList((prev) => prev.map((e) => (e.id === updated.id ? updated : e)));
      setEditingItem(null);
      setSuccessMsg('Figure details updated successfully.');
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to update evidence');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDelete = async (evidenceId: string) => {
    if (!window.confirm('Are you sure you want to remove this project evidence item?')) return;
    try {
      await projectEvidenceApi.delete(projectId, evidenceId);
      setEvidenceList((prev) => prev.filter((e) => e.id !== evidenceId));
      setSuccessMsg('Evidence item removed safely.');
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to delete evidence');
    }
  };

  const handleReorder = async (index: number, direction: 'up' | 'down') => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= filteredList.length) return;

    const newOrdered = [...evidenceList];
    const sourceItem = filteredList[index];
    const targetItem = filteredList[targetIndex];

    const sourceIdx = newOrdered.findIndex((e) => e.id === sourceItem.id);
    const targetIdx = newOrdered.findIndex((e) => e.id === targetItem.id);

    const temp = newOrdered[sourceIdx];
    newOrdered[sourceIdx] = newOrdered[targetIdx];
    newOrdered[targetIdx] = temp;

    setEvidenceList(newOrdered);
    try {
      const reordered = await projectEvidenceApi.reorder(
        projectId,
        newOrdered.map((e) => e.id)
      );
      setEvidenceList(reordered);
    } catch (err) {
      console.error('Failed to persist reordering', err);
      loadEvidence();
    }
  };

  const handleAnalyze = async (item: ProjectEvidenceItem) => {
    setAnalyzingId(item.id);
    setError(null);
    try {
      const result = await projectEvidenceApi.analyze(projectId, item.id);
      setAnalysisResultModal({ item, result });
      // Update item in state with new analysis
      setEvidenceList((prev) =>
        prev.map((e) =>
          e.id === item.id
            ? {
                ...e,
                aiVisualAnalysis: result.visibleSummary || e.aiVisualAnalysis,
                aiAnalysisStatus: result.status,
              }
            : e
        )
      );
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Image analysis failed');
    } finally {
      setAnalyzingId(null);
    }
  };

  const filteredList = evidenceList.filter((item) => {
    if (typeFilter !== 'ALL' && item.evidenceType !== typeFilter) return false;
    if (selectedSectionFilter !== 'ALL' && item.sectionId !== selectedSectionFilter) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchCap = item.caption?.toLowerCase().includes(q);
      const matchFile = item.originalFilename.toLowerCase().includes(q);
      const matchLabel = item.figureLabel?.toLowerCase().includes(q);
      const matchDesc = item.description?.toLowerCase().includes(q);
      if (!matchCap && !matchFile && !matchLabel && !matchDesc) return false;
    }
    return true;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* Header and Controls */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '12px',
        }}
      >
        <div>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, margin: 0 }}>
            Project Evidence, Figures & Results
          </h3>
          <p className="muted" style={{ fontSize: '0.82rem', margin: '2px 0 0' }}>
            Real software screenshots, architecture diagrams, test outcomes, and data tables. Never fabricated.
          </p>
        </div>

        <Button
          variant="primary"
          onClick={() => setUploadModalOpen(true)}
          style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
        >
          <Plus size={15} /> Upload Figure / Evidence
        </Button>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div
          style={{
            padding: '8px 12px',
            background: 'rgba(34, 197, 94, 0.1)',
            border: '1px solid rgba(34, 197, 94, 0.3)',
            borderRadius: '6px',
            color: '#16a34a',
            fontSize: '0.84rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Check size={16} /> {successMsg}
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '8px 12px',
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '6px',
            color: '#dc2626',
            fontSize: '0.84rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {/* Filters Bar */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '10px',
          alignItems: 'center',
          padding: '10px 14px',
          background: 'var(--surface-hover, #f8fafc)',
          borderRadius: '8px',
          border: '1px solid var(--border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flex: 1, minWidth: '180px' }}>
          <Search size={15} className="muted" />
          <Input
            placeholder="Search figures or captions..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ fontSize: '0.82rem', height: '32px' }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--muted-foreground)' }}>Type:</span>
          <Select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            style={{ fontSize: '0.82rem', height: '32px' }}
          >
            <option value="ALL">All Types</option>
            <option value="SCREENSHOT">Screenshots</option>
            <option value="FIGURE">Figures</option>
            <option value="DIAGRAM">Diagrams</option>
            <option value="TABLE">Tables</option>
            <option value="TEST_RESULT">Test Results</option>
            <option value="CHART">Charts</option>
          </Select>
        </div>

        {sections.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--muted-foreground)' }}>Section:</span>
            <Select
              value={selectedSectionFilter}
              onChange={(e) => setSelectedSectionFilter(e.target.value)}
              style={{ fontSize: '0.82rem', height: '32px', maxWidth: '200px' }}
            >
              <option value="ALL">All Sections</option>
              {sections.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.title}
                </option>
              ))}
            </Select>
          </div>
        )}
      </div>

      {/* Grid of Evidence Cards */}
      {loading ? (
        <div style={{ padding: '40px', textAlign: 'center', color: 'var(--muted-foreground)' }}>
          <Loader2 size={28} className="animate-spin" style={{ margin: '0 auto 10px' }} />
          Loading project evidence and figures...
        </div>
      ) : filteredList.length === 0 ? (
        <div
          style={{
            padding: '48px 20px',
            textAlign: 'center',
            background: 'var(--surface)',
            border: '2px dashed var(--border)',
            borderRadius: '8px',
          }}
        >
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: '50%',
              background: 'rgba(37, 99, 235, 0.1)',
              color: 'var(--primary, #2563eb)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 12px',
            }}
          >
            <ImageIcon size={24} />
          </div>
          <h4 style={{ margin: '0 0 6px', fontWeight: 600 }}>No Project Evidence Found</h4>
          <p className="muted" style={{ fontSize: '0.85rem', maxWidth: '420px', margin: '0 auto 16px' }}>
            Upload real software screenshots, system diagrams, test logs, or data tables to ground your Chapter Four results and discussions.
          </p>
          <Button variant="primary" onClick={() => setUploadModalOpen(true)}>
            <Plus size={14} style={{ marginRight: 6 }} /> Add Your First Figure
          </Button>
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(290px, 1fr))',
            gap: '16px',
          }}
        >
          {filteredList.map((item, index) => (
            <Card
              key={item.id}
              style={{
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                borderRadius: '8px',
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
              }}
            >
              {/* Media Preview Box */}
              <div
                style={{
                  height: '160px',
                  background: '#0f172a',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                {item.downloadUrl ? (
                  <img
                    src={item.downloadUrl}
                    alt={item.caption || item.originalFilename}
                    onClick={() => setPreviewItem(item)}
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'contain',
                      cursor: 'pointer',
                    }}
                  />
                ) : (
                  <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                    <TableIcon size={40} style={{ margin: '0 auto 6px' }} />
                    <div style={{ fontSize: '0.78rem' }}>Structured Data Table</div>
                  </div>
                )}

                {/* Figure Numbering Badge */}
                <div
                  style={{
                    position: 'absolute',
                    top: '8px',
                    left: '8px',
                    background: 'rgba(15, 23, 42, 0.85)',
                    backdropFilter: 'blur(4px)',
                    color: '#fff',
                    padding: '3px 8px',
                    borderRadius: '4px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    letterSpacing: '0.02em',
                    border: '1px solid rgba(255, 255, 255, 0.2)',
                  }}
                >
                  {item.figureLabel || 'Figure'}
                </div>

                {/* Type Badge */}
                <div
                  style={{
                    position: 'absolute',
                    top: '8px',
                    right: '8px',
                  }}
                >
                  <Badge tone="info" style={{ fontSize: '0.7rem' }}>
                    {item.evidenceType}
                  </Badge>
                </div>
              </div>

              {/* Card Body */}
              <div style={{ padding: '12px 14px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ fontWeight: 600, fontSize: '0.92rem', color: 'var(--foreground)', lineHeight: 1.35 }}>
                  {item.caption || item.originalFilename}
                </div>

                {item.sectionHeading && (
                  <div
                    style={{
                      fontSize: '0.78rem',
                      color: 'var(--muted-foreground)',
                      marginTop: '6px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px',
                    }}
                  >
                    <ChevronRight size={13} />
                    <span>{item.sectionHeading}</span>
                  </div>
                )}

                {item.description && (
                  <p
                    className="muted"
                    style={{
                      fontSize: '0.78rem',
                      margin: '6px 0 0',
                      lineHeight: 1.4,
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden',
                    }}
                  >
                    {item.description}
                  </p>
                )}

                {/* AI Visual Analysis Status Badge */}
                <div style={{ marginTop: 'auto', paddingTop: '10px' }}>
                  {item.aiVisualAnalysis ? (
                    <Badge tone="success" style={{ fontSize: '0.72rem' }}>
                      <Check size={10} style={{ marginRight: 3 }} /> Visually Analyzed
                    </Badge>
                  ) : item.aiAnalysisStatus === 'ANALYZING' ? (
                    <Badge tone="warning" style={{ fontSize: '0.72rem' }}>
                      <Loader2 size={10} className="animate-spin" style={{ marginRight: 3 }} /> Analyzing
                    </Badge>
                  ) : null}
                </div>

                {/* Actions Toolbar */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    marginTop: '12px',
                    paddingTop: '10px',
                    borderTop: '1px solid var(--border)',
                  }}
                >
                  <div style={{ display: 'flex', gap: '4px' }}>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleAnalyze(item)}
                      disabled={analyzingId === item.id}
                      title="Explain visible screenshot elements using AI"
                      style={{ fontSize: '0.75rem', padding: '3px 7px' }}
                    >
                      {analyzingId === item.id ? (
                        <Loader2 size={13} className="animate-spin" />
                      ) : (
                        <Sparkles size={13} color="#8b5cf6" />
                      )}
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleEditOpen(item)}
                      title="Edit caption and section assignment"
                      style={{ fontSize: '0.75rem', padding: '3px 7px' }}
                    >
                      <Edit2 size={13} />
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleDelete(item.id)}
                      title="Delete evidence"
                      style={{ fontSize: '0.75rem', padding: '3px 7px', color: '#ef4444' }}
                    >
                      <Trash2 size={13} />
                    </Button>
                  </div>

                  <div style={{ display: 'flex', gap: '4px' }}>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleReorder(index, 'up')}
                      disabled={index === 0}
                      title="Move figure up"
                      style={{ fontSize: '0.75rem', padding: '3px 6px' }}
                    >
                      <ArrowUp size={13} />
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleReorder(index, 'down')}
                      disabled={index === filteredList.length - 1}
                      title="Move figure down"
                      style={{ fontSize: '0.75rem', padding: '3px 6px' }}
                    >
                      <ArrowDown size={13} />
                    </Button>
                    {onInsertToEditor && (
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => onInsertToEditor(item)}
                        title="Insert figure into open report section"
                        style={{ fontSize: '0.75rem', padding: '3px 8px' }}
                      >
                        Insert
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Upload Figure Modal */}
      <InsertFigureModal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        projectId={projectId}
        sectionId={selectedSectionFilter !== 'ALL' ? selectedSectionFilter : undefined}
        onInsert={(inserted) => {
          setEvidenceList((prev) => [inserted, ...prev]);
          if (onInsertToEditor) onInsertToEditor(inserted);
        }}
      />

      {/* Edit Evidence Details Modal */}
      {editingItem && (
        <Modal
          isOpen={true}
          onClose={() => setEditingItem(null)}
          title={`Edit ${editingItem.figureLabel || 'Figure'}`}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="secondary" onClick={() => setEditingItem(null)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleEditSave} disabled={savingEdit}>
                {savingEdit ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Evidence Type
              </label>
              <Select
                value={editEvidenceType}
                onChange={(e) => setEditEvidenceType(e.target.value as EvidenceType)}
                style={{ fontSize: '0.85rem', width: '100%' }}
              >
                <option value="SCREENSHOT">Screenshot</option>
                <option value="FIGURE">Figure</option>
                <option value="DIAGRAM">Diagram</option>
                <option value="ARCHITECTURE_DIAGRAM">Architecture Diagram</option>
                <option value="USE_CASE_DIAGRAM">Use Case Diagram</option>
                <option value="ER_DIAGRAM">ER Diagram</option>
                <option value="FLOWCHART">Flowchart</option>
                <option value="TABLE">Table</option>
                <option value="CHART">Chart</option>
                <option value="TEST_RESULT">Test Result</option>
                <option value="SYSTEM_OUTPUT">System Output</option>
                <option value="OTHER">Other</option>
              </Select>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Caption
              </label>
              <Input
                value={editCaption}
                onChange={(e) => setEditCaption(e.target.value)}
                placeholder="e.g. Farmer Product Listing Page"
                style={{ fontSize: '0.85rem', width: '100%' }}
              />
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Assigned Section
              </label>
              <Select
                value={editSectionId}
                onChange={(e) => setEditSectionId(e.target.value)}
                style={{ fontSize: '0.85rem', width: '100%' }}
              >
                <option value="">No Specific Section (Project-wide Evidence)</option>
                {sections.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.chapter} → {s.title}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Description
              </label>
              <textarea
                rows={3}
                value={editDescription}
                onChange={(e) => setEditDescription(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px',
                  fontSize: '0.85rem',
                  borderRadius: '6px',
                  border: '1px solid var(--border)',
                  background: 'var(--surface)',
                  color: 'var(--foreground)',
                }}
              />
            </div>
          </div>
        </Modal>
      )}

      {/* Image Full Preview Modal */}
      {previewItem && (
        <Modal
          isOpen={true}
          onClose={() => setPreviewItem(null)}
          title={`${previewItem.figureLabel || 'Figure'}: ${previewItem.caption || previewItem.originalFilename}`}
          size="lg"
        >
          <div style={{ textAlign: 'center' }}>
            {previewItem.downloadUrl && (
              <img
                src={previewItem.downloadUrl}
                alt={previewItem.caption || previewItem.originalFilename}
                style={{
                  maxWidth: '100%',
                  maxHeight: '520px',
                  objectFit: 'contain',
                  borderRadius: '6px',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                }}
              />
            )}
            <div style={{ marginTop: '12px', fontSize: '0.85rem', color: 'var(--muted-foreground)' }}>
              <strong>Rendered:</strong> {previewItem.renderedCaption}
            </div>
          </div>
        </Modal>
      )}

      {/* AI Image Analysis Feedback Modal */}
      {analysisResultModal && (
        <Modal
          isOpen={true}
          onClose={() => setAnalysisResultModal(null)}
          title={`AI Analysis: ${analysisResultModal.item.figureLabel || 'Figure'}`}
          size="lg"
          footer={
            <Button variant="primary" onClick={() => setAnalysisResultModal(null)}>
              Close
            </Button>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: '0.88rem' }}>
            <div
              style={{
                padding: '12px',
                borderRadius: '6px',
                background: 'rgba(139, 92, 246, 0.08)',
                border: '1px solid rgba(139, 92, 246, 0.25)',
              }}
            >
              <div style={{ fontWeight: 600, color: '#7c3aed', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                <Sparkles size={16} /> Visible Elements Analysis
              </div>
              <p style={{ margin: 0, color: 'var(--foreground)', lineHeight: 1.5 }}>
                {analysisResultModal.result.visibleSummary || 'No visible summary available.'}
              </p>
            </div>

            {analysisResultModal.result.observableComponents?.length > 0 && (
              <div>
                <div style={{ fontWeight: 600, marginBottom: 6 }}>Observable UI Components / Nodes:</div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                  {analysisResultModal.result.observableComponents.map((c, i) => (
                    <span
                      key={i}
                      style={{
                        padding: '3px 8px',
                        background: 'var(--surface-hover, #f1f5f9)',
                        border: '1px solid var(--border)',
                        borderRadius: '4px',
                        fontSize: '0.8rem',
                      }}
                    >
                      {c}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {analysisResultModal.result.observableWorkflow && (
              <div>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Observable Workflow:</div>
                <p className="muted" style={{ margin: 0 }}>
                  {analysisResultModal.result.observableWorkflow}
                </p>
              </div>
            )}

            {analysisResultModal.result.sectionRelevance && (
              <div>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Section Relevance:</div>
                <p className="muted" style={{ margin: 0 }}>
                  {analysisResultModal.result.sectionRelevance}
                </p>
              </div>
            )}

            {analysisResultModal.result.errorMessage && (
              <div
                style={{
                  padding: '10px 12px',
                  borderRadius: '6px',
                  background: 'rgba(245, 158, 11, 0.1)',
                  border: '1px solid rgba(245, 158, 11, 0.3)',
                  color: '#b45309',
                  fontSize: '0.82rem',
                }}
              >
                {analysisResultModal.result.errorMessage}
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
