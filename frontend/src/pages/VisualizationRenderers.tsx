import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Card } from '../components/ui';
import { EmptyState } from '../components/states';
import { displayValue } from '../utils/collections';

export function ChartSpecRenderer({ spec }: { spec: Record<string, unknown> }) {
  const type = String(spec.type ?? 'TABLE').toUpperCase();
  const data = asRows(spec.data ?? spec.rows);
  const xKey = String(spec.xKey ?? 'x');
  const yKey = String(spec.yKey ?? 'y');
  return (
    <Card>
      <h2>{displayValue(spec.title, type)}</h2>
      <div className="chart-frame" role="img" aria-label={String(spec.title ?? type)}>
        <ResponsiveContainer width="100%" height={260}>
          {type === 'LINE' ? <LineChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey={xKey} /><YAxis /><Tooltip /><Line dataKey={yKey} stroke="#155eef" /></LineChart>
            : type === 'SCATTER' ? <ScatterChart><CartesianGrid /><XAxis dataKey={xKey} /><YAxis dataKey={yKey} /><Tooltip /><Scatter data={data} fill="#155eef" /></ScatterChart>
              : <BarChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey={xKey} /><YAxis /><Tooltip /><Bar dataKey={yKey} fill="#155eef">{data.map((_, index) => <Cell key={index} fill={index % 2 ? '#067647' : '#155eef'} />)}</Bar></BarChart>}
        </ResponsiveContainer>
      </div>
      <ChartDataTable rows={data} columns={[xKey, yKey, 'group', 'value']} />
    </Card>
  );
}

function ChartDataTable({ rows, columns }: { rows: Record<string, unknown>[]; columns: string[] }) {
  const headers = columns.filter((column) => rows.some((row) => row[column] !== undefined));
  if (!rows.length) return <EmptyState title="No chart data returned" />;
  return (
    <div className="table-wrap">
      <table><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{headers.map((header) => <td key={header}>{displayValue(row[header])}</td>)}</tr>)}</tbody></table>
    </div>
  );
}

function asRows(value: unknown): Record<string, unknown>[] {
  if (Array.isArray(value)) return value.filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null);
  if (typeof value === 'object' && value !== null) return [value as Record<string, unknown>];
  return [];
}
