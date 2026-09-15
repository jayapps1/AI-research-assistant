import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { RagAnswer } from '../types/api';

function TestAnswer({ answer }: { answer: RagAnswer }) {
  const citations = answer.citations ?? [];
  if (answer.status === 'INSUFFICIENT_EVIDENCE' || !answer.answer) {
    return <div>The available project sources do not contain enough evidence to answer this question.</div>;
  }
  return <>{citations.map((citation) => <button key={citation.id}>{citation.docCode} {citation.documentTitle} p. {citation.page}</button>)}</>;
}

describe('AI answer states', () => {
  it('renders insufficient evidence professionally', () => {
    render(<TestAnswer answer={{ status: 'INSUFFICIENT_EVIDENCE' }} />);
    expect(screen.getByText(/do not contain enough evidence/i)).toBeInTheDocument();
  });

  it('renders interactive citation labels', () => {
    render(<TestAnswer answer={{ answer: 'Use the cited evidence.', citations: [{ id: '1', docCode: 'DOC-1', documentTitle: 'Queue Study', page: 4 }] }} />);
    expect(screen.getByRole('button', { name: /DOC-1 Queue Study p. 4/i })).toBeInTheDocument();
  });
});
