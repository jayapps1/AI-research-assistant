import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AssistantResponse, AssistantSources, normalizeAssistantContent } from './AssistantResponse';

const acceptanceContent = `### Positive effects

1. **Higher productivity**
   Precision farming helps farmers apply the right amount of water, fertilizer,
   and pesticides, improving crop yields.

2. **Reduced costs**
   Digital tools can lower labor, input, and fuel costs by making farm
   operations more efficient.

3. **Better decision-making**
   Weather forecasts, soil data, and crop-monitoring systems help farmers make
   informed decisions.

4. **Improved market access**
   Online platforms connect farmers directly with buyers.

5. **Efficient resource management**
   Smart irrigation and monitoring technologies help conserve water.`;

describe('AssistantResponse', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('renders headings, bold text, and ordered lists without raw Markdown markers', () => {
    const { container } = render(<AssistantResponse content={acceptanceContent} />);

    expect(screen.getByRole('heading', { name: 'Positive effects' })).toBeInTheDocument();
    expect(screen.getByText('Higher productivity').tagName).toBe('STRONG');
    expect(screen.getByText('Efficient resource management').tagName).toBe('STRONG');
    expect(container.querySelector('ol')).toBeInTheDocument();
    expect(container.textContent).not.toContain('###');
    expect(container.textContent).not.toContain('**');
  });

  it('renders unordered lists, paragraphs, blockquotes, links, tables, inline code, code blocks, and horizontal rules', () => {
    render(
      <AssistantResponse
        content={`## Summary

Paragraph with *italic text*, **bold text**, and \`inlineCode()\`.

- First item
- Second item
  - Nested item

> Evidence should stay visually separate.

[Open example](https://example.com/research)

| Method | Result |
| --- | --- |
| Survey | Improved access |

---

\`\`\`ts
const value = 1;
\`\`\``}
      />,
    );

    expect(screen.getByRole('heading', { name: 'Summary' })).toBeInTheDocument();
    expect(screen.getByText('italic text').tagName).toBe('EM');
    expect(screen.getByText('bold text').tagName).toBe('STRONG');
    expect(screen.getByText('inlineCode()').tagName).toBe('CODE');
    expect(screen.getByRole('link', { name: 'Open example' })).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.getByRole('table')).toBeInTheDocument();
    expect(screen.getByText('const value = 1;')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /copy code block/i })).toBeInTheDocument();
  });

  it('normalizes legacy JSON-wrapped, HTML-encoded, indented Markdown before rendering', () => {
    const legacy = JSON.stringify({
      answer: '&nbsp;&nbsp;### Legacy heading\\n\\n&nbsp;&nbsp;1. **Stored bold** item',
    });

    const { container } = render(<AssistantResponse content={legacy} />);

    expect(screen.getByRole('heading', { name: 'Legacy heading' })).toBeInTheDocument();
    expect(screen.getByText('Stored bold').tagName).toBe('STRONG');
    expect(container.textContent).not.toContain('&nbsp;');
    expect(normalizeAssistantContent(legacy)).toContain('### Legacy heading');
  });

  it('copies only the normalized response content and shows copied feedback', async () => {
    render(
      <div>
        <AssistantResponse content={acceptanceContent} />
        <AssistantSources sources={['Source one']} renderSource={(source) => <div>{source}</div>} />
      </div>,
    );

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /copy assistant response/i }));
      await Promise.resolve();
    });

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(acceptanceContent);
    expect(screen.getByRole('button', { name: /copy assistant response/i })).toHaveTextContent('Copied');
    expect(navigator.clipboard.writeText).not.toHaveBeenCalledWith(expect.stringContaining('Sources'));

    act(() => {
      vi.advanceTimersByTime(1800);
    });

    expect(screen.getByRole('button', { name: /copy assistant response/i })).toHaveTextContent('Copy');
  });

  it('handles clipboard failure without reloading the page', async () => {
    vi.mocked(navigator.clipboard.writeText).mockRejectedValueOnce(new Error('denied'));

    render(<AssistantResponse content="Plain answer" />);
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /copy assistant response/i }));
      await Promise.resolve();
    });

    expect(screen.getByText('Copy failed')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(1800);
    });

    expect(screen.getByRole('button', { name: /copy assistant response/i })).toHaveTextContent('Copy');
  });

  it('keeps long responses fully rendered with table and code overflow wrappers', () => {
    const paragraphs = Array.from({ length: 10 }, (_, index) => `Paragraph ${index + 1} explains the result with a long citation https://example.com/research/${index + 1}.`).join('\n\n');
    const items = Array.from({ length: 22 }, (_, index) => `${index + 1}. List item ${index + 1}`).join('\n');
    const longResponse = `# Long synthesis

${paragraphs}

## Findings

${items}

| Citation | Finding | Long URL |
| --- | --- | --- |
| [1] | Market access improved | https://example.com/a/very/long/path/that/should/not/force/prose/overflow |

\`\`\`js
const citations = ["Smith 2026", "Jones 2025"];
\`\`\``;

    const { container } = render(<AssistantResponse content={longResponse} />);

    expect(screen.getByText(/Paragraph 10 explains/)).toBeInTheDocument();
    expect(screen.getByText('List item 22')).toBeInTheDocument();
    expect(container.querySelector('.assistant-markdown-table-wrap')).toBeInTheDocument();
    expect(container.querySelector('.assistant-code-block pre')).toBeInTheDocument();
  });

  it('does not execute or link unsafe raw HTML content', () => {
    render(<AssistantResponse content={'<script>alert("x")</script>\n\n[Bad](javascript:alert("x"))'} />);

    expect(screen.queryByText(/alert/)).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Bad' })).not.toBeInTheDocument();
    expect(screen.getByText('Bad')).toBeInTheDocument();
  });
});
