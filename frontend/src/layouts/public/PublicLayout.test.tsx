import { screen } from '@testing-library/react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { PublicHeader } from './PublicHeader';
import { PublicFooter } from './PublicFooter';
import { PublicLayout } from './PublicLayout';
import { AuthProvider } from '../../auth/AuthProvider';
import { ThemeProvider } from '../../app/ThemeProvider';
import { setTokens, clearTokens } from '../../api/tokens';

describe('PublicLayout & PublicHeader', () => {
  beforeEach(() => {
    clearTokens();
    sessionStorage.clear();
  });
  it('renders public navigation links and does not render app sidebar', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/']}>
          <ThemeProvider>
            <AuthProvider>
              <PublicLayout />
            </AuthProvider>
          </ThemeProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Verify public navigation items exist
    expect(screen.getAllByRole('link', { name: /home/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /services/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /pricing/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /about/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /faq/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /contact/i }).length).toBeGreaterThan(0);

    // Verify unauthenticated visitor buttons exist
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /get started/i })).toBeInTheDocument();

    // Verify application sidebar elements are NOT rendered
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
    expect(screen.queryByText(/switch workspace/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/my tasks/i)).not.toBeInTheDocument();
  });

  it('renders auth-aware CTA when researcher is authenticated', async () => {
    sessionStorage.setItem(
      'raa.user',
      JSON.stringify({
        id: '11111111-1111-1111-1111-111111111111',
        email: 'researcher@university.edu',
        firstName: 'Elena',
        lastName: 'Rostova',
        roles: ['USER'],
      }),
    );
    setTokens({ accessToken: 'mock-access', refreshToken: 'mock-refresh' });

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/']}>
          <ThemeProvider>
            <AuthProvider>
              <PublicHeader
                settings={{
                  siteName: 'AI Research Assistant',
                  registrationEnabled: true,
                  publicPricingEnabled: true,
                }}
              />
            </AuthProvider>
          </ThemeProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Expect "Go to Workbench" with researcher's first name badge
    const workbenchLink = screen.getByRole('link', { name: /go to workbench/i });
    expect(workbenchLink).toBeInTheDocument();
    expect(screen.getByText('Elena')).toBeInTheDocument();

    // Clean up
    clearTokens();
    sessionStorage.clear();
  });

  it('renders complete public footer with academic integrity and contact info', () => {
    render(
      <MemoryRouter>
        <PublicFooter
          settings={{
            siteName: 'AI Research Assistant',
            tagline: 'From Research Question to Final Report.',
            supportEmail: 'contact@researchassistant.ai',
            address: 'University Innovation Hub',
            registrationEnabled: true,
            publicPricingEnabled: true,
          }}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText(/contact@researchassistant.ai/i)).toBeInTheDocument();
    expect(screen.getByText(/university innovation hub/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /privacy policy & data security/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /terms of academic use/i })).toBeInTheDocument();
  });
});
