import { AlertTriangle, FileQuestion, Loader2, Lock, RefreshCw } from 'lucide-react';
import { Button } from './ui';

export function PageLoading({ label = 'Loading' }: { label?: string }) {
  return (
    <main className="page" aria-busy="true">
      <div className="skeleton" />
      <p className="muted"><Loader2 size={16} aria-hidden /> {label}</p>
    </main>
  );
}

export function InlineLoading({ label = 'Loading' }: { label?: string }) {
  return <span className="muted"><Loader2 size={14} aria-hidden /> {label}</span>;
}

export function EmptyState({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
  children?: React.ReactNode;
}) {
  return (
    <div className="panel">
      <FileQuestion aria-hidden />
      <h2>{title}</h2>
      {description ? <p className="muted">{description}</p> : null}
      {action ? <div className="toolbar">{action}</div> : null}
      {children}
    </div>
  );
}

export function ErrorState({ title = 'Unable to load this area', error, onRetry }: { title?: string; error?: unknown; onRetry?: () => void }) {
  const message = error instanceof Error ? error.message : 'Please try again.';
  return (
    <div className="alert danger" role="alert">
      <AlertTriangle aria-hidden />
      <strong>{title}</strong>
      <p>{message}</p>
      {onRetry ? <Button type="button" variant="secondary" onClick={onRetry}><RefreshCw size={16} /> Retry</Button> : null}
    </div>
  );
}

export function PermissionDenied() {
  return (
    <main className="page">
      <div className="alert warning" role="alert">
        <Lock aria-hidden />
        <strong>Permission denied</strong>
        <p>This area is unavailable for your current role. Backend authorization remains authoritative.</p>
      </div>
    </main>
  );
}

export function NotFound() {
  return (
    <main className="page">
      <EmptyState title="Page not found" description="The route or item you requested does not exist." />
    </main>
  );
}
