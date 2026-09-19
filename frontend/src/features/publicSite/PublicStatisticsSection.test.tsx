import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { PublicStatisticsSection } from './PublicStatisticsSection';
import { publicApi } from '../../api/public';
import type { PublicStatistic } from '../../types/publicSite';

describe('PublicStatisticsSection Component', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.clearAllMocks();
  });

  it('renders nothing when publicApi returns empty array', async () => {
    vi.spyOn(publicApi, 'getStatistics').mockResolvedValueOnce([]);

    const { container } = render(
      <QueryClientProvider client={queryClient}>
        <PublicStatisticsSection />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it('renders statistics cards when metrics are returned', async () => {
    const mockStats: PublicStatistic[] = [
      {
        code: 'total-projects',
        label: 'Active Research Projects',
        value: '1,420',
        prefix: '>',
        suffix: '+',
        iconKey: 'folder-kanban',
        featured: true,
      },
      {
        code: 'active-users',
        label: 'Global Scholars',
        value: '8,500',
        suffix: '+',
        iconKey: 'users',
        featured: false,
      },
      {
        code: 'documents-processed',
        label: 'Empirical Documents Analyzed',
        value: '95,000',
        suffix: '+',
        iconKey: 'file-text',
        featured: false,
      },
    ];

    vi.spyOn(publicApi, 'getStatistics').mockResolvedValueOnce(mockStats);

    render(
      <QueryClientProvider client={queryClient}>
        <PublicStatisticsSection />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText('Platform Scale & Empirical Impact')).toBeInTheDocument();
      expect(screen.getByText('>1,420+')).toBeInTheDocument();
      expect(screen.getByText('Active Research Projects')).toBeInTheDocument();
      expect(screen.getByText('8,500+')).toBeInTheDocument();
      expect(screen.getByText('Global Scholars')).toBeInTheDocument();
      expect(screen.getByText('95,000+')).toBeInTheDocument();
      expect(screen.getByText('Featured')).toBeInTheDocument();
    });
  });
});
