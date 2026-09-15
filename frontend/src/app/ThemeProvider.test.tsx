import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ThemeProvider, useTheme } from './ThemeProvider';

function ThemeHarness() {
  const theme = useTheme();
  return <button type="button" onClick={() => theme.setTheme('dark')}>{theme.theme}</button>;
}

describe('ThemeProvider', () => {
  it('persists safe UI theme preference locally', async () => {
    localStorage.clear();
    render(<ThemeProvider><ThemeHarness /></ThemeProvider>);

    await userEvent.click(screen.getByRole('button', { name: /system|light|dark/i }));

    expect(localStorage.getItem('raa.theme')).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });
});
