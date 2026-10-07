import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge } from '../components/ui';
import {
  getDocumentNodeCategory,
  isDeterministicNode,
  isCorruptedDeterministicContent,
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

