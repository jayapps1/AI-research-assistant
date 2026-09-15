import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { QualitativeWorkbenchPage, ResultRenderer } from './ResearchWorkbenches';

vi.mock('../hooks/useProjectId', () => ({ useProjectId: () => 'project-1' }));

function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe('analysis result renderers', () => {
  it('renders descriptive statistics without converting missing values to zero', () => {
    render(<ResultRenderer type="DESCRIPTIVE" payload={{ results: [{ variable: 'age', validN: 12, missing: 3, mean: 41.5 }] }} />);
    expect(screen.getByText('Descriptive statistics')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('renders t-test variant and core statistics', () => {
    render(<ResultRenderer type="T_TEST" payload={{ welch: true, results: [{ group: 'A', n: 20, meanDifference: 1.4, t: 2.1, df: 31, p: 0.04 }] }} />);
    expect(screen.getByText(/Welch/i)).toBeInTheDocument();
    expect(screen.getByText('2.1')).toBeInTheDocument();
    expect(screen.getByText('0.04')).toBeInTheDocument();
  });

  it('renders chi-square warnings from backend payload', () => {
    render(<ResultRenderer type="CHI_SQUARE" payload={{ expectedCountWarning: 'Expected cell counts below threshold.', results: [{ row: 'Yes', column: 'No', count: 5, expected: 8, chiSquare: 4.6, p: 0.03 }] }} />);
    expect(screen.getByText('Chi-square')).toBeInTheDocument();
    expect(screen.getByText(/Expected cell counts below threshold/i)).toBeInTheDocument();
  });

  it('avoids causal wording for correlations', () => {
    render(<ResultRenderer type="CORRELATION" payload={{ results: [{ n: 42, r: 0.62, p: 0.01 }] }} />);
    expect(screen.getByText(/Correlation is not displayed as causation/i)).toBeInTheDocument();
  });

  it('uses academically appropriate null-hypothesis wording', () => {
    render(<ResultRenderer type="REGRESSION" payload={{ results: [{ term: 'x', coefficient: 0.5, decision: 'Accept null hypothesis' }] }} />);
    expect(screen.getByText(/Insufficient evidence to reject null hypothesis/i)).toBeInTheDocument();
  });
});

describe('qualitative workbench', () => {
  it('does not expose fake AI or persistence when backend qualitative APIs are absent', () => {
    renderWithQuery(<QualitativeWorkbenchPage />);
    expect(screen.getByText(/No qualitative backend controller was found/i)).toBeInTheDocument();
    expect(screen.getByText(/No mock AI is used/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Suggest Codes with real AI/i })).toBeDisabled();
  });
});
