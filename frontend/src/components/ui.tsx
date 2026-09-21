import { Slot } from '@radix-ui/react-slot';
import { clsx } from 'clsx';
import {
  useEffect,
  useRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

function getFocusableElements(container: HTMLElement) {
  return Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter((element) => {
    if (element.getAttribute('aria-hidden') === 'true') return false;
    return element.offsetParent !== null || element.getClientRects().length > 0;
  });
}

export function Button({
  variant = 'primary',
  className,
  asChild,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger'; asChild?: boolean }) {
  const Comp = asChild ? Slot : 'button';
  return <Comp className={clsx('button', variant, className)} {...props} />;
}

export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="field">
      <span className="label">{label}</span>
      {children}
      {error ? <span className="badge danger">{error}</span> : null}
    </label>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className="input" {...props} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className="select" {...props} />;
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className="textarea" {...props} />;
}

export function Badge({ children, tone, className, style }: { children: ReactNode; tone?: 'success' | 'warning' | 'danger' | 'info'; className?: string; style?: import('react').CSSProperties }) {
  return <span className={clsx('badge', tone, className)} style={style}>{children}</span>;
}

export function Card({
  children,
  className,
  style,
  ...props
}: {
  children: ReactNode;
  className?: string;
  style?: import('react').CSSProperties;
} & import('react').HTMLAttributes<HTMLElement>) {
  return (
    <section className={clsx('card', className)} style={style} {...props}>
      {children}
    </section>
  );
}

export function Breadcrumbs({ items }: { items: string[] }) {
  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      {items.map((item, index) => (
        <span key={`${item}-${index}`}>{index > 0 ? '> ' : ''}{item}</span>
      ))}
    </nav>
  );
}

export function LoadingButton({
  loading,
  loadingLabel = 'Working...',
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger';
  loading?: boolean;
  loadingLabel?: string;
}) {
  return <Button {...props} disabled={loading || props.disabled}>{loading ? loadingLabel : children}</Button>;
}

export function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="toolbar" aria-label="Pagination">
      <Button type="button" variant="secondary" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Previous</Button>
      <span className="muted">Page {page + 1} of {Math.max(totalPages, 1)}</span>
      <Button type="button" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Next</Button>
    </div>
  );
}

export function ConfirmDialog({
  title,
  description,
  confirmLabel = 'Confirm',
  onConfirm,
}: {
  title: string;
  description: string;
  confirmLabel?: string;
  onConfirm: () => void;
}) {
  return (
    <div className="panel" role="group" aria-label={title}>
      <strong>{title}</strong>
      <p className="muted">{description}</p>
      <Button type="button" variant="danger" onClick={onConfirm}>{confirmLabel}</Button>
    </div>
  );
}

export function Drawer({ title, open, onClose, children }: { title: string; open: boolean; onClose: () => void; children: ReactNode }) {
  if (!open) return null;
  return (
    <div className="drawer-backdrop" role="presentation" onMouseDown={onClose}>
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}>
        <div className="page-header">
          <h2>{title}</h2>
          <Button type="button" variant="secondary" onClick={onClose}>Close</Button>
        </div>
        {children}
      </aside>
    </div>
  );
}

export function Modal({
  title,
  open,
  onClose,
  children,
  footer,
  className,
  bodyClassName,
  closeDisabled = false,
}: {
  title: string;
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  className?: string;
  bodyClassName?: string;
  closeDisabled?: boolean;
}) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocusedElement = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) return undefined;

    const originalOverflow = document.body.style.overflow;
    const originalPaddingRight = document.body.style.paddingRight;
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;

    document.body.style.overflow = 'hidden';
    if (scrollbarWidth > 0) {
      document.body.style.paddingRight = `${scrollbarWidth}px`;
    }

    return () => {
      document.body.style.overflow = originalOverflow;
      document.body.style.paddingRight = originalPaddingRight;
    };
  }, [open]);

  useEffect(() => {
    if (!open) return undefined;

    previouslyFocusedElement.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const frameId = window.requestAnimationFrame(() => {
      const dialog = dialogRef.current;
      if (!dialog) return;

      const autofocusTarget = dialog.querySelector<HTMLElement>('[autofocus]');
      const firstFocusable = getFocusableElements(dialog)[0];
      (autofocusTarget ?? firstFocusable ?? dialog).focus();
    });

    return () => {
      window.cancelAnimationFrame(frameId);
      previouslyFocusedElement.current?.focus({ preventScroll: true });
      previouslyFocusedElement.current = null;
    };
  }, [open]);

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      if (!closeDisabled) {
        onClose();
      }
      return;
    }

    if (event.key !== 'Tab') return;

    const dialog = dialogRef.current;
    if (!dialog) return;

    const focusableElements = getFocusableElements(dialog);
    if (!focusableElements.length) {
      event.preventDefault();
      dialog.focus();
      return;
    }

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (event.shiftKey && document.activeElement === firstElement) {
      event.preventDefault();
      lastElement.focus();
    } else if (!event.shiftKey && document.activeElement === lastElement) {
      event.preventDefault();
      firstElement.focus();
    }
  };

  if (!open) return null;
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={closeDisabled ? undefined : onClose}>
      <div
        ref={dialogRef}
        className={clsx('modal-dialog', className)}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
        onKeyDown={handleKeyDown}
      >
        <div className="modal-header">
          <h3>{title}</h3>
          <button
            type="button"
            className="button secondary"
            onClick={onClose}
            aria-label="Close"
            disabled={closeDisabled}
            style={{ padding: '4px 10px', fontSize: '0.9rem', lineHeight: 1 }}
          >
            X
          </button>
        </div>
        <div className={clsx('modal-body', bodyClassName)}>{children}</div>
        {footer ? <div className="modal-footer">{footer}</div> : null}
      </div>
    </div>
  );
}
