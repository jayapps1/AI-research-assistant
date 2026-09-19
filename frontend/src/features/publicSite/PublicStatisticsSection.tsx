import { useQuery } from '@tanstack/react-query';
import { publicApi } from '../../api/public';
import type { PublicStatistic } from '../../types/publicSite';
import {
  BarChart3,
  BookOpen,
  Building,
  CheckCircle2,
  Database,
  FileCheck,
  FileText,
  FolderKanban,
  Layers,
  Sparkles,
  TrendingUp,
  Users,
} from 'lucide-react';

const ICON_MAP: Record<string, React.ElementType> = {
  'folder-kanban': FolderKanban,
  projects: FolderKanban,
  users: Users,
  'file-text': FileText,
  documents: FileText,
  layers: Layers,
  workspaces: Building,
  building: Building,
  'file-check': FileCheck,
  exports: FileCheck,
  database: Database,
  chart: BarChart3,
  sparkles: Sparkles,
  check: CheckCircle2,
  book: BookOpen,
  trending: TrendingUp,
};

function getStatIcon(iconKey?: string) {
  if (!iconKey) return BarChart3;
  const normalized = iconKey.toLowerCase().trim();
  return ICON_MAP[normalized] || BarChart3;
}

export interface PublicStatisticsSectionProps {
  title?: string;
  eyebrow?: string;
  subtitle?: string;
  className?: string;
}

export function PublicStatisticsSection({
  title = 'Platform Scale & Empirical Impact',
  eyebrow = 'Verified Community Metrics',
  subtitle = 'Real-time indicators demonstrating scholarly activity, verified datasets, and methodological outputs across the platform.',
  className = '',
}: PublicStatisticsSectionProps) {
  const { data: stats, isLoading, isError } = useQuery<PublicStatistic[]>({
    queryKey: ['publicStatistics'],
    queryFn: () => publicApi.getStatistics().catch(() => []),
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading || isError || !stats || stats.length === 0) {
    // Graceful hide when no statistics are configured or available
    return null;
  }

  return (
    <section className={`public-section public-statistics-section ${className}`.trim()} id="public-statistics">
      <div className="public-container">
        <div className="section-header text-center" style={{ marginBottom: '40px' }}>
          <span className="section-eyebrow">{eyebrow}</span>
          <h2 className="section-title">{title}</h2>
          {subtitle && <p className="section-subtitle">{subtitle}</p>}
        </div>

        <div
          className="stats-grid"
          style={{
            display: 'grid',
            gridTemplateColumns: `repeat(auto-fit, minmax(200px, 1fr))`,
            gap: '24px',
            alignItems: 'stretch',
          }}
        >
          {stats.map((stat) => {
            const Icon = getStatIcon(stat.iconKey);
            const displayVal = `${stat.prefix || ''}${stat.value}${stat.suffix || ''}`;

            return (
              <div
                key={stat.code}
                className={`stat-card ${stat.featured ? 'featured' : ''}`}
                style={{
                  background: stat.featured
                    ? 'linear-gradient(145deg, var(--surface, #ffffff), var(--surface-2, #f0f3f7))'
                    : 'var(--surface, #ffffff)',
                  border: stat.featured ? '1.5px solid var(--brand, #155eef)' : '1px solid var(--line, #dce2ea)',
                  borderRadius: '12px',
                  padding: '24px 20px',
                  textAlign: 'center',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  position: 'relative',
                  boxShadow: stat.featured ? '0 10px 25px -5px rgba(21, 94, 239, 0.12)' : 'var(--shadow, 0 4px 12px rgba(0,0,0,0.04))',
                  transition: 'transform 180ms ease, box-shadow 180ms ease',
                }}
              >
                {stat.featured && (
                  <span
                    style={{
                      position: 'absolute',
                      top: '-10px',
                      background: 'var(--brand, #155eef)',
                      color: '#ffffff',
                      fontSize: '0.68rem',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: '12px',
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                    }}
                  >
                    Featured
                  </span>
                )}

                <div
                  style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '10px',
                    background: stat.featured ? 'rgba(21, 94, 239, 0.1)' : 'var(--surface-2, #f0f3f7)',
                    color: stat.featured ? 'var(--brand, #155eef)' : 'var(--text, #111827)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '14px',
                  }}
                >
                  <Icon size={22} />
                </div>

                <div
                  className="stat-value"
                  style={{
                    fontSize: '2.2rem',
                    fontWeight: 800,
                    lineHeight: 1.1,
                    color: stat.featured ? 'var(--brand, #155eef)' : 'var(--text, #111827)',
                    marginBottom: '8px',
                    fontFeatureSettings: '"tnum"',
                    letterSpacing: '-0.02em',
                  }}
                >
                  {displayVal}
                </div>

                <div
                  className="stat-label"
                  style={{
                    fontSize: '0.92rem',
                    fontWeight: 600,
                    color: 'var(--muted, #5f6978)',
                    lineHeight: 1.3,
                  }}
                >
                  {stat.label}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
