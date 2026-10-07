import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { InsertFigureModal } from './InsertFigureModal';
import { ProjectEvidencePanel } from './ProjectEvidencePanel';
import { ReportRichEditor } from '../../components/editor/ReportRichEditor';
import { projectEvidenceApi, reportApi } from '../../api/endpoints';
import type { ProjectEvidenceItem, ProjectImageAnalysisResult } from '../../types/api';

vi.mock('../../api/endpoints', () => ({
  projectEvidenceApi: {
    upload: vi.fn(),
    createStructured: vi.fn(),
    list: vi.fn(),
    get: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    reorder: vi.fn(),
    analyze: vi.fn(),
    listOfFigures: vi.fn(),
    listOfTables: vi.fn(),
  },
  reportApi: {
    structure: vi.fn(),
    getStructure: vi.fn(),
    references: vi.fn(),
  },
}));

describe('Phase 3 Project Evidence & Figures Frontend Components', () => {
  const mockEvidenceItem1: ProjectEvidenceItem = {
    id: 'ev-1',
    projectId: 'proj-1',
    reportId: 'rep-1',
    sectionId: 'sec-4-1',
    sectionHeading: '4.1 System Implementation & Screenshots',
    evidenceType: 'SCREENSHOT',
    figureNumber: 1,
    figureLabel: 'Figure 4.1',
    caption: 'Authentication Dashboard with Dual-Role Selection',
    description: 'Displays the login screen with biometric and password auth tabs.',
    source: 'Author system deployment',
    storageObjectId: 'obj-1',
    originalFilename: 'login_screen.png',
    contentType: 'image/png',
    fileSizeBytes: 204800,
    downloadUrl: '/api/v1/storage/objects/obj-1',
    displayOrder: 1,
    aiAnalysisStatus: 'COMPLETED',
    aiVisualAnalysis: {
      visibleTitle: 'Authentication Dashboard',
      captionSuggestion: 'Authentication Dashboard with Dual-Role Selection',
      visibleSummary: 'Web application interface showing role switcher, OAuth buttons, and login form.',
      observableComponents: ['Navigation bar', 'Email textfield', 'Password textfield', 'Login button'],
      observableWorkflow: 'User authenticates using email credentials or SSO.',
      detectedText: ['Sign in', 'Research Portal', 'Enter your email'],
      methodologyRelevance: 'Demonstrates role-based access control implemented in Chapter 3.',
    },
    createdAt: '2026-10-02T10:00:00Z',
    updatedAt: '2026-10-02T10:00:00Z',
  };

  const mockEvidenceItem2: ProjectEvidenceItem = {
    id: 'ev-2',
    projectId: 'proj-1',
    reportId: 'rep-1',
    sectionId: 'sec-4-2',
    sectionHeading: '4.2 Performance & Benchmark Results',
    evidenceType: 'CHART',
    figureNumber: 2,
    figureLabel: 'Figure 4.2',
    caption: 'Latency Comparison across Vector Retrieval Algorithms',
    description: 'Bar chart comparing cosine similarity against HNSW indexing.',
    source: 'Experimental benchmark results',
    storageObjectId: 'obj-2',
    originalFilename: 'latency_benchmark.png',
    contentType: 'image/png',
    fileSizeBytes: 153600,
    downloadUrl: '/api/v1/storage/objects/obj-2',
    displayOrder: 2,
    aiAnalysisStatus: 'PENDING',
    createdAt: '2026-10-02T10:05:00Z',
    updatedAt: '2026-10-02T10:05:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (reportApi.structure as any).mockResolvedValue({
      id: 'rep-1',
      title: 'Final Research Project Report',
      chapters: [
        {
          id: 'chap-4',
          title: 'Chapter Four: Results and Discussion',
          sections: [
            { id: 'sec-4-1', sectionNumber: '4.1', heading: 'System Implementation & Screenshots' },
            { id: 'sec-4-2', sectionNumber: '4.2', heading: 'Performance & Benchmark Results' },
          ],
        },
      ],
    });
    (reportApi.references as any).mockResolvedValue({ items: [], total: 0 });
  });

  describe('InsertFigureModal', () => {
    it('renders modal with upload zone, evidence type select, and fields', () => {
      render(
        <InsertFigureModal
          isOpen={true}
          onClose={vi.fn()}
          projectId="proj-1"
          sectionId="sec-4-1"
          onInsert={vi.fn()}
        />
      );

      expect(screen.getByText('Insert Project Figure / Screenshot')).toBeInTheDocument();
      expect(screen.getByText(/Click to select screenshot or diagram/i)).toBeInTheDocument();
      expect(screen.getByText('Evidence Type')).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/e.g. Farmer Product Listing Page/i)).toBeInTheDocument();
    });

    it('uploads file, performs AI visual inspection, and displays observable elements without hallucinating invisible code', async () => {
      const mockUploadResponse: ProjectEvidenceItem = {
        ...mockEvidenceItem1,
        id: 'ev-new',
        figureLabel: 'Figure 4.1',
      };

      const mockAnalysisResult: ProjectImageAnalysisResult = {
        visibleTitle: 'System Architecture Diagram',
        captionSuggestion: 'High-Level Microservice Architecture',
        visibleSummary: 'Architectural block diagram showing client layer, Spring Boot API, and PostgreSQL.',
        observableComponents: ['Client UI', 'Spring Boot Gateway', 'PostgreSQL DB'],
        observableWorkflow: 'Client makes HTTPS requests routed to backend services.',
        detectedText: ['Client', 'API Gateway', 'PostgreSQL'],
        methodologyRelevance: 'Validates architectural design formulated in methodology.',
        status: 'COMPLETED',
      };

      (projectEvidenceApi.upload as any).mockResolvedValue(mockUploadResponse);
      (projectEvidenceApi.analyze as any).mockResolvedValue(mockAnalysisResult);

      const handleInsert = vi.fn();
      const handleClose = vi.fn();

      render(
        <InsertFigureModal
          isOpen={true}
          onClose={handleClose}
          projectId="proj-1"
          sectionId="sec-4-1"
          onInsert={handleInsert}
        />
      );

      // Create a mock image file
      const testFile = new File(['fake-image-bytes'], 'architecture_diagram.png', { type: 'image/png' });
      const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
      expect(fileInput).toBeInTheDocument();

      // Simulate file selection
      fireEvent.change(fileInput, { target: { files: [testFile] } });

      // Click "Explain Figure (AI Visual Inspection)"
      const explainBtn = await screen.findByRole('button', { name: /Explain Figure \(AI Visual Inspection\)/i });
      expect(explainBtn).toBeInTheDocument();
      fireEvent.click(explainBtn);

      await waitFor(() => {
        expect(projectEvidenceApi.upload).toHaveBeenCalledTimes(1);
        expect(projectEvidenceApi.analyze).toHaveBeenCalledWith('proj-1', 'ev-new');
      });

      // Assert structured observation outputs appear
      expect(await screen.findByText(/Visible Element Analysis \(No Invisible Code Fabricated\)/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Architectural block diagram showing client layer/i).length).toBeGreaterThan(0);
      expect(screen.getByText('Client UI')).toBeInTheDocument();
      expect(screen.getByText('Spring Boot Gateway')).toBeInTheDocument();

      // Click Insert into Document
      const insertBtn = screen.getByRole('button', { name: /Insert into Document/i });
      fireEvent.click(insertBtn);

      expect(handleInsert).toHaveBeenCalledWith(mockUploadResponse);
      expect(handleClose).toHaveBeenCalled();
    });

    it('handles AI vision unavailable error gracefully with structured fallback message', async () => {
      const mockUploadResponse: ProjectEvidenceItem = {
        ...mockEvidenceItem1,
        id: 'ev-unavail',
      };

      (projectEvidenceApi.upload as any).mockResolvedValue(mockUploadResponse);
      (projectEvidenceApi.analyze as any).mockResolvedValue({
        status: 'UNAVAILABLE',
        errorMessage: 'AI multimodal vision model is not configured. Please enter manual visual observations.',
      });

      render(
        <InsertFigureModal
          isOpen={true}
          onClose={vi.fn()}
          projectId="proj-1"
          sectionId="sec-4-1"
          onInsert={vi.fn()}
        />
      );

      const testFile = new File(['fake-png-content'], 'test_flowchart.png', { type: 'image/png' });
      const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
      fireEvent.change(fileInput, { target: { files: [testFile] } });

      const explainBtn = await screen.findByRole('button', { name: /Explain Figure \(AI Visual Inspection\)/i });
      fireEvent.click(explainBtn);

      await waitFor(() => {
        expect(screen.getByText(/Provider Capability Notice/i)).toBeInTheDocument();
        expect(screen.getByText(/AI multimodal vision model is not configured/i)).toBeInTheDocument();
      });
    });
  });

  describe('ProjectEvidencePanel', () => {
    it('renders list of figures with figure labels, captions, and evidence type badges', async () => {
      (projectEvidenceApi.list as any).mockResolvedValue([mockEvidenceItem1, mockEvidenceItem2]);

      render(
        <ProjectEvidencePanel
          projectId="proj-1"
          reportId="rep-1"
          onInsertToEditor={vi.fn()}
        />
      );

      expect(await screen.findByText('Figure 4.1')).toBeInTheDocument();
      expect(screen.getByText('Authentication Dashboard with Dual-Role Selection')).toBeInTheDocument();
      expect(screen.getByText('Figure 4.2')).toBeInTheDocument();
      expect(screen.getByText('Latency Comparison across Vector Retrieval Algorithms')).toBeInTheDocument();
      expect(screen.getByText('SCREENSHOT')).toBeInTheDocument();
      expect(screen.getByText('CHART')).toBeInTheDocument();
    });

    it('filters items by evidence type', async () => {
      (projectEvidenceApi.list as any).mockResolvedValue([mockEvidenceItem1, mockEvidenceItem2]);

      render(
        <ProjectEvidencePanel
          projectId="proj-1"
          reportId="rep-1"
        />
      );

      await screen.findByText('Figure 4.1');

      // Change Type select to CHART
      const typeSelect = screen.getByDisplayValue('All Types');
      fireEvent.change(typeSelect, { target: { value: 'CHART' } });

      // Figure 4.2 (CHART) should remain, Figure 4.1 (SCREENSHOT) should be filtered out
      expect(screen.getByText('Figure 4.2')).toBeInTheDocument();
      expect(screen.queryByText('Figure 4.1')).not.toBeInTheDocument();
    });

    it('triggers reorder API when reorder buttons are clicked', async () => {
      (projectEvidenceApi.list as any).mockResolvedValue([mockEvidenceItem1, mockEvidenceItem2]);
      (projectEvidenceApi.reorder as any).mockResolvedValue([
        { ...mockEvidenceItem2, displayOrder: 1, figureLabel: 'Figure 4.1' },
        { ...mockEvidenceItem1, displayOrder: 2, figureLabel: 'Figure 4.2' },
      ]);

      render(
        <ProjectEvidencePanel
          projectId="proj-1"
          reportId="rep-1"
        />
      );

      await screen.findByText('Figure 4.1');

      // Click move figure down on first item
      const downButtons = screen.getAllByTitle('Move figure down');
      expect(downButtons.length).toBeGreaterThan(0);
      fireEvent.click(downButtons[0]);

      await waitFor(() => {
        expect(projectEvidenceApi.reorder).toHaveBeenCalledWith('proj-1', ['ev-2', 'ev-1']);
      });
    });

    it('deletes evidence item with confirmation', async () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      (projectEvidenceApi.list as any).mockResolvedValue([mockEvidenceItem1]);
      (projectEvidenceApi.delete as any).mockResolvedValue(undefined);

      render(
        <ProjectEvidencePanel
          projectId="proj-1"
          reportId="rep-1"
        />
      );

      await screen.findByText('Figure 4.1');

      const deleteBtn = screen.getByTitle('Delete evidence');
      fireEvent.click(deleteBtn);

      await waitFor(() => {
        expect(projectEvidenceApi.delete).toHaveBeenCalledWith('proj-1', 'ev-1');
      });
    });
  });

  describe('ReportRichEditor Figure Extension', () => {
    it('renders "Insert Figure" ribbon button when editing report section', () => {
      const queryClient = new QueryClient();

      render(
        <QueryClientProvider client={queryClient}>
          <ReportRichEditor
            content="<p>Chapter 4 text</p>"
            onChange={vi.fn()}
            projectId="proj-1"
            sectionId="sec-4-1"
          />
        </QueryClientProvider>
      );

      const insertFigureBtn = screen.getByRole('button', { name: /Insert Figure/i });
      expect(insertFigureBtn).toBeInTheDocument();

      // Clicking ribbon button opens InsertFigureModal
      fireEvent.click(insertFigureBtn);
      expect(screen.getByText('Insert Project Figure / Screenshot')).toBeInTheDocument();
    });
  });
});
