import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ReportRichEditor } from './ReportRichEditor';

function docJson(text: string) {
  return JSON.stringify({
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [{ type: 'text', text }],
      },
    ],
  });
}

describe('ReportRichEditor hydration', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('hydrates content that arrives after editor initialization', async () => {
    const { rerender } = render(
      <ReportRichEditor contentJson="" hydrationKey="version-loading" isLoading readOnly />
    );

    rerender(
      <ReportRichEditor contentJson={docJson('Loaded complete final document body')} hydrationKey="version-1" readOnly />
    );

    expect(await screen.findByText('Loaded complete final document body')).toBeInTheDocument();
  });

  it('does not autosave the empty loading document before hydration', async () => {
    const onSave = vi.fn();
    const { rerender } = render(
      <ReportRichEditor contentJson="" hydrationKey="version-loading" isLoading onSave={onSave} />
    );

    await new Promise((resolve) => setTimeout(resolve, 1500));
    expect(onSave).not.toHaveBeenCalled();

    rerender(
      <ReportRichEditor contentJson={docJson('Hydrated non-empty final document')} hydrationKey="version-1" onSave={onSave} />
    );

    await waitFor(() => expect(screen.getByText('Hydrated non-empty final document')).toBeInTheDocument());
    await new Promise((resolve) => setTimeout(resolve, 1500));
    expect(onSave).not.toHaveBeenCalled();
  });

  it('stores inserted page breaks as structural editor nodes', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);

    render(
      <ReportRichEditor contentJson={docJson('Chapter one text')} hydrationKey="version-1" onSave={onSave} />
    );

    fireEvent.click(await screen.findByRole('button', { name: /insert page break/i }));
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    const payload = onSave.mock.calls.at(-1)?.[0];
    expect(JSON.parse(payload.contentJson).content.some((node: { type: string }) => node.type === 'pageBreak')).toBe(true);
  });

  it('renders the word processor chrome around the editable page', async () => {
    const { container } = render(
      <ReportRichEditor contentJson={docJson('Paginated editor body')} hydrationKey="version-1" />
    );

    expect(await screen.findByRole('toolbar', { name: /document formatting toolbar/i })).toBeInTheDocument();
    expect(container.querySelector('.document-ruler')).toBeInTheDocument();
    expect(container.querySelector('.document-page')).toBeInTheDocument();
    expect(screen.getByText(/Page 1 of/i)).toBeInTheDocument();
  });

  it('stores inserted section breaks as structural editor nodes', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);

    render(
      <ReportRichEditor contentJson={docJson('Front matter')} hydrationKey="version-1" onSave={onSave} />
    );

    fireEvent.click(await screen.findByRole('button', { name: /insert section break/i }));
    fireEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    const payload = onSave.mock.calls.at(-1)?.[0];
    expect(JSON.parse(payload.contentJson).content.some((node: { type: string }) => node.type === 'sectionBreak')).toBe(true);
  });

  it('shows the active template formatting profile', async () => {
    render(
      <ReportRichEditor
        contentJson={docJson('Template profile text')}
        hydrationKey="version-1"
        formattingProfile={{
          margins: { top: '25mm', bottom: '25mm', left: '35mm', right: '25mm' },
          fontFamily: 'Georgia',
          bodyFontSize: '11pt',
          lineSpacing: '2',
          chapterBreak: 'NEW_PAGE',
          pageNumbering: 'FRONT_ROMAN_MAIN_ARABIC',
        }}
      />
    );

    fireEvent.click(await screen.findByRole('button', { name: /template style/i }));

    const profile = screen.getByRole('region', { name: /current document formatting profile/i });
    expect(profile).toHaveTextContent('Georgia');
    expect(profile).toHaveTextContent('NEW_PAGE');
    expect(profile).toHaveTextContent('FRONT_ROMAN_MAIN_ARABIC');
  });

  it('prevents browser save and performs manual save with Ctrl+S', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);

    render(
      <ReportRichEditor contentJson={docJson('Manual save text')} hydrationKey="version-1" onSave={onSave} />
    );

    const editor = await screen.findByText('Manual save text');
    const event = new KeyboardEvent('keydown', { key: 's', ctrlKey: true, bubbles: true, cancelable: true });
    editor.dispatchEvent(event);

    await waitFor(() => expect(onSave).toHaveBeenCalled());
    expect(event.defaultPrevented).toBe(true);
  });
});
