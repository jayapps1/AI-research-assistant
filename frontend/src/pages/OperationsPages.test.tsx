import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge } from '../components/ui';
import {
  buildExportSelectionPayload,
  getDocumentNodeCategory,
  isDeterministicNode,
  isCorruptedDeterministicContent,
  loadOrPrepareFinalDocument,
  previewDiagnostics,
} from './OperationsPages';

function ComplimentaryBadge({ accessType }: { accessType: string }) {
  const complimentary = ['COMPLIMENTARY', 'DEVELOPER_ACCESS', 'PROMOTIONAL'].includes(accessType);
  return <>{complimentary ? <Badge tone="success">{accessType.replaceAll('_', ' ')}</Badge> : <Badge>Paid subscription</Badge>}</>;
}

describe('billing state display', () => {
  it('does not label developer access as paid', () => {
    render(<ComplimentaryBadge accessType="DEVELOPER_ACCESS" />);
    expect(screen.getByText('DEVELOPER ACCESS')).toBeInTheDocument();
    expect(screen.queryByText(/paid/i)).not.toBeInTheDocument();
  });
});

describe('Report Node Categorization & Generation Policy Routing', () => {
  it('identifies Title Page deterministically from purpose, type, or heading', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'TITLE_PAGE' })).toBe('TITLE_PAGE');
    expect(getDocumentNodeCategory({ type: 'TITLE_PAGE' })).toBe('TITLE_PAGE');
    expect(getDocumentNodeCategory({ heading: 'Title Page' }, { title: 'Preliminary Pages' })).toBe('TITLE_PAGE');
    expect(getDocumentNodeCategory({ heading: 'Title' }, { title: 'Preliminary Pages' })).toBe('TITLE_PAGE');
  });

  it('identifies Table of Contents deterministically', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'TABLE_OF_CONTENTS' })).toBe('TABLE_OF_CONTENTS');
    expect(getDocumentNodeCategory({ type: 'TABLE_OF_CONTENTS' })).toBe('TABLE_OF_CONTENTS');
    expect(getDocumentNodeCategory({ heading: 'Table of Contents' })).toBe('TABLE_OF_CONTENTS');
    expect(getDocumentNodeCategory({ heading: 'Contents' })).toBe('TABLE_OF_CONTENTS');
  });

  it('identifies List of Figures and List of Tables deterministically', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'LIST_OF_FIGURES' })).toBe('LIST_OF_FIGURES');
    expect(getDocumentNodeCategory({ heading: 'List of Figures' })).toBe('LIST_OF_FIGURES');
    expect(getDocumentNodeCategory({ semanticPurpose: 'LIST_OF_TABLES' })).toBe('LIST_OF_TABLES');
    expect(getDocumentNodeCategory({ heading: 'List of Tables' })).toBe('LIST_OF_TABLES');
  });

  it('identifies References deterministically', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'REFERENCES' })).toBe('REFERENCES');
    expect(getDocumentNodeCategory({ type: 'REFERENCES' })).toBe('REFERENCES');
    expect(getDocumentNodeCategory({ heading: 'References' })).toBe('REFERENCES');
    expect(getDocumentNodeCategory({ heading: 'Bibliography' })).toBe('REFERENCES');
  });

  it('identifies Literature Review for source-grounded academic generation', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'LITERATURE_REVIEW' })).toBe('LITERATURE_REVIEW');
    expect(getDocumentNodeCategory({ type: 'LITERATURE_REVIEW' })).toBe('LITERATURE_REVIEW');
    expect(getDocumentNodeCategory({ heading: '2.1 Literature Review' }, { title: 'Chapter Two: Literature Review' })).toBe('LITERATURE_REVIEW');
  });

  it('identifies Project-Derived and Evidence-Gated sections accurately', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'SYSTEM_DESIGN' })).toBe('SYSTEM_DESIGN');
    expect(getDocumentNodeCategory({ heading: 'System Architecture & Design' })).toBe('SYSTEM_DESIGN');
    expect(getDocumentNodeCategory({ semanticPurpose: 'TESTING' })).toBe('TESTING');
    expect(getDocumentNodeCategory({ heading: 'System Testing and Validation' })).toBe('TESTING');
    expect(getDocumentNodeCategory({ semanticPurpose: 'PROBLEM_STATEMENT' })).toBe('PROBLEM_STATEMENT');
    expect(getDocumentNodeCategory({ semanticPurpose: 'AIM_AND_OBJECTIVES' })).toBe('AIM_AND_OBJECTIVES');
  });

  it('supports custom headings without failing or misrouting to literature review', () => {
    expect(getDocumentNodeCategory({ semanticPurpose: 'CUSTOM', heading: '6.2 Installation and Deployment' })).toBe('CUSTOM_CONTENT');
    expect(getDocumentNodeCategory({ type: 'CUSTOM', heading: 'Hardware Configuration' })).toBe('CUSTOM_CONTENT');
  });

  it('determines whether nodes are deterministic (0 AI calls/credits policy)', () => {
    expect(isDeterministicNode('TITLE_PAGE')).toBe(true);
    expect(isDeterministicNode('TABLE_OF_CONTENTS')).toBe(true);
    expect(isDeterministicNode('LIST_OF_FIGURES')).toBe(true);
    expect(isDeterministicNode('LIST_OF_TABLES')).toBe(true);
    expect(isDeterministicNode('REFERENCES')).toBe(true);

    expect(isDeterministicNode('LITERATURE_REVIEW')).toBe(false);
    expect(isDeterministicNode('BACKGROUND')).toBe(false);
    expect(isDeterministicNode('SYSTEM_DESIGN')).toBe(false);
    expect(isDeterministicNode('TESTING')).toBe(false);
    expect(isDeterministicNode('CUSTOM_CONTENT')).toBe(false);
    expect(isDeterministicNode('CONTENT_SECTION')).toBe(false);
  });

  it('detects legacy corrupted AI content on deterministic sections for safe 1-click repair', () => {
    const corruptedSample =
      '## Overview and Thematic Context\n\n' +
      'This section examines the Title Page for Preliminary Pages through a synthesis of grounded empirical literature...';
    expect(isCorruptedDeterministicContent(corruptedSample, 'TITLE_PAGE')).toBe(true);
    expect(isCorruptedDeterministicContent(corruptedSample, 'TABLE_OF_CONTENTS')).toBe(true);
    expect(isCorruptedDeterministicContent(corruptedSample, 'REFERENCES')).toBe(true);

    // Clean metadata title page must NOT be flagged as corrupted
    const cleanTitlePage = '# TAKORADI TECHNICAL UNIVERSITY\n\n**PROJECT REPORT**\n\nJane Doe\n07210001';
    expect(isCorruptedDeterministicContent(cleanTitlePage, 'TITLE_PAGE')).toBe(false);

    // Legitimate literature review containing synthesis terminology must NOT be flagged as corrupted
    expect(isCorruptedDeterministicContent(corruptedSample, 'LITERATURE_REVIEW')).toBe(false);
  });
});

describe('Report preview/export selection payloads', () => {
  it('sends selected stable node ids for selected content without renumbering options', () => {
    const payload = buildExportSelectionPayload({
      exportScopeMode: 'SELECTED',
      selectedExportNodeIds: ['chapter-one-id', 'chapter-two-id'],
      includeExportCoverPage: false,
      includeExportToc: true,
      includeExportListOfFigures: false,
      includeExportListOfTables: false,
      includeExportReferences: true,
      includeExportAppendices: false,
      exportReferenceMode: 'CITED_IN_SELECTION',
    });

    expect(payload).toMatchObject({
      selectionMode: 'SELECTED',
      selectedNodeIds: ['chapter-one-id', 'chapter-two-id'],
      includeToc: true,
      includeReferences: true,
      referenceMode: 'CITED_IN_SELECTION',
    });
  });

  it('uses the canonical full compiled document for entire-report preview', () => {
    const payload = buildExportSelectionPayload({
      exportScopeMode: 'FULL',
      selectedExportNodeIds: ['ignored-selected-id'],
      includeExportCoverPage: false,
      includeExportToc: false,
      includeExportListOfFigures: false,
      includeExportListOfTables: false,
      includeExportReferences: false,
      includeExportAppendices: false,
      exportReferenceMode: 'NONE',
    });

    expect(payload.selectedNodeIds).toEqual([]);
    expect(payload).toMatchObject({
      selectionMode: 'FULL',
      includeCoverPage: true,
      includeToc: true,
      includeReferences: true,
      referenceMode: 'ALL_PROJECT_REFERENCES',
    });
  });

  it('prepares a populated working final draft when the editor has no current document', async () => {
    const apiClient = {
      finalDocument: async () => null,
      prepareFinalDocument: async () => ({
        id: 'version-13',
        versionNumber: 13,
        status: 'FINAL_REVIEW',
        contentJson: '{"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"Chapter One"}]}]}',
        plainText: 'Chapter One\nChapter Two\nChapter Three',
      }),
    };

    const doc = await loadOrPrepareFinalDocument('report-1', apiClient);
    expect(doc).toMatchObject({
      id: 'version-13',
      versionNumber: 13,
      contentJson: expect.stringContaining('Chapter One'),
    });
  });

  it('prepares a fresh working version instead of opening a stale final draft', async () => {
    const apiClient = {
      finalDocument: async () => ({ id: 'version-12', stale: true, plainText: 'Old document' }),
      prepareFinalDocument: async () => ({
        id: 'version-13',
        versionNumber: 13,
        stale: false,
        plainText: 'Chapter One\nChapter Two\nChapter Three',
      }),
    };

    const doc = await loadOrPrepareFinalDocument('report-1', apiClient);
    expect(doc).toMatchObject({ id: 'version-13', stale: false });
  });

  it('reports safe preview diagnostics without body text', () => {
    expect(previewDiagnostics({
      reportId: 'report-1',
      versionId: null,
      tocEntryCount: 8,
      bodyNodeCount: 12,
      nonEmptyBodyNodeCount: 9,
      referenceCount: 4,
      contentJson: '{"type":"doc","content":[]}',
      plainText: 'private body text',
    })).toEqual({
      reportId: 'report-1',
      versionId: null,
      frontMatterNodeCount: 0,
      tocEntryCount: 8,
      bodyNodeCount: 12,
      nonEmptyBodyNodeCount: 9,
      referenceCount: 4,
      contentLength: 27,
    });
  });
});
