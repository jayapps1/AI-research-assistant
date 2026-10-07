import type {
  AcademicProjectType,
  AcademicWorkspaceType,
  ReportTemplateItem,
  ResearchProject,
} from '../../types/api';

export const WORKSPACE_TYPE_LABELS: Record<AcademicWorkspaceType, string> = {
  ACADEMIC_RESEARCH: 'Academic Research',
  ACADEMIC_PROJECT: 'Academic Project',
  COURSEWORK: 'Coursework',
};

export const FINAL_DOCUMENT_LABELS: Record<AcademicWorkspaceType, string> = {
  ACADEMIC_RESEARCH: 'Research Report',
  ACADEMIC_PROJECT: 'Project Report',
  COURSEWORK: 'Coursework Document',
};

export const WORK_AREA_LABELS: Record<AcademicWorkspaceType, string> = {
  ACADEMIC_RESEARCH: 'Study Design',
  ACADEMIC_PROJECT: 'Project Work',
  COURSEWORK: 'Notes / Work',
};

export const PROJECT_TYPE_LABELS: Record<AcademicProjectType, string> = {
  SOFTWARE_SYSTEM_DEVELOPMENT: 'Software/System Development',
  ENGINEERING_PROJECT: 'Engineering Project',
  RESEARCH_BASED_PROJECT: 'Research-Based Project',
  BUSINESS_PROJECT: 'Business Project',
  GENERAL_ACADEMIC_PROJECT: 'General Academic Project',
  OTHER: 'Other',
};

export function workspaceTypeOf(project?: ResearchProject | null): AcademicWorkspaceType {
  return project?.workspaceType ?? 'ACADEMIC_RESEARCH';
}

export function workspaceTypeLabel(projectOrType?: ResearchProject | AcademicWorkspaceType | null): string {
  if (!projectOrType) return WORKSPACE_TYPE_LABELS.ACADEMIC_RESEARCH;
  if (typeof projectOrType === 'string') return WORKSPACE_TYPE_LABELS[projectOrType];
  return projectOrType.workspaceTypeLabel ?? WORKSPACE_TYPE_LABELS[workspaceTypeOf(projectOrType)];
}

export function finalDocumentLabel(projectOrType?: ResearchProject | AcademicWorkspaceType | null): string {
  if (!projectOrType) return FINAL_DOCUMENT_LABELS.ACADEMIC_RESEARCH;
  if (typeof projectOrType === 'string') return FINAL_DOCUMENT_LABELS[projectOrType];
  return projectOrType.finalDocumentLabel ?? FINAL_DOCUMENT_LABELS[workspaceTypeOf(projectOrType)];
}

export function prepareFinalDocumentLabel(projectOrType?: ResearchProject | AcademicWorkspaceType | null): string {
  const type = typeof projectOrType === 'string' ? projectOrType : workspaceTypeOf(projectOrType);
  if (type === 'ACADEMIC_PROJECT') return 'Prepare Final Project Report';
  if (type === 'COURSEWORK') return 'Prepare Final Submission';
  return 'Prepare Final Research Report';
}

export function workAreaLabel(projectOrType?: ResearchProject | AcademicWorkspaceType | null): string {
  if (!projectOrType) return WORK_AREA_LABELS.ACADEMIC_RESEARCH;
  if (typeof projectOrType === 'string') return WORK_AREA_LABELS[projectOrType];
  return projectOrType.workAreaLabel ?? WORK_AREA_LABELS[workspaceTypeOf(projectOrType)];
}

export function projectTypeLabel(type?: AcademicProjectType | null): string {
  return type ? PROJECT_TYPE_LABELS[type] : 'General Academic Project';
}

export function templateSupportsWorkspace(template: ReportTemplateItem, type: AcademicWorkspaceType): boolean {
  const supported = template.supportedWorkspaceTypes;
  if (supported && supported.trim()) {
    return supported.split(/[,\s]+/).includes(type);
  }

  if (!template.type) return true;
  if (type === 'ACADEMIC_RESEARCH') return template.type === 'RESEARCH_REPORT';
  if (type === 'ACADEMIC_PROJECT') {
    return template.type === 'ACADEMIC_PROJECT_REPORT' || template.type === 'FINAL_YEAR_PROJECT';
  }
  return template.type === 'COURSEWORK';
}

export function numberToWords(num: number): string {
  const words = [
    'Zero', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten',
    'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen', 'Twenty'
  ];
  return words[num] ?? String(num);
}

export function formatChapterTitle(chapterNumber: number | null | undefined, title: string): string {
  const clean = title?.trim() || '';
  if (!chapterNumber) return clean;
  if (/^chapter\s+/i.test(clean)) return clean;
  return `Chapter ${numberToWords(chapterNumber)} \u2014 ${clean}`;
}

