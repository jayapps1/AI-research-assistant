import { screen } from '@testing-library/react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import App from './App';
import { AuthProvider } from './auth/AuthProvider';

describe('protected routing', () => {
  it('redirects anonymous users to login', async () => {
    sessionStorage.clear();
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/admin']}>
          <AuthProvider><App /></AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(await screen.findByText(/sign in to your research workspace/i)).toBeInTheDocument();
  });
});
