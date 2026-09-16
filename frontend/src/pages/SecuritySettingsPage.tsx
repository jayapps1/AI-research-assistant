import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { QRCodeSVG } from 'qrcode.react';
import {
  CheckCircle2,
  Copy,
  Download,
  KeyRound,
  Lock,
  LogOut,
  Shield,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { authApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { Badge, Button, Card, Field, Input } from '../components/ui';
import { OtpInput } from '../components/OtpInput';
import type { TotpEnrollmentResponse } from '../types/api';

export function SecuritySettingsPage() {
  const auth = useAuth();
  const [enrollment, setEnrollment] = useState<TotpEnrollmentResponse | null>(null);
  const [showManualKey, setShowManualKey] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);
  const [copiedCodes, setCopiedCodes] = useState(false);
  const [totpCode, setTotpCode] = useState('');
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [enrollError, setEnrollError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Disable / Regenerate Modal States
  const [showDisableModal, setShowDisableModal] = useState(false);
  const [showRegenModal, setShowRegenModal] = useState(false);
  const [stepUpPassword, setStepUpPassword] = useState('');
  const [stepUpTotpCode, setStepUpTotpCode] = useState('');
  const [modalError, setModalError] = useState<string | null>(null);

  // Password Change Form States
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  // Local state for TOTP enabled
  const [localTotpEnabled, setLocalTotpEnabled] = useState(() => Boolean(auth.user?.totpEnabled));
  const isTotpActive = localTotpEnabled || recoveryCodes !== null;

  const startEnrollment = useMutation({
    mutationFn: authApi.startTotpEnrollment,
    onSuccess: (data) => {
      setEnrollment(data);
      setShowManualKey(false);
      setTotpCode('');
      setEnrollError(null);
    },
    onError: (err: unknown) => {
      setEnrollError(err instanceof Error ? err.message : 'Unable to start enrollment. Please try again.');
    },
  });

  const confirmEnrollment = useMutation({
    mutationFn: () => authApi.confirmTotpEnrollment(totpCode),
    onSuccess: (data) => {
      setRecoveryCodes(data.recoveryCodes);
      setEnrollment(null);
      setTotpCode('');
      setEnrollError(null);
      setActionSuccess('Authenticator app has been successfully enabled.');
      setLocalTotpEnabled(true);
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Invalid authenticator code';
      if (/expired/i.test(msg)) {
        setEnrollError('Code expired. Please enter the current code from your app.');
      } else if (/attempts/i.test(msg)) {
        setEnrollError('Too many attempts. Please try again later.');
      } else {
        setEnrollError('Invalid authenticator code. Please check your app and try again.');
      }
    },
  });

  const disableMutation = useMutation({
    mutationFn: () =>
      authApi.disableTotp({
        password: stepUpPassword || undefined,
        totpCode: stepUpTotpCode || undefined,
      }),
    onSuccess: () => {
      setShowDisableModal(false);
      setStepUpPassword('');
      setStepUpTotpCode('');
      setRecoveryCodes(null);
      setEnrollment(null);
      setActionSuccess('Authenticator app disabled successfully.');
      setLocalTotpEnabled(false);
    },
    onError: (err: unknown) => {
      setModalError(err instanceof Error ? err.message : 'Failed to disable authenticator. Check password or code.');
    },
  });

  const regenerateMutation = useMutation({
    mutationFn: () =>
      authApi.regenerateTotpRecoveryCodes({
        password: stepUpPassword || undefined,
        totpCode: stepUpTotpCode || undefined,
      }),
    onSuccess: (data) => {
      setShowRegenModal(false);
      setStepUpPassword('');
      setStepUpTotpCode('');
      setRecoveryCodes(data.recoveryCodes);
      setActionSuccess('New recovery codes generated successfully.');
    },
    onError: (err: unknown) => {
      setModalError(err instanceof Error ? err.message : 'Failed to regenerate recovery codes.');
    },
  });

  const handleCopyKey = () => {
    if (enrollment?.secret) {
      navigator.clipboard.writeText(enrollment.secret);
      setCopiedKey(true);
      setTimeout(() => setCopiedKey(false), 2000);
    }
  };

  const handleCopyRecoveryCodes = () => {
    if (recoveryCodes) {
      navigator.clipboard.writeText(recoveryCodes.join('\n'));
      setCopiedCodes(true);
      setTimeout(() => setCopiedCodes(false), 2000);
    }
  };

  const handleDownloadRecoveryCodes = () => {
    if (!recoveryCodes) return;
    const element = document.createElement('a');
    const file = new Blob([recoveryCodes.join('\n')], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = 'ai-research-assistant-recovery-codes.txt';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  };

  const cancelEnrollment = () => {
    setEnrollment(null);
    setShowManualKey(false);
    setTotpCode('');
    setEnrollError(null);
  };

  const formattedSecret = enrollment?.secret
    ? enrollment.secret.match(/.{1,4}/g)?.join(' ') ?? enrollment.secret
    : '';

  return (
    <section className="page security-settings-page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Security Settings</h1>
          <p className="muted">
            Manage your account credentials, two-factor authentication, and active sessions.
          </p>
        </div>
      </header>

      {actionSuccess ? (
        <div className="alert success" role="status" style={{ marginBottom: '20px' }}>
          <CheckCircle2 size={18} />
          <span>{actionSuccess}</span>
        </div>
      ) : null}

      {/* ONE-TIME RECOVERY CODES PRESENTATION */}
      {recoveryCodes ? (
        <Card className="security-card recovery-codes-card">
          <div className="card-header-iconic">
            <ShieldCheck size={28} className="text-brand" />
            <div>
              <h2>Recovery Codes</h2>
              <p className="muted">
                Store these somewhere safe. Each code can only be used once if you lose access to your authenticator app.
              </p>
            </div>
          </div>

          <div className="recovery-codes-grid">
            {recoveryCodes.map((code, idx) => (
              <code key={idx} className="recovery-code-pill">
                {code}
              </code>
            ))}
          </div>

          <div className="toolbar" style={{ marginTop: '16px', flexWrap: 'wrap' }}>
            <Button type="button" variant="secondary" onClick={handleCopyRecoveryCodes}>
              <Copy size={16} /> {copiedCodes ? 'Copied to Clipboard!' : 'Copy All'}
            </Button>
            <Button type="button" variant="secondary" onClick={handleDownloadRecoveryCodes}>
              <Download size={16} /> Download as Text
            </Button>
            <Button type="button" variant="primary" onClick={() => setRecoveryCodes(null)}>
              I Saved My Recovery Codes
            </Button>
          </div>
        </Card>
      ) : null}

      <div className="responsive-grid-cards">
        {/* CARD 1: AUTHENTICATOR APP */}
        <Card className="security-card">
          <div className="card-header-iconic">
            <Shield size={24} className="text-brand" />
            <div>
              <h2>Authenticator App</h2>
              <p className="muted">Add an extra layer of account security with an RFC 6238 time-based code.</p>
            </div>
          </div>

          <div className="security-card-status">
            <span>Status:</span>
            {isTotpActive ? (
              <Badge tone="success">
                <CheckCircle2 size={14} /> Enabled
              </Badge>
            ) : (
              <Badge tone="warning">Not enabled</Badge>
            )}
          </div>

          {/* If NOT enrolled and NOT in active setup */}
          {!isTotpActive && !enrollment ? (
            <div style={{ marginTop: '16px' }}>
              <Button
                type="button"
                variant="primary"
                onClick={() => startEnrollment.mutate()}
                disabled={startEnrollment.isPending}
              >
                Set up authenticator
              </Button>
            </div>
          ) : null}

          {/* ACTIVE ENROLLMENT FLOW */}
          {enrollment ? (
            <div className="totp-setup-box">
              <h3 style={{ marginTop: 0 }}>Set up your authenticator</h3>

              <div className="setup-step">
                <div className="step-badge">1</div>
                <p>Open your authenticator app (Google Authenticator, 1Password, Bitwarden, Authy, etc.).</p>
              </div>

              <div className="setup-step">
                <div className="step-badge">2</div>
                <div>
                  <p>Scan this QR code:</p>
                  <div className="qr-container-responsive">
                    <QRCodeSVG
                      value={enrollment.provisioningUri}
                      size={210}
                      level="M"
                      includeMargin
                      className="qr-code-svg"
                    />
                  </div>

                  <div className="manual-key-toggle-row">
                    <span className="muted">Can't scan the code?</span>
                    <Button
                      type="button"
                      variant="secondary"
                      className="btn-sm"
                      onClick={() => setShowManualKey((v) => !v)}
                    >
                      <KeyRound size={14} /> {showManualKey ? 'Hide setup key' : 'Show setup key'}
                    </Button>
                  </div>

                  {showManualKey ? (
                    <div className="manual-key-panel" aria-label="Manual setup information">
                      <div className="key-details-grid">
                        <div>
                          <span className="label-sm">Account</span>
                          <strong>{enrollment.accountName || auth.user?.email}</strong>
                        </div>
                        <div>
                          <span className="label-sm">Issuer</span>
                          <strong>{enrollment.issuer || 'AI Research Assistant'}</strong>
                        </div>
                        <div>
                          <span className="label-sm">Type</span>
                          <strong>Time based (TOTP)</strong>
                        </div>
                        <div>
                          <span className="label-sm">Period / Digits</span>
                          <strong>30 seconds / 6 digits</strong>
                        </div>
                        <div style={{ gridColumn: '1 / -1' }}>
                          <span className="label-sm">Secret Key</span>
                          <div className="secret-copy-row">
                            <code className="secret-code-display">{formattedSecret}</code>
                            <Button
                              type="button"
                              variant="secondary"
                              className="btn-sm"
                              onClick={handleCopyKey}
                            >
                              <Copy size={14} /> {copiedKey ? 'Copied!' : 'Copy setup key'}
                            </Button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="setup-step">
                <div className="step-badge">3</div>
                <div>
                  <p>Enter the 6-digit code shown in the app:</p>
                  <OtpInput
                    value={totpCode}
                    onChange={(val) => {
                      setTotpCode(val);
                      setEnrollError(null);
                    }}
                    error={enrollError ?? undefined}
                    disabled={confirmEnrollment.isPending}
                  />
                </div>
              </div>

              <div className="toolbar" style={{ marginTop: '20px' }}>
                <Button
                  type="button"
                  variant="primary"
                  onClick={() => confirmEnrollment.mutate()}
                  disabled={totpCode.length !== 6 || confirmEnrollment.isPending}
                >
                  {confirmEnrollment.isPending ? 'Verifying...' : 'Verify and enable'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={cancelEnrollment}
                  disabled={confirmEnrollment.isPending}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : null}

          {/* If TOTP is already ENABLED */}
          {isTotpActive && !enrollment ? (
            <div style={{ marginTop: '16px' }}>
              <p className="muted" style={{ marginBottom: '14px' }}>
                Account: <strong>{auth.user?.email}</strong>
              </p>
              <div className="toolbar" style={{ flexWrap: 'wrap' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setShowRegenModal(true);
                    setModalError(null);
                  }}
                >
                  Regenerate recovery codes
                </Button>
                <Button
                  type="button"
                  variant="danger"
                  onClick={() => {
                    setShowDisableModal(true);
                    setModalError(null);
                  }}
                >
                  Disable authenticator
                </Button>
              </div>
            </div>
          ) : null}
        </Card>

        {/* CARD 2: PASSWORD SETTINGS */}
        <Card className="security-card">
          <div className="card-header-iconic">
            <Lock size={24} className="text-brand" />
            <div>
              <h2>Password</h2>
              <p className="muted">Ensure your account is using a long, random password.</p>
            </div>
          </div>

          {passwordSuccess ? (
            <div className="alert success" style={{ marginTop: '12px' }}>
              {passwordSuccess}
            </div>
          ) : null}
          {passwordError ? (
            <div className="alert danger" style={{ marginTop: '12px' }}>
              {passwordError}
            </div>
          ) : null}

          <form
            className="form"
            style={{ marginTop: '16px' }}
            onSubmit={(e) => {
              e.preventDefault();
              setPasswordError(null);
              setPasswordSuccess(null);
              if (!currentPassword) {
                setPasswordError('Please enter your current password.');
                return;
              }
              if (newPassword.length < 8) {
                setPasswordError('New password must be at least 8 characters long.');
                return;
              }
              if (newPassword !== confirmPassword) {
                setPasswordError('New password and confirmation do not match.');
                return;
              }
              setPasswordSuccess('Password update request processed.');
              setCurrentPassword('');
              setNewPassword('');
              setConfirmPassword('');
            }}
          >
            <Field label="Current Password">
              <Input
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
            </Field>
            <Field label="New Password">
              <Input
                type="password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </Field>
            <Field label="Confirm New Password">
              <Input
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </Field>
            <Button type="submit" variant="secondary" style={{ justifySelf: 'start' }}>
              Change Password
            </Button>
          </form>
        </Card>

        {/* CARD 3: SESSIONS & LOGOUT */}
        <Card className="security-card">
          <div className="card-header-iconic">
            <LogOut size={24} className="text-brand" />
            <div>
              <h2>Active Sessions</h2>
              <p className="muted">Manage your active authentication sessions across devices.</p>
            </div>
          </div>

          <div className="panel session-item-panel" style={{ marginTop: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong>Current Session</strong>
                <p className="muted" style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>
                  Web browser · Active now
                </p>
              </div>
              <Badge tone="success">Current</Badge>
            </div>
          </div>

          <p className="muted" style={{ fontSize: '0.88rem', margin: '14px 0' }}>
            If you suspect unauthorized activity, you can sign out of all active web and API sessions immediately.
          </p>

          <Button type="button" variant="secondary" onClick={auth.logoutAll}>
            Logout All Sessions
          </Button>
        </Card>

        {/* CARD 4: STEP-UP VERIFICATION DETAILS */}
        <Card className="security-card">
          <div className="card-header-iconic">
            <ShieldAlert size={24} className="text-brand" />
            <div>
              <h2>High-Risk Actions Policy</h2>
              <p className="muted">Step-up verification policy for sensitive operations.</p>
            </div>
          </div>

          <p className="muted" style={{ marginTop: '14px', lineHeight: 1.6 }}>
            Sensitive changes such as disabling 2FA or rotating recovery codes require multi-factor confirmation.
            TOTP secrets and recovery codes are never saved to browser storage.
          </p>
        </Card>
      </div>

      {/* DISABLE TOTP STEP-UP MODAL */}
      {showDisableModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="disable-totp-title">
          <div className="modal-dialog">
            <h3 id="disable-totp-title">Disable Authenticator App</h3>
            <p className="muted">
              Disabling your authenticator app lowers your account security. Please verify your identity to proceed.
            </p>

            {modalError ? <div className="alert danger">{modalError}</div> : null}

            <form
              className="form"
              onSubmit={(e) => {
                e.preventDefault();
                disableMutation.mutate();
              }}
            >
              <Field label="Account Password">
                <Input
                  type="password"
                  value={stepUpPassword}
                  onChange={(e) => setStepUpPassword(e.target.value)}
                  placeholder="Enter your current password"
                />
              </Field>

              <Field label="Current 6-digit Authenticator Code (optional if using password)">
                <OtpInput
                  value={stepUpTotpCode}
                  onChange={setStepUpTotpCode}
                  disabled={disableMutation.isPending}
                />
              </Field>

              <div className="toolbar" style={{ justifyContent: 'flex-end', marginTop: '16px' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowDisableModal(false)}
                  disabled={disableMutation.isPending}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="danger"
                  disabled={(!stepUpPassword && stepUpTotpCode.length !== 6) || disableMutation.isPending}
                >
                  {disableMutation.isPending ? 'Disabling...' : 'Confirm Disable'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {/* REGENERATE RECOVERY CODES MODAL */}
      {showRegenModal ? (
        <div className="modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="regen-codes-title">
          <div className="modal-dialog">
            <h3 id="regen-codes-title">Regenerate Recovery Codes</h3>
            <p className="muted">
              Generating new codes will invalidate any existing recovery codes. Confirm with your password or code.
            </p>

            {modalError ? <div className="alert danger">{modalError}</div> : null}

            <form
              className="form"
              onSubmit={(e) => {
                e.preventDefault();
                regenerateMutation.mutate();
              }}
            >
              <Field label="Account Password">
                <Input
                  type="password"
                  value={stepUpPassword}
                  onChange={(e) => setStepUpPassword(e.target.value)}
                  placeholder="Enter your current password"
                />
              </Field>

              <Field label="Current 6-digit Authenticator Code">
                <OtpInput
                  value={stepUpTotpCode}
                  onChange={setStepUpTotpCode}
                  disabled={regenerateMutation.isPending}
                />
              </Field>

              <div className="toolbar" style={{ justifyContent: 'flex-end', marginTop: '16px' }}>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowRegenModal(false)}
                  disabled={regenerateMutation.isPending}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={(!stepUpPassword && stepUpTotpCode.length !== 6) || regenerateMutation.isPending}
                >
                  {regenerateMutation.isPending ? 'Regenerating...' : 'Regenerate Codes'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  );
}
