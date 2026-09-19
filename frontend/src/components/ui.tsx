import { Slot } from '@radix-ui/react-slot';
import { clsx } from 'clsx';
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';

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

export function LoadingButton({ loading, children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger'; loading?: boolean }) {
  return <Button {...props} disabled={loading || props.disabled}>{loading ? 'Working...' : children}</Button>;
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
}: {
  title: string;
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}) {
  if (!open) return null;
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <h3 style={{ margin: 0 }}>{title}</h3>
          <button
            type="button"
            className="button secondary"
            onClick={onClose}
            aria-label="Close"
            style={{ padding: '4px 10px', fontSize: '0.9rem', lineHeight: 1 }}
          >
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

