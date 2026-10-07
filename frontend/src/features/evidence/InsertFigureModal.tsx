import React, { useState, useEffect, useRef } from 'react';
import {
  Upload,
  Image as ImageIcon,
  Sparkles,
  Check,
  AlertCircle,
  X,
  FileText,
  Eye,
  Loader2,
  Tag,
} from 'lucide-react';
import { Button, Input, Select, Badge, Modal } from '../../components/ui';
import { projectEvidenceApi } from '../../api/endpoints';
import type {
  ProjectEvidenceItem,
  EvidenceType,
  ProjectImageAnalysisResult,
} from '../../types/api';

export interface InsertFigureModalProps {
  isOpen: boolean;
  onClose: () => void;
  projectId: string;
  sectionId?: string;
  onInsert: (evidence: ProjectEvidenceItem) => void;
}

const EVIDENCE_TYPES: { value: EvidenceType; label: string }[] = [
  { value: 'SCREENSHOT', label: 'System Screenshot' },
  { value: 'FIGURE', label: 'Figure' },
  { value: 'DIAGRAM', label: 'System / Technical Diagram' },
  { value: 'ARCHITECTURE_DIAGRAM', label: 'Architecture Diagram' },
  { value: 'USE_CASE_DIAGRAM', label: 'Use Case Diagram' },
  { value: 'ER_DIAGRAM', label: 'Entity-Relationship Diagram' },
  { value: 'FLOWCHART', label: 'Process Flowchart' },
  { value: 'CHART', label: 'Statistical Chart / Graph' },
  { value: 'TABLE', label: 'Structured Table' },
  { value: 'TEST_RESULT', label: 'System Test Result' },
  { value: 'SYSTEM_OUTPUT', label: 'Terminal / System Output' },
  { value: 'DATASET_RESULT', label: 'Dataset Result' },
  { value: 'PHOTO', label: 'Fieldwork / Experimental Photo' },
  { value: 'USER_NOTE', label: 'User Observation / Note' },
  { value: 'OTHER', label: 'Other Project Evidence' },
];

export function InsertFigureModal({
  isOpen,
  onClose,
  projectId,
  sectionId,
  onInsert,
}: InsertFigureModalProps) {
  const [activeTab, setActiveTab] = useState<'upload' | 'library'>('upload');
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [evidenceType, setEvidenceType] = useState<EvidenceType>('SCREENSHOT');
  const [caption, setCaption] = useState('');
  const [description, setDescription] = useState('');
  const [altText, setAltText] = useState('');
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [savedEvidence, setSavedEvidence] = useState<ProjectEvidenceItem | null>(null);
  const [analysisResult, setAnalysisResult] = useState<ProjectImageAnalysisResult | null>(null);
  const [analysisError, setAnalysisError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Library tab state
  const [libraryItems, setLibraryItems] = useState<ProjectEvidenceItem[]>([]);
  const [loadingLibrary, setLoadingLibrary] = useState(false);
  const [selectedLibraryId, setSelectedLibraryId] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      resetForm();
      if (activeTab === 'library') {
        loadLibrary();
      }
    }
  }, [isOpen, projectId]);

  useEffect(() => {
    if (activeTab === 'library' && isOpen) {
      loadLibrary();
    }
  }, [activeTab]);

  const resetForm = () => {
    setFile(null);
    if (previewUrl && previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(null);
    setEvidenceType('SCREENSHOT');
    setCaption('');
    setDescription('');
    setAltText('');
    setUploading(false);
    setAnalyzing(false);
    setSavedEvidence(null);
    setAnalysisResult(null);
    setAnalysisError(null);
    setError(null);
    setSelectedLibraryId(null);
  };

  const loadLibrary = async () => {
    if (!projectId) return;
    setLoadingLibrary(true);
    try {
      const items = await projectEvidenceApi.list(projectId);
      setLibraryItems(items);
    } catch (err: any) {
      console.error('Failed to load project evidence library', err);
    } finally {
      setLoadingLibrary(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (!selected) return;

    if (!selected.type.startsWith('image/')) {
      setError('Please select a valid image file (PNG, JPEG, WEBP, GIF, SVG).');
      return;
    }
    if (selected.size > 25 * 1024 * 1024) {
      setError('Image file exceeds the 25 MB size limit.');
      return;
    }

    setError(null);
    setFile(selected);
    const objectUrl = URL.createObjectURL(selected);
    setPreviewUrl(objectUrl);
    setSavedEvidence(null);
    setAnalysisResult(null);
    setAnalysisError(null);

    // Auto-derive a friendly default caption from the filename if blank
    if (!caption.trim()) {
      const cleanName = selected.name
        .replace(/\.[^/.]+$/, '')
        .replace(/[-_]+/g, ' ')
        .replace(/\b\w/g, (c) => c.toUpperCase());
      setCaption(cleanName);
    }
  };

  const handleUploadOrSave = async (): Promise<ProjectEvidenceItem | null> => {
    if (savedEvidence) return savedEvidence;
    if (!file) {
      setError('Please select an image file to upload.');
      return null;
    }

    setUploading(true);
    setError(null);
    try {
      const res = await projectEvidenceApi.upload(projectId, file, {
        evidenceType,
        caption: caption.trim() || undefined,
        description: description.trim() || undefined,
        sectionId: sectionId || undefined,
        altText: altText.trim() || caption.trim() || undefined,
      });
      setSavedEvidence(res);
      return res;
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to upload figure';
      setError(msg);
      return null;
    } finally {
      setUploading(false);
    }
  };

  const handleExplainFigure = async () => {
    setError(null);
    setAnalysisError(null);

    let targetEvidence = savedEvidence;
    if (!targetEvidence) {
      targetEvidence = await handleUploadOrSave();
      if (!targetEvidence) return;
    }

    setAnalyzing(true);
    try {
      const result = await projectEvidenceApi.analyze(projectId, targetEvidence.id);
      setAnalysisResult(result);
      if (result.status === 'UNAVAILABLE' || result.status === 'FAILED') {
        setAnalysisError(
          result.errorMessage ||
            'AI Multimodal vision understanding is currently unavailable or unconfigured. You may enter a verified manual observation below.'
        );
      } else if (result.visibleSummary && !description.trim()) {
        setDescription(result.visibleSummary);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to analyze figure';
      setAnalysisError(msg);
    } finally {
      setAnalyzing(false);
    }
  };

  const handleConfirmInsert = async () => {
    if (activeTab === 'upload') {
      let evidenceToInsert = savedEvidence;
      if (!evidenceToInsert) {
        evidenceToInsert = await handleUploadOrSave();
        if (!evidenceToInsert) return;
      }
      onInsert(evidenceToInsert);
      onClose();
    } else {
      const selected = libraryItems.find((item) => item.id === selectedLibraryId);
      if (!selected) {
        setError('Please select a figure from the library to insert.');
        return;
      }
      onInsert(selected);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Insert Project Figure / Screenshot"
      size="lg"
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
          <div>
            {savedEvidence && (
              <Badge tone="success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                <Check size={12} /> Stored ({savedEvidence.figureLabel || 'Figure'})
              </Badge>
            )}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <Button variant="secondary" onClick={onClose}>
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmInsert}
              disabled={uploading || (activeTab === 'upload' && !file && !savedEvidence) || (activeTab === 'library' && !selectedLibraryId)}
            >
              {uploading ? (
                <>
                  <Loader2 size={14} className="animate-spin" style={{ marginRight: 6 }} /> Uploading...
                </>
              ) : (
                'Insert into Document'
              )}
            </Button>
          </div>
        </div>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {/* Navigation Tabs */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--border)',
            gap: '8px',
          }}
        >
          <button
            type="button"
            onClick={() => setActiveTab('upload')}
            style={{
              padding: '8px 16px',
              fontSize: '0.88rem',
              fontWeight: 600,
              background: 'transparent',
              border: 'none',
              borderBottom: activeTab === 'upload' ? '2px solid var(--primary, #2563eb)' : '2px solid transparent',
              color: activeTab === 'upload' ? 'var(--primary, #2563eb)' : 'var(--muted-foreground)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <Upload size={15} /> Upload New Figure
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('library')}
            style={{
              padding: '8px 16px',
              fontSize: '0.88rem',
              fontWeight: 600,
              background: 'transparent',
              border: 'none',
              borderBottom: activeTab === 'library' ? '2px solid var(--primary, #2563eb)' : '2px solid transparent',
              color: activeTab === 'library' ? 'var(--primary, #2563eb)' : 'var(--muted-foreground)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <ImageIcon size={15} /> Existing Evidence Library
          </button>
        </div>

        {error && (
          <div
            style={{
              padding: '10px 14px',
              borderRadius: '6px',
              background: 'rgba(239, 68, 68, 0.1)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              color: '#dc2626',
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {/* Tab 1: Upload New */}
        {activeTab === 'upload' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {/* File Upload Zone */}
            {!previewUrl ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                style={{
                  border: '2px dashed var(--border, #cbd5e1)',
                  borderRadius: '8px',
                  padding: '32px 20px',
                  textAlign: 'center',
                  cursor: 'pointer',
                  background: 'var(--surface-hover, #f8fafc)',
                  transition: 'all 0.2s ease',
                }}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                  onChange={handleFileChange}
                  style={{ display: 'none' }}
                />
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
                  <Upload size={22} />
                </div>
                <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--foreground)' }}>
                  Click to select screenshot or diagram
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--muted-foreground)', marginTop: 4 }}>
                  Supports PNG, JPEG, WEBP, GIF, SVG up to 25 MB
                </div>
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  gap: '14px',
                  alignItems: 'flex-start',
                  padding: '12px',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  background: 'var(--surface-hover, #f8fafc)',
                }}
              >
                <div
                  style={{
                    width: '180px',
                    height: '120px',
                    borderRadius: '6px',
                    overflow: 'hidden',
                    background: '#000',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    border: '1px solid var(--border)',
                  }}
                >
                  <img
                    src={previewUrl}
                    alt="Preview"
                    style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                  />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem', truncate: true }}>
                      {file ? file.name : savedEvidence?.originalFilename}
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setFile(null);
                        setPreviewUrl(null);
                        setSavedEvidence(null);
                        setAnalysisResult(null);
                      }}
                      style={{
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        color: 'var(--muted-foreground)',
                      }}
                    >
                      <X size={16} />
                    </button>
                  </div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--muted-foreground)', marginTop: 2 }}>
                    {file ? `${(file.size / 1024).toFixed(1)} KB` : ''} • {evidenceType}
                  </div>

                  {/* Explain Figure Action */}
                  <div style={{ marginTop: '12px' }}>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={handleExplainFigure}
                      disabled={analyzing || uploading}
                      style={{
                        fontSize: '0.82rem',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                      }}
                    >
                      {analyzing ? (
                        <>
                          <Loader2 size={13} className="animate-spin" /> Analyzing observable elements...
                        </>
                      ) : (
                        <>
                          <Sparkles size={13} style={{ color: '#8b5cf6' }} /> Explain Figure (AI Visual Inspection)
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            )}

            {/* AI Image Analysis Feedback Box */}
            {analysisResult && (
              <div
                style={{
                  padding: '12px 14px',
                  borderRadius: '6px',
                  background: 'rgba(139, 92, 246, 0.08)',
                  border: '1px solid rgba(139, 92, 246, 0.25)',
                  fontSize: '0.85rem',
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontWeight: 600,
                    color: '#7c3aed',
                    marginBottom: '6px',
                  }}
                >
                  <Sparkles size={14} /> Visible Element Analysis (No Invisible Code Fabricated)
                </div>
                {analysisResult.visibleSummary && (
                  <p style={{ margin: '0 0 8px', color: 'var(--foreground)', lineHeight: 1.45 }}>
                    {analysisResult.visibleSummary}
                  </p>
                )}
                {analysisResult.observableComponents && analysisResult.observableComponents.length > 0 && (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '6px' }}>
                    {analysisResult.observableComponents.map((comp, idx) => (
                      <span
                        key={idx}
                        style={{
                          fontSize: '0.75rem',
                          background: 'rgba(139, 92, 246, 0.15)',
                          color: '#6d28d9',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontWeight: 500,
                        }}
                      >
                        {comp}
                      </span>
                    ))}
                  </div>
                )}
                {analysisResult.observableWorkflow && (
                  <div style={{ fontSize: '0.8rem', color: 'var(--muted-foreground)', marginTop: '8px' }}>
                    <strong>Workflow:</strong> {analysisResult.observableWorkflow}
                  </div>
                )}
              </div>
            )}

            {analysisError && (
              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '6px',
                  background: 'rgba(245, 158, 11, 0.1)',
                  border: '1px solid rgba(245, 158, 11, 0.3)',
                  color: '#b45309',
                  fontSize: '0.82rem',
                }}
              >
                <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px', marginBottom: 4 }}>
                  <AlertCircle size={14} /> Provider Capability Notice
                </div>
                {analysisError}
              </div>
            )}

            {/* Evidence Metadata Fields */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                  Evidence Type
                </label>
                <Select
                  value={evidenceType}
                  onChange={(e) => setEvidenceType(e.target.value as EvidenceType)}
                  style={{ fontSize: '0.85rem', width: '100%' }}
                >
                  {EVIDENCE_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </Select>
              </div>

              <div>
                <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                  Figure Caption <span style={{ color: '#ef4444' }}>*</span>
                </label>
                <Input
                  type="text"
                  placeholder="e.g. Farmer Product Listing Page"
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                  style={{ fontSize: '0.85rem', width: '100%' }}
                />
              </div>
            </div>

            {/* Caption Live Preview */}
            <div
              style={{
                padding: '8px 12px',
                background: 'var(--surface-hover, #f8fafc)',
                borderRadius: '6px',
                border: '1px solid var(--border)',
                fontSize: '0.82rem',
                color: 'var(--muted-foreground)',
              }}
            >
              <strong style={{ color: 'var(--foreground)' }}>Rendered Format:</strong>{' '}
              <em>
                {savedEvidence?.figureLabel || 'Figure X.X'}: {caption || 'Your Caption'}
              </em>
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Description / Visible Details (used by AI Section Drafting)
              </label>
              <textarea
                rows={3}
                placeholder="Describe visible interface elements, purpose, or test conditions. The AI section draft references this actual context without hallucinating."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                style={{
                  width: '100%',
                  padding: '8px',
                  fontSize: '0.85rem',
                  borderRadius: '6px',
                  border: '1px solid var(--border)',
                  background: 'var(--surface)',
                  color: 'var(--foreground)',
                  resize: 'vertical',
                }}
              />
            </div>

            <div>
              <label style={{ fontSize: '0.82rem', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                Alt Text (Accessibility)
              </label>
              <Input
                type="text"
                placeholder="Accessible text description for screen readers"
                value={altText}
                onChange={(e) => setAltText(e.target.value)}
                style={{ fontSize: '0.85rem', width: '100%' }}
              />
            </div>
          </div>
        )}

        {/* Tab 2: Existing Evidence Library */}
        {activeTab === 'library' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {loadingLibrary ? (
              <div style={{ padding: '30px', textAlign: 'center', color: 'var(--muted-foreground)' }}>
                <Loader2 size={24} className="animate-spin" style={{ margin: '0 auto 8px' }} />
                Loading stored project evidence...
              </div>
            ) : libraryItems.length === 0 ? (
              <div
                style={{
                  padding: '30px',
                  textAlign: 'center',
                  background: 'var(--surface-hover, #f8fafc)',
                  borderRadius: '8px',
                  border: '1px dashed var(--border)',
                  color: 'var(--muted-foreground)',
                }}
              >
                No figures or screenshots uploaded for this project yet. Use the "Upload New Figure" tab to add one.
              </div>
            ) : (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
                  gap: '12px',
                  maxHeight: '380px',
                  overflowY: 'auto',
                  padding: '4px',
                }}
              >
                {libraryItems.map((item) => {
                  const isSelected = selectedLibraryId === item.id;
                  return (
                    <div
                      key={item.id}
                      onClick={() => setSelectedLibraryId(item.id)}
                      style={{
                        border: isSelected ? '2px solid var(--primary, #2563eb)' : '1px solid var(--border)',
                        borderRadius: '8px',
                        overflow: 'hidden',
                        cursor: 'pointer',
                        background: isSelected ? 'rgba(37, 99, 235, 0.04)' : 'var(--surface)',
                        transition: 'all 0.15s ease',
                        display: 'flex',
                        flexDirection: 'column',
                      }}
                    >
                      <div
                        style={{
                          height: '110px',
                          background: '#1e293b',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          overflow: 'hidden',
                        }}
                      >
                        {item.downloadUrl ? (
                          <img
                            src={item.downloadUrl}
                            alt={item.caption || item.originalFilename}
                            style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                          />
                        ) : (
                          <FileText size={32} color="#94a3b8" />
                        )}
                      </div>
                      <div style={{ padding: '8px 10px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontWeight: 700, fontSize: '0.8rem', color: 'var(--primary, #2563eb)' }}>
                            {item.figureLabel || 'Figure'}
                          </span>
                          <span style={{ fontSize: '0.7rem', color: 'var(--muted-foreground)' }}>
                            {item.evidenceType}
                          </span>
                        </div>
                        <div
                          style={{
                            fontSize: '0.82rem',
                            fontWeight: 500,
                            color: 'var(--foreground)',
                            marginTop: 4,
                            lineHeight: 1.3,
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {item.caption || item.originalFilename}
                        </div>
                        {item.sectionHeading && (
                          <div style={{ fontSize: '0.72rem', color: 'var(--muted-foreground)', marginTop: 'auto', paddingTop: 4 }}>
                            {item.sectionHeading}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
