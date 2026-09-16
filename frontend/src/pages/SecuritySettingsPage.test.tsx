import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '../api/endpoints';
import { setTokens, clearTokens } from '../api/tokens';
import { AuthProvider } from '../auth/AuthProvider';
import { SecuritySettingsPage } from './SecuritySettingsPage';

function renderSecuritySettings(user: Record<string, unknown>) {
  clearTokens();
  sessionStorage.clear();
  setTokens({ accessToken: 'test-access-token', refreshToken: 'test-refresh-token' });
  sessionStorage.setItem('raa.user', JSON.stringify(user));

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <SecuritySettingsPage />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe('SecuritySettingsPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders enrollment button when TOTP is not enabled', () => {
    renderSecuritySettings({ id: 'u-1', email: 'user@test.com', totpEnabled: false });

    expect(screen.getByRole('button', { name: /set up authenticator/i })).toBeInTheDocument();
    expect(screen.getByText(/not enabled/i)).toBeInTheDocument();
  });

  it('starts enrollment, renders QR code SVG, and hides raw otpauth:// URI', async () => {
    vi.spyOn(authApi, 'startTotpEnrollment').mockResolvedValue({
      issuer: 'AI Research Assistant',
      accountName: 'user@test.com',
      secret: 'JBSWY3DPEHPK3PXP',
      provisioningUri: 'otpauth://totp/AI%20Research:user@test.com?secret=JBSWY3DPEHPK3PXP&issuer=AI%20Research',
    });

    renderSecuritySettings({ id: 'u-1', email: 'user@test.com', totpEnabled: false });

    fireEvent.click(screen.getByRole('button', { name: /set up authenticator/i }));

    // Wait for enrollment state to render
    await waitFor(() => {
      expect(screen.getByText(/set up your authenticator/i)).toBeInTheDocument();
      expect(screen.getByText(/scan this qr code/i)).toBeInTheDocument();
    });

    // Verify raw otpauth:// URI is NOT rendered in normal text
    expect(screen.queryByText(/otpauth:\/\/totp/i)).not.toBeInTheDocument();

    // Verify SVG QR code exists
    const svgElement = document.querySelector('.qr-container-responsive svg');
    expect(svgElement).toBeInTheDocument();

    // Manual setup key should be hidden by default
    expect(screen.queryByText(/JBSW Y3DP/i)).not.toBeInTheDocument();

    // Toggle manual setup key
    const toggleButton = screen.getByRole('button', { name: /show setup key/i });
    fireEvent.click(toggleButton);

    // Now manual entry key chunks should be visible
    expect(screen.getByText(/JBSW Y3DP EHPK 3PXP/i)).toBeInTheDocument();
  });

  it('submits 6-digit code and presents one-time recovery codes modal with copy & download actions', async () => {
    vi.spyOn(authApi, 'startTotpEnrollment').mockResolvedValue({
      issuer: 'AI Research Assistant',
      accountName: 'user@test.com',
      secret: 'JBSWY3DPEHPK3PXP',
      provisioningUri: 'otpauth://totp/AI%20Research:user@test.com?secret=JBSWY3DPEHPK3PXP',
    });

    vi.spyOn(authApi, 'confirmTotpEnrollment').mockResolvedValue({
      recoveryCodes: ['REC-1111-2222', 'REC-3333-4444', 'REC-5555-6666'],
    });

    renderSecuritySettings({ id: 'u-1', email: 'user@test.com', totpEnabled: false });

    fireEvent.click(screen.getByRole('button', { name: /set up authenticator/i }));

    await waitFor(() => {
      expect(screen.getByText(/scan this qr code/i)).toBeInTheDocument();
    });

    // Type 6 digits into OtpInput
    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: '123456' } });

    // Click confirm enrollment button
    const confirmBtn = screen.getByRole('button', { name: /verify and enable/i });
    fireEvent.click(confirmBtn);

    // Modal with recovery codes should appear
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /recovery codes/i })).toBeInTheDocument();
      expect(screen.getByText('REC-1111-2222')).toBeInTheDocument();
      expect(screen.getByText('REC-3333-4444')).toBeInTheDocument();
    });

    // Verify copy and download buttons exist
    expect(screen.getByRole('button', { name: /copy all/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /download as text/i })).toBeInTheDocument();
  });

  it('renders step-up confirmation modal when disabling active 2FA', async () => {
    renderSecuritySettings({ id: 'u-1', email: 'user@test.com', totpEnabled: true });

    expect(screen.getByText(/enabled/i)).toBeInTheDocument();

    const disableBtn = screen.getByRole('button', { name: /disable authenticator/i });
    fireEvent.click(disableBtn);

    // Step-up modal should open
    expect(screen.getByText(/disable authenticator app/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/enter your current password/i)).toBeInTheDocument();
  });
});
