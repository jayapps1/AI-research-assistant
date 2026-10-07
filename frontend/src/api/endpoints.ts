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
  ConversationAttachment,
  ConversationDetail,
  ConversationSourceScope,
  ConversationStatus,
  ConversationSummary,
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
  LiteratureMatrixResponse,
  PaymentAttemptDetail,
  PaymentAttemptInitialization,
  PaymentTransactionPage,
  PlanPriceBreakdown,
  ProjectDashboardResponse,
  ProjectMember,
  RagAnswer,
  RagConversation,
  RecoveryCodesResponse,
  ReorderStructureRequest,
  ReportStructureResponse,
  ReportTemplateItem,
  ResearchProgressResponse,
  ResearchProject,
  SaveLiteratureMatrixRequest,
  SectionCapabilitiesResponse,
  SubmitConversationResponse,
  TableOfContentsResponse,
  UpdateReportSettingsRequest,
  UpdateTitlePageDetailsRequest,
  DeterministicRepairResponse,
  GenerateSectionRequest,
  TotpEnrollmentResponse,
  UpdateAiCreditPackRequest,
  UpdateProfileRequest,
  UpdateSubscriptionPlanRequest,
  User,
  UserDashboardResponse,
  UserProfileResponse,
  UserTaskSummary,
  ValidationIssue,
  AcademicDocumentGuidelineResponse,
  AcademicFileRole,
  DynamicTocResponse,
  ExtractedAcademicTemplate,
  RecommendedTemplateItem,
  ReportStructureValidationResponse,
  Workspace,
  WorkspaceDashboardResponse,
  AcademicProjectType,
  AcademicWorkspaceType,
  EvidenceType,
  ProjectEvidenceItem,
  ProjectImageAnalysisResult,
  ListOfFiguresItem,
  ListOfTablesItem,
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
  list: (workspaceId: string, page = 0, size = 20, filters?: { q?: string; status?: string; workspaceType?: AcademicWorkspaceType }) =>
    api.get<PageResponse<ResearchProject>>(`/workspaces/${workspaceId}/projects`, { params: { page, size, ...filters } }).then((r) => r.data),
  mine: (page = 0, size = 20, filters?: { q?: string; status?: string; workspaceType?: AcademicWorkspaceType }) =>
    api.get<PageResponse<ResearchProject>>('/projects/mine', { params: { page, size, ...filters } }).then((r) => r.data),
  create: (workspaceId: string, body: {
    title: string;
    description?: string;
    workspaceType?: AcademicWorkspaceType;
    projectType?: AcademicProjectType;
    institution?: string;
    department?: string;
    programme?: string;
    academicYear?: string;
    supervisor?: string;
    courseName?: string;
    courseCode?: string;
    lecturer?: string;
    deadline?: string;
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
  hold: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/hold`).then((r) => r.data),
  activate: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/activate`).then((r) => r.data),
  complete: (projectId: string) => api.post<ResearchProject>(`/projects/${projectId}/complete`).then((r) => r.data),
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
  upload: (projectId: string, file: File, title?: string, role?: AcademicFileRole) => {
    const body = new FormData();
    body.append('file', file);
    if (title) body.append('title', title);
    if (role) body.append('role', role);
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
  rescanReferenceMetadata: (documentId: string) =>
    api.post<DocumentItem>(`/documents/${documentId}/reference-metadata/rescan`).then((r) => r.data),
  rescanProjectReferenceMetadata: (projectId: string) =>
    api.post<DocumentItem[]>(`/projects/${projectId}/documents/reference-metadata/rescan`).then((r) => r.data),
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

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export const researchDesignApi = {
  get: (projectId: string) =>
    api.get<Record<string, unknown>>(`/projects/${projectId}/research-design`).then((r) => r.data),
  save: (
    projectId: string,
    body: { problemStatement?: string; objectives?: string[]; questions?: string[]; hypotheses?: string[] },
  ) =>
    api.put<Record<string, unknown>>(`/projects/${projectId}/research-design`, body).then((r) => r.data),
  generate: (
    projectId: string,
    body?: { documentIds?: string[]; instructions?: string },
  ) =>
    api.post<Record<string, unknown>>(`/projects/${projectId}/research-design/generate`, body ?? {}).then((r) => r.data),
  exportProtocol: (projectId: string, format: 'docx' | 'markdown' = 'docx') =>
    api.get<Blob>(`/projects/${projectId}/research-design/export`, { params: { format }, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      filename: filenameFromContentDisposition(r.headers['content-disposition']) ?? `research_protocol.${format === 'markdown' ? 'md' : 'docx'}`,
    })),
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
  updateConversation: (conversationId: string, body: Record<string, unknown>) =>
    api.patch<RagConversation>(`/rag/conversations/${conversationId}`, body).then((r) => r.data),
  archiveConversation: (conversationId: string) =>
    api.post<RagConversation>(`/rag/conversations/${conversationId}/archive`).then((r) => r.data),
  deleteConversation: (conversationId: string) =>
    api.delete<void>(`/rag/conversations/${conversationId}`).then((r) => r.data),
  ask: (conversationId: string, body: Record<string, unknown>) =>
    api.post<RagAnswer>(`/rag/conversations/${conversationId}/queries`, body).then((r) => r.data),
  evidence: (queryId: string) => api.get<Citation[]>(`/rag/queries/${queryId}/evidence`).then((r) => r.data),
};

export const conversationApi = {
  list: (params?: { status?: ConversationStatus; q?: string; page?: number; size?: number }) =>
    api.get<PageResponse<ConversationSummary>>('/conversations', { params }).then((r) => r.data),
  listProject: (projectId: string, params?: { status?: ConversationStatus; q?: string; page?: number; size?: number }) =>
    api.get<PageResponse<ConversationSummary>>(`/projects/${projectId}/conversations`, { params }).then((r) => r.data),
  create: (body: { content: string; scopeType?: ConversationSourceScope; documentIds?: string[]; evidenceLimit?: number }) =>
    api.post<SubmitConversationResponse>('/conversations', body).then((r) => r.data),
  createProject: (projectId: string, body: { content: string; scopeType?: ConversationSourceScope; documentIds?: string[]; evidenceLimit?: number }) =>
    api.post<SubmitConversationResponse>(`/projects/${projectId}/conversations`, body).then((r) => r.data),
  get: (conversationId: string) =>
    api.get<ConversationDetail>(`/conversations/${conversationId}`).then((r) => r.data),
  getProject: (projectId: string, conversationId: string) =>
    api.get<ConversationDetail>(`/projects/${projectId}/conversations/${conversationId}`).then((r) => r.data),
  submitMessage: (conversationId: string, body: { content: string; scopeType?: ConversationSourceScope; documentIds?: string[]; evidenceLimit?: number }) =>
    api.post<SubmitConversationResponse>(`/conversations/${conversationId}/messages`, body).then((r) => r.data),
  submitProjectMessage: (projectId: string, conversationId: string, body: { content: string; scopeType?: ConversationSourceScope; documentIds?: string[]; evidenceLimit?: number }) =>
    api.post<SubmitConversationResponse>(`/projects/${projectId}/conversations/${conversationId}/messages`, body).then((r) => r.data),
  retryMessage: (conversationId: string, messageId: string) =>
    api.post<SubmitConversationResponse>(`/conversations/${conversationId}/messages/${messageId}/retry`).then((r) => r.data),
  retryProjectMessage: (projectId: string, conversationId: string, messageId: string) =>
    api.post<SubmitConversationResponse>(`/projects/${projectId}/conversations/${conversationId}/messages/${messageId}/retry`).then((r) => r.data),
  rename: (conversationId: string, body: { title: string }) =>
    api.patch<ConversationSummary>(`/conversations/${conversationId}`, body).then((r) => r.data),
  archive: (conversationId: string) =>
    api.post<ConversationSummary>(`/conversations/${conversationId}/archive`).then((r) => r.data),
  restore: (conversationId: string) =>
    api.post<ConversationSummary>(`/conversations/${conversationId}/restore`).then((r) => r.data),
  trash: (conversationId: string) =>
    api.post<ConversationSummary>(`/conversations/${conversationId}/trash`).then((r) => r.data),
  moveToProject: (conversationId: string, projectId: string) =>
    api.post<ConversationSummary>(`/conversations/${conversationId}/project`, { projectId }).then((r) => r.data),
  listAttachments: (conversationId: string) =>
    api.get<ConversationAttachment[]>(`/conversations/${conversationId}/attachments`).then((r) => r.data),
  uploadAttachment: (conversationId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<ConversationAttachment>(`/conversations/${conversationId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },
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
  ensure: (projectId: string) => api.post<Record<string, unknown>>(`/projects/${projectId}/reports/ensure`).then((r) => r.data),
  createReport: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/reports`, body).then((r) => r.data),
  report: (reportId: string) => api.get<Record<string, unknown>>(`/reports/${reportId}`).then((r) => r.data),
  updateReport: (reportId: string, body: Record<string, unknown>) => api.patch<Record<string, unknown>>(`/reports/${reportId}`, body).then((r) => r.data),
  finalDocument: (reportId: string) => api.get<Record<string, unknown> | null>(`/reports/${reportId}/final-document`).then((r) => r.data),
  prepareFinalDocument: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/final-document/prepare`).then((r) => r.data),
  updateFinalDocument: (reportId: string, body: Record<string, unknown>) => api.put<Record<string, unknown>>(`/reports/${reportId}/final-document`, body).then((r) => r.data),
  structure: (reportId: string) =>
    api.get<ReportStructureResponse>(`/reports/${reportId}/structure`).then((r) => r.data),
  reorderStructure: (reportId: string, body: ReorderStructureRequest) =>
    api.post<ReportStructureResponse>(`/reports/${reportId}/structure/reorder`, body).then((r) => r.data),
  chapters: (reportId: string) => api.get<Record<string, unknown>[]>(`/reports/${reportId}/chapters`).then((r) => r.data),
  createChapter: (reportId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/reports/${reportId}/chapters`, body).then((r) => r.data),
  updateChapter: (chapterId: string, body: Record<string, unknown>) => api.patch<Record<string, unknown>>(`/report-chapters/${chapterId}`, body).then((r) => r.data),
  deleteChapter: (chapterId: string) => api.delete<void>(`/report-chapters/${chapterId}`).then((r) => r.data),
  sections: (chapterId: string) => api.get<Record<string, unknown>[]>(`/report-chapters/${chapterId}/sections`).then((r) => r.data),
  createSection: (chapterId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/report-chapters/${chapterId}/sections`, body).then((r) => r.data),
  updateSection: (sectionId: string, body: Record<string, unknown>) => api.patch<Record<string, unknown>>(`/report-sections/${sectionId}`, body).then((r) => r.data),
  deleteSection: (sectionId: string) => api.delete<void>(`/report-sections/${sectionId}`).then((r) => r.data),
  refreshReferences: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/references/refresh`).then((r) => r.data),
  refreshTitlePage: (reportId: string) =>
    api.post<Record<string, unknown>>(`/reports/${reportId}/title-page/refresh`).then((r) => r.data),
  updateTitlePageDetails: (reportId: string, body: UpdateTitlePageDetailsRequest) =>
    api.post<Record<string, unknown>>(`/reports/${reportId}/title-page/details`, body).then((r) => r.data),
  refreshTableOfContents: (reportId: string) =>
    api.post<Record<string, unknown>>(`/reports/${reportId}/toc/refresh`).then((r) => r.data),
  refreshListOfFigures: (reportId: string) =>
    api.post<Record<string, unknown>>(`/reports/${reportId}/figures/refresh`).then((r) => r.data),
  refreshListOfTables: (reportId: string) =>
    api.post<Record<string, unknown>>(`/reports/${reportId}/tables/refresh`).then((r) => r.data),
  repairDeterministicNodes: (reportId: string, forceReset = false) =>
    api.post<DeterministicRepairResponse>(`/reports/${reportId}/repair-deterministic?forceReset=${forceReset}`).then((r) => r.data),
  updateSettings: (reportId: string, body: UpdateReportSettingsRequest) => api.patch<Record<string, unknown>>(`/reports/${reportId}/settings`, body).then((r) => r.data),
  literatureMatrix: (projectId: string) => api.get<LiteratureMatrixResponse>(`/projects/${projectId}/literature-matrix`).then((r) => r.data),
  saveLiteratureMatrix: (projectId: string, body: SaveLiteratureMatrixRequest) => api.post<LiteratureMatrixResponse>(`/projects/${projectId}/literature-matrix`, body).then((r) => r.data),
  generateSection: (
    sectionId: string,
    body?: GenerateSectionRequest,
  ) => api.post<Record<string, unknown>>(`/report-sections/${sectionId}/generate`, body ?? {}).then((r) => r.data),
  generateSectionForReport: (
    reportId: string,
    body?: GenerateSectionRequest,
  ) => api.post<Record<string, unknown>>(`/reports/${reportId}/generate-section`, body ?? {}).then((r) => r.data),
  validate: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/validate`).then((r) => r.data),
  assemble: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/assemble`).then((r) => r.data),
  applyTemplateFormatting: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/template-formatting/apply`).then((r) => r.data),
  finalize: (reportId: string) => api.post<Record<string, unknown>>(`/reports/${reportId}/finalize`).then((r) => r.data),
  references: (projectId: string, page = 0, size = 20, filters?: Record<string, unknown>) =>
    api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/references`, { params: { page, size, ...filters } }).then((r) => r.data),
  createReference: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references`, body).then((r) => r.data),
  updateReference: (referenceId: string, body: Record<string, unknown>) => api.patch<Record<string, unknown>>(`/references/${referenceId}`, body).then((r) => r.data),
  setCitationEnabled: (referenceId: string, enabled: boolean) => api.post<Record<string, unknown>>(`/references/${referenceId}/citation-enabled?enabled=${enabled}`).then((r) => r.data),
  setResearchEnabled: (referenceId: string, enabled: boolean) => api.post<Record<string, unknown>>(`/references/${referenceId}/research-enabled?enabled=${enabled}`).then((r) => r.data),
  setUsageScope: (referenceId: string, body: { availableForResearchAi?: boolean; availableForCitation?: boolean }) => api.post<Record<string, unknown>>(`/references/${referenceId}/usage-scope`, body).then((r) => r.data),
  rescanDocumentMetadata: (documentId: string) => api.post<Record<string, unknown>>(`/documents/${documentId}/reference-metadata/rescan`).then((r) => r.data),
  rescanProjectMetadata: (projectId: string) => api.post<Record<string, unknown>[]>(`/projects/${projectId}/documents/reference-metadata/rescan`).then((r) => r.data),
  formatCitation: (body: Record<string, unknown>) => api.post<Record<string, unknown>>('/references/format-citation', body).then((r) => r.data),
  duplicateReferences: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references/check-duplicates`, body).then((r) => r.data),
  importReferences: (projectId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/projects/${projectId}/references/import`, body).then((r) => r.data),
  importPreview: (importJobId: string) => api.get<Record<string, unknown>[]>(`/reference-imports/${importJobId}/preview`).then((r) => r.data),
  confirmReferenceImport: (importJobId: string, body: Record<string, unknown>) => api.post<Record<string, unknown>>(`/reference-imports/${importJobId}/confirm`, body).then((r) => r.data),
  exportReferencesUrl: (projectId: string, format: string) => `${api.defaults.baseURL}/projects/${projectId}/references/export?format=${encodeURIComponent(format)}`,
  exportReferences: (projectId: string, format: string) =>
    api.get<Blob>(`/projects/${projectId}/references/export`, { params: { format }, responseType: 'blob' }).then((r) => ({
      blob: r.data,
      filename: filenameFromContentDisposition(r.headers['content-disposition']) ?? `references.${format.toLowerCase()}`,
    })),
  exports: (reportId: string, body: { format: string; draft?: boolean }) => api.post<Record<string, unknown>>(`/reports/${reportId}/exports`, body).then((r) => r.data),
  exportJob: (exportId: string) => api.get<Record<string, unknown>>(`/report-exports/${exportId}`).then((r) => r.data),
  exportDownloadUrl: (exportId: string) => `${api.defaults.baseURL}/report-exports/${exportId}/download`,
  downloadExport: (exportId: string) =>
    api.get<Blob>(`/report-exports/${exportId}/download`, { responseType: 'blob' }).then((r) => ({
      blob: r.data,
      filename: filenameFromContentDisposition(r.headers['content-disposition']) ?? 'research-report',
    })),
  saveBlob: downloadBlob,
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

export const academicTemplateApi = {
  uploadGuideline: (workspaceId: string, file: File, projectId?: string | null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (projectId) {
      formData.append('projectId', projectId);
    }
    return api.post<AcademicDocumentGuidelineResponse>(
      `/workspaces/${workspaceId}/academic-templates/upload`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    ).then((r) => r.data);
  },
  getGuideline: (guidelineId: string) =>
    api.get<AcademicDocumentGuidelineResponse>(`/academic-templates/${guidelineId}`).then((r) => r.data),
  listGuidelines: (workspaceId: string, projectId?: string) =>
    api.get<AcademicDocumentGuidelineResponse[]>(`/workspaces/${workspaceId}/academic-templates`, {
      params: projectId ? { projectId } : undefined,
    }).then((r) => r.data),
  updateGuideline: (guidelineId: string, data: ExtractedAcademicTemplate) =>
    api.put<AcademicDocumentGuidelineResponse>(`/academic-templates/${guidelineId}`, data).then((r) => r.data),
  approveAndApply: (guidelineId: string, payload: { targetProjectId?: string; applyToProject: boolean; preserveExistingContent: boolean }) =>
    api.post<AcademicDocumentGuidelineResponse>(`/academic-templates/${guidelineId}/approve`, payload).then((r) => r.data),
  getDynamicToc: (reportId: string) =>
    api.get<DynamicTocResponse>(`/reports/${reportId}/dynamic-toc`).then((r) => r.data),
  validateStructure: (reportId: string, guidelineId?: string) =>
    api.get<ReportStructureValidationResponse>(`/reports/${reportId}/validate-structure`, {
      params: guidelineId ? { guidelineId } : undefined,
    }).then((r) => r.data),
  getRecommendations: (workspaceType: string, projectType?: string, institution?: string, department?: string) =>
    api.get<RecommendedTemplateItem[]>('/academic-templates/recommendations', {
      params: { workspaceType, projectType, institution, department },
    }).then((r) => r.data),
};

export const projectEvidenceApi = {
  upload: (
    projectId: string,
    file: File,
    options?: {
      evidenceType?: EvidenceType;
      caption?: string;
      description?: string;
      sectionId?: string;
      altText?: string;
    }
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (options?.evidenceType) formData.append('evidenceType', options.evidenceType);
    if (options?.caption) formData.append('caption', options.caption);
    if (options?.description) formData.append('description', options.description);
    if (options?.sectionId) formData.append('sectionId', options.sectionId);
    if (options?.altText) formData.append('altText', options.altText);
    return api
      .post<ProjectEvidenceItem>(`/projects/${projectId}/evidence/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },
  createStructured: (
    projectId: string,
    body: {
      evidenceType: EvidenceType;
      originalFilename: string;
      caption?: string;
      description?: string;
      sectionId?: string;
      figureLabel?: string;
      metadataJson?: string;
    }
  ) => api.post<ProjectEvidenceItem>(`/projects/${projectId}/evidence/structured`, body).then((r) => r.data),
  list: (projectId: string, params?: { sectionId?: string; type?: EvidenceType }) =>
    api.get<ProjectEvidenceItem[]>(`/projects/${projectId}/evidence`, { params }).then((r) => r.data),
  get: (projectId: string, evidenceId: string) =>
    api.get<ProjectEvidenceItem>(`/projects/${projectId}/evidence/${evidenceId}`).then((r) => r.data),
  update: (
    projectId: string,
    evidenceId: string,
    body: {
      caption?: string;
      description?: string;
      altText?: string;
      sectionId?: string | null;
      evidenceType?: EvidenceType;
      figureLabel?: string;
    }
  ) => api.put<ProjectEvidenceItem>(`/projects/${projectId}/evidence/${evidenceId}`, body).then((r) => r.data),
  delete: (projectId: string, evidenceId: string) =>
    api.delete<void>(`/projects/${projectId}/evidence/${evidenceId}`).then((r) => r.data),
  reorder: (projectId: string, evidenceIds: string[]) =>
    api.post<ProjectEvidenceItem[]>(`/projects/${projectId}/evidence/reorder`, { evidenceIds }).then((r) => r.data),
  analyze: (projectId: string, evidenceId: string) =>
    api.post<ProjectImageAnalysisResult>(`/projects/${projectId}/evidence/${evidenceId}/analyze`).then((r) => r.data),
  listOfFigures: (projectId: string, reportId: string) =>
    api.get<ListOfFiguresItem[]>(`/projects/${projectId}/reports/${reportId}/list-of-figures`).then((r) => r.data),
  listOfTables: (projectId: string, reportId: string) =>
    api.get<ListOfTablesItem[]>(`/projects/${projectId}/reports/${reportId}/list-of-tables`).then((r) => r.data),
};

