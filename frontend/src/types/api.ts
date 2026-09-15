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
  firstName?: string;
  lastName?: string;
  fullName?: string;
  name?: string;
  status?: string;
  emailVerified?: boolean;
  locale?: string;
  roles?: string[];
  systemRoles?: string[];
  totpEnabled?: boolean;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  expiresInSeconds?: number;
  user?: User;
}

export interface Workspace {
  id: UUID;
  name: string;
  type?: string;
  role?: string;
  currentUserRole?: string;
  status?: string;
  memberCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ResearchProject {
  id: UUID;
  workspaceId?: UUID;
  title: string;
  description?: string;
  status?: string;
  role?: string;
  currentUserRole?: string;
  lastActivityAt?: string;
  updatedAt?: string;
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
  pageNumber?: number;
  quote?: string;
  snippet?: string;
  supportingExcerpt?: string;
  relevance?: number;
  number?: number;
  versionNumber?: number;
  documentCode?: string;
}

export interface TotpEnrollmentResponse {
  issuer: string;
  accountName: string;
  secret: string;
  provisioningUri: string;
}

export interface RecoveryCodesResponse {
  recoveryCodes: string[];
}

export interface PaymentAttemptInitialization {
  paymentIntentId?: UUID;
  attemptId?: UUID;
  attemptNumber?: number;
  authorizationUrl?: string;
  internalReference?: string;
  status?: string;
}

export interface DatasetSummary {
  datasetId: UUID;
  recordCount: number;
  variableCount: number;
  missingValuesByVariable?: Record<string, number>;
  categoryCounts?: Record<string, Record<string, number>>;
  numericRanges?: Record<string, { min?: string; max?: string }>;
}

export interface DatasetItem {
  id: UUID;
  projectId?: UUID;
  name: string;
  description?: string;
  status?: string;
  sourceType?: string;
  createdAt?: string;
  updatedAt?: string;
  recordCount?: number;
  variableCount?: number;
}

export interface DatasetVariableItem {
  id: UUID;
  variableName: string;
  label?: string;
  type?: string;
  measurementLevel?: string;
  nullable?: boolean;
  unit?: string;
  missingValueCode?: string;
  sourceInstrumentItemId?: UUID;
  displayOrder?: number;
}

export interface DatasetCellValue {
  variableId?: UUID;
  variableName: string;
  type?: string;
  missing?: boolean;
  missingReason?: string;
  value?: string | number | boolean | null;
}

export interface DatasetRecordItem {
  id: UUID;
  rowNumber: number;
  externalRecordId?: string;
  createdAt?: string;
  values: DatasetCellValue[];
}

export interface ImportPreviewColumn {
  sourceColumn: string;
  inferredVariableName?: string;
  inferredType?: string;
  suggestedMeasurementLevel?: string;
  sampleValues?: string[];
  missingCount?: number;
  distinctCount?: number;
  warnings?: string[];
}

export interface DatasetImportPreview {
  importJobId: UUID;
  columns: ImportPreviewColumn[];
  sampleRows: Record<string, string>[];
  warnings?: string[];
}

export interface DatasetImportStart {
  importJobId: UUID;
  datasetId?: UUID;
  status?: string;
}

export interface ValidationIssue {
  id?: UUID;
  rowNumber?: number;
  variableId?: UUID;
  severity?: 'ERROR' | 'WARNING' | 'INFO' | string;
  code?: string;
  message?: string;
  rejectedValueSnapshot?: string;
}
