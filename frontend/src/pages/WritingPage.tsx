import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  CheckCircle2,
  Save,
  Sparkles,
} from 'lucide-react';
import { projectApi, reportApi } from '../api/endpoints';
import { Breadcrumbs, Button, Card, Field, Badge } from '../components/ui';
import { EmptyState, PageLoading } from '../components/states';
import { useProjectId } from '../hooks/useProjectId';
import { pageContent } from '../utils/collections';
import { paths } from '../routes/paths';
import { ReportRichEditor } from '../components/editor/ReportRichEditor';

interface WritingSectionDef {
  key: string;
  title: string;
  type: string;
  defaultChapter: string;
  desc: string;
}

const WRITING_SECTIONS: WritingSectionDef[] = [
  { key: 'PROBLEM_STATEMENT', title: 'Problem Statement', type: 'PROBLEM_STATEMENT', defaultChapter: 'INTRODUCTION', desc: 'Clear formulation of the empirical or practical problem' },
  { key: 'BACKGROUND', title: 'Background of the Study', type: 'BACKGROUND', defaultChapter: 'INTRODUCTION', desc: 'Broader scholarly and contextual foundation' },
  { key: 'OBJECTIVES', title: 'Research Objectives', type: 'OBJECTIVES', defaultChapter: 'INTRODUCTION', desc: 'General and specific empirical aims of the study' },
  { key: 'LITERATURE_REVIEW', title: 'Literature Review', type: 'LITERATURE_REVIEW', defaultChapter: 'LITERATURE_REVIEW', desc: 'Thematic synthesis with citations from uploaded sources' },
  { key: 'METHODOLOGY', title: 'Methodology', type: 'METHODOLOGY', defaultChapter: 'METHODOLOGY', desc: 'Research paradigm, design, sampling strategy, and instruments' },
  { key: 'FINDINGS', title: 'Findings & Results', type: 'FINDINGS', defaultChapter: 'RESULTS', desc: 'Empirical data analysis results, statistics, or qualitative themes' },
  { key: 'DISCUSSION', title: 'Discussion of Findings', type: 'DISCUSSION', defaultChapter: 'DISCUSSION', desc: 'Scholarly interpretation and contextualization of results against literature' },
  { key: 'CONCLUSIONS', title: 'Conclusions', type: 'CONCLUSIONS', defaultChapter: 'DISCUSSION', desc: 'Core takeaways synthesized directly from the research findings' },
  { key: 'RECOMMENDATIONS', title: 'Recommendations', type: 'RECOMMENDATIONS', defaultChapter: 'DISCUSSION', desc: 'Actionable policy, practical, and future research recommendations' },
];

export function WritingPage() {
  const projectId = useProjectId();
  const navigate = useNavigate();

  const [activeSectionKey, setActiveSectionKey] = useState<string>('PROBLEM_STATEMENT');

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.get(projectId),
    enabled: Boolean(projectId),
  });

  const reportsQuery = useQuery({
    queryKey: ['reports', projectId],
    queryFn: () => reportApi.reports(projectId),
    enabled: Boolean(projectId),
  });

  const activeReport = pageContent(reportsQuery.data)?.[0];
  const reportId = activeReport?.id as string | undefined;

  const chaptersQuery = useQuery({
    queryKey: ['chapters', reportId],
    queryFn: () => reportApi.chapters(reportId!),
    enabled: Boolean(reportId),
  });

  // Fetch sections across chapters
  const sectionsQuery = useQuery({
    queryKey: ['report-sections-all', reportId, chaptersQuery.data],
    queryFn: async () => {
      if (!chaptersQuery.data || chaptersQuery.data.length === 0) return [];
      const allSections: any[] = [];
      for (const ch of chaptersQuery.data) {
        try {
          const chSections = await reportApi.sections(ch.id as string);
          allSections.push(...chSections.map((s: any) => ({ ...s, chapterId: ch.id, chapterType: ch.type })));
        } catch {
          // ignore
        }
      }
      return allSections;
    },
    enabled: Boolean(chaptersQuery.data && chaptersQuery.data.length > 0),
  });

  const currentSectionDef = WRITING_SECTIONS.find((s) => s.key === activeSectionKey) ?? WRITING_SECTIONS[0];
  const matchedSection = sectionsQuery.data?.find(
    (s: any) => s.type === currentSectionDef.type || s.heading === currentSectionDef.title
  );
  const initialContent = matchedSection?.content ?? (activeSectionKey === 'PROBLEM_STATEMENT' ? projectQuery.data?.description ?? '' : '');
  const initialContentJson = matchedSection?.contentJson ?? '';
  const initialOrigin = matchedSection?.origin ?? 'USER';

  if (!projectId) {
    return (
      <main className="page">
        <EmptyState title="Select a project" description="Open a research project to manage its writing sections." />
      </main>
    );
  }

  if (projectQuery.isLoading || reportsQuery.isLoading) {
    return <PageLoading label="Loading writing workspace..." />;
  }

  return (
    <section className="page">
      <Breadcrumbs items={['Projects', 'Writing']} />

      {/* Header */}
      <div className="page-header" style={{ alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
        <div>
          <h1 className="page-title" style={{ fontSize: '1.65rem', fontWeight: 700, margin: 0 }}>
            Research Writing Workspace
          </h1>
          <p className="muted" style={{ fontSize: '0.92rem', marginTop: 4 }}>
            Review, edit, and assemble accepted research sections. Drafts generated by the AI Assistant can be saved and edited here.
          </p>
        </div>
        <Button
          type="button"
          variant="secondary"
          onClick={() => navigate(paths.projectAssistant(projectId) + `?mode=generate&section=${activeSectionKey}`)}
          style={{ display: 'flex', alignItems: 'center', gap: 6 }}
        >
          <Sparkles size={15} /> Generate with AI Assistant
        </Button>
      </div>

      <div className="grid cols-2" style={{ gap: 24, alignItems: 'flex-start' }}>
        {/* Left Column: Section List */}
        <Card>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: 12 }}>
            Study Sections
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {WRITING_SECTIONS.map((sec) => {
              const isSelected = activeSectionKey === sec.key;
              const sectionData = sectionsQuery.data?.find(
                (s: any) => s.type === sec.type || s.heading === sec.title
              );
              const hasContent = Boolean(sectionData?.content?.trim() || (sec.key === 'PROBLEM_STATEMENT' && projectQuery.data?.description));
              const secWords = sectionData?.content?.trim() ? sectionData.content.trim().split(/\s+/).length : 0;

              return (
                <div
                  key={sec.key}
                  onClick={() => setActiveSectionKey(sec.key)}
                  style={{
                    padding: '10px 14px',
                    borderRadius: 8,
                    border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border)',
                    background: isSelected ? 'var(--surface-hover)' : 'transparent',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{sec.title}</div>
                    <span className="muted" style={{ fontSize: '0.78rem' }}>{sec.desc}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    {hasContent ? (
                      <Badge tone="success">
                        <CheckCircle2 size={11} style={{ marginRight: 4 }} /> {secWords} words
                      </Badge>
                    ) : (
                      <Badge tone="info">Not Started</Badge>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* Right Column: Active Section Editor */}
        <ActiveSectionEditor
          key={`${activeSectionKey}-${matchedSection?.id ?? 'new'}`}
          projectId={projectId}
          reportId={reportId}
          citationStyle={projectQuery.data?.citationStyle ?? 'APA_7'}
          sectionDef={currentSectionDef}
          initialContent={initialContent}
          initialContentJson={initialContentJson}
          initialOrigin={initialOrigin}
          initialSectionId={matchedSection?.id ?? null}
          initialChapterId={matchedSection?.chapterId ?? null}
          existingSectionsCount={sectionsQuery.data?.length ?? 0}
          onNavigateToAssistant={() => navigate(paths.projectAssistant(projectId) + `?mode=generate&section=${activeSectionKey}`)}
        />
      </div>
    </section>
  );
}

function ActiveSectionEditor({
  projectId,
  reportId,
  citationStyle = 'APA_7',
  sectionDef,
  initialContent,
  initialContentJson = '',
  initialOrigin,
  initialSectionId,
  initialChapterId,
  existingSectionsCount,
  onNavigateToAssistant,
}: {
  projectId: string;
  reportId?: string;
  citationStyle?: string;
  sectionDef: WritingSectionDef;
  initialContent: string;
  initialContentJson?: string;
  initialOrigin: string;
  initialSectionId: string | null;
  initialChapterId: string | null;
  existingSectionsCount: number;
  onNavigateToAssistant: () => void;
}) {
  const client = useQueryClient();
  const [content, setContent] = useState(initialContent);
  const [contentJson, setContentJson] = useState(initialContentJson);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;

  const saveMutation = useMutation({
    mutationFn: async (payload?: { content?: string; contentJson?: string; plainText?: string }) => {
      let currentReportId = reportId;
      if (!currentReportId) {
        const created = await reportApi.createReport(projectId, {
          title: 'Research Report',
          type: 'RESEARCH_REPORT',
          citationStyle: citationStyle || 'APA_7',
        });
        currentReportId = created.id as string;
      }

      let targetChapterId = initialChapterId;
      if (!targetChapterId) {
        const chapters = await reportApi.chapters(currentReportId);
        let ch = chapters.find((c: any) => c.type === sectionDef.defaultChapter) ?? chapters[0];
        if (!ch) {
          ch = await reportApi.createChapter(currentReportId, {
            title: 'Research Sections',
            type: 'CUSTOM',
            displayOrder: 1,
          });
        }
        targetChapterId = ch.id as string;
      }

      const textContent = payload?.content ?? content;
      const jsonContent = payload?.contentJson ?? contentJson;
      const plainText = payload?.plainText;

      if (initialSectionId) {
        await reportApi.updateSection(initialSectionId, {
          content: textContent,
          contentJson: jsonContent,
          plainText,
          origin: initialOrigin,
        });
      } else {
        await reportApi.createSection(targetChapterId, {
          heading: sectionDef.title,
          type: sectionDef.type,
          content: textContent,
          displayOrder: existingSectionsCount + 1,
          origin: initialOrigin,
        });
      }

      if (sectionDef.key === 'PROBLEM_STATEMENT') {
        await projectApi.update(projectId, {
          description: textContent.slice(0, 4900),
        });
      }

      return true;
    },
    onSuccess: () => {
      setSaveMessage('Saved successfully!');
      client.invalidateQueries({ queryKey: ['report-sections-all', reportId] });
      client.invalidateQueries({ queryKey: ['project', projectId] });
      client.invalidateQueries({ queryKey: ['project-dashboard', projectId] });
      setTimeout(() => setSaveMessage(null), 3000);
    },
  });

  return (
    <Card style={{ padding: '1.25rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <div>
          <h2 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
            {sectionDef.title}
          </h2>
          <span className="muted" style={{ fontSize: '0.82rem' }}>
            {wordCount} words • Origin: {initialOrigin}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {saveMessage && <Badge tone="success">{saveMessage}</Badge>}
          <Button
            type="button"
            variant="secondary"
            onClick={onNavigateToAssistant}
            style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.82rem', padding: '3px 8px' }}
          >
            <Sparkles size={13} /> Generate Draft
          </Button>
        </div>
      </div>

      <Field label="Section Content (Rich-Text Word Editor)">
        <ReportRichEditor
          key={`${sectionDef.key}-${initialSectionId ?? 'new'}`}
          content={content}
          contentJson={contentJson}
          projectId={projectId}
          citationStyle={citationStyle}
          minHeight="480px"
          onSave={({ contentJson: cJson, plainText, markdown }) => {
            setContent(markdown);
            setContentJson(cJson);
            saveMutation.mutate({ content: markdown, contentJson: cJson, plainText });
          }}
        />
      </Field>

      <div style={{ display: 'flex', gap: 10, marginTop: 14, flexWrap: 'wrap' }}>
        <Button
          type="button"
          variant="primary"
          disabled={saveMutation.isPending}
          onClick={() => saveMutation.mutate()}
          style={{ display: 'flex', alignItems: 'center', gap: 6 }}
        >
          <Save size={14} />
          {saveMutation.isPending ? 'Saving...' : 'Save Changes'}
        </Button>
      </div>
    </Card>
  );
}
