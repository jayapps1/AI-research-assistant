import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { Button, Field, Input } from '../components/ui';

const authSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  fullName: z.string().optional(),
  totpCode: z.string().optional(),
});

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<z.infer<typeof authSchema>>({ resolver: zodResolver(authSchema), defaultValues: { email: '', password: '' } });
  if (auth.isAuthenticated) return <Navigate to="/" replace />;

  return (
    <AuthLayout title="Sign in to your research workspace">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await auth.login(values);
          const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/';
          navigate(destination, { replace: true });
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Unable to sign in.');
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <Field label="Email" error={form.formState.errors.email?.message}><Input autoComplete="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="current-password" {...form.register('password')} /></Field>
        <Field label="TOTP code, if required"><Input inputMode="numeric" autoComplete="one-time-code" {...form.register('totpCode')} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Sign in</Button>
        <p className="muted"><Link to="/forgot-password">Forgot password?</Link> · <Link to="/register">Create account</Link></p>
      </form>
    </AuthLayout>
  );
}

export function RegisterPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<z.infer<typeof authSchema>>({ resolver: zodResolver(authSchema), defaultValues: { email: '', password: '', fullName: '' } });
  return (
    <AuthLayout title="Create a secure research account">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await auth.register(values);
          navigate('/');
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Unable to register.');
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <Field label="Full name"><Input autoComplete="name" {...form.register('fullName')} /></Field>
        <Field label="Email" error={form.formState.errors.email?.message}><Input autoComplete="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="new-password" {...form.register('password')} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Create account</Button>
        <p className="muted"><Link to="/login">Already have an account?</Link></p>
      </form>
    </AuthLayout>
  );
}

export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const form = useForm<{ email: string }>({ defaultValues: { email: '' } });
  return (
    <AuthLayout title="Recover account access">
      <form className="form" onSubmit={form.handleSubmit(async ({ email }) => {
        await authApi.forgotPassword(email);
        setSent(true);
      })}>
        {sent ? <div className="alert info">If recovery is available for this account, instructions have been sent.</div> : null}
        <Field label="Account email"><Input type="email" {...form.register('email', { required: true })} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Continue recovery</Button>
        <p className="muted"><Link to="/login">Back to sign in</Link></p>
      </form>
    </AuthLayout>
  );
}

function AuthLayout({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <main className="auth-layout">
      <section className="auth-panel">
        <div className="auth-card">
          <h1>{title}</h1>
          {children}
        </div>
      </section>
      <section className="auth-context" aria-label="Product context">
        <h1>Research operations, evidence, AI, billing, and governance in one workspace.</h1>
        <p>All AI, billing, exports, notifications, and permissions are mediated by the Spring Boot backend.</p>
      </section>
    </main>
  );
}
