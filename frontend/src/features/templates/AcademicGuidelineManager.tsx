import { useState, useRef } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  ListTree,
  ShieldCheck,
  RefreshCw
} from 'lucide-react';
import { academicTemplateApi } from '../../api/endpoints';
import { Badge, Button, Modal } from '../../components/ui';
import type {
  AcademicDocumentGuidelineResponse,
  ExtractedAcademicTemplate,
} from '../../types/api';
import { TemplateReviewModal } from './TemplateReviewModal';

interface AcademicGuidelineManagerProps {
  workspaceId: string;
  projectId: string;
  reportId?: string;
  onStructureUpdated?: () => void;
}

export function AcademicGuidelineManager({
  workspaceId,
  projectId,
  reportId,
  onStructureUpdated,
}: AcademicGuidelineManagerProps) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [activeGuideline, setActiveGuideline] = useState<AcademicDocumentGuidelineResponse | null>(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [isTocOpen, setIsTocOpen] = useState(false);
  const [isValidationOpen, setIsValidationOpen] = useState(false);

  // Dynamic TOC Query
  const tocQuery = useQuery({
    queryKey: ['report-dynamic-toc', reportId],
    queryFn: () => academicTemplateApi.getDynamicToc(reportId!),
    enabled: Boolean(reportId && isTocOpen),
  });

  // Structure Validation Query
  const validationQuery = useQuery({
    queryKey: ['report-structure-validation', reportId],
    queryFn: () => academicTemplateApi.validateStructure(reportId!),
    enabled: Boolean(reportId),
  });

  const handleFileUpload = async (file: File) => {
    setIsUploading(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const guideline = await academicTemplateApi.uploadGuideline(workspaceId, file, projectId);
      setActiveGuideline(guideline);
      setIsReviewOpen(true);
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to upload and extract guideline.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleApproveAndApply = async (updatedTemplate: ExtractedAcademicTemplate, preserveContent: boolean) => {
    if (!activeGuideline) return;
    try {
      // 1. If edited, save template updates
      await academicTemplateApi.updateGuideline(activeGuideline.id, updatedTemplate);

      // 2. Approve and apply to project report
      await academicTemplateApi.approveAndApply(activeGuideline.id, {
        targetProjectId: projectId,
        applyToProject: true,
        preserveExistingContent: preserveContent,
      });

      setSuccessMessage('Guideline approved and applied successfully to project report!');
      setIsReviewOpen(false);
      setActiveGuideline(null);

      // Invalidate queries
      queryClient.invalidateQueries({ queryKey: ['reports', projectId] });
      queryClient.invalidateQueries({ queryKey: ['report-chapters', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-sections'] });
      queryClient.invalidateQueries({ queryKey: ['report-structure', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-dynamic-toc', reportId] });
      queryClient.invalidateQueries({ queryKey: ['report-structure-validation', reportId] });

      if (onStructureUpdated) {
        onStructureUpdated();
      }

      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to apply template structure.');
    }
  };

  const validation = validationQuery.data;

  return (
    <div className="academic-guideline-manager" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      {/* Hidden file input for guideline upload */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.docx"
        style={{ display: 'none' }}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) void handleFileUpload(file);
          e.target.value = '';
        }}
      />

      {/* Control Buttons */}
      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
        <Button
          type="button"
          variant="secondary"
          disabled={isUploading}
          onClick={() => fileInputRef.current?.click()}
          style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          {isUploading ? <RefreshCw size={14} className="spin" /> : <UploadCloud size={14} />}
          {isUploading ? 'Analyzing Guideline...' : 'Attach / Change Guideline'}
        </Button>

        {reportId && (
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsTocOpen(true)}
              style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <ListTree size={14} />
              Dynamic Table of Contents
            </Button>

            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsValidationOpen(true)}
              style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <ShieldCheck size={14} />
              Structure Validation
              {validation && !validation.valid && (
                <Badge tone="warning" style={{ marginLeft: '4px', fontSize: '0.7rem' }}>
                  {validation.missingRequiredCount} Missing
                </Badge>
              )}
              {validation && validation.valid && (
                <Badge tone="success" style={{ marginLeft: '4px', fontSize: '0.7rem' }}>
                  Compliant
                </Badge>
              )}
            </Button>
          </>
        )}
      </div>

      {/* Feedback Messages */}
      {successMessage && (
        <div className="alert success" style={{ fontSize: '0.84rem', padding: '6px 12px' }}>
          {successMessage}
        </div>
      )}
      {errorMessage && (
        <div className="alert danger" style={{ fontSize: '0.84rem', padding: '6px 12px' }}>
          {errorMessage}
        </div>
      )}

      {/* Template Review Modal */}
      {activeGuideline && (
        <TemplateReviewModal
          isOpen={isReviewOpen}
          guideline={activeGuideline}
          onClose={() => {
            setIsReviewOpen(false);
            setActiveGuideline(null);
          }}
          onApproveAndApply={handleApproveAndApply}
        />
      )}

      {/* Dynamic TOC Modal */}
      {isTocOpen && (
        <Modal
          open={isTocOpen}
          onClose={() => setIsTocOpen(false)}
          title="Dynamic Table of Contents"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <p className="muted" style={{ fontSize: '0.85rem', margin: 0 }}>
              Generated live from the current report chapters, sections, and subsections. Page numbers update dynamically.
            </p>

            {tocQuery.isLoading ? (
              <div style={{ textAlign: 'center', padding: '24px 0' }}>
                <RefreshCw size={24} className="spin muted" />
                <p className="muted" style={{ fontSize: '0.85rem', marginTop: '8px' }}>Generating dynamic TOC...</p>
              </div>
            ) : tocQuery.data?.items ? (
              <div
                style={{
                  maxHeight: '55vh',
                  overflowY: 'auto',
                  border: '1px solid var(--border-subtle, #e2e8f0)',
                  borderRadius: '6px',
                  padding: '12px'
                }}
              >
                {tocQuery.data.items.map((item, idx) => {
                  const isChapter = item.level === 0;
                  const indent = (item.level) * 18;
                  return (
                    <div
                      key={item.id || idx}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'baseline',
                        padding: '4px 0',
                        paddingLeft: `${indent}px`,
                        fontWeight: isChapter ? 700 : item.level === 1 ? 500 : 400,
                        fontSize: isChapter ? '0.92rem' : '0.86rem',
                        borderBottom: isChapter ? '1px solid var(--border-subtle, #f1f5f9)' : 'none',
                        marginTop: isChapter ? '6px' : '0'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {item.sectionNumber && (
                          <span style={{ color: 'var(--muted)', minWidth: '32px' }}>{item.sectionNumber}</span>
                        )}
                        <span>{item.title}</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
                        <span style={{ borderBottom: '1px dotted var(--muted)', flexGrow: 1, minWidth: '40px' }} />
                        <span style={{ fontFamily: 'monospace', fontSize: '0.85rem', color: 'var(--muted)' }}>
                          {item.pageNumber}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="muted" style={{ textAlign: 'center', padding: '16px' }}>No sections found in report.</div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
              <Button variant="secondary" onClick={() => setIsTocOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Structure Validation Modal */}
      {isValidationOpen && (
        <Modal
          open={isValidationOpen}
          onClose={() => setIsValidationOpen(false)}
          title="Document Structure Validation"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {validation ? (
              <>
                <div
                  style={{
                    padding: '12px 16px',
                    borderRadius: '8px',
                    background: validation.valid ? 'rgba(22, 163, 74, 0.08)' : 'rgba(234, 179, 8, 0.1)',
                    border: `1px solid ${validation.valid ? 'rgba(22, 163, 74, 0.3)' : 'rgba(234, 179, 8, 0.3)'}`,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px'
                  }}
                >
                  {validation.valid ? (
                    <CheckCircle2 size={24} color="var(--success, #16a34a)" />
                  ) : (
                    <AlertTriangle size={24} color="var(--warning, #ca8a04)" />
                  )}
                  <div>
                    <strong>{validation.valid ? 'Guideline Compliant' : 'Structure Warnings Detected'}</strong>
                    <p className="muted" style={{ margin: '2px 0 0 0', fontSize: '0.84rem' }}>
                      {validation.totalPresent} of {validation.totalRequired} required sections present.
                      {!validation.valid && ' Missing required sections produce warnings but do not block drafting.'}
                    </p>
                  </div>
                </div>

                {validation.warnings && validation.warnings.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <strong style={{ fontSize: '0.88rem' }}>Warnings:</strong>
                    {validation.warnings.map((w, idx) => (
                      <div
                        key={idx}
                        style={{
                          padding: '6px 10px',
                          background: 'var(--surface-sunken, rgba(0,0,0,0.02))',
                          borderRadius: '6px',
                          fontSize: '0.84rem',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px'
                        }}
                      >
                        <AlertTriangle size={14} color="var(--warning, #ca8a04)" />
                        <span>{w}</span>
                      </div>
                    ))}
                  </div>
                )}

                {validation.missingRequiredSections && validation.missingRequiredSections.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <strong style={{ fontSize: '0.88rem', color: 'var(--danger)' }}>
                      Missing Required Sections ({validation.missingRequiredSections.length}):
                    </strong>
                    <div style={{ maxHeight: '40vh', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                      {validation.missingRequiredSections.map((m, idx) => (
                        <div
                          key={idx}
                          style={{
                            padding: '8px 12px',
                            border: '1px solid var(--border-subtle, #e2e8f0)',
                            borderRadius: '6px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            fontSize: '0.85rem'
                          }}
                        >
                          <div>
                            <strong>{m.heading}</strong>
                            <div className="muted" style={{ fontSize: '0.78rem' }}>
                              In Chapter: {m.chapterTitle}
                            </div>
                            {m.description && (
                              <div className="muted" style={{ fontSize: '0.78rem', fontStyle: 'italic' }}>
                                Guideline: {m.description}
                              </div>
                            )}
                          </div>
                          <Badge tone="danger">REQUIRED</Badge>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="muted" style={{ textAlign: 'center', padding: '16px' }}>
                No active guideline validation data available.
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
              <Button variant="secondary" onClick={() => setIsValidationOpen(false)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
