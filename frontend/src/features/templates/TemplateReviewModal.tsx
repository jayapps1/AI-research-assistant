import { useState } from 'react';
import {
  AlertTriangle,
  BookOpen,
  ChevronDown,
  ChevronRight,
  Edit3,
  FileCheck,
  Layers,
  Settings
} from 'lucide-react';
import { Button, Card, Badge, Input, Select, Modal } from '../../components/ui';
import type {
  AcademicDocumentGuidelineResponse,
  ExtractedAcademicTemplate,
  SectionRequirementLevel
} from '../../types/api';

interface TemplateReviewModalProps {
  guideline: AcademicDocumentGuidelineResponse;
  isOpen: boolean;
  onClose: () => void;
  onApproveAndApply: (updatedTemplate: ExtractedAcademicTemplate, preserveContent: boolean) => Promise<void>;
  isApplying?: boolean;
}

export function TemplateReviewModal({
  guideline,
  isOpen,
  onClose,
  onApproveAndApply,
  isApplying = false,
}: TemplateReviewModalProps) {
  const [template, setTemplate] = useState<ExtractedAcademicTemplate>(() =>
    JSON.parse(JSON.stringify(guideline.extractedTemplate))
  );
  const [isEditing, setIsEditing] = useState(false);
  const [preserveContent, setPreserveContent] = useState(true);
  const [activeTab, setActiveTab] = useState<'hierarchy' | 'metadata' | 'formatting' | 'uncertain'>('hierarchy');
  const [expandedChapters, setExpandedChapters] = useState<Record<number, boolean>>({ 0: true, 1: true });

  if (!isOpen) return null;

  const toggleChapter = (index: number) => {
    setExpandedChapters(prev => ({ ...prev, [index]: !prev[index] }));
  };

  const handleApply = async () => {
    await onApproveAndApply(template, preserveContent);
  };

  const isNotSpecified = (val?: string | null) => !val || val === 'NOT_SPECIFIED';

  const formatBadge = (val?: string | null) => {
    if (isNotSpecified(val)) {
      return <Badge tone="warning" className="uppercase text-xs">NOT SPECIFIED</Badge>;
    }
    return <span className="font-semibold text-sm">{val}</span>;
  };

  const renderRequirementBadge = (level: SectionRequirementLevel) => {
    switch (level) {
      case 'REQUIRED':
        return <Badge tone="danger" style={{ fontSize: '0.72rem' }}>REQUIRED</Badge>;
      case 'RECOMMENDED':
        return <Badge tone="info" style={{ fontSize: '0.72rem' }}>RECOMMENDED</Badge>;
      case 'OPTIONAL':
      default:
        return <Badge style={{ fontSize: '0.72rem' }}>OPTIONAL</Badge>;
    }
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title={`Review Academic Guideline: ${guideline.originalFileName}`}
    >
      <div className="template-review-container" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Banner / Header Summary */}
        <div
          style={{
            background: 'var(--surface-sunken, rgba(0,0,0,0.03))',
            padding: '1rem 1.25rem',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle, #e2e8f0)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '0.75rem'
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FileCheck size={20} color="var(--primary)" />
              <strong style={{ fontSize: '1.05rem' }}>
                {template.institution !== 'NOT_SPECIFIED' ? template.institution : 'Guideline Review'}
              </strong>
              {template.department !== 'NOT_SPECIFIED' && (
                <span className="muted" style={{ fontSize: '0.9rem' }}>• {template.department}</span>
              )}
            </div>
            <p className="muted" style={{ fontSize: '0.85rem', margin: '4px 0 0 0' }}>
              Extracted from <strong>{guideline.originalFileName}</strong> ({guideline.sourceType}). Review detected layout before initializing project report.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <Button
              variant="secondary"
              onClick={() => setIsEditing(!isEditing)}
              style={{ fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Edit3 size={15} />
              {isEditing ? 'Done Editing' : 'Edit Structure'}
            </Button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-subtle, #e2e8f0)', paddingBottom: '4px' }}>
          <button
            type="button"
            className={`button ${activeTab === 'hierarchy' ? 'primary' : 'secondary'}`}
            style={{ fontSize: '0.85rem', padding: '6px 14px' }}
            onClick={() => setActiveTab('hierarchy')}
          >
            <Layers size={14} style={{ marginRight: '6px', verticalAlign: 'middle' }} />
            Chapters & Sections ({template.chapters.length})
          </button>

          <button
            type="button"
            className={`button ${activeTab === 'metadata' ? 'primary' : 'secondary'}`}
            style={{ fontSize: '0.85rem', padding: '6px 14px' }}
            onClick={() => setActiveTab('metadata')}
          >
            <BookOpen size={14} style={{ marginRight: '6px', verticalAlign: 'middle' }} />
            Institution & Metadata
          </button>

          <button
            type="button"
            className={`button ${activeTab === 'formatting' ? 'primary' : 'secondary'}`}
            style={{ fontSize: '0.85rem', padding: '6px 14px' }}
            onClick={() => setActiveTab('formatting')}
          >
            <Settings size={14} style={{ marginRight: '6px', verticalAlign: 'middle' }} />
            Formatting Rules
          </button>

          {template.uncertainItems && template.uncertainItems.length > 0 && (
            <button
              type="button"
              className={`button ${activeTab === 'uncertain' ? 'primary' : 'secondary'}`}
              style={{ fontSize: '0.85rem', padding: '6px 14px' }}
              onClick={() => setActiveTab('uncertain')}
            >
              <AlertTriangle size={14} style={{ marginRight: '6px', verticalAlign: 'middle', color: 'var(--warning)' }} />
              Uncertain Items ({template.uncertainItems.length})
            </button>
          )}
        </div>

        {/* Tab 1: Chapters & Sections Hierarchy */}
        {activeTab === 'hierarchy' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '55vh', overflowY: 'auto', paddingRight: '4px' }}>
            {/* Front Matter Card */}
            {template.frontMatter && template.frontMatter.length > 0 && (
              <Card style={{ padding: '0.85rem 1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <strong style={{ fontSize: '0.95rem', color: 'var(--primary)' }}>PRELIMINARY PAGES (FRONT MATTER)</strong>
                  <Badge tone="info">DETERMINISTIC / AUTHORED</Badge>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '8px' }}>
                  {template.frontMatter.map((fm, i) => (
                    <div
                      key={i}
                      style={{
                        padding: '6px 10px',
                        background: 'var(--surface-sunken, rgba(0,0,0,0.02))',
                        border: '1px solid var(--border-subtle, #e2e8f0)',
                        borderRadius: '6px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.85rem'
                      }}
                    >
                      <span>{fm.heading}</span>
                      {renderRequirementBadge(fm.requirementLevel)}
                    </div>
                  ))}
                </div>
              </Card>
            )}

            {/* Chapters Accordion */}
            {template.chapters.map((ch, chIdx) => {
              const isExpanded = expandedChapters[chIdx] !== false;
              return (
                <Card key={chIdx} style={{ padding: '0.85rem 1rem' }}>
                  <div
                    onClick={() => toggleChapter(chIdx)}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      cursor: 'pointer',
                      userSelect: 'none'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      {isExpanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                      {isEditing ? (
                        <input
                          type="text"
                          className="input"
                          value={ch.title}
                          onClick={(e) => e.stopPropagation()}
                          onChange={(e) => {
                            const updated = [...template.chapters];
                            updated[chIdx] = { ...updated[chIdx], title: e.target.value };
                            setTemplate({ ...template, chapters: updated });
                          }}
                          style={{ fontSize: '0.92rem', fontWeight: 600, padding: '4px 8px' }}
                        />
                      ) : (
                        <strong style={{ fontSize: '0.95rem' }}>{ch.title}</strong>
                      )}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className="muted" style={{ fontSize: '0.8rem' }}>
                        {ch.sections.length} {ch.sections.length === 1 ? 'section' : 'sections'}
                      </span>
                      {ch.required ? <Badge tone="danger" style={{ fontSize: '0.7rem' }}>REQUIRED</Badge> : null}
                    </div>
                  </div>

                  {isExpanded && (
                    <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '6px', paddingLeft: '1.5rem', borderLeft: '2px solid var(--border-subtle, #e2e8f0)' }}>
                      {ch.sections.length === 0 ? (
                        <div className="muted" style={{ fontSize: '0.85rem', fontStyle: 'italic', padding: '4px 0' }}>
                          No specific subsections detected in guideline. You can add them during research.
                        </div>
                      ) : (
                        ch.sections.map((sec, secIdx) => (
                          <div
                            key={secIdx}
                            style={{
                              padding: '6px 10px',
                              background: 'var(--surface-sunken, rgba(0,0,0,0.02))',
                              borderRadius: '6px',
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              fontSize: '0.87rem'
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              {sec.sectionNumber && (
                                <span className="muted" style={{ fontWeight: 600 }}>{sec.sectionNumber}</span>
                              )}
                              {isEditing ? (
                                <input
                                  type="text"
                                  className="input"
                                  value={sec.heading}
                                  onChange={(e) => {
                                    const updatedChapters = [...template.chapters];
                                    const updatedSections = [...updatedChapters[chIdx].sections];
                                    updatedSections[secIdx] = { ...updatedSections[secIdx], heading: e.target.value };
                                    updatedChapters[chIdx] = { ...updatedChapters[chIdx], sections: updatedSections };
                                    setTemplate({ ...template, chapters: updatedChapters });
                                  }}
                                  style={{ padding: '2px 6px', fontSize: '0.85rem' }}
                                />
                              ) : (
                                <span>{sec.heading}</span>
                              )}
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              {sec.semanticPurpose && (
                                <span className="muted" style={{ fontSize: '0.72rem', background: 'rgba(0,0,0,0.05)', padding: '2px 6px', borderRadius: '4px' }}>
                                  {sec.semanticPurpose}
                                </span>
                              )}
                              {renderRequirementBadge(sec.requirementLevel)}
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  )}
                </Card>
              );
            })}

            {/* Appendices */}
            {template.appendices && template.appendices.length > 0 && (
              <Card style={{ padding: '0.85rem 1rem' }}>
                <strong style={{ fontSize: '0.95rem', color: 'var(--muted)' }}>APPENDICES</strong>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '8px' }}>
                  {template.appendices.map((app, i) => (
                    <div
                      key={i}
                      style={{
                        padding: '6px 10px',
                        background: 'var(--surface-sunken, rgba(0,0,0,0.02))',
                        borderRadius: '6px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.85rem'
                      }}
                    >
                      <span>{app.sectionNumber ? `${app.sectionNumber}: ` : ''}{app.heading}</span>
                      {renderRequirementBadge(app.requirementLevel)}
                    </div>
                  ))}
                </div>
              </Card>
            )}
          </div>
        )}

        {/* Tab 2: Institution & Metadata */}
        {activeTab === 'metadata' && (
          <Card style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Institution</span>
                {isEditing ? (
                  <Input
                    value={template.institution === 'NOT_SPECIFIED' ? '' : template.institution}
                    placeholder="Enter Institution Name"
                    onChange={(e) => setTemplate({ ...template, institution: e.target.value || 'NOT_SPECIFIED' })}
                  />
                ) : (
                  formatBadge(template.institution)
                )}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Department</span>
                {isEditing ? (
                  <Input
                    value={template.department === 'NOT_SPECIFIED' ? '' : template.department}
                    placeholder="Enter Department Name"
                    onChange={(e) => setTemplate({ ...template, department: e.target.value || 'NOT_SPECIFIED' })}
                  />
                ) : (
                  formatBadge(template.department)
                )}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Programme</span>
                {isEditing ? (
                  <Input
                    value={template.programme === 'NOT_SPECIFIED' ? '' : template.programme}
                    placeholder="Enter Programme"
                    onChange={(e) => setTemplate({ ...template, programme: e.target.value || 'NOT_SPECIFIED' })}
                  />
                ) : (
                  formatBadge(template.programme)
                )}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Document Type</span>
                {isEditing ? (
                  <Input
                    value={template.documentType === 'NOT_SPECIFIED' ? '' : template.documentType}
                    placeholder="e.g. Project Report, Dissertation"
                    onChange={(e) => setTemplate({ ...template, documentType: e.target.value || 'NOT_SPECIFIED' })}
                  />
                ) : (
                  formatBadge(template.documentType)
                )}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Citation Style</span>
                {isEditing ? (
                  <Select
                    value={template.citationStyle}
                    onChange={(e) => setTemplate({ ...template, citationStyle: e.target.value })}
                  >
                    <option value="APA_7">APA 7th Edition</option>
                    <option value="IEEE">IEEE Numerical</option>
                    <option value="HARVARD">Harvard Author-Date</option>
                    <option value="CHICAGO_AUTHOR_DATE">Chicago Author-Date</option>
                    <option value="VANCOUVER">Vancouver Numerical</option>
                    <option value="MLA_9">MLA 9th Edition</option>
                    <option value="NOT_SPECIFIED">NOT_SPECIFIED</option>
                  </Select>
                ) : (
                  formatBadge(template.citationStyle)
                )}
              </div>
            </div>
          </Card>
        )}

        {/* Tab 3: Formatting Rules */}
        {activeTab === 'formatting' && (
          <Card style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Font Family</span>
                {formatBadge(template.formattingRules?.fontFamily)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Body Font Size</span>
                {formatBadge(template.formattingRules?.bodyFontSize)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Heading 1 Font Size</span>
                {formatBadge(template.formattingRules?.heading1FontSize)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Heading 2 Font Size</span>
                {formatBadge(template.formattingRules?.heading2FontSize)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Line Spacing</span>
                {formatBadge(template.formattingRules?.lineSpacing)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Margins</span>
                {template.formattingRules?.margins ? (
                  <span style={{ fontSize: '0.85rem' }}>
                    T: {template.formattingRules.margins.top} | B: {template.formattingRules.margins.bottom} | L: {template.formattingRules.margins.left} | R: {template.formattingRules.margins.right}
                  </span>
                ) : (
                  formatBadge(null)
                )}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Numbering Style</span>
                {formatBadge(template.formattingRules?.numberingStyle)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Page Numbering</span>
                {formatBadge(template.formattingRules?.pageNumbering)}
              </div>

              <div>
                <span className="muted" style={{ fontSize: '0.8rem', display: 'block', marginBottom: '4px' }}>Chapter Break</span>
                {formatBadge(template.formattingRules?.chapterBreak)}
              </div>
            </div>
          </Card>
        )}

        {/* Tab 4: Uncertain Items */}
        {activeTab === 'uncertain' && template.uncertainItems && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <div className="alert warning" style={{ fontSize: '0.85rem', marginBottom: '6px' }}>
              The extraction engine detected some ambiguous or unspecified requirements in the guideline. Default platform standards will be used for these items without hallucinating.
            </div>
            {template.uncertainItems.map((item, idx) => (
              <Card key={idx} style={{ padding: '0.75rem 1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <strong style={{ fontSize: '0.9rem' }}>{item.field}</strong>
                  <p className="muted" style={{ margin: '2px 0 0 0', fontSize: '0.82rem' }}>{item.reason}</p>
                </div>
                <Badge tone="warning">UNCERTAIN</Badge>
              </Card>
            ))}
          </div>
        )}

        {/* Action Controls & Confirmation */}
        <div
          style={{
            borderTop: '1px solid var(--border-subtle, #e2e8f0)',
            paddingTop: '1rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '1rem'
          }}
        >
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.88rem' }}>
            <input
              type="checkbox"
              checked={preserveContent}
              onChange={(e) => setPreserveContent(e.target.checked)}
            />
            <span>Preserve and remap existing section text to matching headings</span>
          </label>

          <div style={{ display: 'flex', gap: '10px' }}>
            <Button variant="secondary" onClick={onClose} disabled={isApplying}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleApply} disabled={isApplying}>
              {isApplying ? 'Applying Template...' : 'Approve & Apply Structure'}
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
