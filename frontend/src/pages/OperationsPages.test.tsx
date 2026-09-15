import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Badge } from '../components/ui';

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
