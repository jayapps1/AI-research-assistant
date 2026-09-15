import { lazy, Suspense, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { analysisApi, datasetApi } from '../api/endpoints';
import { Badge, Breadcrumbs, Button, Card, Field, Input, Pagination, Select, Textarea } from '../components/ui';
import { EmptyState, ErrorState } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import type { DatasetCellValue, DatasetImportPreview, DatasetItem, DatasetRecordItem, DatasetVariableItem, PageResponse, ValidationIssue } from '../types/api';
import { displayValue, pageContent } from '../utils/collections';

const pageSizes = [10, 25, 50, 100];
const LazyChartSpecRenderer = lazy(() => import('./VisualizationRenderers').then((module) => ({ default: module.ChartSpecRenderer })));
const measurementLevels = ['NOMINAL', 'ORDINAL', 'INTERVAL', 'RATIO', 'TEXT', 'UNKNOWN'];
const variableTypes = ['STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'CATEGORY', 'ORDINAL', 'TEXT'];
const missingPolicies = ['EXCLUDE_ANALYSIS_BY_ANALYSIS', 'LISTWISE_DELETE', 'PAIRWISE_DELETE', 'INCLUDE_AS_CATEGORY'];
const analysisTypes = ['DESCRIPTIVE', 'FREQUENCY', 'CROSSTAB', 'T_TEST', 'PAIRED_T_TEST', 'ANOVA', 'CHI_SQUARE', 'CORRELATION', 'REGRESSION', 'VISUALIZATION'];

export function DatasetWorkbenchPage() {
  const projectId = useProjectId();
  const [selectedDatasetId, setSelectedDatasetId] = useState('');
  const [tab, setTab] = useState('Overview');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const datasets = useQuery({
    queryKey: ['datasets', projectId, page, size],
    queryFn: () => datasetApi.list(projectId, page, size),
    enabled: Boolean(projectId),
  });
  const selected = pageContent(datasets.data).find((dataset) => dataset.id === selectedDatasetId) ?? pageContent(datasets.data)[0];
  const datasetId = selected?.id ?? '';

  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Data']} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Dataset workbench</h1>
          <p className="muted">Dataset records are read through server-side pagination. Missing values stay visibly missing.</p>
        </div>
        <DatasetCreateForm projectId={projectId} />
      </div>
      {datasets.isError ? <ErrorState title="Datasets failed to load" error={datasets.error} /> : null}
      <div className="grid cols-2">
        <Card>
          <div className="page-header">
            <h2>Datasets</h2>
            <Select aria-label="Dataset page size" value={size} onChange={(event) => { setPage(0); setSize(Number(event.target.value)); }}>
              {pageSizes.map((value) => <option key={value} value={value}>{value} rows</option>)}
            </Select>
          </div>
          <DatasetList datasets={datasets.data} selectedId={datasetId} onSelect={setSelectedDatasetId} />
          <Pagination page={page} totalPages={datasets.data?.totalPages ?? 1} onPageChange={setPage} />
        </Card>
        <DatasetImportWizard projectId={projectId} />
      </div>
      {selected ? (
        <Card className="workbench-card">
          <div className="page-header">
            <div>
              <h2>{selected.name}</h2>
              <p className="muted">{displayValue(selected.description, 'No description provided')}</p>
            </div>
            <div className="toolbar">
              <Badge tone={statusTone(selected.status)}>{displayValue(selected.status, 'DRAFT')}</Badge>
              <Button type="button" variant="secondary">Rename/Edit metadata</Button>
              <Button type="button" variant="secondary">Analyze</Button>
              <Button type="button" variant="secondary">Archive</Button>
            </div>
          </div>
          <Tabs tabs={['Overview', 'Variables', 'Data', 'Validation', 'Analysis History']} active={tab} onChange={setTab} />
          {tab === 'Overview' ? <DatasetOverview datasetId={datasetId} dataset={selected} /> : null}
          {tab === 'Variables' ? <VariableManager datasetId={datasetId} /> : null}
          {tab === 'Data' ? <DatasetGrid datasetId={datasetId} /> : null}
          {tab === 'Validation' ? <DatasetValidation datasetId={datasetId} /> : null}
          {tab === 'Analysis History' ? <AnalysisHistory projectId={projectId} datasetId={datasetId} /> : null}
        </Card>
      ) : <EmptyState title="No datasets yet" description="Upload CSV/XLSX or create a dataset to begin analysis." />}
    </section>
  );
}

function DatasetList({ datasets, selectedId, onSelect }: { datasets?: PageResponse<DatasetItem>; selectedId?: string; onSelect: (id: string) => void }) {
  const rows = pageContent(datasets);
  if (!rows.length) return <EmptyState title="No datasets" description="Use the import workflow to create the first dataset." />;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Name</th><th>Status</th><th>Records</th><th>Variables</th><th>Source</th><th>Updated</th><th>Actions</th></tr></thead>
        <tbody>
          {rows.map((dataset) => (
            <tr key={dataset.id} className={dataset.id === selectedId ? 'selected-row' : undefined}>
              <td><strong>{dataset.name}</strong><p className="muted">{displayValue(dataset.description, 'No description')}</p></td>
              <td><Badge tone={statusTone(dataset.status)}>{displayValue(dataset.status, 'DRAFT')}</Badge></td>
              <td>{displayValue(dataset.recordCount, 'Use summary')}</td>
              <td>{displayValue(dataset.variableCount, 'Use summary')}</td>
              <td>{displayValue(dataset.sourceType, 'MANUAL')}</td>
              <td>{displayValue(dataset.updatedAt ?? dataset.createdAt, 'Not returned')}</td>
              <td><Button type="button" variant="secondary" onClick={() => onSelect(dataset.id)}>Open</Button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DatasetCreateForm({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const create = useMutation({
    mutationFn: () => datasetApi.create(projectId, { name, description, sourceType: 'MANUAL' }),
    onSuccess: () => {
      setName('');
      setDescription('');
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['datasets', projectId] });
    },
  });
  if (!open) return <Button type="button" onClick={() => setOpen(true)}>New dataset</Button>;
  return (
    <form className="form compact-form" onSubmit={(event) => { event.preventDefault(); create.mutate(); }}>
      <Field label="Dataset name"><Input value={name} onChange={(event) => setName(event.target.value)} /></Field>
      <Field label="Description"><Input value={description} onChange={(event) => setDescription(event.target.value)} /></Field>
      <div className="toolbar"><Button type="submit" disabled={!name || create.isPending}>Create</Button><Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button></div>
    </form>
  );
}

function DatasetImportWizard({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const [step, setStep] = useState(1);
  const [importJobId, setImportJobId] = useState('');
  const preview = useQuery({
    queryKey: ['dataset-import-preview', importJobId],
    queryFn: () => datasetApi.importPreview(importJobId),
    enabled: Boolean(importJobId),
  });
  const upload = useMutation({
    mutationFn: (file: File) => datasetApi.startImport(projectId, file),
    onSuccess: (data) => {
      setImportJobId(data.importJobId);
      setStep(2);
    },
  });
  const confirm = useMutation({
    mutationFn: () => datasetApi.confirmImport(importJobId, { mappings: buildMappings(preview.data) }),
    onSuccess: () => {
      setStep(6);
      void queryClient.invalidateQueries({ queryKey: ['datasets', projectId] });
    },
  });
  return (
    <Card>
      <h2>Import workflow</h2>
      <Stepper steps={['Upload CSV/XLSX', 'Preview', 'Map columns', 'Configure types', 'Validation', 'Confirm import']} active={step} />
      {step === 1 ? (
        <Field label="CSV or XLSX file">
          <Input type="file" accept=".csv,.xlsx" onChange={(event) => {
            const file = event.currentTarget.files?.[0];
            if (file) upload.mutate(file);
          }} />
        </Field>
      ) : null}
      {upload.isError ? <ErrorState title="Import upload failed" error={upload.error} /> : null}
      {preview.isError ? <ErrorState title="Import preview failed" error={preview.error} /> : null}
      {preview.data ? <ImportPreview preview={preview.data} /> : null}
      {preview.data ? (
        <div className="toolbar">
          <Button type="button" variant="secondary" onClick={() => setStep(Math.min(step + 1, 5))}>Next step</Button>
          <Button type="button" onClick={() => confirm.mutate()} disabled={confirm.isPending}>Confirm import</Button>
        </div>
      ) : null}
      {confirm.data ? <div className="alert info">Import status: {confirm.data.status}. Dataset ID: {confirm.data.datasetId}</div> : null}
    </Card>
  );
}

function ImportPreview({ preview }: { preview: DatasetImportPreview }) {
  return (
    <div className="grid">
      <div className="alert info">Preview uses backend inference. Invalid data is not silently coerced.</div>
      {preview.warnings?.map((warning) => <div className="alert warning" key={warning}>{warning}</div>)}
      <div className="table-wrap">
        <table>
          <thead><tr><th>Column</th><th>Variable</th><th>Type</th><th>Measurement</th><th>Missing</th><th>Distinct</th><th>Samples</th><th>Validation</th></tr></thead>
          <tbody>
            {preview.columns.map((column) => (
              <tr key={column.sourceColumn}>
                <td>{column.sourceColumn}</td>
                <td>{column.inferredVariableName}</td>
                <td>{column.inferredType}</td>
                <td>{column.suggestedMeasurementLevel}</td>
                <td>{column.missingCount}</td>
                <td>{column.distinctCount}</td>
                <td>{column.sampleValues?.join(', ')}</td>
                <td>{column.warnings?.length ? column.warnings.map((w) => <Badge key={w} tone="warning">{w}</Badge>) : <Badge tone="info">INFO</Badge>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <PreviewRows rows={preview.sampleRows} />
    </div>
  );
}

function PreviewRows({ rows }: { rows: Record<string, string>[] }) {
  const headers = Object.keys(rows[0] ?? {});
  if (!rows.length) return <EmptyState title="No preview rows returned" />;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead>
        <tbody>{rows.slice(0, 5).map((row, index) => <tr key={index}>{headers.map((header) => <td key={header}>{displayValue(row[header], 'Missing')}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}

function buildMappings(preview?: DatasetImportPreview) {
  return (preview?.columns ?? []).map((column) => ({
    sourceColumn: column.sourceColumn,
    proposedVariableName: column.inferredVariableName,
    targetType: column.inferredType ?? 'STRING',
    measurementLevel: column.suggestedMeasurementLevel ?? 'UNKNOWN',
    ignored: false,
  }));
}

function DatasetOverview({ datasetId, dataset }: { datasetId: string; dataset: DatasetItem }) {
  const summary = useQuery({ queryKey: ['dataset-summary', datasetId], queryFn: () => datasetApi.summary(datasetId), enabled: Boolean(datasetId) });
  return (
    <div className="grid cols-3">
      <Metric label="Records" value={summary.data?.recordCount ?? dataset.recordCount ?? 'Not returned'} />
      <Metric label="Variables" value={summary.data?.variableCount ?? dataset.variableCount ?? 'Not returned'} />
      <Metric label="Source/import type" value={dataset.sourceType ?? 'MANUAL'} />
      <Card><h3>Missing values</h3><KeyValueRows data={summary.data?.missingValuesByVariable} /></Card>
      <Card><h3>Numeric ranges</h3><KeyValueRows data={summary.data?.numericRanges} /></Card>
      <Card><h3>Frequencies</h3><KeyValueRows data={summary.data?.categoryCounts} /></Card>
    </div>
  );
}

function VariableManager({ datasetId }: { datasetId: string }) {
  const queryClient = useQueryClient();
  const variables = useQuery({ queryKey: ['dataset-variables', datasetId], queryFn: () => datasetApi.variables(datasetId), enabled: Boolean(datasetId) });
  const [form, setForm] = useState({ variableName: '', label: '', type: 'STRING', measurementLevel: 'UNKNOWN', nullable: true, missingValueCode: '' });
  const add = useMutation({
    mutationFn: () => datasetApi.addVariable(datasetId, form),
    onSuccess: () => {
      setForm({ variableName: '', label: '', type: 'STRING', measurementLevel: 'UNKNOWN', nullable: true, missingValueCode: '' });
      void queryClient.invalidateQueries({ queryKey: ['dataset-variables', datasetId] });
    },
  });
  return (
    <div className="grid">
      <form className="grid cols-3" onSubmit={(event) => { event.preventDefault(); add.mutate(); }}>
        <Field label="Variable name"><Input value={form.variableName} onChange={(event) => setForm({ ...form, variableName: event.target.value })} /></Field>
        <Field label="Label"><Input value={form.label} onChange={(event) => setForm({ ...form, label: event.target.value })} /></Field>
        <Field label="Storage type"><Select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>{variableTypes.map((type) => <option key={type}>{type}</option>)}</Select></Field>
        <Field label="Measurement level"><Select value={form.measurementLevel} onChange={(event) => setForm({ ...form, measurementLevel: event.target.value })}>{measurementLevels.map((level) => <option key={level}>{level}</option>)}</Select></Field>
        <Field label="Missing value code"><Input value={form.missingValueCode} onChange={(event) => setForm({ ...form, missingValueCode: event.target.value })} /></Field>
        <label className="field"><span className="label">Nullable</span><input type="checkbox" checked={form.nullable} onChange={(event) => setForm({ ...form, nullable: event.target.checked })} /></label>
        <Button type="submit" disabled={!form.variableName || !form.label || add.isPending}>Add variable</Button>
      </form>
      {add.isError ? <ErrorState title="Variable was not created" error={add.error} /> : null}
      <VariableTable variables={variables.data ?? []} />
    </div>
  );
}

function VariableTable({ variables }: { variables: DatasetVariableItem[] }) {
  if (!variables.length) return <EmptyState title="No variables" description="Variables can be created manually or from imports." />;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Name</th><th>Label</th><th>Storage</th><th>Measurement</th><th>Missing config</th><th>Instrument item</th></tr></thead>
        <tbody>
          {variables.map((variable) => (
            <tr key={variable.id}>
              <td><strong>{variable.variableName}</strong></td>
              <td>{displayValue(variable.label)}</td>
              <td>{variable.type}</td>
              <td>{variable.measurementLevel}</td>
              <td>{variable.nullable ? 'Nullable' : 'Required'} {variable.missingValueCode ? `- ${variable.missingValueCode}` : ''}</td>
              <td>{displayValue(variable.sourceInstrumentItemId, 'Not linked')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DatasetGrid({ datasetId }: { datasetId: string }) {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const variables = useQuery({ queryKey: ['dataset-variables', datasetId], queryFn: () => datasetApi.variables(datasetId), enabled: Boolean(datasetId) });
  const records = useQuery({ queryKey: ['dataset-records', datasetId, page, size], queryFn: () => datasetApi.records(datasetId, page, size), enabled: Boolean(datasetId) });
  return (
    <div className="grid">
      <div className="toolbar">
        <Select aria-label="Dataset record page size" value={size} onChange={(event) => { setPage(0); setSize(Number(event.target.value)); }}>{pageSizes.map((value) => <option key={value} value={value}>{value} rows</option>)}</Select>
        <span className="muted">Sort is server-owned where supported. Page size is bounded to protect large datasets.</span>
      </div>
      {records.isError ? <ErrorState title="Dataset records failed to load" error={records.error} /> : null}
      <div className="data-grid-wrap">
        <table className="data-grid">
          <thead><tr><th className="sticky-col">Row</th>{(variables.data ?? []).map((variable) => <th key={variable.id} title={variable.label}>{variable.variableName}<span className="muted block">{variable.type}</span></th>)}</tr></thead>
          <tbody>
            {pageContent(records.data).map((record) => <DatasetRecordRow key={record.id} record={record} variables={variables.data ?? []} />)}
          </tbody>
        </table>
      </div>
      {!pageContent(records.data).length ? <EmptyState title="No records on this page" /> : null}
      <Pagination page={page} totalPages={records.data?.totalPages ?? 1} onPageChange={setPage} />
    </div>
  );
}

function DatasetRecordRow({ record, variables }: { record: DatasetRecordItem; variables: DatasetVariableItem[] }) {
  const byName = new Map(record.values.map((value) => [value.variableName, value]));
  return <tr><td className="sticky-col">{record.rowNumber}</td>{variables.map((variable) => <td key={variable.id}>{renderCell(byName.get(variable.variableName))}</td>)}</tr>;
}

function renderCell(value?: DatasetCellValue) {
  if (!value || value.missing || value.value === null || value.value === undefined || value.value === '') {
    return <span className="missing-value">Missing{value?.missingReason ? ` (${value.missingReason})` : ''}</span>;
  }
  if (value.type === 'BOOLEAN') return <Badge tone={String(value.value) === 'true' ? 'success' : 'warning'}>{String(value.value)}</Badge>;
  return String(value.value);
}

function DatasetValidation({ datasetId }: { datasetId: string }) {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const issues = useQuery({ queryKey: ['dataset-validation-issues', datasetId, page], queryFn: () => datasetApi.issues(datasetId, page, 20), enabled: Boolean(datasetId) });
  const validate = useMutation({
    mutationFn: () => datasetApi.validate(datasetId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['dataset-validation-issues', datasetId] }),
  });
  return (
    <div className="grid">
      <div className="toolbar"><Button type="button" onClick={() => validate.mutate()} disabled={validate.isPending}>Run validation</Button><span className="muted">Errors, warnings, and info come from backend validation.</span></div>
      <ValidationIssueTable issues={pageContent(issues.data)} />
      <Pagination page={page} totalPages={issues.data?.totalPages ?? 1} onPageChange={setPage} />
    </div>
  );
}

function ValidationIssueTable({ issues }: { issues: ValidationIssue[] }) {
  if (!issues.length) return <EmptyState title="No validation issues returned" />;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Severity</th><th>Row</th><th>Column</th><th>Code</th><th>Message</th><th>Rejected value</th></tr></thead>
        <tbody>{issues.map((issue, index) => <tr key={issue.id ?? index}><td><Badge tone={severityTone(issue.severity)}>{issue.severity}</Badge></td><td>{displayValue(issue.rowNumber)}</td><td>{displayValue(issue.variableId)}</td><td>{issue.code}</td><td>{issue.message}</td><td>{displayValue(issue.rejectedValueSnapshot)}</td></tr>)}</tbody>
      </table>
    </div>
  );
}

export function AnalysisWorkbenchPage() {
  const projectId = useProjectId();
  const [tab, setTab] = useState('Descriptive');
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Analysis']} />
      <h1 className="page-title">Analysis workbench</h1>
      <Tabs tabs={['Descriptive', 'Test Planner', 'Inferential', 'Visualizations', 'Analysis History', 'Qualitative']} active={tab} onChange={setTab} />
      {tab === 'Descriptive' ? <DescriptiveAnalysisPanel projectId={projectId} /> : null}
      {tab === 'Test Planner' ? <StatisticalTestPlanner projectId={projectId} /> : null}
      {tab === 'Inferential' ? <InferentialResultsPanel projectId={projectId} /> : null}
      {tab === 'Visualizations' ? <VisualizationPanel projectId={projectId} /> : null}
      {tab === 'Analysis History' ? <AnalysisHistory projectId={projectId} /> : null}
      {tab === 'Qualitative' ? <QualitativeWorkbenchPage embedded /> : null}
    </section>
  );
}

function DescriptiveAnalysisPanel({ projectId }: { projectId: string }) {
  return <AnalysisPlanWizard projectId={projectId} defaultType="DESCRIPTIVE" helper="Request descriptive statistics, frequencies, or crosstabs from the backend statistical engine." />;
}

function StatisticalTestPlanner({ projectId }: { projectId: string }) {
  return (
    <div className="grid cols-2">
      <AnalysisPlanWizard projectId={projectId} defaultType="T_TEST" helper="The planner records design information and shows backend recommendations when a completed run returns them." />
      <Card>
        <h2>Recommendation preview</h2>
        <p className="muted">Backend recommendation fields are displayed here: recommended test, rationale, assumptions, warnings, and blocking problems.</p>
        <AssumptionChecks checks={[{ name: 'Normality', status: 'NOT_TESTED' }, { name: 'Homogeneity', status: 'NOT_TESTED' }, { name: 'Expected cell counts', status: 'NOT_APPLICABLE' }]} />
      </Card>
    </div>
  );
}

function AnalysisPlanWizard({ projectId, defaultType, helper }: { projectId: string; defaultType: string; helper: string }) {
  const queryClient = useQueryClient();
  const datasets = useQuery({ queryKey: ['datasets', projectId, 0, 100], queryFn: () => datasetApi.list(projectId, 0, 100), enabled: Boolean(projectId) });
  const [datasetId, setDatasetId] = useState('');
  const [analysisType, setAnalysisType] = useState(defaultType);
  const [variables, setVariables] = useState('');
  const [alpha, setAlpha] = useState('0.05');
  const [missingPolicy, setMissingPolicy] = useState(missingPolicies[0]);
  const [settings, setSettings] = useState('');
  const variableQuery = useQuery({ queryKey: ['dataset-variables', datasetId], queryFn: () => datasetApi.variables(datasetId), enabled: Boolean(datasetId) });
  const create = useMutation({
    mutationFn: () => analysisApi.createRun(projectId, {
      datasetId,
      title: `${analysisType} plan`,
      analysisType,
      methodDescription: helper,
      parametersJson: JSON.stringify({ variables: variables.split(',').map((value) => value.trim()).filter(Boolean), alpha: Number(alpha), missingPolicy, settings }),
    }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['analysis-runs', projectId] }),
  });
  return (
    <Card>
      <h2>Create analysis plan</h2>
      <Stepper steps={['Dataset', 'Analysis type', 'Variables', 'Missing-data policy', 'Additional settings', 'Validate', 'Run']} active={4} />
      <p className="muted">{helper}</p>
      <form className="grid cols-2" onSubmit={(event) => { event.preventDefault(); create.mutate(); }}>
        <Field label="Dataset"><Select value={datasetId} onChange={(event) => setDatasetId(event.target.value)}><option value="">Select dataset</option>{pageContent(datasets.data).map((dataset) => <option key={dataset.id} value={dataset.id}>{dataset.name}</option>)}</Select></Field>
        <Field label="Analysis type"><Select value={analysisType} onChange={(event) => setAnalysisType(event.target.value)}>{analysisTypes.map((type) => <option key={type}>{type}</option>)}</Select></Field>
        <Field label="Variable roles"><Select multiple value={variables ? variables.split(',') : []} onChange={(event) => setVariables(Array.from(event.currentTarget.selectedOptions).map((option) => option.value).join(','))}>{(variableQuery.data ?? []).map((variable) => <option key={variable.id} value={variable.variableName}>{variable.variableName} - {variable.measurementLevel}</option>)}</Select></Field>
        <Field label="Missing-data policy"><Select value={missingPolicy} onChange={(event) => setMissingPolicy(event.target.value)}>{missingPolicies.map((policy) => <option key={policy}>{policy}</option>)}</Select></Field>
        <Field label="Alpha"><Input inputMode="decimal" value={alpha} onChange={(event) => setAlpha(event.target.value)} /></Field>
        <Field label="Additional settings"><Input value={settings} onChange={(event) => setSettings(event.target.value)} placeholder="Grouping, paired variables, controls, weights" /></Field>
        <div className="toolbar"><Button type="submit" disabled={!datasetId || create.isPending}>Validate and create run</Button><Badge tone="info">Backend authoritative</Badge></div>
      </form>
      {create.isError ? <ErrorState title="Analysis plan was not created" error={create.error} /> : null}
    </Card>
  );
}

function InferentialResultsPanel({ projectId }: { projectId: string }) {
  const runs = useQuery({ queryKey: ['analysis-runs', projectId, 0, 50], queryFn: () => analysisApi.runs(projectId, 0, 50), enabled: Boolean(projectId) });
  return (
    <div className="grid">
      {pageContent(runs.data).map((run) => <AnalysisRunCard key={String(run.id)} run={run} />)}
      {!pageContent(runs.data).length ? <EmptyState title="No analysis runs" description="Create a plan and run it through the backend engine." /> : null}
    </div>
  );
}

function AnalysisRunCard({ run }: { run: Record<string, unknown> }) {
  const payload = parseJson<Record<string, unknown>>(run.resultPayloadJson) ?? run;
  return (
    <Card>
      <div className="page-header">
        <div><h2>{displayValue(run.title ?? payload.title, 'Analysis run')}</h2><p className="muted">Run ID: {displayValue(run.id)} | Dataset: {displayValue(run.datasetId)} | Executed: {displayValue(run.completedAt ?? run.startedAt)}</p></div>
        <Badge tone={statusTone(String(run.status ?? payload.status ?? ''))}>{displayValue(run.status ?? payload.status, 'DRAFT')}</Badge>
      </div>
      <ResultRenderer payload={payload} type={String(run.analysisType ?? payload.analysisType ?? payload.type ?? '')} />
    </Card>
  );
}

export function ResultRenderer({ payload, type }: { payload: Record<string, unknown>; type: string }) {
  const normalizedType = type.toUpperCase();
  if (normalizedType.includes('PAIRED')) return <PairedTTestResult payload={payload} />;
  if (normalizedType.includes('T_TEST')) return <TTestResult payload={payload} />;
  if (normalizedType.includes('ANOVA')) return <AnovaResult payload={payload} />;
  if (normalizedType.includes('CHI')) return <ChiSquareResult payload={payload} />;
  if (normalizedType.includes('CORRELATION')) return <CorrelationResult payload={payload} />;
  if (normalizedType.includes('REGRESSION')) return <RegressionResult payload={payload} />;
  if (normalizedType.includes('FREQUENCY')) return <FrequencyResult payload={payload} />;
  if (normalizedType.includes('CROSSTAB')) return <CrosstabResult payload={payload} />;
  return <DescriptiveResult payload={payload} />;
}

function DescriptiveResult({ payload }: { payload: Record<string, unknown> }) {
  const rows = asRows(payload.numericResults ?? payload.descriptiveStatistics ?? payload.variables ?? payload.results);
  return <ResultTable title="Descriptive statistics" rows={rows} preferred={['variable', 'n', 'validN', 'missing', 'mean', 'median', 'minimum', 'maximum', 'range', 'variance', 'standardDeviation', 'quartiles', 'iqr']} />;
}

function FrequencyResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title="Frequencies" rows={asRows(payload.frequencies ?? payload.results)} preferred={['category', 'frequency', 'percentage', 'validPercentage', 'cumulativePercentage']} />;
}

function CrosstabResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title="Crosstab" rows={asRows(payload.cells ?? payload.table ?? payload.results)} preferred={['row', 'column', 'count', 'rowPercentage', 'columnPercentage', 'overallPercentage']} />;
}

function TTestResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title={payload.welch ? 'Independent samples t-test - Welch' : 'Independent samples t-test'} rows={asRows(payload.groups ?? payload.results ?? [payload])} preferred={['group', 'n', 'mean', 'standardDeviation', 'meanDifference', 't', 'df', 'p', 'effectSize', 'confidenceInterval']} />;
}

function PairedTTestResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title="Paired t-test" rows={asRows(payload.results ?? [payload])} preferred={['pairCount', 'meanDifference', 'sdDifference', 't', 'df', 'p']} />;
}

function AnovaResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title="ANOVA" rows={asRows(payload.groups ?? payload.results ?? [payload])} preferred={['group', 'n', 'mean', 'standardDeviation', 'f', 'betweenDf', 'withinDf', 'p', 'effectSize']} />;
}

function ChiSquareResult({ payload }: { payload: Record<string, unknown> }) {
  return <div className="grid"><ResultTable title="Chi-square" rows={asRows(payload.cells ?? payload.results ?? [payload])} preferred={['row', 'column', 'count', 'expected', 'chiSquare', 'df', 'p', 'cramersV']} />{String(payload.expectedCountWarning ?? '') ? <div className="alert warning">{String(payload.expectedCountWarning)}</div> : null}</div>;
}

function CorrelationResult({ payload }: { payload: Record<string, unknown> }) {
  return <div className="grid"><ResultTable title="Correlation" rows={asRows(payload.results ?? [payload])} preferred={['n', 'r', 'rho', 'p', 'confidenceInterval']} /><p className="muted">Correlation is not displayed as causation.</p></div>;
}

function RegressionResult({ payload }: { payload: Record<string, unknown> }) {
  return <ResultTable title="Regression" rows={asRows(payload.coefficients ?? payload.results ?? [payload])} preferred={['term', 'n', 'intercept', 'coefficient', 'standardError', 't', 'p', 'rSquared', 'adjustedRSquared', 'modelStatistic']} />;
}

function ResultTable({ title, rows, preferred }: { title: string; rows: Record<string, unknown>[]; preferred: string[] }) {
  const headers = preferred.filter((header) => rows.some((row) => row[header] !== undefined));
  const allHeaders = headers.length ? headers : Array.from(new Set(rows.flatMap((row) => Object.keys(row)))).slice(0, 8);
  return (
    <div>
      <h3>{title}</h3>
      {!rows.length ? <EmptyState title="No structured result rows returned" /> : (
        <div className="table-wrap">
          <table><thead><tr>{allHeaders.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{allHeaders.map((header) => <td key={header}>{displayValue(row[header])}</td>)}</tr>)}</tbody></table>
        </div>
      )}
      {payloadDecision(rows)}
    </div>
  );
}

function payloadDecision(rows: Record<string, unknown>[]) {
  const decision = rows.find((row) => row.decision)?.decision;
  if (!decision) return null;
  const text = String(decision);
  const safe = text.toUpperCase().includes('ACCEPT') ? 'Insufficient evidence to reject null hypothesis' : text;
  return <div className="alert info">Decision: {safe}</div>;
}

function AssumptionChecks({ checks }: { checks: { name: string; status: string; message?: string }[] }) {
  return <div className="grid">{checks.map((check) => <div className="stat" key={check.name}><span>{check.name}</span><Badge tone={assumptionTone(check.status)}>{check.status}</Badge>{check.message ? <span className="muted">{check.message}</span> : null}</div>)}</div>;
}

function VisualizationPanel({ projectId }: { projectId: string }) {
  const runs = useQuery({ queryKey: ['analysis-runs', projectId, 0, 50], queryFn: () => analysisApi.runs(projectId, 0, 50), enabled: Boolean(projectId) });
  const specs = pageContent(runs.data).map((run) => parseJson<Record<string, unknown>>(run.resultPayloadJson)?.visualizationSpec).filter(Boolean) as Record<string, unknown>[];
  return (
    <div className="grid cols-2">
      {specs.map((spec, index) => <Suspense key={index} fallback={<Card><div className="skeleton" /></Card>}><LazyChartSpecRenderer spec={spec} /></Suspense>)}
      {!specs.length ? <Card><EmptyState title="No server visualization specifications" description="Charts render only backend-provided data/specifications." /></Card> : null}
    </div>
  );
}

function AnalysisHistory({ projectId, datasetId }: { projectId: string; datasetId?: string }) {
  const [page, setPage] = useState(0);
  const runs = useQuery({ queryKey: ['analysis-runs', projectId, page], queryFn: () => analysisApi.runs(projectId, page, 20), enabled: Boolean(projectId) });
  const rows = datasetId ? pageContent(runs.data).filter((run) => run.datasetId === datasetId) : pageContent(runs.data);
  return (
    <div className="grid">
      <div className="table-wrap"><table><thead><tr><th>Run</th><th>Dataset revision/snapshot</th><th>Method</th><th>Alpha</th><th>Missing policy</th><th>Status</th><th>Timestamp</th></tr></thead><tbody>{rows.map((run) => {
        const params = parseJson<Record<string, unknown>>(run.parametersJson) ?? {};
        return <tr key={String(run.id)}><td>{displayValue(run.id)}</td><td>{displayValue(run.datasetId)}</td><td>{displayValue(run.analysisType ?? run.methodDescription)}</td><td>{displayValue(params.alpha)}</td><td>{displayValue(params.missingPolicy)}</td><td><Badge tone={statusTone(String(run.status))}>{displayValue(run.status)}</Badge></td><td>{displayValue(run.completedAt ?? run.startedAt)}</td></tr>;
      })}</tbody></table></div>
      <Pagination page={page} totalPages={runs.data?.totalPages ?? 1} onPageChange={setPage} />
    </div>
  );
}

export function QualitativeWorkbenchPage({ embedded = false }: { embedded?: boolean }) {
  const projectId = useProjectId();
  const [selectedText, setSelectedText] = useState('');
  const [code, setCode] = useState('');
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className={embedded ? 'grid' : 'page'}>
      {!embedded ? <Breadcrumbs items={['Projects', projectId, 'Qualitative']} /> : null}
      <h1 className="page-title">Qualitative analysis</h1>
      <div className="alert warning">No qualitative backend controller was found during inspection. This workspace does not persist codes, themes, or memos until backend APIs are added.</div>
      <div className="qual-layout">
        <Card><h2>Sources</h2>{['P-000014', 'SES-00032', 'Interview Source 03'].map((source) => <button className="source-row" type="button" key={source}>{source}</button>)}</Card>
        <Card>
          <h2>Text / coding workspace</h2>
          <p className="source-text" onMouseUp={() => setSelectedText(window.getSelection()?.toString() ?? '')}>This area preserves the exact source snapshot for offset-safe coding. Select source text, then apply a code after backend qualitative APIs are available.</p>
          <Field label="Selected excerpt"><Textarea value={selectedText} onChange={(event) => setSelectedText(event.target.value)} /></Field>
          <div className="toolbar"><Button type="button" disabled>Apply Code</Button><Badge tone="warning">Backend unavailable</Badge></div>
        </Card>
        <Card>
          <h2>Codes / themes / memos</h2>
          <Field label="Code"><Input value={code} onChange={(event) => setCode(event.target.value)} /></Field>
          <CodebookPreview />
          <div className="toolbar"><Button type="button" variant="secondary" disabled>Suggest Codes with real AI</Button><Button type="button" variant="secondary" disabled>Suggest Themes with real AI</Button></div>
          <p className="muted">AI suggestions remain disabled until a real backend AI qualitative endpoint exists. No mock AI is used.</p>
        </Card>
      </div>
    </section>
  );
}

function CodebookPreview() {
  return (
    <div className="grid">
      {['Create code', 'Edit definition', 'Archive code', 'Show coded excerpts', 'Create finding from theme'].map((item) => <div className="panel" key={item}><Badge tone="info">{item}</Badge><p className="muted">Requires qualitative backend support.</p></div>)}
    </div>
  );
}

export function FindingsWorkbenchPage() {
  const projectId = useProjectId();
  const [tab, setTab] = useState('Findings');
  if (!projectId) return <EmptyState title="Select a project" />;
  return (
    <section className="page">
      <Breadcrumbs items={['Projects', projectId, 'Findings']} />
      <h1 className="page-title">Findings and interpretation</h1>
      <Tabs tabs={['Findings', 'Discussion', 'Conclusions', 'Recommendations', 'Traceability']} active={tab} onChange={setTab} />
      {tab === 'Findings' ? <FindingsPanel projectId={projectId} /> : null}
      {tab === 'Discussion' ? <DiscussionPanel projectId={projectId} /> : null}
      {tab === 'Conclusions' ? <ConclusionPanel projectId={projectId} /> : null}
      {tab === 'Recommendations' ? <RecommendationPanel projectId={projectId} /> : null}
      {tab === 'Traceability' ? <TraceabilityPanel projectId={projectId} /> : null}
    </section>
  );
}

function FindingsPanel({ projectId }: { projectId: string }) {
  const findings = useQuery({ queryKey: ['findings', projectId, 0, 50], queryFn: () => analysisApi.findings(projectId, 0, 50), enabled: Boolean(projectId) });
  const generate = useMutation({ mutationFn: () => analysisApi.generateFinding(projectId, { objectiveId: crypto.randomUUID(), analysisResultIds: [crypto.randomUUID()], instructions: 'Draft from selected backend-authoritative analysis results.' }) });
  return (
    <div className="grid cols-2">
      <Card><h2>Create quantitative finding</h2><p className="muted">Choose objective, question, optional hypothesis, and analysis result IDs from backend records. AI drafts cannot alter result numbers.</p><div className="toolbar"><Button type="button" variant="secondary" onClick={() => generate.mutate()}>Generate Draft with AI</Button><Button type="button">Write manually</Button></div>{generate.data ? <DraftPanel draft={generate.data} /> : null}{generate.isError ? <ErrorState title="AI finding draft failed" error={generate.error} /> : null}</Card>
      <RecordTable title="Findings" rows={pageContent(findings.data)} columns={['title', 'type', 'status', 'questionId', 'hypothesisId', 'analysisResultIds', 'origin', 'revisionNumber']} />
    </div>
  );
}

function DiscussionPanel({ projectId }: { projectId: string }) {
  const findings = useQuery({ queryKey: ['findings', projectId, 0, 20], queryFn: () => analysisApi.findings(projectId, 0, 20), enabled: Boolean(projectId) });
  return <div className="grid">{pageContent(findings.data).map((finding) => <Card key={String(finding.id)}><h2>{displayValue(finding.title, 'Finding')}</h2><p>{displayValue(finding.findingText)}</p><Badge tone="info">{displayValue(finding.objectiveId, 'Objective not linked')}</Badge><p className="muted">Discussion generation uses verified literature evidence and server citation metadata when backend returns it.</p></Card>)}</div>;
}

function ConclusionPanel({ projectId }: { projectId: string }) {
  const conclusions = useQuery({ queryKey: ['conclusions', projectId, 0, 50], queryFn: () => analysisApi.conclusions(projectId, 0, 50), enabled: Boolean(projectId) });
  const generate = useMutation({ mutationFn: () => analysisApi.generateConclusion(projectId) });
  return <div className="grid"><div className="toolbar"><Button type="button" variant="secondary" onClick={() => generate.mutate()}>Generate from approved findings</Button></div>{generate.data ? <DraftPanel draft={generate.data} /> : null}<RecordTable title="Conclusions" rows={pageContent(conclusions.data)} columns={['title', 'type', 'status', 'objectiveId', 'findingIds', 'revisionNumber']} /></div>;
}

function RecommendationPanel({ projectId }: { projectId: string }) {
  const recommendations = useQuery({ queryKey: ['recommendations', projectId, 0, 50], queryFn: () => analysisApi.recommendations(projectId, 0, 50), enabled: Boolean(projectId) });
  const generate = useMutation({ mutationFn: () => analysisApi.generateRecommendation(projectId) });
  return <div className="grid"><div className="toolbar"><Button type="button" variant="secondary" onClick={() => generate.mutate()}>Generate recommendations</Button></div>{generate.data ? <DraftPanel draft={generate.data} /> : null}<RecordTable title="Recommendations" rows={pageContent(recommendations.data)} columns={['title', 'type', 'targetAudience', 'priority', 'status', 'findingIds', 'conclusionIds']} /></div>;
}

function TraceabilityPanel({ projectId }: { projectId: string }) {
  const traceability = useQuery({ queryKey: ['traceability', projectId], queryFn: () => analysisApi.traceability(projectId), enabled: Boolean(projectId) });
  const rows = asRows(traceability.data?.rows);
  return (
    <div className="grid">
      {(traceability.data?.projectWarnings as string[] | undefined)?.map((warning) => <div className="alert warning" key={warning}>{warning}</div>)}
      {rows.map((row, index) => <Card key={index}><div className="trace-row"><Badge tone={(row.warnings as string[] | undefined)?.length ? 'warning' : 'success'}>{(row.warnings as string[] | undefined)?.length ? 'WARNING' : 'COMPLETE'}</Badge><strong>{displayValue(row.objectiveText, `Objective ${index + 1}`)}</strong><span>Question/Hypothesis: {displayValue(row.questionId ?? row.hypothesisId)}</span><span>Analysis: {displayValue(row.analysisCount)}</span><span>Finding: {displayValue(row.findingCount)}</span><span>Conclusion: {displayValue(row.conclusionCount)}</span><span>Recommendation: {displayValue(row.recommendationCount)}</span>{(row.warnings as string[] | undefined)?.map((warning) => <div className="alert warning" key={warning}>{warning} <a href={`/app/projects/${projectId}/analysis`}>Go to Analysis</a></div>)}</div></Card>)}
      {!rows.length ? <EmptyState title="No traceability rows" /> : null}
    </div>
  );
}

function DraftPanel({ draft }: { draft: Record<string, unknown> }) {
  return <div className="panel"><Badge tone="info">AI draft</Badge><p>{displayValue(draft.draftText ?? draft.text)}</p><div className="toolbar"><Button type="button">Accept</Button><Button type="button" variant="secondary">Edit</Button><Button type="button" variant="danger">Reject</Button></div></div>;
}

function Tabs({ tabs, active, onChange }: { tabs: string[]; active: string; onChange: (tab: string) => void }) {
  return <div className="tabs" role="tablist">{tabs.map((tab) => <button key={tab} className={tab === active ? 'tab active' : 'tab'} type="button" role="tab" aria-selected={tab === active} onClick={() => onChange(tab)}>{tab}</button>)}</div>;
}

function Stepper({ steps, active }: { steps: string[]; active: number }) {
  return <ol className="stepper">{steps.map((step, index) => <li key={step} className={index + 1 <= active ? 'active' : ''}>{index + 1}. {step}</li>)}</ol>;
}

function Metric({ label, value }: { label: string; value: unknown }) {
  return <Card><span className="muted">{label}</span><p className="metric">{displayValue(value)}</p></Card>;
}

function KeyValueRows({ data }: { data?: Record<string, unknown> }) {
  if (!data || !Object.keys(data).length) return <EmptyState title="No values returned" />;
  return <div className="grid">{Object.entries(data).slice(0, 8).map(([key, value]) => <p key={key}><strong>{key}</strong>: {displayValue(value)}</p>)}</div>;
}

function RecordTable({ title, rows, columns }: { title: string; rows: Record<string, unknown>[]; columns: string[] }) {
  return <Card><h2>{title}</h2>{!rows.length ? <EmptyState title={`No ${title.toLowerCase()}`} /> : <div className="table-wrap"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={String(row.id ?? index)}>{columns.map((column) => <td key={column}>{displayValue(row[column])}</td>)}</tr>)}</tbody></table></div>}</Card>;
}

function asRows(value: unknown): Record<string, unknown>[] {
  if (Array.isArray(value)) return value.filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null);
  if (typeof value === 'object' && value !== null) return [value as Record<string, unknown>];
  return [];
}

function parseJson<T>(value: unknown): T | null {
  if (typeof value !== 'string' || !value.trim()) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function statusTone(status?: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  const normalized = String(status ?? '').toUpperCase();
  if (['READY', 'COMPLETED', 'APPROVED', 'FINAL'].some((value) => normalized.includes(value))) return 'success';
  if (['FAILED', 'ERROR', 'BROKEN', 'REJECTED'].some((value) => normalized.includes(value))) return 'danger';
  if (['WARNING', 'DRAFT', 'PENDING', 'VALIDATING', 'RUNNING'].some((value) => normalized.includes(value))) return 'warning';
  return 'info';
}

function severityTone(severity?: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  if (severity === 'ERROR') return 'danger';
  if (severity === 'WARNING') return 'warning';
  return 'info';
}

function assumptionTone(status: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
  if (status === 'PASS') return 'success';
  if (status === 'FAIL') return 'danger';
  if (status === 'WARNING') return 'warning';
  return 'info';
}
