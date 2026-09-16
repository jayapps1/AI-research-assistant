import { screen, fireEvent, waitFor } from '@testing-library/react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { AdminPublicSitePage } from './AdminPublicSitePage';
import { AdminContactInboxPage } from './AdminContactInboxPage';
import { publicApi } from '../../api/public';

describe('Admin Public Site & Contact Inbox Suite', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    vi.clearAllMocks();
  });

  it('AdminPublicSitePage renders tabs and allows updating site settings', async () => {
    vi.spyOn(publicApi, 'getAdminSiteSettings').mockResolvedValueOnce({
      siteName: 'AI Research Portal',
      tagline: 'Empirical AI Workbench',
      supportEmail: 'admin@portal.edu',
      registrationEnabled: true,
      publicPricingEnabled: true,
    });

    const updateSpy = vi.spyOn(publicApi, 'updateAdminSiteSettings').mockResolvedValueOnce({
      siteName: 'Updated Research Portal',
      tagline: 'Empirical AI Workbench',
      supportEmail: 'admin@portal.edu',
      registrationEnabled: true,
      publicPricingEnabled: true,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminPublicSitePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByDisplayValue('AI Research Portal')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save site settings/i })).toBeInTheDocument();

    // Change site name and submit
    fireEvent.change(screen.getByDisplayValue('AI Research Portal'), {
      target: { value: 'Updated Research Portal' },
    });
    fireEvent.click(screen.getByRole('button', { name: /save site settings/i }));

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({ siteName: 'Updated Research Portal' }),
      );
    });
  });

  it('AdminContactInboxPage lists submissions, views detail, and records internal notes', async () => {
    vi.spyOn(publicApi, 'getAdminContactSubmissions').mockResolvedValueOnce({
      content: [
        {
          id: 'sub-1',
          referenceCode: 'CNT-2026-000100',
          name: 'Prof. Amara Diallo',
          email: 'adiallo@univ.edu',
          subject: 'Cohort Licensing Inquiry',
          message: 'Can we license for 100 students?',
          status: 'NEW',
          source: 'PUBLIC_WEBSITE',
          submittedAt: '2026-09-15T20:00:00Z',
          responses: [],
        },
      ],
      page: 0,
      size: 15,
      totalElements: 1,
      totalPages: 1,
    });

    vi.spyOn(publicApi, 'getAdminContactSubmissionDetail').mockResolvedValueOnce({
      id: 'sub-1',
      referenceCode: 'CNT-2026-000100',
      name: 'Prof. Amara Diallo',
      email: 'adiallo@univ.edu',
      subject: 'Cohort Licensing Inquiry',
      message: 'Can we license for 100 students?',
      status: 'READ',
      source: 'PUBLIC_WEBSITE',
      submittedAt: '2026-09-15T20:00:00Z',
      responses: [],
    });

    const replySpy = vi.spyOn(publicApi, 'replyToAdminContactSubmission').mockResolvedValueOnce({
      id: 'resp-1',
      channel: 'INTERNAL_NOTE',
      status: 'SENT',
      messageBody: 'Discussed with Dean; sending proposal tomorrow.',
      createdAt: '2026-09-15T21:00:00Z',
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminContactInboxPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Verify row in table
    expect(await screen.findByText('CNT-2026-000100')).toBeInTheDocument();
    expect(screen.getByText('Prof. Amara Diallo')).toBeInTheDocument();

    // Click on row to open detail
    fireEvent.click(screen.getByText('CNT-2026-000100'));

    // Verify detail pane opened
    expect(await screen.findByText('Can we license for 100 students?')).toBeInTheDocument();

    // Select Internal Staff Note
    fireEvent.click(screen.getByLabelText(/internal staff note/i));

    // Type reply note
    const replyInput = screen.getByPlaceholderText(/write an internal note/i);
    fireEvent.change(replyInput, {
      target: { value: 'Discussed with Dean; sending proposal tomorrow.' },
    });

    // Submit reply
    fireEvent.click(screen.getByRole('button', { name: /record response/i }));

    await waitFor(() => {
      expect(replySpy).toHaveBeenCalledWith(
        'sub-1',
        expect.objectContaining({
          channel: 'INTERNAL_NOTE',
          messageBody: 'Discussed with Dean; sending proposal tomorrow.',
        }),
      );
    });
  });
});
