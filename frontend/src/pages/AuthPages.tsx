import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { authApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { TotpChallengeRequired } from '../auth/errors';
import { Button, Field, Input } from '../components/ui';
import { paths } from '../routes/paths';

const loginSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(1, 'Password is required').max(128),
});

const totpSchema = loginSchema.extend({
  totpCode: z.string().regex(/^\d{6}$/, 'Enter the 6-digit authenticator code'),
});

const totpOnlySchema = z.object({
  email: z.string().email().max(255),
  totpCode: z.string().regex(/^\d{6}$/, 'Enter the 6-digit authenticator code'),
});

const totpChallengeSchema = z.object({
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
  const [method, setMethod] = useState<'PASSWORD' | 'TOTP'>('PASSWORD');
  const [challenge, setChallenge] = useState<{ id: string; email: string; expiresIn: number } | null>(null);
  const form = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });
  const totpForm = useForm<z.infer<typeof totpOnlySchema>>({ resolver: zodResolver(totpOnlySchema), defaultValues: { email: '', totpCode: '' } });
  const challengeForm = useForm<z.infer<typeof totpChallengeSchema>>({ resolver: zodResolver(totpChallengeSchema), defaultValues: { totpCode: '' } });
  if (auth.isAuthenticated) return <Navigate to={paths.dashboard} replace />;

  const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? paths.dashboard;

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to continue your research.">
      {challenge ? (
        <form className="form" onSubmit={challengeForm.handleSubmit(async (values) => {
          setError(null);
          try {
            await auth.completeTotpChallenge({ challengeId: challenge.id, totpCode: values.totpCode });
            navigate(destination, { replace: true });
          } catch (err) {
            setError(authErrorMessage(err, 'Invalid or expired authenticator code.'));
          }
        })}>
          <div className="auth-step-note">
            <ShieldCheck size={18} aria-hidden />
            <span>Enter the authenticator code for {challenge.email}. The password has been accepted.</span>
          </div>
          {error ? <div className="alert danger" role="alert">{error}</div> : null}
          <Field label="Authenticator code" error={challengeForm.formState.errors.totpCode?.message}>
            <Input
              inputMode="numeric"
              pattern="[0-9]*"
              autoComplete="one-time-code"
              maxLength={6}
              {...challengeForm.register('totpCode', {
                onChange: (e) => {
                  e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
                },
              })}
            />
          </Field>
          <div className="auth-row">
            <Link to="/forgot-password">Lost authenticator? Use a recovery code</Link>
          </div>
          <Button type="submit" disabled={challengeForm.formState.isSubmitting}>
            {challengeForm.formState.isSubmitting ? 'Verifying...' : 'Verify & sign in'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => { setChallenge(null); setError(null); challengeForm.reset(); }}>
            Back to password
          </Button>
        </form>
      ) : (
        <>
          <div className="auth-method-tabs" role="tablist" aria-label="Authentication method">
            <button type="button" role="tab" aria-selected={method === 'PASSWORD'} className={method === 'PASSWORD' ? 'active' : ''} onClick={() => { setMethod('PASSWORD'); setError(null); }}>
              <LockKeyhole size={16} aria-hidden /> Password
            </button>
            <button type="button" role="tab" aria-selected={method === 'TOTP'} className={method === 'TOTP' ? 'active' : ''} onClick={() => { setMethod('TOTP'); setError(null); }}>
              <ShieldCheck size={16} aria-hidden /> Authenticator Code
            </button>
          </div>
          {method === 'PASSWORD' ? (
            <form className="form" onSubmit={form.handleSubmit(async (values) => {
              setError(null);
              try {
                await auth.login({ ...values, authenticationMethod: 'PASSWORD' });
                form.reset({ email: values.email, password: '' });
                navigate(destination, { replace: true });
              } catch (err) {
                if (err instanceof TotpChallengeRequired) {
                  setChallenge({ id: err.challengeId, email: values.email, expiresIn: err.expiresIn });
                  form.reset({ email: values.email, password: '' });
                  return;
                }
                setError(authErrorMessage(err, 'Unable to sign in.'));
              }
            })}>
              {error ? <div className="alert danger" role="alert">{error}</div> : null}
              <Field label="Email" error={form.formState.errors.email?.message}><Input type="email" autoComplete="email" {...form.register('email')} /></Field>
              <Field label="Password" error={form.formState.errors.password?.message}><PasswordInput autoComplete="current-password" {...form.register('password')} /></Field>
              <div className="auth-row"><Link to="/forgot-password">Forgot password?</Link></div>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? 'Signing in...' : 'Sign in'}
              </Button>
            </form>
          ) : (
            <form className="form" onSubmit={totpForm.handleSubmit(async (values) => {
              setError(null);
              try {
                await auth.login({ email: values.email, totpCode: values.totpCode, authenticationMethod: 'TOTP' });
                totpForm.reset({ email: values.email, totpCode: '' });
                navigate(destination, { replace: true });
              } catch (err) {
                setError(authErrorMessage(err, 'Unable to sign in with authenticator.'));
              }
            })}>
              {error ? <div className="alert danger" role="alert">{error}</div> : null}
              <Field label="Email" error={totpForm.formState.errors.email?.message}><Input type="email" autoComplete="email" {...totpForm.register('email')} /></Field>
              <Field label="Authenticator code" error={totpForm.formState.errors.totpCode?.message}>
                <Input
                  inputMode="numeric"
                  pattern="[0-9]*"
                  autoComplete="one-time-code"
                  maxLength={6}
                  {...totpForm.register('totpCode', {
                    onChange: (e) => {
                      e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
                    },
                  })}
                />
              </Field>
              <Button type="submit" disabled={totpForm.formState.isSubmitting}>
                {totpForm.formState.isSubmitting ? 'Signing in...' : 'Sign in with authenticator'}
              </Button>
            </form>
          )}
        </>
      )}
      <p className="auth-card-footer">New to AI Research Assistant? <Link to="/register">Create account</Link></p>
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
          await auth.login({ ...values, authenticationMethod: 'PASSWORD_AND_TOTP' });
          navigate(paths.dashboard, { replace: true });
        } catch (err) {
          setError(err instanceof Error ? err.message : 'Invalid or expired authenticator code.');
        }
      })}>
        {error ? <div className="alert danger" role="alert">{error}</div> : null}
        <Field label="Email" error={form.formState.errors.email?.message}><Input autoComplete="email" {...form.register('email')} /></Field>
        <Field label="Password" error={form.formState.errors.password?.message}><PasswordInput autoComplete="current-password" {...form.register('password')} /></Field>
        <Field label="6-digit code" error={form.formState.errors.totpCode?.message}>
          <Input
            inputMode="numeric"
            pattern="[0-9]*"
            autoComplete="one-time-code"
            maxLength={6}
            {...form.register('totpCode', {
              onChange: (e) => {
                e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
              },
            })}
          />
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
        <Field label="Password" error={form.formState.errors.password?.message}><PasswordInput autoComplete="new-password" {...form.register('password')} /></Field>
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
        <Field label="New password"><PasswordInput autoComplete="new-password" {...form.register('newPassword', { required: true, minLength: 8 })} /></Field>
        <Button type="submit" disabled={form.formState.isSubmitting}>Reset password</Button>
        <p className="muted"><Link to="/login">Back to sign in</Link></p>
      </form>
    </AuthLayout>
  );
}

function AuthLayout({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <main className="auth-layout">
      <section className="auth-context" aria-label="Product context">
        <div className="auth-brand">
          <span className="brand-mark">RA</span>
          <span>AI Research Assistant</span>
        </div>
        <div className="auth-context-body">
          <h1>AI Research Assistant</h1>
          <p>Secure workspace access for research planning, evidence review, writing support, and governance.</p>
          <ul>
            <li>Source-grounded research workflows</li>
            <li>Workspace and project access controls</li>
            <li>Authenticator-ready account security</li>
          </ul>
        </div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-card-brand">
            <span className="brand-mark">RA</span>
            <span>AI Research Assistant</span>
          </div>
          <h1>{title}</h1>
          {subtitle ? <p className="auth-subtitle">{subtitle}</p> : null}
          {children}
        </div>
      </section>
    </main>
  );
}

function PasswordInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  const [visible, setVisible] = useState(false);
  return (
    <span className="password-input">
      <Input type={visible ? 'text' : 'password'} {...props} />
      <button
        type="button"
        aria-label={visible ? 'Hide password' : 'Show password'}
        title={visible ? 'Hide password' : 'Show password'}
        onClick={() => setVisible((value) => !value)}
      >
        {visible ? <EyeOff size={16} aria-hidden /> : <Eye size={16} aria-hidden />}
      </button>
    </span>
  );
}

function authErrorMessage(error: unknown, fallback: string) {
  if (typeof navigator !== 'undefined' && !navigator.onLine) {
    return 'Network unavailable. Check your connection and try again.';
  }
  if (error instanceof Error) {
    const msg = error.message;
    if (/stack trace|exception|\bat\b\s+[a-z0-9_$.]+|\bjava\./i.test(msg)) {
      return fallback;
    }
    if (msg.toLowerCase().includes('network error') || msg.toLowerCase().includes('failed to fetch')) {
      return 'AI research and authentication services are temporarily unavailable. Please try again later.';
    }
    return msg;
  }
  return fallback;
}
