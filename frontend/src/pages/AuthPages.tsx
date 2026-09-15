import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { Button, Field, Input } from '../components/ui';
import { paths } from '../routes/paths';

const loginSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(1, 'Password is required').max(128),
});

const totpSchema = loginSchema.extend({
  totpCode: z.string().regex(/^\d{6}$/, 'Enter the 6-digit authenticator code'),
});

const registerSchema = z.object({
  firstName: z.string().max(100).optional(),
  lastName: z.string().max(100).optional(),
  email: z.string().email().max(255),
  password: z.string().min(8).max(128),
});

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });
  if (auth.isAuthenticated) return <Navigate to={paths.dashboard} replace />;

  return (
    <AuthLayout title="Sign in to your research workspace">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await auth.login(values);
          const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? paths.dashboard;
          navigate(destination, { replace: true });
        } catch (err) {
          const message = err instanceof Error ? err.message : 'Unable to sign in.';
          if (/totp|code|required/i.test(message)) {
            navigate('/auth/totp', { state: values });
            return;
          }
          setError(message);
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <Field label="Email" error={form.formState.errors.email?.message}><Input autoComplete="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="current-password" {...form.register('password')} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Sign in</Button>
        <p className="muted"><Link to="/forgot-password">Forgot password?</Link> | <Link to="/register">Create account</Link></p>
      </form>
    </AuthLayout>
  );
}

export function TotpPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const previous = location.state as { email?: string; password?: string } | null;
  const [error, setError] = useState<string | null>(null);
  const form = useForm<z.infer<typeof totpSchema>>({
    resolver: zodResolver(totpSchema),
    defaultValues: { email: previous?.email ?? '', password: previous?.password ?? '', totpCode: '' },
  });

  return (
    <AuthLayout title="Verify authenticator code">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await auth.login(values);
          navigate(paths.dashboard, { replace: true });
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Invalid or expired authenticator code.');
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <Field label="Email" error={form.formState.errors.email?.message}><Input autoComplete="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="current-password" {...form.register('password')} /></Field>
        <Field label="6-digit code" error={form.formState.errors.totpCode?.message}>
          <Input inputMode="numeric" autoComplete="one-time-code" maxLength={6} {...form.register('totpCode')} />
        </Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Verify and continue</Button>
      </form>
    </AuthLayout>
  );
}

export function RegisterPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<z.infer<typeof registerSchema>>({ resolver: zodResolver(registerSchema), defaultValues: { email: '', password: '', firstName: '', lastName: '' } });
  return (
    <AuthLayout title="Create a secure research account">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await auth.register({ ...values, locale: 'en' });
          navigate(paths.dashboard);
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Unable to register.');
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <div className="grid cols-2">
          <Field label="First name"><Input autoComplete="given-name" {...form.register('firstName')} /></Field>
          <Field label="Last name"><Input autoComplete="family-name" {...form.register('lastName')} /></Field>
        </div>
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
  const [error, setError] = useState<string | null>(null);
  const form = useForm<{ email: string }>({ defaultValues: { email: '' } });
  return (
    <AuthLayout title="Recover account access">
      <form className="form" onSubmit={form.handleSubmit(async ({ email }) => {
        setError(null);
        try {
          await authApi.forgotPassword(email);
          setSent(true);
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Unable to start password recovery.');
        }
      })}>
        {sent ? <div className="alert info">If recovery is available for this account, instructions have been sent.</div> : null}
        {error ? <div className="alert danger">{error}</div> : null}
        <Field label="Account email"><Input type="email" autoComplete="email" {...form.register('email', { required: true })} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Continue recovery</Button>
        <p className="muted"><Link to="/reset-password">I have a reset token</Link> | <Link to="/login">Back to sign in</Link></p>
      </form>
    </AuthLayout>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const form = useForm<{ resetToken: string; newPassword: string }>({ defaultValues: { resetToken: params.get('token') ?? '', newPassword: '' } });
  return (
    <AuthLayout title="Set a new password">
      <form className="form" onSubmit={form.handleSubmit(async (values) => {
        setError(null);
        try {
          await authApi.resetPassword(values);
          setDone(true);
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Unable to reset password.');
        }
      })}>
        {done ? <div className="alert info">Password reset complete. You can sign in with the new password.</div> : null}
        {error ? <div className="alert danger">{error}</div> : null}
        <Field label="Reset token"><Input {...form.register('resetToken', { required: true })} /></Field>
        <Field label="New password"><Input type="password" autoComplete="new-password" {...form.register('newPassword', { required: true, minLength: 8 })} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Reset password</Button>
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
