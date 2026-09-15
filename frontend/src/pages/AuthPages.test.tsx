import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { LoginPage, TotpPage } from './AuthPages';
import { renderApp } from '../test/test-utils';

describe('LoginPage', () => {
  it('renders required authentication fields', async () => {
    renderApp(<LoginPage />);

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/invalid email/i)).toBeInTheDocument();
  });

  it('renders the dedicated TOTP verification form', () => {
    renderApp(<TotpPage />);
    expect(screen.getByLabelText(/6-digit code/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify and continue/i })).toBeInTheDocument();
  });
});
