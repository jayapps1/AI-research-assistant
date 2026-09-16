import { screen, fireEvent, waitFor } from '@testing-library/react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { HomePage } from './HomePage';
import { ServicesPage } from './ServicesPage';
import { PricingPage } from './PricingPage';
import { ContactPage } from './ContactPage';
import { FaqPage } from './FaqPage';
import { AboutPage } from './AboutPage';
import { publicApi } from '../../api/public';
import { AuthProvider } from '../../auth/AuthProvider';

describe('Public Pages Suite', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    vi.clearAllMocks();
  });

  it('HomePage renders high-impact academic hero and workflow steps', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>
            <HomePage />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/From Research Question to/i);
    expect(screen.getAllByText(/Literature Matrix/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Methodology Design/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Qualitative Thematic Coding/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Evidence-Anchored Citations/i)).toBeInTheDocument();
  });

  it('ServicesPage renders search bar, filters, and dynamic service cards', async () => {
    vi.spyOn(publicApi, 'getServices').mockResolvedValueOnce([
      {
        id: '1',
        slug: 'literature-matrix',
        name: 'Systematic Literature Synthesis',
        tagline: 'Comprehensive empirical synthesis',
        description: 'Synthesize empirical papers into matrix.',
        academicBenefit: 'Accelerates systematic literature reviews.',
        category: 'Literature Review',
        featured: true,
        displayOrder: 1,
      },
      {
        id: '2',
        slug: 'statistical-modeling',
        name: 'Inferential Statistical Modeling',
        tagline: 'Hypothesis testing & regression',
        description: 'Verify statistical assumptions and run regressions.',
        academicBenefit: 'Provides reproducible APA 7 tables.',
        category: 'Quantitative',
        featured: true,
        displayOrder: 2,
      },
    ]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ServicesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText('Systematic Literature Synthesis')).toBeInTheDocument();
    expect(screen.getByText('Inferential Statistical Modeling')).toBeInTheDocument();

    // Test search filter
    const searchInput = screen.getByPlaceholderText(/search capabilities/i);
    fireEvent.change(searchInput, { target: { value: 'statistical' } });

    expect(screen.queryByText('Systematic Literature Synthesis')).not.toBeInTheDocument();
    expect(screen.getByText('Inferential Statistical Modeling')).toBeInTheDocument();
  });

  it('PricingPage derives tiers exclusively from public pricing API and toggles billing cycles', async () => {
    vi.spyOn(publicApi, 'getPricing').mockResolvedValueOnce([
      {
        id: 'plan-1',
        code: 'FREE',
        name: 'Academic Free',
        description: 'Individual exploratory research',
        monthlyPrice: 0,
        annualPrice: 0,
        currency: 'USD',
        displayOrder: 1,
        highlighted: false,
        features: ['1 Workspace', '100 Document Pages', 'Standard Citations'],
      },
      {
        id: 'plan-2',
        code: 'PRO',
        name: 'Doctoral Pro',
        description: 'Full dissertation suite',
        monthlyPrice: 20,
        annualPrice: 192,
        currency: 'USD',
        displayOrder: 2,
        highlighted: true,
        highlightBadge: 'Most Popular',
        features: ['Unlimited Workspaces', 'Advanced Statistical Run', 'Full Viva Simulator'],
        academicDiscountsAvailable: '50% student discount with .edu email',
      },
    ]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>
            <PricingPage />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText('Academic Free')).toBeInTheDocument();
    expect(screen.getByText('Doctoral Pro')).toBeInTheDocument();
    expect(screen.getByText(/50% student discount with .edu email/i)).toBeInTheDocument();

    // Default is annual ($192 / 12 = 16)
    expect(screen.getByText('16')).toBeInTheDocument();

    // Toggle to Monthly ($20)
    const monthlyBtn = screen.getByRole('button', { name: /monthly billing/i });
    fireEvent.click(monthlyBtn);
    expect(screen.getByText('20')).toBeInTheDocument();
  });

  it('ContactPage validates required fields and renders tracking reference code on submission', async () => {
    vi.spyOn(publicApi, 'submitContact').mockResolvedValueOnce({
      referenceCode: 'CNT-2026-000042',
      message: 'Thank you. Your academic inquiry has been received.',
      receivedAt: '2026-09-15T21:00:00Z',
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ContactPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Click submit empty -> triggers validation errors
    const submitBtn = screen.getByRole('button', { name: /transmit inquiry/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/full name is required/i)).toBeInTheDocument();
    expect(screen.getByText(/email address is required/i)).toBeInTheDocument();

    // Fill valid data
    fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Dr. Kwame Appiah' } });
    fireEvent.change(screen.getByLabelText(/academic \/ work email/i), {
      target: { value: 'kwame.appiah@legon.edu.gh' },
    });
    fireEvent.change(screen.getByLabelText(/subject/i), {
      target: { value: 'Departmental License for Postgraduate Supervision' },
    });
    fireEvent.change(screen.getByLabelText(/inquiry message/i), {
      target: { value: 'We would like to pilot the AI Research Assistant across 40 doctoral candidates.' },
    });

    fireEvent.click(submitBtn);

    // Expect success state with reference code
    await waitFor(() => {
      expect(screen.getByText('Inquiry Successfully Received')).toBeInTheDocument();
      expect(screen.getByText('CNT-2026-000042')).toBeInTheDocument();
    });
  });

  it('AboutPage renders academic integrity mission and COPE alignment', () => {
    render(
      <MemoryRouter>
        <AboutPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /Empowering Scholars with Auditable AI/i })).toBeInTheDocument();
    expect(screen.getByText(/COPE & ICMJE Compliance/i)).toBeInTheDocument();
    expect(screen.getByText(/100% Citation Grounding/i)).toBeInTheDocument();
  });

  it('FaqPage renders questions and expands accordion on click', async () => {
    vi.spyOn(publicApi, 'getFaqs').mockResolvedValueOnce([
      {
        id: 'faq-1',
        category: 'ACADEMIC_INTEGRITY',
        question: 'Does this platform write full essays or dissertations?',
        answerMarkdown: 'No. The AI Research Assistant does not generate ghostwritten prose.',
        displayOrder: 1,
      },
    ]);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <FaqPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const questionBtn = await screen.findByRole('button', {
      name: /Does this platform write full essays or dissertations/i,
    });
    expect(questionBtn).toBeInTheDocument();

    // Click to expand answer
    fireEvent.click(questionBtn);
    expect(
      screen.getByText(/No\. The AI Research Assistant does not generate ghostwritten prose\./i),
    ).toBeInTheDocument();
  });
});
