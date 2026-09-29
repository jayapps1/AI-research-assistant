import { Fragment, useMemo, useRef, useState, type ReactNode } from 'react';
import { clsx } from 'clsx';
import { Copy } from 'lucide-react';
import ReactMarkdown, { type Components, type UrlTransform } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button } from './ui';

type CopyState = 'idle' | 'copied' | 'failed';

export interface AssistantResponseProps {
  content?: string | null;
  actions?: ReactNode;
  className?: string;
  contentClassName?: string;
  copyLabel?: string;
  ariaLabel?: string;
}

export function AssistantResponse({
  content,
  actions,
  className,
  contentClassName,
  copyLabel = 'Copy',
  ariaLabel = 'Assistant response',
}: AssistantResponseProps) {
  const normalizedContent = useMemo(() => normalizeAssistantContent(content), [content]);
  const [copyState, setCopyState] = useState<CopyState>('idle');

  async function handleCopy() {
    try {
      if (!navigator.clipboard?.writeText) {
        throw new Error('Clipboard API is unavailable.');
      }
      await navigator.clipboard.writeText(normalizedContent);
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    } finally {
      window.setTimeout(() => setCopyState('idle'), 1800);
    }
  }

  const statusLabel = copyState === 'copied' ? 'Copied' : copyState === 'failed' ? 'Copy failed' : copyLabel;

  return (
    <section className={clsx('assistant-response', className)} aria-label={ariaLabel}>
      <div className={clsx('assistant-markdown', contentClassName)}>
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          skipHtml
          urlTransform={safeUrlTransform}
          components={markdownComponents}
        >
          {normalizedContent}
        </ReactMarkdown>
      </div>
      <div className="assistant-response-actions" aria-live="polite">
        <Button
          type="button"
          variant="secondary"
          className="btn-compact assistant-copy-button"
          onClick={handleCopy}
          aria-label="Copy assistant response"
        >
          <Copy size={14} aria-hidden />
          {statusLabel}
        </Button>
        {actions}
      </div>
    </section>
  );
}

export interface AssistantSourcesProps<T> {
  sources?: T[] | null;
  title?: string;
  initiallyOpen?: boolean;
  renderSource: (source: T, index: number) => ReactNode;
}

export function AssistantSources<T>({
  sources,
  title = 'Sources',
  initiallyOpen = false,
  renderSource,
}: AssistantSourcesProps<T>) {
  const [open, setOpen] = useState(initiallyOpen);
  const sourceList = sources ?? [];
  if (!sourceList.length) return null;

  return (
    <section className="assistant-sources" aria-label={title}>
      <button
        type="button"
        className="assistant-sources-toggle"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
      >
        {title} ({sourceList.length})
      </button>
      {open ? (
        <div className="assistant-sources-list">
          {sourceList.map((source, index) => (
            <Fragment key={index}>{renderSource(source, index)}</Fragment>
          ))}
        </div>
      ) : null}
    </section>
  );
}

const markdownComponents: Components = {
  h1: (props) => <h2 {...withoutMarkdownNode(props)} />,
  h2: (props) => <h3 {...withoutMarkdownNode(props)} />,
  h3: (props) => <h4 {...withoutMarkdownNode(props)} />,
  h4: (props) => <h5 {...withoutMarkdownNode(props)} />,
  h5: (props) => <h6 {...withoutMarkdownNode(props)} />,
  h6: (props) => <h6 {...withoutMarkdownNode(props)} />,
  a: ({ node, href, children, ...props }) => {
    void node;
    const safeHref = sanitizeUrl(href);
    if (!safeHref) return <span>{children}</span>;
    const external = isExternalHref(safeHref);
    return (
      <a
        {...props}
        href={safeHref}
        target={external ? '_blank' : undefined}
        rel={external ? 'noopener noreferrer' : undefined}
      >
        {children}
      </a>
    );
  },
  table: (props) => (
    <div className="assistant-markdown-table-wrap">
      <table {...withoutMarkdownNode(props)} />
    </div>
  ),
  pre: ({ children }) => <CodeBlock>{children}</CodeBlock>,
};

const safeUrlTransform: UrlTransform = (url) => sanitizeUrl(url) ?? '';

function withoutMarkdownNode<T extends { node?: unknown }>(props: T) {
  const { node, ...rest } = props;
  void node;
  return rest;
}

function CodeBlock({ children }: { children: ReactNode }) {
  const preRef = useRef<HTMLPreElement>(null);
  const [copyState, setCopyState] = useState<CopyState>('idle');

  async function handleCopy() {
    try {
      if (!navigator.clipboard?.writeText) {
        throw new Error('Clipboard API is unavailable.');
      }
      await navigator.clipboard.writeText(preRef.current?.textContent ?? '');
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    } finally {
      window.setTimeout(() => setCopyState('idle'), 1800);
    }
  }

  return (
    <div className="assistant-code-block">
      <Button
        type="button"
        variant="secondary"
        className="btn-compact assistant-code-copy"
        onClick={handleCopy}
        aria-label="Copy code block"
      >
        <Copy size={13} aria-hidden />
        {copyState === 'copied' ? 'Copied' : copyState === 'failed' ? 'Copy failed' : 'Copy'}
      </Button>
      <pre ref={preRef}>{children}</pre>
    </div>
  );
}

export function normalizeAssistantContent(content?: string | null) {
  let value = typeof content === 'string' ? content : '';
  value = stripByteOrderMark(normalizeLineEndings(value));

  const parsedBeforeDecode = parseJsonWrappedContent(value);
  if (parsedBeforeDecode !== null) {
    value = parsedBeforeDecode;
  }

  value = decodeHtmlEntities(value);

  const parsedAfterDecode = parseJsonWrappedContent(value);
  if (parsedAfterDecode !== null) {
    value = parsedAfterDecode;
  }

  if (shouldDecodeLiteralNewlines(value)) {
    value = decodeEscapedWhitespace(value);
  }

  value = normalizeLineEndings(value);
  value = dedentLegacyMarkdown(value);
  return trimOuterBlankLines(value);
}

function parseJsonWrappedContent(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const first = trimmed[0];
  if (first !== '"' && first !== '{' && first !== '[') return null;

  try {
    return extractContentFromParsedJson(JSON.parse(trimmed), 0);
  } catch {
    return null;
  }
}

function extractContentFromParsedJson(value: unknown, depth: number): string | null {
  if (typeof value === 'string') return value;
  if (depth > 2 || value === null || typeof value !== 'object' || Array.isArray(value)) return null;

  const record = value as Record<string, unknown>;
  for (const key of ['answer', 'content', 'draftText', 'text', 'message', 'response']) {
    const extracted = extractContentFromParsedJson(record[key], depth + 1);
    if (extracted !== null) return extracted;
  }
  return null;
}

function decodeHtmlEntities(value: string) {
  if (!value.includes('&') || typeof document === 'undefined') return value;
  const textarea = document.createElement('textarea');
  textarea.innerHTML = value;
  return textarea.value;
}

function shouldDecodeLiteralNewlines(value: string) {
  if (countOccurrences(value, '\n') > 0) return false;
  const escapedNewlineCount = countOccurrences(value, '\\n');
  if (escapedNewlineCount < 2) return false;
  return /\\n(?:\\n|\s*(?:#{1,6}\s|[-*+]\s|\d+[.)]\s|>\s|\|))/.test(value);
}

function decodeEscapedWhitespace(value: string) {
  let output = '';
  let escaped = false;

  for (const char of value) {
    if (!escaped) {
      if (char === '\\') {
        escaped = true;
      } else {
        output += char;
      }
      continue;
    }

    if (char === 'n') output += '\n';
    else if (char === 'r') output += '\r';
    else if (char === 't') output += '\t';
    else output += `\\${char}`;
    escaped = false;
  }

  if (escaped) output += '\\';
  return output;
}

function dedentLegacyMarkdown(value: string) {
  const lines = value.split('\n');
  const nonBlankLines = lines.filter((line) => line.trim().length > 0);
  if (nonBlankLines.length < 2) return value;

  const commonIndent = Math.min(...nonBlankLines.map(countLeadingSpaces));
  if (commonIndent < 2) return value;

  const hasMarkdownAfterIndent = nonBlankLines.some((line) => {
    const trimmedStart = line.slice(commonIndent);
    return (
      trimmedStart.startsWith('#') ||
      trimmedStart.startsWith('>') ||
      trimmedStart.startsWith('|') ||
      /^[-*+]\s/.test(trimmedStart) ||
      /^\d+[.)]\s/.test(trimmedStart)
    );
  });
  if (!hasMarkdownAfterIndent) return value;

  return lines
    .map((line) => (line.trim() ? line.slice(Math.min(commonIndent, countLeadingSpaces(line))) : ''))
    .join('\n');
}

function trimOuterBlankLines(value: string) {
  const lines = value.split('\n');
  let start = 0;
  let end = lines.length;

  while (start < end && lines[start].trim() === '') start += 1;
  while (end > start && lines[end - 1].trim() === '') end -= 1;

  return lines.slice(start, end).join('\n');
}

function normalizeLineEndings(value: string) {
  return value.replace(/\r\n?/g, '\n');
}

function stripByteOrderMark(value: string) {
  return value.charCodeAt(0) === 0xfeff ? value.slice(1) : value;
}

function countLeadingSpaces(value: string) {
  let count = 0;
  while (value[count] === ' ' || value[count] === '\u00a0') count += 1;
  return count;
}

function countOccurrences(value: string, sequence: string) {
  let count = 0;
  let index = 0;
  while (index < value.length) {
    const nextIndex = value.indexOf(sequence, index);
    if (nextIndex === -1) break;
    count += 1;
    index = nextIndex + sequence.length;
  }
  return count;
}

function sanitizeUrl(url?: string | null) {
  const value = String(url ?? '').trim();
  if (!value) return undefined;
  if (value.startsWith('#') || value.startsWith('/')) return value;

  try {
    const base = typeof window !== 'undefined' ? window.location.origin : 'https://localhost';
    const parsed = new URL(value, base);
    if (['http:', 'https:', 'mailto:', 'tel:'].includes(parsed.protocol)) return value;
  } catch {
    return undefined;
  }

  return undefined;
}

function isExternalHref(href: string) {
  if (href.startsWith('#') || href.startsWith('/')) return false;
  try {
    const base = typeof window !== 'undefined' ? window.location.origin : 'https://localhost';
    return new URL(href, base).origin !== base;
  } catch {
    return false;
  }
}
