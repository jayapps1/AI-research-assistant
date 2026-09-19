import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProfilePage } from './ProfilePage';
import { profileApi } from '../api/endpoints';
import * as AuthModule from '../auth/AuthProvider';

describe('ProfilePage', () => {
  let queryClient: QueryClient;

  const mockUser = {
    id: '11111111-1111-1111-1111-111111111111',
    email: 'researcher@institution.edu',
    firstName: 'Ada',
    lastName: 'Lovelace',
    fullName: 'Ada Lovelace',
    roles: ['ROLE_USER'],
    status: 'ACTIVE',
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    vi.clearAllMocks();

    vi.spyOn(AuthModule, 'useAuth').mockReturnValue({
      user: mockUser,
      status: 'authenticated',
      isAuthenticated: true,
      login: vi.fn(),
      completeTotpChallenge: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      logoutAll: vi.fn(),
      hasCapability: vi.fn().mockReturnValue(false),
      updateUser: vi.fn(),
    });

    vi.spyOn(profileApi, 'getProfile').mockResolvedValue({
      userId: mockUser.id,
      email: mockUser.email,
      firstName: mockUser.firstName,
      lastName: mockUser.lastName,
      fullName: mockUser.fullName,
      avatarUrl: null,
      profileImage: null,
    });
  });

  it('renders profile details and upload photo button', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <ProfilePage />
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'My Profile' })).toBeInTheDocument();
      expect(screen.getAllByText('Ada Lovelace').length).toBeGreaterThan(0);
      expect(screen.getAllByText('researcher@institution.edu').length).toBeGreaterThan(0);
      expect(screen.getByRole('button', { name: /Upload Photo/i })).toBeInTheDocument();
    });
  });

  it('validates file format on upload attempt', async () => {
    const { container } = render(
      <QueryClientProvider client={queryClient}>
        <ProfilePage />
      </QueryClientProvider>,
    );

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;
    expect(fileInput).toBeInTheDocument();

    const invalidFile = new File(['dummy content'], 'document.pdf', { type: 'application/pdf' });
    fireEvent.change(fileInput, { target: { files: [invalidFile] } });

    await waitFor(() => {
      expect(screen.getByText(/Invalid file format. Please upload a JPEG, PNG, or WebP image/i)).toBeInTheDocument();
    });
  });

  it('validates file size on upload attempt', async () => {
    const { container } = render(
      <QueryClientProvider client={queryClient}>
        <ProfilePage />
      </QueryClientProvider>,
    );

    const fileInput = container.querySelector('input[type="file"]') as HTMLInputElement;

    // 6MB file
    const largeContent = new Uint8Array(6 * 1024 * 1024);
    const largeFile = new File([largeContent], 'huge.png', { type: 'image/png' });
    fireEvent.change(fileInput, { target: { files: [largeFile] } });

    await waitFor(() => {
      expect(screen.getByText(/exceeds the 5MB limit/i)).toBeInTheDocument();
    });
  });
});
