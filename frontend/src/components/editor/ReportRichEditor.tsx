import { useEffect, useRef, useState, useCallback, type CSSProperties } from 'react';
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
import { Extension, Node, mergeAttributes } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

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
  Table as TableIcon,
  Plus,
  Trash2,
  Undo,
  Redo,
  Bookmark,
  Pilcrow,
  AlertCircle,
  CheckCircle2,
  Clock,
  Wand2,
  Image as ImageIcon,
  Search,
  Save,
  FilePlus2,
  Palette,
  Highlighter,
  ChevronLeft,
  ChevronRight,
  PanelLeft,
  Maximize2,
  Minimize2,
  IndentIncrease,
  IndentDecrease,
} from 'lucide-react';
import { InsertCitationModal } from './InsertCitationModal';
import { InsertFigureModal } from '../../features/evidence/InsertFigureModal';
import type { ProjectEvidenceItem } from '../../types/api';

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

export const FigureNode = Node.create({
  name: 'figure',
  group: 'block',
  selectable: true,
  draggable: true,
  atom: true,

  addAttributes() {
    return {
      figureId: { default: null },
      evidenceId: { default: null },
      src: { default: '' },
      alt: { default: '' },
      figureLabel: { default: 'Figure' },
      caption: { default: '' },
      alignment: { default: 'center' },
      width: { default: '100%' },
      structuredDefinition: { default: '' },
      definitionFormat: { default: '' },
    };
  },

  parseHTML() {
    return [
      {
        tag: 'figure[data-type="project-figure"]',
        getAttrs: (element: HTMLElement | string) => {
          if (typeof element === 'string') return {};
          const img = element.querySelector('img');
          const figcaption = element.querySelector('figcaption');
          return {
            figureId: element.getAttribute('data-figure-id') || element.getAttribute('data-evidence-id'),
            evidenceId: element.getAttribute('data-evidence-id'),
            src: img?.getAttribute('src') || element.getAttribute('data-src') || '',
            alt: img?.getAttribute('alt') || element.getAttribute('data-alt') || '',
            figureLabel: element.getAttribute('data-figure-label') || 'Figure',
            caption: figcaption?.textContent?.replace(/^Figure\s+[\d.]*:\s*/i, '') || element.getAttribute('data-caption') || '',
            alignment: element.getAttribute('data-alignment') || 'center',
            width: img?.style.width || element.getAttribute('data-width') || '100%',
            structuredDefinition: element.getAttribute('data-structured-definition') || '',
            definitionFormat: element.getAttribute('data-definition-format') || '',
          };
        },
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    const label = HTMLAttributes.figureLabel || 'Figure';
    const cap = HTMLAttributes.caption || '';
    const renderedCaption = cap ? `${label}: ${cap}` : label;

    return [
      'figure',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'project-figure',
        'data-figure-id': HTMLAttributes.figureId || HTMLAttributes.evidenceId,
        'data-evidence-id': HTMLAttributes.evidenceId,
        'data-src': HTMLAttributes.src,
        'data-alt': HTMLAttributes.alt,
        'data-figure-label': HTMLAttributes.figureLabel,
        'data-caption': HTMLAttributes.caption,
        'data-alignment': HTMLAttributes.alignment || 'center',
        'data-width': HTMLAttributes.width || '100%',
        'data-structured-definition': HTMLAttributes.structuredDefinition,
        'data-definition-format': HTMLAttributes.definitionFormat,
        class: 'report-figure-block',
        style: `display: flex; flex-direction: column; align-items: ${HTMLAttributes.alignment === 'left' ? 'flex-start' : HTMLAttributes.alignment === 'right' ? 'flex-end' : 'center'}; margin: 18px 0; text-align: center;`,
      }),
      ...(HTMLAttributes.src ? [[
        'img',
        {
          src: HTMLAttributes.src,
          alt: HTMLAttributes.alt || cap || 'Figure',
          style: `width: ${HTMLAttributes.width || '100%'}; max-width: 100%; max-height: 480px; object-fit: contain; border-radius: 6px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); border: 1px solid #cbd5e1;`,
        },
      ]] : [[
        'pre',
        {
          style: 'max-width: 100%; overflow: auto; padding: 12px; border-radius: 6px; border: 1px solid #cbd5e1; background: #f8fafc; color: #334155; font-size: 0.8rem; text-align: left;',
        },
        HTMLAttributes.structuredDefinition || 'Structured figure',
      ]]),
      [
        'figcaption',
        {
          style: 'margin-top: 8px; font-size: 0.88rem; font-weight: 600; color: #475569; font-style: italic;',
        },
        renderedCaption,
      ],
    ];
  },
});

export const FigureReferenceNode = Node.create({
  name: 'figureReference',
  group: 'inline',
  inline: true,
  selectable: true,
  atom: true,

  addAttributes() {
    return {
      figureId: { default: null },
      evidenceId: { default: null },
      label: { default: 'Figure' },
    };
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-type="figure-reference"]',
        getAttrs: (element: HTMLElement | string) => {
          if (typeof element === 'string') return {};
          return {
            figureId: element.getAttribute('data-figure-id') || element.getAttribute('data-evidence-id'),
            evidenceId: element.getAttribute('data-evidence-id'),
            label: element.textContent || 'Figure',
          };
        },
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'figure-reference',
        'data-figure-id': HTMLAttributes.figureId || HTMLAttributes.evidenceId,
        'data-evidence-id': HTMLAttributes.evidenceId,
        class: 'figure-reference-node',
        style: 'font-weight: 600; color: #334155;',
      }),
      HTMLAttributes.label || 'Figure',
    ];
  },
});

export const PageBreakNode = Node.create({
  name: 'pageBreak',
  group: 'block',
  atom: true,
  selectable: true,

  parseHTML() {
    return [
      { tag: 'div[data-type="page-break"]' },
      { tag: 'hr[data-type="page-break"]' },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'page-break',
        class: 'report-page-break',
        contenteditable: 'false',
      }),
      ['span', {}, 'Page Break'],
    ];
  },
});

export const SectionBreakNode = Node.create({
  name: 'sectionBreak',
  group: 'block',
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      sectionType: { default: 'nextPage' },
      numbering: { default: 'continue' },
      headerFooter: { default: 'continue' },
    };
  },

  parseHTML() {
    return [{ tag: 'div[data-type="section-break"]' }];
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        'data-type': 'section-break',
        'data-section-type': HTMLAttributes.sectionType,
        'data-numbering': HTMLAttributes.numbering,
        'data-header-footer': HTMLAttributes.headerFooter,
        class: 'report-section-break',
        contenteditable: 'false',
      }),
      ['span', {}, 'Section Break'],
    ];
  },
});

const AcademicFormattingExtension = Extension.create({
  name: 'academicFormatting',

  addGlobalAttributes() {
    return [
      {
        types: ['textStyle'],
        attributes: {
          fontFamily: {
            default: null,
            parseHTML: (element) => element.style.fontFamily?.replace(/['"]/g, '') || null,
            renderHTML: (attributes) => attributes.fontFamily ? { style: `font-family: ${attributes.fontFamily}` } : {},
          },
          fontSize: {
            default: null,
            parseHTML: (element) => element.style.fontSize || null,
            renderHTML: (attributes) => attributes.fontSize ? { style: `font-size: ${attributes.fontSize}` } : {},
          },
        },
      },
      {
        types: ['paragraph', 'heading', 'blockquote'],
        attributes: {
          lineHeight: {
            default: null,
            parseHTML: (element) => element.style.lineHeight || null,
            renderHTML: (attributes) => attributes.lineHeight ? { style: `line-height: ${attributes.lineHeight}` } : {},
          },
          spaceBefore: {
            default: null,
            parseHTML: (element) => element.style.marginTop || null,
            renderHTML: (attributes) => attributes.spaceBefore ? { style: `margin-top: ${attributes.spaceBefore}` } : {},
          },
          spaceAfter: {
            default: null,
            parseHTML: (element) => element.style.marginBottom || null,
            renderHTML: (attributes) => attributes.spaceAfter ? { style: `margin-bottom: ${attributes.spaceAfter}` } : {},
          },
          textIndent: {
            default: null,
            parseHTML: (element) => element.style.textIndent || null,
            renderHTML: (attributes) => attributes.textIndent ? { style: `text-indent: ${attributes.textIndent}` } : {},
          },
          marginLeft: {
            default: null,
            parseHTML: (element) => element.style.marginLeft || null,
            renderHTML: (attributes) => attributes.marginLeft ? { style: `margin-left: ${attributes.marginLeft}` } : {},
          },
        },
      },
      {
        types: ['orderedList'],
        attributes: {
          listStyleType: {
            default: null,
            parseHTML: (element) => element.style.listStyleType || null,
            renderHTML: (attributes) => attributes.listStyleType ? { style: `list-style-type: ${attributes.listStyleType}` } : {},
          },
        },
      },
    ];
  },
});

const paginationPluginKey = new PluginKey('reportPagination');

const PaginationDecorationExtension = Extension.create({
  name: 'paginationDecorations',

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: paginationPluginKey,
        state: {
          init: () => DecorationSet.empty,
          apply(transaction, decorationSet) {
            const meta = transaction.getMeta(paginationPluginKey);
            if (meta?.decorationSet) {
              return meta.decorationSet;
            }
            return decorationSet.map(transaction.mapping, transaction.doc);
          },
        },
        props: {
          decorations(state) {
            return paginationPluginKey.getState(state);
          },
        },
      }),
    ];
  },
});

const PAGE_WIDTH_PX = 794;
const PAGE_HEIGHT_PX = 1123;
const PAGE_GAP_PX = 34;
const PAGE_MARGIN_TOP_PX = 96;
const PAGE_MARGIN_RIGHT_PX = 96;
const PAGE_MARGIN_BOTTOM_PX = 96;
const PAGE_MARGIN_LEFT_PX = 120;

const ACADEMIC_FONTS = ['Times New Roman', 'Arial', 'Calibri', 'Georgia'];
const FONT_SIZES = [8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32];
const ZOOM_OPTIONS = [
  { label: '50%', value: '0.5' },
  { label: '75%', value: '0.75' },
  { label: '90%', value: '0.9' },
  { label: '100%', value: '1' },
  { label: '110%', value: '1.1' },
  { label: '125%', value: '1.25' },
  { label: '150%', value: '1.5' },
  { label: 'Fit Width', value: 'fit-width' },
  { label: 'Fit Page', value: 'fit-page' },
];

type SaveState = 'idle' | 'saving' | 'saved' | 'failed';
type OutlineItem = { id: string; text: string; level: number; pos: number };
type PageNumberMode = 'plain' | 'romanFrontMatter';

export type DocumentFormattingProfile = {
  pageSize?: 'A4';
  margins?: {
    top?: string;
    right?: string;
    bottom?: string;
    left?: string;
  };
  fontFamily?: string;
  bodyFontSize?: string;
  lineSpacing?: string;
  chapterBreak?: string;
  pageNumbering?: string;
  headerText?: string;
  footerText?: string;
};

const DEFAULT_FORMATTING_PROFILE: Required<DocumentFormattingProfile> = {
  pageSize: 'A4',
  margins: {
    top: '1in',
    right: '1in',
    bottom: '1in',
    left: '1.25in',
  },
  fontFamily: 'Times New Roman',
  bodyFontSize: '12pt',
  lineSpacing: '1.5',
  chapterBreak: 'AUTO',
  pageNumbering: 'FRONT_ROMAN_MAIN_ARABIC',
  headerText: '',
  footerText: '',
};

function lengthToPx(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const match = value.trim().match(/^([\d.]+)\s*(in|cm|mm|pt|px)?$/i);
  if (!match) return fallback;
  const amount = Number.parseFloat(match[1]);
  const unit = (match[2] || 'px').toLowerCase();
  if (!Number.isFinite(amount)) return fallback;
  if (unit === 'in') return amount * 96;
  if (unit === 'cm') return amount * 37.7952755906;
  if (unit === 'mm') return amount * 3.7795275591;
  if (unit === 'pt') return amount * 1.3333333333;
  return amount;
}

function normalizeFormattingProfile(profile?: DocumentFormattingProfile): Required<DocumentFormattingProfile> {
  return {
    ...DEFAULT_FORMATTING_PROFILE,
    ...profile,
    margins: {
      ...DEFAULT_FORMATTING_PROFILE.margins,
      ...(profile?.margins || {}),
    },
  };
}

function toRoman(value: number): string {
  const numerals: Array<[number, string]> = [
    [1000, 'm'],
    [900, 'cm'],
    [500, 'd'],
    [400, 'cd'],
    [100, 'c'],
    [90, 'xc'],
    [50, 'l'],
    [40, 'xl'],
    [10, 'x'],
    [9, 'ix'],
    [5, 'v'],
    [4, 'iv'],
    [1, 'i'],
  ];
  let remaining = Math.max(1, value);
  let result = '';
  for (const [amount, symbol] of numerals) {
    while (remaining >= amount) {
      result += symbol;
      remaining -= amount;
    }
  }
  return result;
}

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

    // Markdown Figure / Image: ![caption](url)
    const imgMatch = line.match(/^!\[(.*?)\]\((.*?)\)$/);
    if (imgMatch) {
      if (inList) { out.push(listType === 'ul' ? '</ul>' : '</ol>'); inList = false; listType = null; }
      const captionText = imgMatch[1];
      const srcUrl = imgMatch[2];
      out.push(
        `<figure data-type="project-figure" data-src="${srcUrl}" data-caption="${captionText}" class="report-figure-block" style="display: flex; flex-direction: column; align-items: center; margin: 18px 0; text-align: center;"><img src="${srcUrl}" alt="${captionText}" style="max-width: 100%; max-height: 480px; object-fit: contain; border-radius: 6px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); border: 1px solid #cbd5e1;" /><figcaption style="margin-top: 8px; font-size: 0.88rem; font-weight: 600; color: #475569; font-style: italic;">${captionText}</figcaption></figure>`
      );
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
  hydrationKey?: string;
  isLoading?: boolean;
  formattingProfile?: DocumentFormattingProfile;
  projectId?: string;
  sectionId?: string;
  citationStyle?: string;
  readOnly?: boolean;
  minHeight?: string | number;
  onSave?: (data: { contentJson: string; plainText: string; markdown: string }) => void | Promise<void>;
}

export function ReportRichEditor({
  content = '',
  contentJson = '',
  hydrationKey,
  isLoading = false,
  formattingProfile,
  projectId = '',
  sectionId = '',
  citationStyle = 'APA_7',
  readOnly = false,
  minHeight = '420px',
  onSave,
}: ReportRichEditorProps) {
  const [saveState, setSaveState] = useState<SaveState>('idle');
  const [citationModalOpen, setCitationModalOpen] = useState(false);
  const [figureModalOpen, setFigureModalOpen] = useState(false);
  const [findOpen, setFindOpen] = useState(false);
  const [findTerm, setFindTerm] = useState('');
  const [replaceTerm, setReplaceTerm] = useState('');
  const [caseSensitive, setCaseSensitive] = useState(false);
  const [templateProfileOpen, setTemplateProfileOpen] = useState(false);
  const [zoomMode, setZoomMode] = useState('1');
  const [zoomScale, setZoomScale] = useState(1);
  const [pageCount, setPageCount] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [mainBodyStartPage, setMainBodyStartPage] = useState(1);
  const [outline, setOutline] = useState<OutlineItem[]>([]);
  const [navigationOpen, setNavigationOpen] = useState(true);
  const [focusMode, setFocusMode] = useState(false);
  const canvasScrollRef = useRef<HTMLDivElement | null>(null);
  const saveTimeoutRef = useRef<any>(null);
  const lastSavedJsonRef = useRef<string>('');
  const hydratedContentKeyRef = useRef<string>('');
  const isHydratedRef = useRef(false);
  const hydratingRef = useRef(true);

  const profile = normalizeFormattingProfile(formattingProfile);
  const marginTopPx = lengthToPx(profile.margins.top, PAGE_MARGIN_TOP_PX);
  const marginRightPx = lengthToPx(profile.margins.right, PAGE_MARGIN_RIGHT_PX);
  const marginBottomPx = lengthToPx(profile.margins.bottom, PAGE_MARGIN_BOTTOM_PX);
  const marginLeftPx = lengthToPx(profile.margins.left, PAGE_MARGIN_LEFT_PX);
  const pageBodyHeightPx = PAGE_HEIGHT_PX - marginTopPx - marginBottomPx;
  const chapterBreaksEnabled = /new|page|chapter/i.test(profile.chapterBreak || '');
  const pageNumberMode: PageNumberMode = /roman/i.test(profile.pageNumbering || '') ? 'romanFrontMatter' : 'plain';
  const cssVars = {
    '--wp-page-width': `${PAGE_WIDTH_PX}px`,
    '--wp-page-height': `${PAGE_HEIGHT_PX}px`,
    '--wp-page-gap': `${PAGE_GAP_PX}px`,
    '--wp-margin-top': `${Math.round(marginTopPx)}px`,
    '--wp-margin-right': `${Math.round(marginRightPx)}px`,
    '--wp-margin-bottom': `${Math.round(marginBottomPx)}px`,
    '--wp-margin-left': `${Math.round(marginLeftPx)}px`,
    '--wp-font-family': profile.fontFamily,
    '--wp-body-size': profile.bodyFontSize,
    '--wp-line-spacing': profile.lineSpacing,
  } as CSSProperties;

  const incomingContentKey = `${hydrationKey ?? ''}|${contentJson ? contentJson.length : 0}|${content ? content.length : 0}`;

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

  const saveSnapshot = useCallback((activeEditor: any, force = false) => {
    if (readOnly || !onSave || !activeEditor || !isHydratedRef.current || hydratingRef.current) return;
    const json = JSON.stringify(activeEditor.getJSON());
    if (!force && json === lastSavedJsonRef.current) {
      setSaveState('saved');
      return;
    }
    setSaveState('saving');
    const text = activeEditor.getText();
    const html = activeEditor.getHTML();
    Promise.resolve(onSave({
      contentJson: json,
      plainText: text,
      markdown: html,
    }))
      .then(() => {
        lastSavedJsonRef.current = json;
        setSaveState('saved');
        setTimeout(() => setSaveState('idle'), 2500);
      })
      .catch(() => {
        setSaveState('failed');
      });
  }, [onSave, readOnly]);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        link: false,
        underline: false,
        heading: {
          levels: [1, 2, 3, 4],
        },
      }),
      Underline,
      Subscript,
      Superscript,
      TextStyle,
      AcademicFormattingExtension,
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
      FigureNode,
      FigureReferenceNode,
      PageBreakNode,
      SectionBreakNode,
      PaginationDecorationExtension,
    ],
    content: initialParsedContent(),
    editable: !readOnly,
    onCreate: ({ editor }) => {
      lastSavedJsonRef.current = JSON.stringify(editor.getJSON());
      const hasInitialContent = Boolean((contentJson && contentJson.trim()) || (content && content.trim()));
      hydratedContentKeyRef.current = hasInitialContent ? incomingContentKey : '';
      isHydratedRef.current = hasInitialContent || readOnly;
      hydratingRef.current = !hasInitialContent && !readOnly;
    },
    onUpdate: ({ editor }) => {
      if (readOnly || !onSave) return;
      if (hydratingRef.current) return;
      if (!isHydratedRef.current) return;
      setSaveState('saving');
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);

      saveTimeoutRef.current = setTimeout(() => {
        saveSnapshot(editor);
      }, 1400);
    },
  });

  // Re-sync editor content if incoming props change externally or arrive asynchronously.
  useEffect(() => {
    if (!editor) return;
    if (isLoading) return;
    if (hydratedContentKeyRef.current === incomingContentKey) return;
    const currentJson = JSON.stringify(editor.getJSON());
    const hasUnsavedUserEdits = !readOnly && currentJson !== lastSavedJsonRef.current;
    if (hasUnsavedUserEdits && editor.isFocused) return;

    hydratingRef.current = true;
    if (contentJson && contentJson.trim()) {
      try {
        const parsed = JSON.parse(contentJson);
        if (JSON.stringify(parsed) !== currentJson) {
          editor.commands.setContent(parsed, { emitUpdate: false });
        }
        lastSavedJsonRef.current = JSON.stringify(editor.getJSON());
        hydratedContentKeyRef.current = incomingContentKey;
        isHydratedRef.current = true;
        hydratingRef.current = false;
        return;
      } catch {
        // fallback
      }
    }
    if (content && content !== editor.getText()) {
      editor.commands.setContent(markdownToHtml(content), { emitUpdate: false });
      lastSavedJsonRef.current = JSON.stringify(editor.getJSON());
    }
    hydratedContentKeyRef.current = incomingContentKey;
    isHydratedRef.current = true;
    hydratingRef.current = false;
  }, [content, contentJson, editor, incomingContentKey, isLoading, readOnly]);

  useEffect(() => () => {
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }
  }, []);

  useEffect(() => {
    if (!editor) return;
    const updateOutline = () => {
      const items: OutlineItem[] = [];
      editor.state.doc.descendants((node, pos) => {
        if (node.type.name === 'heading') {
          const text = node.textContent.trim() || 'Untitled heading';
          const level = Number(node.attrs.level || 1);
          items.push({
            id: `heading-${pos}`,
            text,
            level,
            pos,
          });
        }
      });
      setOutline(items);
    };
    updateOutline();
    editor.on('update', updateOutline);
    return () => {
      editor.off('update', updateOutline);
    };
  }, [editor]);

  useEffect(() => {
    if (!editor) return;
    let animationFrame = 0;

    const updatePagination = () => {
      const dom = editor.view.dom as HTMLElement;
      const topLevelNodes = Array.from(dom.children).filter((child) => {
        const element = child as HTMLElement;
        return !element.classList.contains('pagination-soft-break');
      }) as HTMLElement[];

      let usedHeight = 0;
      let calculatedPage = 1;
      let calculatedMainBodyStartPage = 1;
      const decorations: Decoration[] = [];

      editor.state.doc.forEach((node, offset, index) => {
        const element = topLevelNodes[index];
        if (!element) return;
        const style = window.getComputedStyle(element);
        const marginTop = Number.parseFloat(style.marginTop || '0') || 0;
        const marginBottom = Number.parseFloat(style.marginBottom || '0') || 0;
        const nodeHeight = element.getBoundingClientRect().height / zoomScale + marginTop + marginBottom;
        const isChapterHeading =
          node.type.name === 'heading' &&
          Number(node.attrs.level || 0) === 1 &&
          /^(chapter|ch\.?\s+\d+|chapter\s+(one|two|three|four|five|six|seven|eight|nine|ten))/i.test(node.textContent.trim());

        if (isChapterHeading && calculatedMainBodyStartPage === 1) {
          calculatedMainBodyStartPage = calculatedPage;
        }

        if (isChapterHeading && chapterBreaksEnabled && usedHeight > 0) {
          const fillHeight = Math.max(
            PAGE_GAP_PX + marginTopPx + marginBottomPx,
            pageBodyHeightPx - usedHeight + PAGE_GAP_PX + marginTopPx + marginBottomPx,
          );
          calculatedPage += 1;
          calculatedMainBodyStartPage = Math.max(calculatedMainBodyStartPage, calculatedPage);
          const pageNumber = calculatedPage;
          decorations.push(
            Decoration.widget(
              offset,
              () => {
                const separator = document.createElement('div');
                separator.className = 'pagination-soft-break chapter-soft-break';
                separator.contentEditable = 'false';
                separator.setAttribute('aria-hidden', 'true');
                separator.style.height = `${Math.round(fillHeight)}px`;
                separator.dataset.page = String(pageNumber);
                return separator;
              },
              { side: -1 },
            ),
          );
          usedHeight = 0;
        }

        if (node.type.name === 'pageBreak' || node.type.name === 'sectionBreak') {
          const fillHeight = Math.max(
            PAGE_GAP_PX + marginTopPx + marginBottomPx,
            pageBodyHeightPx - usedHeight + PAGE_GAP_PX + marginTopPx + marginBottomPx,
          );
          element.style.setProperty('--page-break-fill', `${Math.round(fillHeight)}px`);
          usedHeight = 0;
          calculatedPage += 1;
          return;
        }

        if (usedHeight > 0 && usedHeight + nodeHeight > pageBodyHeightPx) {
          const fillHeight = Math.max(
            PAGE_GAP_PX + marginTopPx + marginBottomPx,
            pageBodyHeightPx - usedHeight + PAGE_GAP_PX + marginTopPx + marginBottomPx,
          );
          calculatedPage += 1;
          const pageNumber = calculatedPage;
          decorations.push(
            Decoration.widget(
              offset,
              () => {
                const separator = document.createElement('div');
                separator.className = 'pagination-soft-break';
                separator.contentEditable = 'false';
                separator.setAttribute('aria-hidden', 'true');
                separator.style.height = `${Math.round(fillHeight)}px`;
                separator.dataset.page = String(pageNumber);
                return separator;
              },
              { side: -1 },
            ),
          );
          usedHeight = nodeHeight;
          return;
        }
        usedHeight += nodeHeight;
      });

      editor.view.dispatch(
        editor.state.tr.setMeta(paginationPluginKey, {
          decorationSet: DecorationSet.create(editor.state.doc, decorations),
        }),
      );
      setPageCount(Math.max(1, calculatedPage));
      setMainBodyStartPage(Math.max(1, calculatedMainBodyStartPage));
    };

    const schedule = () => {
      window.cancelAnimationFrame(animationFrame);
      animationFrame = window.requestAnimationFrame(updatePagination);
    };

    schedule();
    editor.on('update', schedule);
    window.addEventListener('resize', schedule);
    return () => {
      window.cancelAnimationFrame(animationFrame);
      editor.off('update', schedule);
      window.removeEventListener('resize', schedule);
    };
  }, [chapterBreaksEnabled, editor, marginBottomPx, marginTopPx, pageBodyHeightPx, zoomScale]);

  useEffect(() => {
    if (zoomMode === 'fit-width') {
      const availableWidth = Math.max(320, (canvasScrollRef.current?.clientWidth || PAGE_WIDTH_PX) - 48);
      setZoomScale(Math.min(1.5, Math.max(0.5, availableWidth / PAGE_WIDTH_PX)));
      return;
    }
    if (zoomMode === 'fit-page') {
      const availableWidth = Math.max(320, (canvasScrollRef.current?.clientWidth || PAGE_WIDTH_PX) - 48);
      const availableHeight = Math.max(480, (canvasScrollRef.current?.clientHeight || PAGE_HEIGHT_PX) - 48);
      setZoomScale(Math.min(1.5, Math.max(0.5, Math.min(availableWidth / PAGE_WIDTH_PX, availableHeight / PAGE_HEIGHT_PX))));
      return;
    }
    setZoomScale(Number.parseFloat(zoomMode) || 1);
  }, [zoomMode]);

  useEffect(() => {
    if (!editor) return;
    const onKeyDown = (event: KeyboardEvent) => {
      const isMod = event.ctrlKey || event.metaKey;
      if (!isMod) return;
      if (event.key.toLowerCase() === 's') {
        event.preventDefault();
        if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
        saveSnapshot(editor, true);
      }
      if (event.key.toLowerCase() === 'f') {
        event.preventDefault();
        setFindOpen(true);
      }
    };
    const dom = editor.view.dom;
    dom.addEventListener('keydown', onKeyDown);
    return () => dom.removeEventListener('keydown', onKeyDown);
  }, [editor, saveSnapshot]);

  if (!editor) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading rich document editor...</div>;
  }

  const applyAcademicTemplate = () => {
    editor.chain().focus().selectAll().setTextAlign('justify').setMark('textStyle', {
      fontFamily: profile.fontFamily,
      fontSize: profile.bodyFontSize,
    }).run();
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

  const insertFigure = (evidence: ProjectEvidenceItem) => {
    const src =
      evidence.downloadUrl ||
      (evidence.storageObjectId ? `/api/v1/storage-objects/${evidence.storageObjectId}/download` : '');
    const label = evidence.figureLabel || 'Figure';
    const cap = evidence.caption || '';
    editor
      .chain()
      .focus()
      .insertContent({
        type: 'figure',
        attrs: {
          figureId: evidence.id,
          evidenceId: evidence.id,
          src,
          alt: evidence.altText || cap || label,
          figureLabel: label,
          caption: cap,
          alignment: 'center',
        },
      })
      .insertContent('<p></p>')
      .run();
  };

  const setStyle = (style: string) => {
    const chain = editor.chain().focus();
    if (style === 'title') chain.setHeading({ level: 1 }).setTextAlign('center').run();
    else if (style === 'subtitle') chain.setHeading({ level: 2 }).setTextAlign('center').run();
    else if (style === 'h1') chain.setHeading({ level: 1 }).run();
    else if (style === 'h2') chain.setHeading({ level: 2 }).run();
    else if (style === 'h3') chain.setHeading({ level: 3 }).run();
    else if (style === 'quote') chain.toggleBlockquote().run();
    else chain.setParagraph().run();
  };

  const setFontFamily = (fontFamily: string) => {
    (editor.chain().focus() as any).setMark('textStyle', { fontFamily }).run();
  };

  const setFontSize = (size: string) => {
    (editor.chain().focus() as any).setMark('textStyle', { fontSize: `${size}pt` }).run();
  };

  const setLineSpacing = (lineHeight: string) => {
    editor.chain().focus().updateAttributes(editor.isActive('heading') ? 'heading' : 'paragraph', { lineHeight }).run();
  };

  const setParagraphSpacing = (spaceAfter: string) => {
    editor.chain().focus().updateAttributes(editor.isActive('heading') ? 'heading' : 'paragraph', { spaceAfter }).run();
  };

  const setParagraphSpacingBefore = (spaceBefore: string) => {
    editor.chain().focus().updateAttributes(editor.isActive('heading') ? 'heading' : 'paragraph', { spaceBefore }).run();
  };

  const adjustIndent = (direction: 1 | -1) => {
    const current = Number.parseFloat(String(editor.getAttributes('paragraph').marginLeft || editor.getAttributes('heading').marginLeft || '0')) || 0;
    const next = Math.max(0, current + direction * 24);
    editor.chain().focus().updateAttributes(editor.isActive('heading') ? 'heading' : 'paragraph', { marginLeft: next ? `${next}px` : null }).run();
  };

  const setFirstLineIndent = () => {
    editor.chain().focus().updateAttributes(editor.isActive('heading') ? 'heading' : 'paragraph', { textIndent: '0.5in' }).run();
  };

  const setOrderedListStyle = (listStyleType: string) => {
    const chain = editor.chain().focus();
    if (!editor.isActive('orderedList')) {
      chain.toggleOrderedList().run();
    }
    editor.chain().focus().updateAttributes('orderedList', { listStyleType }).run();
  };

  const insertPageBreak = () => {
    editor.chain().focus().insertContent([{ type: 'pageBreak' }, { type: 'paragraph' }]).run();
  };

  const insertSectionBreak = () => {
    editor.chain().focus().insertContent([
      { type: 'sectionBreak', attrs: { sectionType: 'nextPage', numbering: 'restart', headerFooter: 'new' } },
      { type: 'paragraph' },
    ]).run();
  };

  const setFigureAlignment = (alignment: 'left' | 'center' | 'right') => {
    editor.chain().focus().updateAttributes('figure', { alignment }).run();
  };

  const setFigureWidth = (width: string) => {
    editor.chain().focus().updateAttributes('figure', { width }).run();
  };

  const deleteSelectedFigure = () => {
    editor.chain().focus().deleteSelection().run();
  };

  const pageLabel = (page: number) => {
    if (pageNumberMode === 'romanFrontMatter' && mainBodyStartPage > 1 && page < mainBodyStartPage) {
      return toRoman(page);
    }
    if (pageNumberMode === 'romanFrontMatter' && mainBodyStartPage > 1) {
      return String(page - mainBodyStartPage + 1);
    }
    return String(page);
  };

  const scrollToPage = (page: number) => {
    const targetPage = Math.min(Math.max(page, 1), pageCount);
    setCurrentPage(targetPage);
    const scrollTop = (targetPage - 1) * (PAGE_HEIGHT_PX + PAGE_GAP_PX) * zoomScale;
    canvasScrollRef.current?.scrollTo({ top: scrollTop, behavior: 'smooth' });
  };

  const scrollToHeading = (pos: number) => {
    editor.chain().focus().setTextSelection(pos + 1).run();
    const coords = editor.view.coordsAtPos(pos + 1);
    const container = canvasScrollRef.current;
    if (!container) return;
    container.scrollTo({
      top: container.scrollTop + coords.top - container.getBoundingClientRect().top - 120,
      behavior: 'smooth',
    });
  };

  const matchesForFind = () => {
    if (!findTerm) return [] as Array<{ from: number; to: number }>;
    const matches: Array<{ from: number; to: number }> = [];
    const needle = caseSensitive ? findTerm : findTerm.toLowerCase();
    editor.state.doc.descendants((node, pos) => {
      if (!node.isText || !node.text) return;
      const haystack = caseSensitive ? node.text : node.text.toLowerCase();
      let index = haystack.indexOf(needle);
      while (index >= 0) {
        matches.push({ from: pos + index, to: pos + index + findTerm.length });
        index = haystack.indexOf(needle, index + Math.max(1, needle.length));
      }
    });
    return matches;
  };

  const findRelative = (direction: 1 | -1) => {
    const matches = matchesForFind();
    if (!matches.length) return;
    const currentFrom = editor.state.selection.from;
    const ordered = direction === 1 ? matches : [...matches].reverse();
    const match = ordered.find((item) => direction === 1 ? item.from > currentFrom : item.to < currentFrom) || ordered[0];
    editor.chain().focus().setTextSelection(match).run();
  };

  const replaceCurrent = () => {
    const { from, to } = editor.state.selection;
    if (from === to) {
      findRelative(1);
      return;
    }
    editor.chain().focus().deleteRange({ from, to }).insertContent(replaceTerm).run();
  };

  const replaceAll = () => {
    const matches = matchesForFind().reverse();
    if (!matches.length) return;
    let chain = editor.chain().focus();
    for (const match of matches) {
      chain = chain.deleteRange(match).insertContentAt(match.from, replaceTerm);
    }
    chain.run();
  };

  const wordCount = editor.getText().trim() ? editor.getText().trim().split(/\s+/).length : 0;
  const charCount = editor.getText().length;
  const pageLayerHeight = pageCount * PAGE_HEIGHT_PX + Math.max(0, pageCount - 1) * PAGE_GAP_PX;
  const activeStyle =
    editor.isActive('heading', { level: 1 })
      ? 'h1'
      : editor.isActive('heading', { level: 2 })
        ? 'h2'
        : editor.isActive('heading', { level: 3 })
          ? 'h3'
          : editor.isActive('blockquote')
            ? 'quote'
            : 'p';

  return (
    <div
      className={`report-rich-editor word-processor-shell ${focusMode ? 'focus-mode' : ''}`}
      style={{ minHeight, ...cssVars }}
    >
      {!readOnly && (
        <div className="editor-ribbon document-toolbar" role="toolbar" aria-label="Document formatting toolbar">
          <div className="toolbar-group">
            <button type="button" title="Undo" aria-label="Undo" onClick={() => editor.chain().focus().undo().run()} disabled={!editor.can().undo()} className="toolbar-btn">
              <Undo size={15} />
            </button>
            <button type="button" title="Redo" aria-label="Redo" onClick={() => editor.chain().focus().redo().run()} disabled={!editor.can().redo()} className="toolbar-btn">
              <Redo size={15} />
            </button>
            <button type="button" title="Save" aria-label="Save" onClick={() => saveSnapshot(editor, true)} className="toolbar-btn">
              <Save size={15} />
            </button>
          </div>

          <div className="toolbar-group toolbar-selects">
            <select aria-label="Academic style" value={activeStyle} onChange={(event) => setStyle(event.target.value)}>
              <option value="p">Normal Text</option>
              <option value="title">Document Title</option>
              <option value="subtitle">Subtitle</option>
              <option value="h1">Heading 1</option>
              <option value="h2">Heading 2</option>
              <option value="h3">Heading 3</option>
              <option value="quote">Block Quote</option>
            </select>
            <select aria-label="Font family" defaultValue="Times New Roman" onChange={(event) => setFontFamily(event.target.value)}>
              {ACADEMIC_FONTS.map((font) => <option key={font} value={font}>{font}</option>)}
            </select>
            <select aria-label="Font size" defaultValue="12" onChange={(event) => setFontSize(event.target.value)}>
              {FONT_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
            </select>
          </div>

          <div className="toolbar-group">
            <button type="button" title="Bold" aria-label="Bold" onClick={() => editor.chain().focus().toggleBold().run()} className={`toolbar-btn ${editor.isActive('bold') ? 'active' : ''}`}>
              <Bold size={15} />
            </button>
            <button type="button" title="Italic" aria-label="Italic" onClick={() => editor.chain().focus().toggleItalic().run()} className={`toolbar-btn ${editor.isActive('italic') ? 'active' : ''}`}>
              <Italic size={15} />
            </button>
            <button type="button" title="Underline" aria-label="Underline" onClick={() => editor.chain().focus().toggleUnderline().run()} className={`toolbar-btn ${editor.isActive('underline') ? 'active' : ''}`}>
              <UnderlineIcon size={15} />
            </button>
            <button type="button" title="Strikethrough" aria-label="Strikethrough" onClick={() => editor.chain().focus().toggleStrike().run()} className={`toolbar-btn ${editor.isActive('strike') ? 'active' : ''}`}>
              <Strikethrough size={15} />
            </button>
            <button type="button" title="Superscript" aria-label="Superscript" onClick={() => editor.chain().focus().toggleSuperscript().run()} className={`toolbar-btn ${editor.isActive('superscript') ? 'active' : ''}`}>
              <SuperscriptIcon size={14} />
            </button>
            <button type="button" title="Subscript" aria-label="Subscript" onClick={() => editor.chain().focus().toggleSubscript().run()} className={`toolbar-btn ${editor.isActive('subscript') ? 'active' : ''}`}>
              <SubscriptIcon size={14} />
            </button>
            <label className="toolbar-color" title="Text color" aria-label="Text color">
              <Palette size={14} />
              <input type="color" onChange={(event) => editor.chain().focus().setColor(event.target.value).run()} />
            </label>
            <label className="toolbar-color" title="Highlight" aria-label="Highlight">
              <Highlighter size={14} />
              <input type="color" defaultValue="#fff3a3" onChange={(event) => editor.chain().focus().toggleHighlight({ color: event.target.value }).run()} />
            </label>
          </div>

          <div className="toolbar-group">
            <button type="button" title="Align left" aria-label="Align left" onClick={() => editor.chain().focus().setTextAlign('left').run()} className={`toolbar-btn ${editor.isActive({ textAlign: 'left' }) ? 'active' : ''}`}>
              <AlignLeft size={15} />
            </button>
            <button type="button" title="Align center" aria-label="Align center" onClick={() => editor.chain().focus().setTextAlign('center').run()} className={`toolbar-btn ${editor.isActive({ textAlign: 'center' }) ? 'active' : ''}`}>
              <AlignCenter size={15} />
            </button>
            <button type="button" title="Align right" aria-label="Align right" onClick={() => editor.chain().focus().setTextAlign('right').run()} className={`toolbar-btn ${editor.isActive({ textAlign: 'right' }) ? 'active' : ''}`}>
              <AlignRight size={15} />
            </button>
            <button type="button" title="Justify" aria-label="Justify" onClick={() => editor.chain().focus().setTextAlign('justify').run()} className={`toolbar-btn ${editor.isActive({ textAlign: 'justify' }) ? 'active' : ''}`}>
              <AlignJustify size={15} />
            </button>
          </div>

          <div className="toolbar-group">
            <button type="button" title="Bullet list" aria-label="Bullet list" onClick={() => editor.chain().focus().toggleBulletList().run()} className={`toolbar-btn ${editor.isActive('bulletList') ? 'active' : ''}`}>
              <List size={15} />
            </button>
            <button type="button" title="Numbered list" aria-label="Numbered list" onClick={() => setOrderedListStyle('decimal')} className={`toolbar-btn ${editor.isActive('orderedList') ? 'active' : ''}`}>
              <ListOrdered size={15} />
            </button>
            <button type="button" title="Upper Roman list" aria-label="Upper Roman list" onClick={() => setOrderedListStyle('upper-roman')} className="toolbar-btn">I.</button>
            <button type="button" title="Lower Roman list" aria-label="Lower Roman list" onClick={() => setOrderedListStyle('lower-roman')} className="toolbar-btn">i.</button>
            <button type="button" title="Upper Alpha list" aria-label="Upper Alpha list" onClick={() => setOrderedListStyle('upper-alpha')} className="toolbar-btn">A.</button>
            <button type="button" title="Lower Alpha list" aria-label="Lower Alpha list" onClick={() => setOrderedListStyle('lower-alpha')} className="toolbar-btn">a.</button>
          </div>

          <div className="toolbar-group">
            <button type="button" title="Decrease indent" aria-label="Decrease indent" onClick={() => adjustIndent(-1)} className="toolbar-btn">
              <IndentDecrease size={15} />
            </button>
            <button type="button" title="Increase indent" aria-label="Increase indent" onClick={() => adjustIndent(1)} className="toolbar-btn">
              <IndentIncrease size={15} />
            </button>
            <button type="button" title="First-line indent" aria-label="First-line indent" onClick={setFirstLineIndent} className="toolbar-btn">
              <Pilcrow size={15} />
            </button>
          </div>

          <div className="toolbar-group toolbar-selects">
            <select aria-label="Line spacing" defaultValue="1.5" onChange={(event) => setLineSpacing(event.target.value)}>
              <option value="1">1.0</option>
              <option value="1.15">1.15</option>
              <option value="1.5">1.5</option>
              <option value="2">2.0</option>
            </select>
            <select aria-label="Paragraph spacing before" defaultValue="0pt" onChange={(event) => setParagraphSpacingBefore(event.target.value)}>
              <option value="0pt">Before 0pt</option>
              <option value="6pt">Before 6pt</option>
              <option value="12pt">Before 12pt</option>
              <option value="18pt">Before 18pt</option>
            </select>
            <select aria-label="Paragraph spacing after" defaultValue="6pt" onChange={(event) => setParagraphSpacing(event.target.value)}>
              <option value="0pt">After 0pt</option>
              <option value="6pt">After 6pt</option>
              <option value="12pt">After 12pt</option>
              <option value="18pt">After 18pt</option>
            </select>
          </div>

          <div className="toolbar-group">
            <button type="button" title="Insert table" aria-label="Insert table" onClick={() => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()} className="toolbar-btn">
              <TableIcon size={15} />
            </button>
            {editor.isActive('table') && (
              <>
                <button type="button" title="Add row" aria-label="Add row" onClick={() => editor.chain().focus().addRowAfter().run()} className="toolbar-btn"><Plus size={13} />R</button>
                <button type="button" title="Delete row" aria-label="Delete row" onClick={() => editor.chain().focus().deleteRow().run()} className="toolbar-btn"><Trash2 size={13} />R</button>
                <button type="button" title="Add column" aria-label="Add column" onClick={() => editor.chain().focus().addColumnAfter().run()} className="toolbar-btn"><Plus size={13} />C</button>
                <button type="button" title="Delete column" aria-label="Delete column" onClick={() => editor.chain().focus().deleteColumn().run()} className="toolbar-btn"><Trash2 size={13} />C</button>
                <button type="button" title="Merge cells" aria-label="Merge cells" onClick={() => editor.chain().focus().mergeCells().run()} className="toolbar-btn">Merge</button>
                <button type="button" title="Split cell" aria-label="Split cell" onClick={() => editor.chain().focus().splitCell().run()} className="toolbar-btn">Split</button>
                <button type="button" title="Delete table" aria-label="Delete table" onClick={() => editor.chain().focus().deleteTable().run()} className="toolbar-btn danger"><Trash2 size={13} /></button>
              </>
            )}
          </div>

          {editor.isActive('figure') && (
            <div className="toolbar-group toolbar-selects" aria-label="Figure tools">
              <button type="button" title="Align figure left" aria-label="Align figure left" onClick={() => setFigureAlignment('left')} className="toolbar-btn">
                <AlignLeft size={15} />
              </button>
              <button type="button" title="Center figure" aria-label="Center figure" onClick={() => setFigureAlignment('center')} className="toolbar-btn">
                <AlignCenter size={15} />
              </button>
              <button type="button" title="Align figure right" aria-label="Align figure right" onClick={() => setFigureAlignment('right')} className="toolbar-btn">
                <AlignRight size={15} />
              </button>
              <select aria-label="Figure width" value={String(editor.getAttributes('figure').width || '100%')} onChange={(event) => setFigureWidth(event.target.value)}>
                <option value="50%">50%</option>
                <option value="65%">65%</option>
                <option value="80%">80%</option>
                <option value="100%">100%</option>
              </select>
              <button type="button" title="Delete figure" aria-label="Delete figure" onClick={deleteSelectedFigure} className="toolbar-btn danger">
                <Trash2 size={15} />
              </button>
            </div>
          )}

          <div className="toolbar-group">
            {projectId && (
              <>
                <button type="button" title="Insert figure" aria-label="Insert figure" onClick={() => setFigureModalOpen(true)} className="toolbar-text-btn">
                  <ImageIcon size={14} /> Figure
                </button>
                <button type="button" title="Insert citation" aria-label="Insert citation" onClick={() => setCitationModalOpen(true)} className="toolbar-text-btn">
                  <Bookmark size={14} /> Citation
                </button>
              </>
            )}
            <button type="button" title="Insert page break" aria-label="Insert page break" onClick={insertPageBreak} className="toolbar-text-btn">
              <FilePlus2 size={14} /> Page Break
            </button>
            <button type="button" title="Insert section break" aria-label="Insert section break" onClick={insertSectionBreak} className="toolbar-text-btn">
              <FilePlus2 size={14} /> Section
            </button>
            <button type="button" title="Find and replace" aria-label="Find and replace" onClick={() => setFindOpen((open) => !open)} className="toolbar-btn">
              <Search size={15} />
            </button>
            <button type="button" title="Template Style" aria-label="Template Style" onClick={() => setTemplateProfileOpen((open) => !open)} className="toolbar-text-btn">
              <Wand2 size={14} /> Template Style
            </button>
          </div>

          <div className="toolbar-group toolbar-right">
            <select aria-label="Zoom" value={zoomMode} onChange={(event) => setZoomMode(event.target.value)}>
              {ZOOM_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
            <button type="button" title="Navigation pane" aria-label="Navigation pane" onClick={() => setNavigationOpen((open) => !open)} className="toolbar-btn">
              <PanelLeft size={15} />
            </button>
            <button type="button" title="Focus mode" aria-label="Focus mode" onClick={() => setFocusMode((enabled) => !enabled)} className="toolbar-btn">
              {focusMode ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            </button>
          </div>
        </div>
      )}

      {templateProfileOpen && (
        <div className="template-profile-panel" role="region" aria-label="Current document formatting profile">
          <span><strong>Page:</strong> {profile.pageSize}</span>
          <span><strong>Margins:</strong> T {profile.margins.top} / B {profile.margins.bottom} / L {profile.margins.left} / R {profile.margins.right}</span>
          <span><strong>Font:</strong> {profile.fontFamily}, {profile.bodyFontSize}</span>
          <span><strong>Line:</strong> {profile.lineSpacing}</span>
          <span><strong>Chapter breaks:</strong> {profile.chapterBreak}</span>
          <span><strong>Page numbering:</strong> {profile.pageNumbering}</span>
          <button type="button" onClick={applyAcademicTemplate}>Apply Allowed Overrides</button>
        </div>
      )}

      {findOpen && (
        <div className="find-replace-bar" role="search" aria-label="Find and replace">
          <input aria-label="Find" placeholder="Find" value={findTerm} onChange={(event) => setFindTerm(event.target.value)} />
          <input aria-label="Replace with" placeholder="Replace" value={replaceTerm} onChange={(event) => setReplaceTerm(event.target.value)} />
          <label>
            <input type="checkbox" checked={caseSensitive} onChange={(event) => setCaseSensitive(event.target.checked)} />
            Case
          </label>
          <button type="button" onClick={() => findRelative(-1)}>Previous</button>
          <button type="button" onClick={() => findRelative(1)}>Next</button>
          <button type="button" onClick={replaceCurrent}>Replace</button>
          <button type="button" onClick={replaceAll}>Replace All</button>
        </div>
      )}

      <div className="document-workspace">
        {navigationOpen && (
          <aside className="document-navigation-pane" aria-label="Document outline">
            <div className="nav-pane-header">Navigation</div>
            <div className="nav-pane-tabs">
              <button type="button" className="active">Headings</button>
              <button type="button" disabled>Pages</button>
            </div>
            <div className="outline-list">
              {outline.length === 0 ? (
                <span className="outline-empty">No headings yet</span>
              ) : outline.map((item) => (
                <button
                  type="button"
                  key={item.id}
                  className={`outline-item level-${Math.min(item.level, 4)}`}
                  onClick={() => scrollToHeading(item.pos)}
                >
                  {item.text}
                </button>
              ))}
            </div>
          </aside>
        )}

        <main
          ref={canvasScrollRef}
          className="editor-canvas-container word-processor-canvas"
          style={{ minHeight }}
          onScroll={(event) => {
            const top = event.currentTarget.scrollTop / Math.max(zoomScale, 0.1);
            setCurrentPage(Math.min(pageCount, Math.max(1, Math.floor(top / (PAGE_HEIGHT_PX + PAGE_GAP_PX)) + 1)));
          }}
        >
          <div className="document-ruler" aria-hidden="true">
            <div className="ruler-margin left" style={{ width: marginLeftPx }} />
            <div className="ruler-body">
              {Array.from({ length: 8 }).map((_, index) => <span key={index}>{index + 1}</span>)}
            </div>
            <div className="ruler-margin right" style={{ width: marginRightPx }} />
            <div className="ruler-indent first-line" />
            <div className="ruler-indent hanging" />
          </div>

          <div
            className="paginated-editor-scale"
            style={{
              width: PAGE_WIDTH_PX,
              height: pageLayerHeight,
              transform: `scale(${zoomScale})`,
            }}
          >
            <div className="document-pages-layer" style={{ height: pageLayerHeight }}>
              {Array.from({ length: pageCount }).map((_, index) => (
                <section
                  key={index}
                  className="document-page"
                  aria-hidden="true"
                  style={{
                    top: index * (PAGE_HEIGHT_PX + PAGE_GAP_PX),
                  }}
                >
                  <header className="page-header-area">{profile.headerText || (index === 0 ? '' : 'Academic Report')}</header>
                  <footer className="page-footer-area">{profile.footerText || pageLabel(index + 1)}</footer>
                </section>
              ))}
            </div>
            <div className="editable-page-layer" style={{ minHeight: pageLayerHeight }}>
              <EditorContent editor={editor} />
            </div>
          </div>
        </main>
      </div>

      <div className="editor-footer document-status-bar">
        <div>
          <button type="button" className="status-nav-button" aria-label="Previous page" onClick={() => scrollToPage(currentPage - 1)} disabled={currentPage <= 1}>
            <ChevronLeft size={14} />
          </button>
          <span>Page {pageLabel(currentPage)} of {pageCount}</span>
          <button type="button" className="status-nav-button" aria-label="Next page" onClick={() => scrollToPage(currentPage + 1)} disabled={currentPage >= pageCount}>
            <ChevronRight size={14} />
          </button>
          <span>Words: {wordCount.toLocaleString()}</span>
          <span>Characters: {charCount.toLocaleString()}</span>
          <span>Language: English</span>
        </div>
        <div>
          <span>A4 / {profile.fontFamily} / {Math.round(zoomScale * 100)}%</span>
          {saveState === 'saving' && <span className="save-state saving"><Clock size={13} /> Saving...</span>}
          {saveState === 'saved' && <span className="save-state saved"><CheckCircle2 size={13} /> Saved</span>}
          {saveState === 'failed' && <span className="save-state failed"><AlertCircle size={13} /> Save failed</span>}
          {saveState === 'idle' && <span className="save-state">Saved</span>}
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

      {/* Figure Insertion Modal */}
      {projectId && (
        <InsertFigureModal
          isOpen={figureModalOpen}
          onClose={() => setFigureModalOpen(false)}
          projectId={projectId}
          sectionId={sectionId}
          onInsert={insertFigure}
        />
      )}
    </div>
  );
}
