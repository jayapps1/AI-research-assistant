export type UUID = string;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorResponse {
  type?: string;
  title?: string;
  timestamp?: string;
  requestId?: string;
  status: number;
  error?: string;
  errorCode?: string;
  message?: string;
  detail?: string;
  instance?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface ApiClientError extends Error {
  status?: number;
  code?: string;
  requestId?: string;
  validationErrors?: Record<string, string>;
  metadata?: Record<string, string>;
}

export interface User {
  id: UUID;
  email: string;
  fullName?: string;
  name?: string;
  roles?: string[];
  systemRoles?: string[];
  totpEnabled?: boolean;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds?: number;
  user?: User;
  requiresTotp?: boolean;
  totpChallengeToken?: string;
}

export interface Workspace {
  id: UUID;
  name: string;
  type?: string;
  role?: string;
  status?: string;
  memberCount?: number;
  createdAt?: string;
}

export interface ResearchProject {
  id: UUID;
  workspaceId?: UUID;
  title: string;
  description?: string;
  status?: string;
  role?: string;
  lastActivityAt?: string;
  memberCount?: number;
  readinessStatus?: string;
}

export interface ProjectMember {
  id?: UUID;
  userId?: UUID;
  name?: string;
  fullName?: string;
  email?: string;
  role?: string;
  status?: string;
}

export interface DocumentItem {
  id: UUID;
  documentId?: UUID;
  docCode?: string;
  filename?: string;
  originalFilename?: string;
  title?: string;
  version?: number;
  currentVersion?: number;
  processingStatus?: string;
  semanticIndexStatus?: string;
  uploadedBy?: string;
  uploadedAt?: string;
}

export interface NotificationItem {
  id: UUID;
  title?: string;
  message?: string;
  read?: boolean;
  targetUrl?: string;
  createdAt?: string;
  type?: string;
}

export interface RagConversation {
  id: UUID;
  title?: string;
  createdAt?: string;
}

export interface RagAnswer {
  id?: UUID;
  queryId?: UUID;
  answer?: string;
  status?: string;
  citations?: Citation[];
  evidence?: Citation[];
}

export interface Citation {
  id?: UUID;
  documentId?: UUID;
  docCode?: string;
  documentTitle?: string;
  page?: number;
  quote?: string;
  snippet?: string;
  relevance?: number;
}
