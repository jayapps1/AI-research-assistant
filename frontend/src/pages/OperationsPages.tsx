import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { adminApi, analysisApi, authApi, billingApi, notificationApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Input, Textarea, Badge, Select } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useAuth } from '../auth/AuthProvider';
import { useWorkspace } from '../features/workspaces/WorkspaceProvider';
import { useProjectId } from '../hooks/useProjectId';
import { displayValue, pageContent } from '../utils/collections';

export function ReportPage() {
  const projectId = useProjectId();
  const [selectedReportId, setSelectedReportId] = useState('');
  const [selectedChapterId, setSelectedChapterId] = useState('');
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [sectionContent, setSectionContent] = useState('');
  const reports = useQuery({ queryKey: ['reports', projectId, 0, 20], queryFn: () => reportApi.reports(projectId, 0, 20), enabled: Boolean(projectId) });
  const selectedReport = pageContent(reports.data).find((report) => report.id === selectedReportId) ?? pageContent(reports.data)[0];
  const reportId = String(selectedReport?.id ?? '');
  const chapters = useQuery({ queryKey: ['report-chapters', reportId], queryFn: () => reportApi.chapters(reportId), enabled: Boolean(reportId) });
  const selectedChapter = (chapters.data ?? []).find((chapter) => chapter.id === selectedChapterId) ?? chapters.data?.[0];
  const chapterId = String(selectedChapter?.id ?? '');
  const sections = useQuery({ queryKey: ['report-sections', chapterId], queryFn: () => reportApi.sections(chapterId), enabled: Boolean(chapterId) });
  const selectedSection = (sections.data ?? []).find((section) => section.id === selectedSectionId) ?? sections.data?.[0];
  const validation = useMutation({ mutationFn: () => reportApi.validate(reportId) });
  const assemble = useMutation({ mutationFn: () => reportApi.assemble(reportId) });
  const finalize = useMutation({ mutationFn: () => reportApi.finalize(reportId) });
  const saveSection = useMutation({ mutationFn: () => reportApi.updateSection(String(selectedSection?.id), { content: sectionContent || selectedSection?.content }) });
  const generateSection = useMutation({ mutationFn: () => reportApi.generateSection(String(selectedSection?.id)) });
  const docxExport = useMutation({ mutationFn: () => reportApi.exports(reportId, { format: 'DOCX' }) });
  const pdfExport = useMutation({ mutationFn: () => reportApi.exports(reportId, { format: 'PDF' }) });
  const integrity = useMutation({ mutationFn: () => reportApi.integrity(reportId) });
  const writing = useMutation({ mutationFn: () => reportApi.writingReview(reportId) });
  const similarity = useMutation({ mutationFn: () => reportApi.similarity(projectId, { targetType: 'REPORT', targetId: reportId }) });
  const aiUsage = useQuery({ queryKey: ['ai-usage', projectId], queryFn: () => analysisApi.aiUsage(projectId), enabled: Boolean(projectId) });
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Report']} />
      <div className="page-header">
        <div><h1 className="page-title">Professional report builder</h1><p className="muted">Browser preview is a working academic preview, not a final DOCX/PDF pagination guarantee.</p></div>
        <div className="toolbar"><Button type="button" variant="secondary" onClick={() => validation.mutate()}>Validate</Button><Button type="button" variant="secondary" onClick={() => assemble.mutate()}>Assemble</Button><Button type="button" onClick={() => finalize.mutate()}>Finalize</Button></div>
      </div>
      <div className="grid cols-3">
        <Metric label="Report" value={selectedReport?.title ?? 'No report'} />
        <Metric label="Status" value={selectedReport?.status ?? 'Not returned'} />
        <Metric label="Citation style" value={selectedReport?.citationStyle ?? 'Backend default'} />
      </div>
      <div className="report-builder">
        <Card>
          <h2>Report structure</h2>
          <Field label="Open report">
            <Select value={reportId} onChange={(event) => setSelectedReportId(event.target.value)}>
              {pageContent(reports.data).map((report) => <option key={String(report.id)} value={String(report.id)}>{displayValue(report.title)}</option>)}
            </Select>
          </Field>
          {(chapters.data ?? []).map((chapter) => (
            <div className="tree-node" key={String(chapter.id)}>
              <button type="button" className="tree-button" onClick={() => setSelectedChapterId(String(chapter.id))}>{displayValue(chapter.title ?? chapter.type)}</button>
              {chapter.id === chapterId ? (sections.data ?? []).map((section) => <button key={String(section.id)} type="button" className="tree-leaf" onClick={() => { setSelectedSectionId(String(section.id)); setSectionContent(String(section.content ?? '')); }}>{displayValue(section.heading ?? section.type)}</button>) : null}
            </div>
          ))}
          {!pageContent(reports.data).length ? <EmptyState title="No reports" description="Create or assemble a report from the backend workflow." /> : null}
        </Card>
        <Card>
          <h2>Section editor</h2>
          {selectedSection ? (
            <div className="grid">
              <div className="toolbar"><Badge>{displayValue(selectedSection.origin, 'USER')}</Badge><Badge tone={selectedSection.sourceOutOfDate ? 'warning' : 'success'}>{selectedSection.sourceOutOfDate ? 'STALE SOURCE' : 'CURRENT SOURCE'}</Badge><Badge>Revision {displayValue(selectedSection.revisionNumber)}</Badge></div>
              {selectedSection.sourceOutOfDate ? <div className="alert warning">The source material for this section has changed since it was last assembled.<div className="toolbar"><Button type="button" variant="secondary">View Source Changes</Button><Button type="button" variant="secondary" onClick={() => generateSection.mutate()}>Regenerate Draft</Button><Button type="button" variant="secondary">Keep Current Section</Button></div></div> : null}
              <Field label="Heading"><Input value={String(selectedSection.heading ?? '')} readOnly /></Field>
              <Field label="Content"><Textarea value={sectionContent || String(selectedSection.content ?? '')} onChange={(event) => setSectionContent(event.target.value)} /></Field>
              <div className="toolbar"><Button type="button" onClick={() => saveSection.mutate()} disabled={saveSection.isPending}>Save revision</Button><Button type="button" variant="secondary" onClick={() => generateSection.mutate()} disabled={generateSection.isPending}>AI draft</Button></div>
              {generateSection.data ? <DraftPreview draft={generateSection.data} /> : null}
            </div>
          ) : <EmptyState title="Select a report section" />}
        </Card>
        <Card>
          <h2>Evidence / comments / AI usage</h2>
          <p className="muted">Section citations resolve to backend evidence and reference metadata where available.</p>
          <RecordRows rows={aiUsage.data ? [aiUsage.data] : []} />
        </Card>
      </div>
      <div className="grid cols-2">
        <ValidationPanel validation={validation.data} />
        <Card><h2>Export</h2><div className="toolbar"><Button type="button" onClick={() => docxExport.mutate()} disabled={!reportId}>Export DOCX</Button><Button type="button" variant="secondary" onClick={() => pdfExport.mutate()} disabled={!reportId}>Export PDF</Button></div><ExportJob data={docxExport.data} /><ExportJob data={pdfExport.data} /></Card>
        <Card><h2>Similarity review</h2><p>Similarity, matching text, potential overlap, review required.</p><p className="muted">This comparison checks only sources available in the project.</p><Button type="button" variant="secondary" onClick={() => similarity.mutate()} disabled={!reportId}>Run similarity check</Button><RecordRows rows={similarity.data ? [similarity.data] : []} /></Card>
        <Card><h2>Integrity and writing review</h2><div className="toolbar"><Button type="button" variant="secondary" onClick={() => integrity.mutate()} disabled={!reportId}>Integrity review</Button><Button type="button" variant="secondary" onClick={() => writing.mutate()} disabled={!reportId}>Writing review</Button></div><RecordRows rows={[integrity.data, writing.data].filter(Boolean) as Record<string, unknown>[]} /></Card>
      </div>
    </section>
  );
}

export function ReferencesPage() {
  const projectId = useProjectId();
  const [q, setQ] = useState('');
  const [style, setStyle] = useState('APA_7');
  const references = useQuery({ queryKey: ['references', projectId, q], queryFn: () => reportApi.references(projectId, 0, 20, { title: q || undefined }), enabled: Boolean(projectId) });
  const format = useMutation({ mutationFn: () => reportApi.formatCitation({ referenceId: pageContent(references.data)[0]?.id, style }) });
  const duplicates = useMutation({ mutationFn: () => reportApi.duplicateReferences(projectId, { referenceIds: pageContent(references.data).map((reference) => reference.id) }) });
  const importRefs = useMutation({ mutationFn: (formatName: string) => reportApi.importReferences(projectId, { format: formatName, content: '' }) });
  return (
    <section className="page">
      <h1 className="page-title">Reference library</h1>
      <div className="toolbar">
        <Field label="Search"><Input value={q} onChange={(event) => setQ(event.target.value)} /></Field>
        <Button type="button">Create</Button>
        <Button type="button" variant="secondary" onClick={() => importRefs.mutate('RIS')}>Import RIS</Button>
        <Button type="button" variant="secondary" onClick={() => importRefs.mutate('BIBTEX')}>Import BibTeX</Button>
        <Button type="button" variant="secondary" onClick={() => importRefs.mutate('ENDNOTE_XML')}>Import EndNote XML</Button>
        <Button type="button" variant="secondary" onClick={() => duplicates.mutate()}>Check duplicates</Button>
      </div>
      <Card>
        <h2>Citation style</h2>
        <div className="toolbar">
          <Select value={style} onChange={(event) => setStyle(event.target.value)}>
            {['APA_7', 'HARVARD', 'IEEE', 'CHICAGO_AUTHOR_DATE', 'VANCOUVER', 'MLA_9'].map((item) => <option key={item}>{item}</option>)}
          </Select>
          <Button type="button" variant="secondary" onClick={() => format.mutate()}>Preview citation</Button>
        </div>
        {format.data ? <div className="panel">{displayValue(format.data.formattedCitation ?? format.data.citation)}</div> : null}
      </Card>
      <RecordRows rows={pageContent(references.data)} />
      {duplicates.data ? <Card><h2>Duplicates</h2><p className="muted">Exact duplicate and possible duplicate findings require user review; uncertain references are not auto-merged.</p><RecordRows rows={[duplicates.data]} /></Card> : null}
    </section>
  );
}

export function BillingPage() {
  const { selectedWorkspaceId: workspaceId } = useWorkspace();
  const subscription = useQuery({ queryKey: ['billing', workspaceId, 'subscription'], queryFn: () => billingApi.subscription(workspaceId), enabled: Boolean(workspaceId) });
  const transactions = useQuery({ queryKey: ['billing', workspaceId, 'transactions'], queryFn: () => billingApi.transactions(workspaceId), enabled: Boolean(workspaceId) });
  const initialize = useMutation({ mutationFn: (body: { planCode: string; billingInterval: string }) => billingApi.initialize(workspaceId, body) });
  const retry = useMutation({ mutationFn: (paymentIntentId: string) => billingApi.retry(paymentIntentId) });
  return <section className="page"><h1 className="page-title">Billing</h1>{!workspaceId ? <EmptyState title="Open billing from a workspace" /> : null}<div className="grid cols-2"><Card><h2>Current access</h2><PlanState data={subscription.data} /><QuotaUsage data={subscription.data} /></Card><Card><h2>Available plans</h2><BillingCheckoutForm onSubmit={(values) => initialize.mutate(values)} loading={initialize.isPending} />{initialize.data ? <CheckoutResult data={initialize.data} /> : null}</Card><Card><h2>Payment history</h2><RecordRows rows={transactions.data ?? []} /><div className="alert info">Pending Mobile Money: Waiting for Mobile Money approval. Use Refresh Status until the backend reports a retryable failed, expired, or abandoned state.</div><Field label="Retry payment intent ID"><Input id="retryIntent" /></Field><Button type="button" variant="secondary" onClick={() => retry.mutate((document.getElementById('retryIntent') as HTMLInputElement)?.value)}>Pay Again</Button>{retry.data ? <CheckoutResult data={retry.data} /> : null}</Card></div></section>;
}

function PlanState({ data }: { data?: Record<string, unknown> }) {
  const access = String(data?.accessType ?? data?.grantType ?? data?.subscriptionType ?? '');
  const complimentary = ['COMPLIMENTARY', 'DEVELOPER_ACCESS', 'PROMOTIONAL'].includes(access);
  return <div>{complimentary ? <Badge tone="success">{access.replaceAll('_', ' ')}</Badge> : <Badge>{displayValue(data?.status, 'No active plan')}</Badge>}<p className="muted">{complimentary ? 'No billing required.' : 'Paid status is displayed only when backend reports a verified subscription.'}</p></div>;
}

export function NotificationsPage() {
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: notificationApi.list });
  const markAll = useMutation({ mutationFn: notificationApi.markAllRead });
  return <section className="page"><div className="page-header"><h1 className="page-title">Notifications</h1><Button type="button" variant="secondary" onClick={() => markAll.mutate()}>Mark all read</Button></div>{notifications.isError ? <ErrorState error={notifications.error} /> : <RecordRows rows={pageContent(notifications.data) as never} />}</section>;
}

export function ProfilePage() {
  const auth = useAuth();
  const displayName = auth.user?.fullName ?? (`${auth.user?.firstName ?? ''} ${auth.user?.lastName ?? ''}`.trim() || 'Signed-in user');
  return <section className="page"><h1 className="page-title">Profile</h1><div className="grid cols-2"><Card><h2>Profile information</h2><p>{displayName}</p><p className="muted">{auth.user?.email}</p><Badge>{auth.user?.status ?? 'Authenticated'}</Badge></Card><Card><h2>Settings</h2><p><a href="/app/settings/security">Security settings</a></p><p><a href="/app/settings/notifications">Notification preferences</a></p></Card></div></section>;
}

export function SecuritySettingsPage() {
  const auth = useAuth();
  const [totpCode, setTotpCode] = useState('');
  const [password, setPassword] = useState('');
  const enroll = useMutation({ mutationFn: authApi.startTotpEnrollment });
  const confirm = useMutation({ mutationFn: () => authApi.confirmTotpEnrollment(totpCode) });
  const disable = useMutation({ mutationFn: () => authApi.disableTotp({ password, totpCode }) });
  return (
    <section className="page">
      <h1 className="page-title">Security settings</h1>
      <div className="grid cols-2">
        <Card>
          <h2>TOTP enrollment</h2>
          <Button type="button" onClick={() => enroll.mutate()}>Start enrollment</Button>
          {enroll.data ? <div className="panel"><p><strong>{enroll.data.issuer}</strong> · {enroll.data.accountName}</p><p className="muted">Provisioning URI is shown only during enrollment.</p><code>{enroll.data.provisioningUri}</code></div> : null}
          <Field label="6-digit code"><Input value={totpCode} maxLength={6} inputMode="numeric" onChange={(event) => setTotpCode(event.target.value)} /></Field>
          <Button type="button" variant="secondary" onClick={() => confirm.mutate()} disabled={confirm.isPending}>Confirm enrollment</Button>
          {confirm.data ? <div className="alert warning"><strong>Recovery codes are shown now only.</strong><p>{confirm.data.recoveryCodes.join(' ')}</p></div> : null}
        </Card>
        <Card>
          <h2>Sessions and sensitive actions</h2>
          <Field label="Password for step-up"><Input type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></Field>
          <div className="toolbar"><Button type="button" variant="secondary" onClick={auth.logoutAll}>Logout all sessions</Button><Button type="button" variant="danger" onClick={() => disable.mutate()}>Disable TOTP</Button></div>
          <p className="muted">TOTP secrets and recovery codes are not persisted long-term in the browser.</p>
        </Card>
      </div>
    </section>
  );
}

export function NotificationSettingsPage() {
  return <section className="page"><h1 className="page-title">Notification settings</h1><div className="grid cols-2"><Card><h2>Channels</h2>{['In-app', 'Email', 'SMS', 'Push'].map((channel) => <label key={channel} className="field"><span className="label">{channel}</span><input type="checkbox" /></label>)}</Card><Card><h2>Types</h2><p className="muted">Preferences are shown for backend-supported notification types. Unsupported channels remain disabled by backend policy.</p></Card></div></section>;
}

export function AdminPage() {
  const dashboard = useQuery({ queryKey: ['admin', 'dashboard'], queryFn: adminApi.dashboard });
  const operations = useQuery({ queryKey: ['admin', 'operations'], queryFn: adminApi.operations });
  const users = useQuery({ queryKey: ['admin', 'users'], queryFn: adminApi.users });
  const grant = useMutation({ mutationFn: adminApi.grantComplimentaryAccess });
  return <section className="page"><h1 className="page-title">System administration</h1><div className="grid cols-2"><Card><h2>Dashboard</h2><RecordRows rows={dashboard.data ? [dashboard.data] : []} /></Card><Card><h2>Operations</h2><RecordRows rows={operations.data ? [operations.data] : []} /></Card><Card><h2>Users</h2><RecordRows rows={pageContent(users.data)} /></Card><Card><h2>Complimentary access</h2><form className="form"><Field label="Workspace/User"><Input /></Field><Field label="Plan"><Input /></Field><Field label="Grant type"><select className="select"><option>DEVELOPER_ACCESS</option><option>COMPLIMENTARY</option><option>PROMOTIONAL</option></select></Field><Field label="Reason"><Textarea /></Field><Button type="button" onClick={() => grant.mutate({})}>Grant</Button></form><p className="muted">Paid subscriptions and complimentary grants are always visually distinct.</p></Card></div></section>;
}

function RecordRows({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows.length) return <EmptyState title="No backend records returned" />;
  return <div className="grid">{rows.slice(0, 10).map((row, index) => <div className="panel" key={index}><Badge>{displayValue(row.status ?? row.type ?? row.id ?? 'Record')}</Badge><p>{displayValue(row.title ?? row.name ?? row.message ?? row.description ?? row.email ?? row.id)}</p></div>)}</div>;
}

function Metric({ label, value }: { label: string; value: unknown }) {
  return <Card><span className="muted">{label}</span><p className="metric">{displayValue(value)}</p></Card>;
}

function DraftPreview({ draft }: { draft: Record<string, unknown> }) {
  return <div className="panel"><Badge tone="info">AI draft</Badge><p>{displayValue(draft.draftText ?? draft.text)}</p><div className="toolbar"><Button type="button">Accept</Button><Button type="button" variant="secondary">Edit</Button><Button type="button" variant="danger">Reject</Button></div></div>;
}

function ValidationPanel({ validation }: { validation?: Record<string, unknown> }) {
  const errors = Array.isArray(validation?.errors) ? validation.errors as Record<string, unknown>[] : [];
  const warnings = Array.isArray(validation?.warnings) ? validation.warnings as Record<string, unknown>[] : [];
  const information = Array.isArray(validation?.information) ? validation.information as Record<string, unknown>[] : [];
  return (
    <Card>
      <h2>Report validation</h2>
      {[['ERROR', errors], ['WARNING', warnings], ['INFO', information]].map(([severity, rows]) => (
        <div key={String(severity)}>
          <h3>{String(severity)}</h3>
          {(rows as Record<string, unknown>[]).length ? (rows as Record<string, unknown>[]).map((issue, index) => <div className={`alert ${String(severity).toLowerCase()}`} key={String(issue.code ?? index)}>{displayValue(issue.message)} {issue.artifactId ? <a href={`#${issue.artifactId}`}>Open artifact</a> : null}</div>) : <p className="muted">No {String(severity).toLowerCase()} issues.</p>}
        </div>
      ))}
    </Card>
  );
}

function ExportJob({ data }: { data?: Record<string, unknown> }) {
  if (!data) return null;
  const id = String(data.id ?? '');
  const status = String(data.status ?? '');
  return <div className="panel"><Badge tone={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning'}>{displayValue(data.format)} {status}</Badge><p className="muted">Revision {displayValue(data.reportRevisionNumber)} | Size {displayValue(data.fileSizeBytes)}</p>{status === 'COMPLETED' && id ? <a className="button secondary" href={reportApi.exportDownloadUrl(id)}>Download</a> : null}</div>;
}

function BillingCheckoutForm({ onSubmit, loading }: { onSubmit: (values: { planCode: string; billingInterval: string }) => void; loading?: boolean }) {
  const [planCode, setPlanCode] = useState('');
  const [billingInterval, setBillingInterval] = useState('MONTHLY');
  return <form className="form" onSubmit={(event) => { event.preventDefault(); onSubmit({ planCode, billingInterval }); }}><Field label="Plan code"><Input value={planCode} onChange={(event) => setPlanCode(event.target.value)} /></Field><Field label="Billing interval"><Select value={billingInterval} onChange={(event) => setBillingInterval(event.target.value)}><option>MONTHLY</option><option>YEARLY</option></Select></Field><Button type="submit" disabled={!planCode || loading}>Start Paystack TEST checkout</Button><Badge tone="warning">TEST PAYMENT</Badge></form>;
}

function CheckoutResult({ data }: { data: { authorizationUrl?: string } }) {
  const url = String(data.authorizationUrl ?? '');
  const trusted = /^https:\/\/(checkout|standard)\.paystack\.com\//.test(url);
  return <div className="alert info">Backend created a new payment attempt. {trusted ? <a href={url} rel="noopener noreferrer">Open Paystack TEST checkout</a> : 'Authorization URL is not displayed because it did not match the expected Paystack host.'}</div>;
}

function QuotaUsage({ data }: { data?: Record<string, unknown> }) {
  const rows = ['aiRequests', 'aiTokens', 'projects', 'storage', 'collaborators', 'exports'];
  return <div className="grid">{rows.map((key) => <p key={key}><strong>{key}</strong>: {String(data?.[key] ?? 'Backend scoped')}</p>)}</div>;
}
