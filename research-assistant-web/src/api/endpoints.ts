import { api } from './client';
import type { AuthTokenResponse, Citation, DocumentItem, NotificationItem, PageResponse, ProjectMember, RagAnswer, RagConversation, ResearchProject, User, Workspace } from '../types/api';

export const authApi = {
  register: (body: { email: string; password: string; fullName?: string; name?: string }) =>
    api.post<User>('/auth/register', body).then((r) => r.data),
  login: (body: { email: string; password: string; totpCode?: string; totpChallengeToken?: string }) =>
    api.post<AuthTokenResponse>('/auth/login', body).then((r) => r.data),
  logout: (refreshToken: string) => api.post('/auth/logout', { refreshToken }).then((r) => r.data),
  logoutAll: () => api.post('/auth/logout-all').then((r) => r.data),
  forgotPassword: (email: string) => api.post('/auth/password/forgot', { email }).then((r) => r.data),
  verifyPasswordTotp: (body: Record<string, string>) => api.post('/auth/password/verify-totp', body).then((r) => r.data),
  verifyPasswordRecoveryCode: (body: Record<string, string>) => api.post('/auth/password/verify-recovery-code', body).then((r) => r.data),
  resetPassword: (body: Record<string, string>) => api.post('/auth/password/reset', body).then((r) => r.data),
  startTotpEnrollment: () => api.post('/auth/totp/enrollment').then((r) => r.data),
};

export const workspaceApi = {
  list: () => api.get<Workspace[]>('/workspaces').then((r) => r.data),
  create: (body: Record<string, unknown>) => api.post<Workspace>('/workspaces', body).then((r) => r.data),
  members: (workspaceId: string) => api.get<ProjectMember[]>(`/workspaces/${workspaceId}/members`).then((r) => r.data),
};

export const projectApi = {
  list: (workspaceId: string, page = 0, size = 20) =>
    api.get<PageResponse<ResearchProject>>(`/workspaces/${workspaceId}/projects`, { params: { page, size } }).then((r) => r.data),
  create: (workspaceId: string, body: Record<string, unknown>) =>
    api.post<ResearchProject>(`/workspaces/${workspaceId}/projects`, body).then((r) => r.data),
  get: (projectId: string) => api.get<ResearchProject>(`/projects/${projectId}`).then((r) => r.data),
  members: (projectId: string) => api.get<ProjectMember[]>(`/projects/${projectId}/members`).then((r) => r.data),
  tasks: (projectId: string) => api.get<PageResponse<Record<string, unknown>> | Record<string, unknown>[]>(`/projects/${projectId}/tasks`).then((r) => r.data),
  myTasks: (projectId: string) => api.get<PageResponse<Record<string, unknown>> | Record<string, unknown>[]>(`/projects/${projectId}/tasks/mine`).then((r) => r.data),
  comments: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/comments`).then((r) => r.data),
  reviews: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/reviews`).then((r) => r.data),
  activity: (projectId: string) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/activity`).then((r) => r.data),
  contributions: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/contributions`).then((r) => r.data),
};

export const documentApi = {
  list: (projectId: string) => api.get<DocumentItem[]>(`/projects/${projectId}/documents`).then((r) => r.data),
  processing: (documentId: string) => api.get<Record<string, unknown>>(`/documents/${documentId}/processing`).then((r) => r.data),
  downloadUrl: (documentId: string) => `${api.defaults.baseURL}/documents/${documentId}/download`,
};

export const researchApi = {
  methodologies: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/methodologies`).then((r) => r.data),
  conceptualFrameworks: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/conceptual-frameworks`).then((r) => r.data),
  theoreticalFrameworks: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/theoretical-frameworks`).then((r) => r.data),
  ethicsReadiness: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/ethics-readiness`).then((r) => r.data),
  participants: (projectId: string) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/participants`).then((r) => r.data),
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
  runs: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/analysis-runs`).then((r) => r.data),
  datasets: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/datasets`).then((r) => r.data),
  findings: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/findings`).then((r) => r.data),
  traceability: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/traceability-matrix`).then((r) => r.data),
  aiUsage: (projectId: string) => api.get<Record<string, unknown>>(`/projects/${projectId}/ai-usage-summary`).then((r) => r.data),
};

export const reportApi = {
  reports: (projectId: string) => api.get<Record<string, unknown>[]>(`/projects/${projectId}/reports`).then((r) => r.data),
  references: (projectId: string) => api.get<PageResponse<Record<string, unknown>>>(`/projects/${projectId}/references`).then((r) => r.data),
  exports: (reportId: string, body: Record<string, unknown>) => api.post(`/reports/${reportId}/exports`, body).then((r) => r.data),
  integrity: (reportId: string) => api.post(`/reports/${reportId}/integrity-review`).then((r) => r.data),
};

export const billingApi = {
  subscription: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/billing/subscription`).then((r) => r.data),
  transactions: (workspaceId: string) => api.get<Record<string, unknown>[]>(`/workspaces/${workspaceId}/billing/transactions`).then((r) => r.data),
  usage: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/usage`).then((r) => r.data),
  entitlements: (workspaceId: string) => api.get<Record<string, unknown>>(`/workspaces/${workspaceId}/entitlements`).then((r) => r.data),
  initialize: (workspaceId: string, body: { planCode: string; billingInterval: string }) =>
    api.post<Record<string, unknown>>(`/workspaces/${workspaceId}/billing/initialize`, body).then((r) => r.data),
  retry: (paymentIntentId: string) => api.post<Record<string, unknown>>(`/billing/payment-intents/${paymentIntentId}/retry`).then((r) => r.data),
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
  users: () => api.get<PageResponse<Record<string, unknown>>>('/admin/users').then((r) => r.data),
  workspaces: () => api.get<PageResponse<Record<string, unknown>>>('/admin/workspaces').then((r) => r.data),
  plans: () => api.get<Record<string, unknown>[]>('/admin/subscription-plans').then((r) => r.data),
  complimentaryAccess: () => api.get<PageResponse<Record<string, unknown>>>('/admin/complimentary-access').then((r) => r.data),
  grantComplimentaryAccess: (body: Record<string, unknown>) => api.post('/admin/complimentary-access', body).then((r) => r.data),
};
