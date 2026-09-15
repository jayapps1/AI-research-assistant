import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { LoginPage } from './AuthPages';
import { renderApp } from '../test/test-utils';

describe('LoginPage', () => {
  it('renders required authentication fields including optional TOTP', async () => {
    renderApp(<LoginPage />);

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/totp/i)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/invalid email/i)).toBeInTheDocument();
  });
});
