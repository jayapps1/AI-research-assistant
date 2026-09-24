import { useEffect, useRef, useState, useCallback } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
import TextAlign from '@tiptap/extension-text-align';
import { TextStyle } from '@tiptap/extension-text-style';
import Color from '@tiptap/extension-color';
import Highlight from '@tiptap/extension-highlight';
import { Table } from '@tiptap/extension-table';
import TableRow from '@tiptap/extension-table-row';
import TableHeader from '@tiptap/extension-table-header';
import TableCell from '@tiptap/extension-table-cell';
import Link from '@tiptap/extension-link';
import { Node, mergeAttributes } from '@tiptap/core';

import {
  Bold,
  Italic,
  Underline as UnderlineIcon,
  Strikethrough,
  Subscript as SubscriptIcon,
  Superscript as SuperscriptIcon,
  AlignLeft,
  AlignCenter,
  AlignRight,
  AlignJustify,
  List,
  ListOrdered,
  Quote,
  Table as TableIcon,
  Plus,
  Trash2,
  Undo,
  Redo,
  Bookmark,
  CheckCircle2,
  Clock,
  Wand2,
} from 'lucide-react';
import { Button } from '../ui';
import { InsertCitationModal } from './InsertCitationModal';

export const CitationNode = Node.create({
  name: 'citation',
  group: 'inline',
  inline: true,
  selectable: true,
  atom: true,

  addAttributes() {
    return {
      referenceId: { default: null },
      citationKey: { default: null },
      label: { default: '[Citation]' },
    };
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-type="citation"]',
        getAttrs: (element: HTMLElement | string) => {
          if (typeof element === 'string') return {};
          return {
            referenceId: element.getAttribute('data-reference-id'),
            citationKey: element.getAttribute('data-citation-key'),
            label: element.textContent || '[Citation]',
          };
        },
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'citation',
        'data-reference-id': HTMLAttributes.referenceId,
        'data-citation-key': HTMLAttributes.citationKey,
        class: 'citation-node',
        style:
          'display: inline-flex; align-items: center; padding: 1px 6px; margin: 0 2px; font-weight: 600; font-size: 0.85em; border-radius: 4px; background: rgba(59, 130, 246, 0.12); color: #2563eb; border: 1px solid rgba(59, 130, 246, 0.3); cursor: pointer; user-select: none;',
      }),
      HTMLAttributes.label || '[Citation]',
    ];
  },
});

function markdownToHtml(md: string): string {
  if (!md) return '<p></p>';

  // Clean lines
  const lines = md.split(/\r?\n/);
  const out: string[] = [];
  let inList = false;
  let listType: 'ul' | 'ol' | null = null;

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    const line = rawLine.trim();

    if (!line) {
      if (inList) {
        out.push(listType === 'ul' ? '</ul>' : '</ol>');
        inList = false;
        listType = null;
      }
      continue;
    }

    // Markdown Headings
    if (line.startsWith('### ')) {
      if (inList) { out.push(listType === 'ul' ? '</ul>' : '</ol>'); inList = false; }
      out.push(`<h3>${formatInline(line.substring(4))}</h3>`);
      continue;
    }
    if (line.startsWith('## ')) {
      if (inList) { out.push(listType === 'ul' ? '</ul>' : '</ol>'); inList = false; }
      out.push(`<h2>${formatInline(line.substring(3))}</h2>`);
      continue;
    }
    if (line.startsWith('# ')) {
      if (inList) { out.push(listType === 'ul' ? '</ul>' : '</ol>'); inList = false; }
      out.push(`<h1>${formatInline(line.substring(2))}</h1>`);
      continue;
    }

    // Blockquote
    if (line.startsWith('> ')) {
      if (inList) { out.push(listType === 'ul' ? '</ul>' : '</ol>'); inList = false; }
      out.push(`<blockquote><p>${formatInline(line.substring(2))}</p></blockquote>`);
      continue;
    }

    // Unordered List
    if (/^[-*+]\s+/.test(line)) {
      if (!inList || listType !== 'ul') {
        if (inList) out.push(listType === 'ul' ? '</ul>' : '</ol>');
        out.push('<ul>');
        inList = true;
        listType = 'ul';
      }
      const itemText = line.replace(/^[-*+]\s+/, '');
      out.push(`<li>${formatInline(itemText)}</li>`);
      continue;
    }

    // Ordered List
    if (/^\d+\.\s+/.test(line)) {
      if (!inList || listType !== 'ol') {
        if (inList) out.push(listType === 'ul' ? '</ul>' : '</ol>');
        out.push('<ol>');
        inList = true;
        listType = 'ol';
      }
      const itemText = line.replace(/^\d+\.\s+/, '');
      out.push(`<li>${formatInline(itemText)}</li>`);
      continue;
    }

    // Normal paragraph
    if (inList) {
      out.push(listType === 'ul' ? '</ul>' : '</ol>');
      inList = false;
      listType = null;
    }
    out.push(`<p>${formatInline(rawLine)}</p>`);
  }

  if (inList) {
    out.push(listType === 'ul' ? '</ul>' : '</ol>');
  }

  return out.join('');
}

function formatInline(text: string): string {
  let str = text;
  // Bold
  str = str.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  // Italic
  str = str.replace(/\*(.*?)\*/g, '<em>$1</em>');
  // Inline code
  str = str.replace(/`([^`]+)`/g, '<code>$1</code>');
  // Replace [[citation:id:key]]
  str = str.replace(
    /\[\[citation:([a-f0-9-]+):([^\]]+)\]\]/gi,
    '<span data-type="citation" data-reference-id="$1" data-citation-key="$2">[$2]</span>'
  );
  return str;
}

export interface ReportRichEditorProps {
  content?: string;
  contentJson?: string;
  projectId?: string;
  citationStyle?: string;
  readOnly?: boolean;
  minHeight?: string | number;
  onSave?: (data: { contentJson: string; plainText: string; markdown: string }) => void;
}

export function ReportRichEditor({
  content = '',
  contentJson = '',
  projectId = '',
  citationStyle = 'APA_7',
  readOnly = false,
  minHeight = '420px',
  onSave,
}: ReportRichEditorProps) {
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved'>('idle');
  const [citationModalOpen, setCitationModalOpen] = useState(false);
  const saveTimeoutRef = useRef<any>(null);
  const lastSavedJsonRef = useRef<string>('');

  const initialParsedContent = useCallback(() => {
    if (contentJson && contentJson.trim()) {
      try {
        const parsed = JSON.parse(contentJson);
        if (parsed && typeof parsed === 'object') return parsed;
      } catch (err) {
        console.warn('Invalid contentJson provided to ReportRichEditor, falling back to markdown', err);
      }
    }
    return markdownToHtml(content);
  }, [content, contentJson]);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3, 4],
        },
      }),
      Underline,
      Subscript,
      Superscript,
      TextStyle,
      Color,
      Highlight.configure({ multicolor: true }),
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
      Table.configure({
        resizable: true,
      }),
      TableRow,
      TableHeader,
      TableCell,
      Link.configure({
        openOnClick: false,
      }),
      CitationNode,
    ],
    content: initialParsedContent(),
    editable: !readOnly,
    onUpdate: ({ editor }) => {
      if (readOnly || !onSave) return;
      setSaveState('saving');
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);

      saveTimeoutRef.current = setTimeout(() => {
        const json = JSON.stringify(editor.getJSON());
        if (json === lastSavedJsonRef.current) {
          setSaveState('saved');
          return;
        }
        lastSavedJsonRef.current = json;
        const text = editor.getText();
        const html = editor.getHTML();
        onSave({
          contentJson: json,
          plainText: text,
          markdown: html,
        });
        setSaveState('saved');
        setTimeout(() => setSaveState('idle'), 2500);
      }, 1400);
    },
  });

  // Re-sync editor content if incoming props change externally
  useEffect(() => {
    if (!editor) return;
    const currentJson = JSON.stringify(editor.getJSON());
    if (contentJson && contentJson.trim()) {
      try {
        const parsed = JSON.parse(contentJson);
        if (JSON.stringify(parsed) !== currentJson) {
          editor.commands.setContent(parsed);
          lastSavedJsonRef.current = contentJson;
        }
        return;
      } catch {
        // fallback
      }
    }
    if (content && content !== editor.getText() && !editor.isFocused) {
      editor.commands.setContent(markdownToHtml(content));
    }
  }, [content, contentJson, editor]);

  if (!editor) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading rich document editor...</div>;
  }

  const applyAcademicTemplate = () => {
    editor.chain().focus().selectAll().setTextAlign('justify').run();
  };

  const insertCitation = (ref: { id: string; citationKey: string; label: string; title: string }) => {
    editor
      .chain()
      .focus()
      .insertContent({
        type: 'citation',
        attrs: {
          referenceId: ref.id,
          citationKey: ref.citationKey,
          label: ref.label,
        },
      })
      .insertContent(' ')
      .run();
  };

  const wordCount = editor.getText().trim() ? editor.getText().trim().split(/\s+/).length : 0;
  const charCount = editor.getText().length;

  return (
    <div
      className="report-rich-editor"
      style={{
        display: 'flex',
        flexDirection: 'column',
        border: '1px solid var(--border)',
        borderRadius: '8px',
        overflow: 'hidden',
        background: 'var(--surface, #ffffff)',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
      }}
    >
      {/* Top Word-Style Ribbon Toolbar */}
      {!readOnly && (
        <div
          className="editor-ribbon"
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: '0.35rem',
            padding: '0.5rem 0.75rem',
            background: 'var(--surface-hover, #f8fafc)',
            borderBottom: '1px solid var(--border)',
            userSelect: 'none',
          }}
        >
          {/* History */}
          <div style={{ display: 'flex', gap: '2px', paddingRight: '0.4rem', borderRight: '1px solid var(--border)' }}>
            <button
              type="button"
              title="Undo"
              onClick={() => editor.chain().focus().undo().run()}
              disabled={!editor.can().undo()}
              className="toolbar-btn"
              style={toolBtnStyle}
            >
              <Undo size={15} />
            </button>
            <button
              type="button"
              title="Redo"
              onClick={() => editor.chain().focus().redo().run()}
              disabled={!editor.can().redo()}
              className="toolbar-btn"
              style={toolBtnStyle}
            >
              <Redo size={15} />
            </button>
          </div>

          {/* Heading / Paragraph Selector */}
          <select
            style={{
              padding: '3px 8px',
              fontSize: '0.85rem',
              borderRadius: '4px',
              border: '1px solid var(--border)',
              background: 'var(--surface, #fff)',
              cursor: 'pointer',
            }}
            value={
              editor.isActive('heading', { level: 1 })
                ? 'h1'
                : editor.isActive('heading', { level: 2 })
                ? 'h2'
                : editor.isActive('heading', { level: 3 })
                ? 'h3'
                : 'p'
            }
            onChange={(e) => {
              const val = e.target.value;
              if (val === 'h1') editor.chain().focus().toggleHeading({ level: 1 }).run();
              else if (val === 'h2') editor.chain().focus().toggleHeading({ level: 2 }).run();
              else if (val === 'h3') editor.chain().focus().toggleHeading({ level: 3 }).run();
              else editor.chain().focus().setParagraph().run();
            }}
          >
            <option value="p">Normal text</option>
            <option value="h1">Heading 1 (Chapter)</option>
            <option value="h2">Heading 2 (Section)</option>
            <option value="h3">Heading 3 (Sub-section)</option>
          </select>

          {/* Character Styling */}
          <div style={{ display: 'flex', gap: '2px', padding: '0 0.4rem', borderRight: '1px solid var(--border)' }}>
            <button
              type="button"
              title="Bold"
              onClick={() => editor.chain().focus().toggleBold().run()}
              style={{ ...toolBtnStyle, fontWeight: 'bold', background: editor.isActive('bold') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('bold') ? '#2563eb' : 'inherit' }}
            >
              <Bold size={15} />
            </button>
            <button
              type="button"
              title="Italic"
              onClick={() => editor.chain().focus().toggleItalic().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('italic') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('italic') ? '#2563eb' : 'inherit' }}
            >
              <Italic size={15} />
            </button>
            <button
              type="button"
              title="Underline"
              onClick={() => editor.chain().focus().toggleUnderline().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('underline') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('underline') ? '#2563eb' : 'inherit' }}
            >
              <UnderlineIcon size={15} />
            </button>
            <button
              type="button"
              title="Strikethrough"
              onClick={() => editor.chain().focus().toggleStrike().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('strike') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('strike') ? '#2563eb' : 'inherit' }}
            >
              <Strikethrough size={15} />
            </button>
            <button
              type="button"
              title="Superscript"
              onClick={() => editor.chain().focus().toggleSuperscript().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('superscript') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('superscript') ? '#2563eb' : 'inherit' }}
            >
              <SuperscriptIcon size={14} />
            </button>
            <button
              type="button"
              title="Subscript"
              onClick={() => editor.chain().focus().toggleSubscript().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('subscript') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('subscript') ? '#2563eb' : 'inherit' }}
            >
              <SubscriptIcon size={14} />
            </button>
          </div>

          {/* Alignment */}
          <div style={{ display: 'flex', gap: '2px', padding: '0 0.4rem', borderRight: '1px solid var(--border)' }}>
            <button
              type="button"
              title="Align Left"
              onClick={() => editor.chain().focus().setTextAlign('left').run()}
              style={{ ...toolBtnStyle, background: editor.isActive({ textAlign: 'left' }) ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive({ textAlign: 'left' }) ? '#2563eb' : 'inherit' }}
            >
              <AlignLeft size={15} />
            </button>
            <button
              type="button"
              title="Align Center"
              onClick={() => editor.chain().focus().setTextAlign('center').run()}
              style={{ ...toolBtnStyle, background: editor.isActive({ textAlign: 'center' }) ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive({ textAlign: 'center' }) ? '#2563eb' : 'inherit' }}
            >
              <AlignCenter size={15} />
            </button>
            <button
              type="button"
              title="Align Right"
              onClick={() => editor.chain().focus().setTextAlign('right').run()}
              style={{ ...toolBtnStyle, background: editor.isActive({ textAlign: 'right' }) ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive({ textAlign: 'right' }) ? '#2563eb' : 'inherit' }}
            >
              <AlignRight size={15} />
            </button>
            <button
              type="button"
              title="Justify (Academic)"
              onClick={() => editor.chain().focus().setTextAlign('justify').run()}
              style={{ ...toolBtnStyle, background: editor.isActive({ textAlign: 'justify' }) ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive({ textAlign: 'justify' }) ? '#2563eb' : 'inherit' }}
            >
              <AlignJustify size={15} />
            </button>
          </div>

          {/* Lists & Quotes */}
          <div style={{ display: 'flex', gap: '2px', padding: '0 0.4rem', borderRight: '1px solid var(--border)' }}>
            <button
              type="button"
              title="Bullet List"
              onClick={() => editor.chain().focus().toggleBulletList().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('bulletList') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('bulletList') ? '#2563eb' : 'inherit' }}
            >
              <List size={15} />
            </button>
            <button
              type="button"
              title="Numbered List"
              onClick={() => editor.chain().focus().toggleOrderedList().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('orderedList') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('orderedList') ? '#2563eb' : 'inherit' }}
            >
              <ListOrdered size={15} />
            </button>
            <button
              type="button"
              title="Blockquote"
              onClick={() => editor.chain().focus().toggleBlockquote().run()}
              style={{ ...toolBtnStyle, background: editor.isActive('blockquote') ? 'rgba(37, 99, 235, 0.15)' : 'transparent', color: editor.isActive('blockquote') ? '#2563eb' : 'inherit' }}
            >
              <Quote size={15} />
            </button>
          </div>

          {/* Table Tools */}
          <div style={{ display: 'flex', gap: '2px', padding: '0 0.4rem', borderRight: '1px solid var(--border)' }}>
            <button
              type="button"
              title="Insert Table (3x3)"
              onClick={() => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()}
              style={toolBtnStyle}
            >
              <TableIcon size={15} />
            </button>
            {editor.isActive('table') && (
              <>
                <button
                  type="button"
                  title="Add Row Below"
                  onClick={() => editor.chain().focus().addRowAfter().run()}
                  style={toolBtnStyle}
                >
                  <Plus size={13} />R
                </button>
                <button
                  type="button"
                  title="Add Column Right"
                  onClick={() => editor.chain().focus().addColumnAfter().run()}
                  style={toolBtnStyle}
                >
                  <Plus size={13} />C
                </button>
                <button
                  type="button"
                  title="Delete Table"
                  onClick={() => editor.chain().focus().deleteTable().run()}
                  style={{ ...toolBtnStyle, color: '#ef4444' }}
                >
                  <Trash2 size={13} />
                </button>
              </>
            )}
          </div>

          {/* Actions: Insert Citation & Academic Template */}
          <div style={{ display: 'flex', gap: '6px', marginLeft: 'auto', alignItems: 'center' }}>
            {projectId && (
              <Button
                type="button"
                variant="secondary"
                onClick={() => setCitationModalOpen(true)}
                style={{
                  fontSize: '0.82rem',
                  padding: '3px 8px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <Bookmark size={13} /> Insert Citation
              </Button>
            )}
            <Button
              type="button"
              variant="secondary"
              onClick={applyAcademicTemplate}
              title="Apply standard institutional formatting: Times New Roman, 12pt, justified alignment, 1.5 line spacing"
              style={{
                fontSize: '0.82rem',
                padding: '3px 8px',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
              }}
            >
              <Wand2 size={13} /> Template Style
            </Button>
          </div>
        </div>
      )}

      {/* A4 Paper Document Canvas */}
      <div
        className="editor-canvas-container"
        style={{
          background: 'var(--surface-bg, #f1f5f9)',
          padding: '1.5rem',
          display: 'flex',
          justifyContent: 'center',
          overflowY: 'auto',
          minHeight,
        }}
      >
        <div
          className="a4-document-page"
          style={{
            width: '100%',
            maxWidth: '820px',
            minHeight: '400px',
            background: '#ffffff',
            color: '#1e293b',
            boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)',
            borderRadius: '2px',
            padding: '2.5rem 3rem',
            fontFamily: '"Times New Roman", Times, Georgia, serif',
            fontSize: '12pt',
            lineHeight: 1.65,
          }}
        >
          <EditorContent editor={editor} />
        </div>
      </div>

      {/* Editor Status Footer */}
      <div
        className="editor-footer"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '0.4rem 0.75rem',
          background: 'var(--surface-hover, #f8fafc)',
          borderTop: '1px solid var(--border)',
          fontSize: '0.8rem',
          color: 'var(--color-muted, #64748b)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span>{wordCount} words</span>
          <span>{charCount} characters</span>
          <span>Format: Academic A4 / Times New Roman</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          {saveState === 'saving' && (
            <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#f59e0b' }}>
              <Clock size={13} /> Autosaving...
            </span>
          )}
          {saveState === 'saved' && (
            <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#10b981' }}>
              <CheckCircle2 size={13} /> All changes saved
            </span>
          )}
        </div>
      </div>

      {/* Citation Insertion Modal */}
      {projectId && (
        <InsertCitationModal
          open={citationModalOpen}
          onClose={() => setCitationModalOpen(false)}
          projectId={projectId}
          citationStyle={citationStyle}
          onSelectReference={insertCitation}
        />
      )}
    </div>
  );
}

const toolBtnStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '4px 6px',
  borderRadius: '4px',
  border: 'none',
  background: 'transparent',
  color: 'inherit',
  cursor: 'pointer',
  fontSize: '0.82rem',
  transition: 'background 0.15s ease',
};
