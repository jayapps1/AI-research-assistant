import { Slot } from '@radix-ui/react-slot';
import { clsx } from 'clsx';
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react';

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

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className="textarea" {...props} />;
}

export function Badge({ children, tone }: { children: ReactNode; tone?: 'success' | 'warning' | 'danger' | 'info' }) {
  return <span className={clsx('badge', tone)}>{children}</span>;
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <section className={clsx('card', className)}>{children}</section>;
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
