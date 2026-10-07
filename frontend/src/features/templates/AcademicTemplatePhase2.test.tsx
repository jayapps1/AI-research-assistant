import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TemplateReviewModal } from './TemplateReviewModal';
import type { AcademicDocumentGuidelineResponse } from '../../types/api';

describe('Phase 2 Academic Templates Frontend Components', () => {
  const mockGuideline: AcademicDocumentGuidelineResponse = {
    id: 'guide-test-1',
    workspaceId: 'ws-1',
    projectId: 'proj-1',
    reportId: 'rep-1',
    originalFileName: 'Official_Thesis_Guidelines.pdf',
    sourceType: 'PDF',
    status: 'EXTRACTED',
    documentType: 'DISSERTATION',
    extractedTemplate: {
      institution: 'University of Nairobi',
      department: 'School of Computing and Informatics',
      programme: 'MSc Computer Science',
      documentType: 'DISSERTATION',
      citationStyle: 'APA_7',
      formattingRules: {
        fontFamily: 'Times New Roman',
        bodyFontSize: '12pt',
        heading1FontSize: '14pt Bold',
        heading2FontSize: '12pt Bold',
        lineSpacing: '1.5',
        margins: { top: '2.54cm', bottom: '2.54cm', left: '3.81cm', right: '2.54cm' },
      },
      frontMatter: [
        { heading: 'Title Page', requirementLevel: 'REQUIRED' },
        { heading: 'Declaration', requirementLevel: 'REQUIRED' },
        { heading: 'Abstract', requirementLevel: 'REQUIRED' },
        { heading: 'List of Acronyms', requirementLevel: 'RECOMMENDED' },
      ],
      chapters: [
        {
          chapterNumber: 1,
          title: 'Introduction',
          required: true,
          requirementLevel: 'REQUIRED',
          sections: [
            {
              sectionNumber: '1.1',
              heading: 'Background of the Study',
              requirementLevel: 'REQUIRED',
              subsections: [],
            },
            {
              sectionNumber: '1.2',
              heading: 'Problem Statement',
              requirementLevel: 'REQUIRED',
              subsections: [],
            },
          ],
        },
        {
          chapterNumber: 2,
          title: 'Literature Review',
          required: true,
          requirementLevel: 'REQUIRED',
          sections: [
            {
              sectionNumber: '2.1',
              heading: 'Theoretical Framework',
              requirementLevel: 'REQUIRED',
              subsections: [],
            },
          ],
        },
      ],
      appendices: [
        {
          sectionNumber: 'Appendix A',
          heading: 'Questionnaire Sample',
          requirementLevel: 'OPTIONAL',
        },
      ],
      uncertainItems: [
        {
          field: 'Citation Style',
          reason: 'Referencing style detected as APA 7th edition based on citation hints.',
        },
      ],
    },
    version: 1,
    createdAt: '2026-10-01T12:00:00Z',
    updatedAt: '2026-10-01T12:00:00Z',
  };

  it('renders guideline header, institution, and chapter hierarchy correctly', () => {
    const handleApprove = vi.fn().mockResolvedValue(undefined);
    const handleClose = vi.fn();

    render(
      <TemplateReviewModal
        guideline={mockGuideline}
        isOpen={true}
        onClose={handleClose}
        onApproveAndApply={handleApprove}
      />
    );

    // Modal title & institution summary
    expect(screen.getByText(/Review Academic Guideline: Official_Thesis_Guidelines.pdf/i)).toBeInTheDocument();
    expect(screen.getByText('University of Nairobi')).toBeInTheDocument();
    expect(screen.getByText(/School of Computing and Informatics/i)).toBeInTheDocument();

    // Front matter preliminary pages
    expect(screen.getByText('PRELIMINARY PAGES (FRONT MATTER)')).toBeInTheDocument();
    expect(screen.getByText('Title Page')).toBeInTheDocument();
    expect(screen.getByText('Declaration')).toBeInTheDocument();

    // Chapters and sections
    expect(screen.getByText('Introduction')).toBeInTheDocument();
    expect(screen.getByText('Background of the Study')).toBeInTheDocument();
    expect(screen.getByText('Problem Statement')).toBeInTheDocument();

    // Badges
    const requiredBadges = screen.getAllByText('REQUIRED');
    expect(requiredBadges.length).toBeGreaterThan(0);
  }, 40000);

  it('switches tabs to inspect Institution metadata, Formatting rules, and Uncertain items', () => {
    render(
      <TemplateReviewModal
        guideline={mockGuideline}
        isOpen={true}
        onClose={vi.fn()}
        onApproveAndApply={vi.fn().mockResolvedValue(undefined)}
      />
    );

    // Click Institution & Metadata tab
    fireEvent.click(screen.getByText(/Institution & Metadata/i));
    expect(screen.getByText('MSc Computer Science')).toBeInTheDocument();
    expect(screen.getByText('APA_7')).toBeInTheDocument();

    // Click Formatting Rules tab
    fireEvent.click(screen.getByText(/Formatting Rules/i));
    expect(screen.getByText('Times New Roman')).toBeInTheDocument();
    expect(screen.getByText('12pt')).toBeInTheDocument();
    expect(screen.getByText('1.5')).toBeInTheDocument();

    // Click Uncertain Items tab
    fireEvent.click(screen.getByText(/Uncertain Items \(1\)/i));
    expect(screen.getByText('Citation Style')).toBeInTheDocument();
    expect(
      screen.getByText(/Referencing style detected as APA 7th edition based on citation hints/i)
    ).toBeInTheDocument();
  }, 40000);

  it('invokes onApproveAndApply with preserveContent=true by default', async () => {
    const handleApprove = vi.fn().mockResolvedValue(undefined);

    render(
      <TemplateReviewModal
        guideline={mockGuideline}
        isOpen={true}
        onClose={vi.fn()}
        onApproveAndApply={handleApprove}
      />
    );

    // Verify approve button exists
    const approveBtn = screen.getByRole('button', { name: /Approve & Apply Structure/i });
    expect(approveBtn).toBeInTheDocument();

    fireEvent.click(approveBtn);

    expect(handleApprove).toHaveBeenCalledTimes(1);
    const [passedTemplate, preserveContent] = handleApprove.mock.calls[0];
    expect(passedTemplate.institution).toBe('University of Nairobi');
    expect(preserveContent).toBe(true);
  }, 40000);
});
