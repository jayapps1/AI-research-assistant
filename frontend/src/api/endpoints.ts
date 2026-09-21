import { api } from './client';
import type {
  AdminPaymentItem,
  AdminPlanEntitlement,
  AdminSubscriptionPlan,
  AiCreditBalance,
  AiCreditLedgerItem,
  AiCreditPack,
  AdminGrantAiCreditsRequest,
  AuthTokenResponse,
  Citation,
  CreateAiCreditPackRequest,
  CreateSubscriptionPlanRequest,
  DatasetImportPreview,
  DatasetImportStart,
  DatasetItem,
  DatasetRecordItem,
  DatasetSummary,
  DatasetVariableItem,
  DocumentItem,
  LoginResponse,
  NotificationItem,
  PageResponse,
  PaymentAttemptDetail,
  PaymentAttemptInitialization,
  PaymentTransactionPage,
  PlanPriceBreakdown,
  ProjectDashboardResponse,
  ProjectMember,
  RagAnswer,
  RagConversation,
  RecoveryCodesResponse,
  ReportTemplateItem,
  ResearchProgressResponse,
  ResearchProject,
  SectionCapabilitiesResponse,
  TableOfContentsResponse,
  TotpEnrollmentResponse,
  UpdateAiCreditPackRequest,
  UpdateProfileRequest,
  UpdateSubscriptionPlanRequest,
  User,
  UserDashboardResponse,
  UserProfileResponse,
  UserTaskSummary,
  ValidationIssue,
  Workspace,
  WorkspaceDashboardResponse,
} from '../types/api';

export const authApi = {
  register: (body: { email: string; password: string; firstName?: string; lastName?: string; locale?: string }) =>
    api.post<User>('/auth/register', body).then((r) => r.data),
  login: (body: { email: string; password?: string; totpCode?: string; recoveryCode?: string; authenticationMethod?: 'PASSWORD' | 'TOTP' | 'PASSWORD_AND_TOTP' | 'PASSWORD_OR_TOTP' }) =>
    api.post<LoginResponse>('/auth/login', body).then((r) => r.data),
  completeTotpLoginChallenge: (body: { challengeId: string; totpCode?: string; recoveryCode?: string }) =>
    api.post<AuthTokenResponse>('/auth/login/totp-challenge', body).then((r) => r.data),
  logout: (refreshToken: string) => api.post('/auth/logout', { refreshToken }).then((r) => r.data),
  logoutAll: () => api.post('/auth/logout-all').then((r) => r.data),
  forgotPassword: (email: string) => api.post('/auth/password/forgot', { email }).then((r) => r.data),
  verifyPasswordTotp: (body: Record<string, string>) => api.post('/auth/password/verify-totp', body).then((r) => r.data),
  verifyPasswordRecoveryCode: (body: Record<string, string>) => api.post('/auth/password/verify-recovery-code', body).then((r) => r.data),
  resetPassword: (body: { resetToken: string; newPassword: string }) => api.post('/auth/password/reset', body).then((r) => r.data),
  startTotpEnrollment: () => api.post<TotpEnrollmentResponse>('/auth/totp/enrollment').then((r) => r.data),
  confirmTotpEnrollment: (totpCode: string) => api.post<RecoveryCodesResponse>('/auth/totp/enrollment/confirm', { totpCode }).then((r) => r.data),
  regenerateTotpRecoveryCodes: (body: { password?: string; totpCode?: string; recoveryCode?: string }) =>
    api.post<RecoveryCodesResponse>('/auth/totp/recovery-codes/regenerate', body).then((r) => r.data),
  disableTotp: (body: { password?: string; totpCode?: string; recoveryCode?: string }) => api.post('/auth/totp/disable', body).then((r) => r.data),
  startTotpReEnrollment: (body: { password?: string; totpCode?: string; recoveryCode?: string }) =>
    api.post<TotpEnrollmentResponse>('/auth/totp/re-enrollment', body).then((r) => r.data),
};

export const workspaceApi = {
  list: () => api.get<Workspace[]>('/workspaces').then((r) => r.data),
  create: (body: { name: string; type: string }) => api.post<Workspace>('/workspaces', body).then((r) => r.data),
  ensurePersonal: () => api.post<Workspace>('/workspaces/personal/ensure').then((r) => r.data),
  getPersonal: () => api.get<Workspace>('/workspaces/personal').then((r) => r.data),
  members: (workspaceId: string) => api.get<ProjectMember[]>(`/workspaces/${workspaceId}/members`).then((r) => r.data),
};

export const projectApi = {
  list: (workspaceId: string, page = 0, size = 20, filters?: { q?: string; status?: string }) =>
    api.get<PageResponse<ResearchProject>>(`/workspaces/${workspaceId}/projects`, { params: { page, size, ...filters } }).then((r) => r.data),
  mine: (page = 0, size = 20, filters?: { q?: string; status?: string }) =>
    api.get<PageResponse<ResearchProject>>('/projects/mine', { params: { page, size, ...filters } }).then((r) => r.data),
  create: (workspaceId: string, body: {
    title: string;
    description?: string;
    researchAim?: string;
    studyArea?: string;
    researchType?: string;
    keywords?: string;
    reportTemplateId?: string;
    citationStyle?: string;
    citationStyleLocked?: boolean;
  }) =>
    api.post<ResearchProject>(`/workspaces/${workspaceId}/projects`, { ...body, workspaceId }).then((r) => r.data),
  get: (projectId: string) => api.get<ResearchProject>(`/projects/${projectId}`).then((r) => r.data),
  update: (projectId: string, body: Record<string, unknown>) =>
    api.patch<ResearchProject>(`/projects/${projectId}`, body).then((r) => r.data),
  archive: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/archive`).then((r) => r.data),
  trash: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/trash`).then((r) => r.data),
  restore: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/restore`).then((r) => r.data),
  permanentDelete: (projectId: string, confirmation: string) =>
    api.delete<void>(`/projects/${projectId}/permanent`, { data: { confirmation } }).then((r) => r.data),
  members: (projectId: string) => api.get<ProjectMember[]>(`/projects/${projectId}/members`).then((r) => r.data),
  invite: (projectId: string, body: { email: string; role: string }) => api.post(`/projects/${projectId}/invitations`, body).then((r) => r.data),
  invitations: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/invitations`).then((r) => r.data),
  changeMemberRole: (projectId: string, memberId: string, role: string) => api.patch(`/projects/${projectId}/memberships/${memberId}/role`, { role }).then((r) => r.data),
  suspendMember: (projectId: string, memberId: string) => api.post(`/projects/${projectId}/memberships/${memberId}/suspend`).then((r) => r.data),
  reactivateMember: (projectId: string, memberId: string) => api.post(`/projects/${projectId}/memberships/${memberId}/reactivate`).then((r) => r.data),
  removeMember: (projectId: string, memberId: string) => api.delete(`/projects/${projectId}/memberships/${memberId}`).then((r) => r.data),
  createTask: (projectId: string, body: Record<string, unknown>) => api.post(`/projects/${projectId}/tasks`, body).then((r) => r.data),
  tasks: (projectId: string) => api.get<PageResponse<Record<string, unknown>> | Record<string, unknown>[]>(`/projects/${projectId}/tasks`).then((r) => r.data),
  myTasks: (projectId: string) => api.get<PageResponse<Record<string, unknown>> | Record<string, unknown>[]>(`/projects/${projectId}/tasks/mine`).then((r) => r.data),
  allMyTasks: (page = 0, size = 20, filters?: { status?: string; priority?: string; projectId?: string }) =>
    api.get<PageResponse<UserTaskSummary>>('/tasks/mine', { params: { page, size, ...filters } }).then((r) => r.data),
  comments: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/comments`).then((r) => r.data),
  createComment: (projectId: string, body: Record<string, unknown>) => api.post(`/projects/${projectId}/comments`, body).then((r) => r.data),
  reviews: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/reviews`).then((r) => r.data),
  createReview: (projectId: string, body: Record<string, unknown>) => api.post(`/projects/${projectId}/reviews`, body).then((r) => r.data),
  activity: (projectId: string) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/activity`).then((r) => r.data),
  contributions: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/contributions`).then((r) => r.data),
};

export const documentApi = {
  list: (projectId: string, page = 0, size = 20, filters?: { status?: string; type?: string }) =>
    api.get<PageResponse<DocumentItem>>(`/projects/${projectId}/documents`, { params: { page, size, ...filters } }).then((r) => r.data),
  mine: (page = 0, size = 20, filters?: { status?: string }) =>
    api.get<PageResponse<DocumentItem>>('/documents/mine', { params: { page, size, ...filters } }).then((r) => r.data),
  get: (documentId: string) => api.get<DocumentItem>(`/documents/${documentId}`).then((r) => r.data),
  updateMetadata: (documentId: string, body: Partial<{
    title: string;
    bibliographicTitle: string;
    authors: string;
    publicationYear: number;
    journal: string;
    conference: string;
    publisher: string;
    volume: string;
    issue: string;
    pages: string;
    doi: string;
    url: string;
    sourceType: string;
    keywords: string;
  }>) =>
    api.patch<DocumentItem>(`/documents/${documentId}`, body).then((r) => r.data),
  upload: (projectId: string, file: File, title?: string) => {
    const body = new FormData();
    body.append('file', file);
    if (title) body.append('title', title);
    return api.post<DocumentItem>(`/projects/${projectId}/documents`, body, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  uploadVersion: (documentId: string, file: File) => {
    const body = new FormData();
    body.append('file', file);
    return api.post<DocumentItem>(`/documents/${documentId}/versions`, body, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  versions: (documentId: string) => api.get<Record<string, unknown>[]>(`/documents/${documentId}/versions`).then((r) => r.data),
  processing: (documentId: string) => api.get<Record<string, unknown>>(`/documents/${documentId}/processing`).then((r) => r.data),
  retryProcessing: (documentId: string) => api.post(`/documents/${documentId}/processing/retry`).then((r) => r.data),
  archive: (documentId: string) => api.post<DocumentItem>(`/documents/${documentId}/archive`).then((r) => r.data),
  delete: (documentId: string) => api.delete<DocumentItem>(`/documents/${documentId}`).then((r) => r.data),
  restore: (documentId: string) => api.post<DocumentItem>(`/documents/${documentId}/restore`).then((r) => r.data),
  permanentDelete: (documentId: string) => api.delete<void>(`/documents/${documentId}/permanent`).then((r) => r.data),
  download: (documentId: string) =>
    api.get<Blob>(`/documents/${documentId}/download`, { responseType: 'blob' }).then((r) => ({
      blob: r.data,
      filename: filenameFromContentDisposition(r.headers['content-disposition']) ?? 'document',
    })),
  downloadUrl: (documentId: string) => `${api.defaults.baseURL}/documents/${documentId}/download`,
};

function filenameFromContentDisposition(value?: string) {
  if (!value) return null;
  const match = /filename="?([^";]+)"?/i.exec(value);
  return match ? match[1] : null;
}

export const researchDesignApi = {
  get: (projectId: string) =>
    api.get<Record<string, unknown>>(`/projects/${projectId}/research-design`).then((r) => r.data),
  save: (
    projectId: string,
    body: { problemStatement?: string; objectives?: string[]; questions?: string[]; hypotheses?: string[] },
  ) =>
    api.put<Record<string, unknown>>(`/projects/${projectId}/research-design`, body).then((r) => r.data),
};

export const researchApi = {
  methodologies: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/methodologies`).then((r) => r.data),
  createMethodology: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/methodologies`, body).then((r) => r.data),
  generateMethodology: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/methodologies/generate`).then((r) => r.data),
  conceptualFrameworks: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/conceptual-frameworks`).then((r) => r.data),
  createConceptualFramework: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/conceptual-frameworks`, body).then((r) => r.data),
  generateConceptualFramework: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/conceptual-frameworks/generate`).then((r) => r.data),
  theoreticalFrameworks: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/theoretical-frameworks`).then((r) => r.data),
  createTheoreticalFramework: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/theoretical-frameworks`, body).then((r) => r.data),
  generateTheoreticalFramework: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/theoretical-frameworks/generate`).then((r) => r.data),
  ethicsReadiness: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/ethics-readiness`).then((r) => r.data),
  participants: (projectId: string) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/participants`).then((r) => r.data),
  validateResearchDesign: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/research-design/validate`).then((r) => r.data),
  reviewResearchDesign: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/research-design/review`).then((r) => r.data),
};

export const ragApi = {
  conversations: (projectId: string) => api.get<RagConversation[]>(`/projects/${projectId}/rag/conversations`).then((r) => r.data),
  createConversation: (projectId: string, body: Record<string, unknown>) =>
    api.post<RagConversation>(`/projects/${projectId}/rag/conversations`, body).then((r) => r.data),
  ask: (conversationId: string, body: Record<string, unknown>) =>
    api.post<RagAnswer>(`/rag/conversations/${conversationId}/queries`, body).then((r) => r.data),
  evidence: (queryId: string) => api.get<Citation[]>(`/rag/queries/${queryId}/evidence`).then((r) => r.data),
};

export const analysisApi = {
  runs: (projectId: string, page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/analysis-runs`, { params: { page, size } }).then((r) => r.data),
  createRun: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/analysis-runs`, body).then((r) => r.data),
  completeRun: (analysisRunId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/analysis-runs/${analysisRunId}/complete`, body).then((r) => r.data),
  findings: (projectId: string, page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/findings`, { params: { page, size } }).then((r) => r.data),
  createFinding: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/findings`, body).then((r) => r.data),
  generateFinding: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/findings/generate`, body).then((r) => r.data),
  discussions: (findingId: string) => api.get<Record<string, unknown>[]>(`/findings/${findingId}/discussion`).then((r) => r.data),
  createDiscussion: (findingId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/findings/${findingId}/discussion`, body).then((r) => r.data),
  generateDiscussion: (findingId: string) => api.post<Record<string, unknown>>(`/findings/${findingId}/discussion/generate`).then((r) => r.data),
  discussionEvidence: (discussionId: string) => api.get<Record<string, unknown>[]>(`/discussions/${discussionId}/evidence`).then((r) => r.data),
  conclusions: (projectId: string, page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/conclusions`, { params: { page, size } }).then((r) => r.data),
  createConclusion: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/conclusions`, body).then((r) => r.data),
  generateConclusion: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/conclusions/generate`).then((r) => r.data),
  recommendations: (projectId: string, page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/recommendations`, { params: { page, size } }).then((r) => r.data),
  createRecommendation: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/recommendations`, body).then((r) => r.data),
  generateRecommendation: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/recommendations/generate`).then((r) => r.data),
  traceability: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/traceability-matrix`).then((r) => r.data),
  aiUsage: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/ai-usage-summary`).then((r) => r.data),
};

export const datasetApi = {
  list: (projectId: string, page = 0, size = 20) =>
    api.get<PageResponse<DatasetItem>>(`/projects/${projectId}/datasets`, { params: { page, size } }).then((r) => r.data),
  create: (projectId: string, body: { name: string; description?: string; sourceType?: string }) =>
    api.post<DatasetItem>(`/projects/${projectId}/datasets`, body).then((r) => r.data),
  variables: (datasetId: string) => api.get<DatasetVariableItem[]>(`/datasets/${datasetId}/variables`).then((r) => r.data),
  addVariable: (datasetId: string, body: Record<string, unknown>) =>
    api.post<DatasetVariableItem>(`/datasets/${datasetId}/variables`, body).then((r) => r.data),
  records: (datasetId: string, page = 0, size = 25) =>
    api.get<PageResponse<DatasetRecordItem>>(`/datasets/${datasetId}/records`, { params: { page, size } }).then((r) => r.data),
  summary: (datasetId: string) => api.get<DatasetSummary>(`/datasets/${datasetId}/summary`).then((r) => r.data),
  validate: (datasetId: string) => api.post(`/datasets/${datasetId}/validate`).then((r) => r.data),
  issues: (datasetId: string, page = 0, size = 20) =>
    api.get<PageResponse<ValidationIssue>>(`/datasets/${datasetId}/validation-issues`, { params: { page, size } }).then((r) => r.data),
  startImport: (projectId: string, file: File) => {
    const body = new FormData();
    body.append('file', file);
    return api.post<DatasetImportStart>(`/projects/${projectId}/datasets/import`, body, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  importPreview: (importJobId: string) => api.get<DatasetImportPreview>(`/dataset-imports/${importJobId}/preview`).then((r) => r.data),
  confirmImport: (importJobId: string, body: Record<string, unknown>) =>
    api.post<DatasetImportStart>(`/dataset-imports/${importJobId}/confirm`, body).then((r) => r.data),
};

export const reportApi = {
  templates: () => api.get<ReportTemplateItem[]>('/report-templates').then((r) => r.data),
  sectionCapabilities: (projectId: string) =>
    api.get<SectionCapabilitiesResponse>(`/projects/${projectId}/section-capabilities`).then((r) => r.data),
  tableOfContents: (reportId: string) =>
    api.get<TableOfContentsResponse>(`/reports/${reportId}/table-of-contents`).then((r) => r.data),
  reports: (projectId: string, page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/reports`, { params: { page, size } }).then((r) => r.data),
  createReport: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/reports`, body).then((r) => r.data),
  report: (reportId: string) => api.get<Record<string, unknown>>(`/reports/${reportId}`).then((r) => r.data),
  chapters: (reportId: string) => api.get<Record<string, unknown>[]>(`/reports/${reportId}/chapters`).then((r) => r.data),
  createChapter: (reportId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/reports/${reportId}/chapters`, body).then((r) => r.data),
  sections: (chapterId: string) => api.get<Record<string, unknown>[]>(`/report-chapters/${chapterId}/sections`).then((r) => r.data),
  createSection: (chapterId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/report-chapters/${chapterId}/sections`, body).then((r) => r.data),
  updateSection: (sectionId: string, body: Record<string, unknown>) => api.patch<Record<string, unknown>>(`/report-sections/${sectionId}`, body).then((r) => r.data),
  generateSection: (sectionId: string) => api.post<Record<string, unknown>>(`/report-sections/${sectionId}/generate`).then((r) => r.data),
  validate: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/validate`).then((r) => r.data),
  assemble: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/assemble`).then((r) => r.data),
  finalize: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/finalize`).then((r) => r.data),
  references: (projectId: string, page = 0, size = 20, filters?: Record<string, unknown>) =>
    api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/references`, { params: { page, size, ...filters } }).then((r) => r.data),
  createReference: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references`, body).then((r) => r.data),
  formatCitation: (body: Record<string, unknown>) => api.post<Record<string, unknown>>('/references/format-citation', body).then((r) => r.data),
  duplicateReferences: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references/check-duplicates`, body).then((r) => r.data),
  importReferences: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references/import`, body).then((r) => r.data),
  importPreview: (importJobId: string) => api.get<Record<string, unknown>[]>(`/reference-imports/${importJobId}/preview`).then((r) => r.data),
  confirmReferenceImport: (importJobId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/reference-imports/${importJobId}/confirm`, body).then((r) => r.data),
  exportReferencesUrl: (projectId: string, format: string) => `${api.defaults.baseURL}/projects/${projectId}/references/export?format=${encodeURIComponent(format)}`,
  exports: (reportId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/reports/${reportId}/exports`, body).then((r) => r.data),
  exportJob: (exportId: string) => api.get<Record<string, unknown>>(`/report-exports/${exportId}`).then((r) => r.data),
  exportDownloadUrl: (exportId: string) => `${api.defaults.baseURL}/report-exports/${exportId}/download`,
  integrity: (reportId: string) => api.post(`/reports/${reportId}/integrity-review`).then((r) => r.data),
  writingReview: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/writing-review`).then((r) => r.data),
  similarity: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/similarity-checks`, body).then((r) => r.data),
};

export const billingApi = {
  subscription: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/billing/subscription`).then((r) => r.data),
  transactions: (workspaceId: string, page = 0, size = 10) =>
    api.get<PaymentTransactionPage>(`/workspaces/${workspaceId}/billing/transactions`, { params: { page, size } }).then((r) => r.data),
  usage: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/usage`).then((r) => r.data),
  entitlements: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/entitlements`).then((r) => r.data),
  initialize: (workspaceId: string, body: { planCode: string; billingInterval: string }) =>
    api.post<PaymentAttemptInitialization>(`/workspaces/${workspaceId}/billing/initialize`, body).then((r) => r.data),
  retry: (paymentIntentId: string) => api.post<PaymentAttemptInitialization>(`/billing/payment-intents/${paymentIntentId}/retry`).then((r) => r.data),
  verifyAttempt: (attemptId: string) => api.post<PaymentAttemptDetail>(`/billing/payment-attempts/${attemptId}/verify`).then((r) => r.data),
  verifyAttemptByReference: (reference: string) => api.get<PaymentAttemptDetail>(`/billing/payment-attempts/by-reference/${reference}`).then((r) => r.data),
  getAttempt: (attemptId: string) => api.get<PaymentAttemptDetail>(`/billing/payment-attempts/${attemptId}`).then((r) => r.data),
  priceBreakdown: (planCode: string, interval = 'MONTHLY') =>
    api.get<PlanPriceBreakdown>(`/billing/plans/${planCode}/breakdown`, { params: { interval } }).then((r) => r.data),
  aiCredits: (workspaceId: string) =>
    api.get<AiCreditBalance>(`/workspaces/${workspaceId}/ai-credits`).then((r) => r.data),
  aiCreditPacks: (workspaceId: string) =>
    api.get<AiCreditPack[]>(`/workspaces/${workspaceId}/billing/ai-credit-packs`).then((r) => r.data),
  buyAiCreditPack: (workspaceId: string, packId: string) =>
    api.post<PaymentAttemptInitialization>(`/workspaces/${workspaceId}/billing/ai-credits/purchase`, { packId }).then((r) => r.data),
  aiCreditLedger: (workspaceId: string, page = 0, size = 15) =>
    api.get<PageResponse<AiCreditLedgerItem>>(`/workspaces/${workspaceId}/ai-credits/ledger`, { params: { page, size } }).then((r) => r.data),
};

export const notificationApi = {
  list: () => api.get<PageResponse<NotificationItem>>('/me/notifications').then((r) => r.data),
  unreadCount: () => api.get<{ count: number }>('/me/notifications/unread-count').then((r) => r.data),
  markRead: (id: string) => api.post(`/me/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => api.post('/me/notifications/read-all').then((r) => r.data),
};

export const adminApi = {
  dashboard: () => api.get<Record<string, unknown>>('/admin/dashboard').then((r) => r.data),
  operations: () => api.get<Record<string, unknown>>('/admin/operations/status').then((r) => r.data),
  users: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/users', { params: { page, size } }).then((r) => r.data),
  suspendUser: (userId: string) => api.post(`/admin/users/${userId}/suspend`).then((r) => r.data),
  reactivateUser: (userId: string) => api.post(`/admin/users/${userId}/reactivate`).then((r) => r.data),
  workspaces: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/workspaces', { params: { page, size } }).then((r) => r.data),
  projects: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/projects', { params: { page, size } }).then((r) => r.data),
  documents: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/documents', { params: { page, size } }).then((r) => r.data),
  processingJobs: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/processing-jobs', { params: { page, size } }).then((r) => r.data),
  researchTemplates: () => api.get<Record<string, unknown>[]>('/admin/research-templates').then((r) => r.data),
  reportTemplates: () => api.get<Record<string, unknown>[]>('/admin/report-templates').then((r) => r.data),
  aiOperations: (page = 0, size = 20) => api.get<PageResponse<Record<string, unknown>>>('/admin/ai-operations', { params: { page, size } }).then((r) => r.data),
  aiUsage: () => api.get<Record<string, unknown>>('/admin/ai-usage').then((r) => r.data),
  storage: () => api.get<Record<string, unknown>>('/admin/storage').then((r) => r.data),
  referenceStyles: () => api.get<Record<string, unknown>[]>('/admin/references/styles').then((r) => r.data),
  systemHealth: () => api.get<Record<string, unknown>>('/admin/system-health').then((r) => r.data),
  settings: () => api.get<Record<string, unknown>>('/admin/settings').then((r) => r.data),
  auditEvents: (page = 0, size = 25) => api.get<PageResponse<Record<string, unknown>>>('/admin/audit-events', { params: { page, size } }).then((r) => r.data),
  plans: () => api.get<AdminSubscriptionPlan[]>('/admin/subscription-plans').then((r) => r.data),
  getPlan: (planId: string) => api.get<AdminSubscriptionPlan>(`/admin/subscription-plans/${planId}`).then((r) => r.data),
  createPlan: (body: CreateSubscriptionPlanRequest) => api.post<AdminSubscriptionPlan>('/admin/subscription-plans', body).then((r) => r.data),
  updatePlan: (planId: string, body: UpdateSubscriptionPlanRequest) => api.patch<AdminSubscriptionPlan>(`/admin/subscription-plans/${planId}`, body).then((r) => r.data),
  activatePlan: (planId: string) => api.post<AdminSubscriptionPlan>(`/admin/subscription-plans/${planId}/activate`).then((r) => r.data),
  deactivatePlan: (planId: string) => api.post<AdminSubscriptionPlan>(`/admin/subscription-plans/${planId}/deactivate`).then((r) => r.data),
  getPlanEntitlements: (planId: string) => api.get<AdminPlanEntitlement[]>(`/admin/subscription-plans/${planId}/entitlements`).then((r) => r.data),
  updatePlanEntitlements: (planId: string, entitlements: AdminPlanEntitlement[]) =>
    api.put<AdminPlanEntitlement[]>(`/admin/subscription-plans/${planId}/entitlements`, { entitlements }).then((r) => r.data),
  payments: (page = 0, size = 20) => api.get<PageResponse<AdminPaymentItem>>('/admin/payments', { params: { page, size } }).then((r) => r.data),
  complimentaryAccess: () => api.get<PageResponse<Record<string, unknown>>>('/admin/complimentary-access').then((r) => r.data),
  grantComplimentaryAccess: (body: Record<string, unknown>) => api.post('/admin/complimentary-access', body).then((r) => r.data),
  aiCreditPacks: () =>
    api.get<AiCreditPack[]>('/admin/billing/ai-credit-packs').then((r) => r.data),
  createAiCreditPack: (body: CreateAiCreditPackRequest) =>
    api.post<AiCreditPack>('/admin/billing/ai-credit-packs', body).then((r) => r.data),
  updateAiCreditPack: (packId: string, body: UpdateAiCreditPackRequest) =>
    api.patch<AiCreditPack>(`/admin/billing/ai-credit-packs/${packId}`, body).then((r) => r.data),
  grantAiCredits: (workspaceId: string, body: AdminGrantAiCreditsRequest) =>
    api.post<AiCreditBalance>(`/admin/workspaces/${workspaceId}/ai-credits/grant`, body).then((r) => r.data),
  workspaceAiCredits: (workspaceId: string) =>
    api.get<AiCreditBalance>(`/admin/workspaces/${workspaceId}/ai-credits`).then((r) => r.data),
};

export const publicApi = {
  pricing: () => api.get<any[]>('/public/pricing').then((r) => r.data),
  siteSettings: () => api.get<Record<string, unknown>>('/public/site').then((r) => r.data),
  statistics: () => api.get<any[]>('/public/statistics').then((r) => r.data),
};

export const dashboardApi = {
  userDashboard: () => api.get<UserDashboardResponse>('/dashboard').then((r) => r.data),
  workspaceDashboard: (workspaceId: string) =>
    api.get<WorkspaceDashboardResponse>(`/workspaces/${workspaceId}/dashboard`).then((r) => r.data),
  projectDashboard: (projectId: string) =>
    api.get<ProjectDashboardResponse>(`/projects/${projectId}/dashboard`).then((r) => r.data),
  researchProgress: (projectId: string) =>
    api.get<ResearchProgressResponse>(`/projects/${projectId}/research-progress`).then((r) => r.data),
};

export const profileApi = {
  getProfile: () => api.get<UserProfileResponse>('/me/profile').then((r) => r.data),
  updateProfile: (data: UpdateProfileRequest) =>
    api.patch<UserProfileResponse>('/me/profile', data).then((r) => r.data),
  uploadImage: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api
      .post<UserProfileResponse>('/me/profile/image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      .then((r) => r.data);
  },
  deleteImage: () => api.delete<UserProfileResponse>('/me/profile/image').then((r) => r.data),
  getAvatarUrl: (userId: string) => `/api/v1/users/${userId}/avatar`,
};
