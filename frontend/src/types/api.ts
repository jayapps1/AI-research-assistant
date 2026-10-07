export type UUID = string;

export interface PageResponse<T> {
  content: T[];
  page?: number;
  number?: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

export interface PaymentTransaction {
  id: UUID;
  workspaceId: UUID;
  reference: string;
  internalReference?: string;
  environment: 'TEST' | 'LIVE' | string;
  status: 'SUCCESS' | 'FAILED' | 'PENDING' | 'INITIALIZED' | 'CANCELLED' | string;
  amount: number;
  currency: string;
  planCode: string;
  billingInterval: 'NONE' | 'MONTHLY' | 'YEARLY' | string;
  createdAt: string;
  paymentIntentId?: UUID;
}

export type PaymentTransactionPage = PageResponse<PaymentTransaction>;

export interface PlanPriceBreakdown {
  planCode: string;
  billingInterval: string;
  currency: string;
  baseAmount: number;
  processingRate: number;
  processingAmount: number;
  aiGenerationRate: number;
  aiGenerationAmount: number;
  totalAmount: number;
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
  phoneNumber?: string | null;
  roles?: string[];
  systemRoles?: string[];
  totpEnabled?: boolean;
  avatarUrl?: string | null;
  profileImage?: ProfileImageInfo | null;
}

export interface ProfileImageInfo {
  id: UUID;
  status: 'ACTIVE' | 'REPLACED' | 'DELETED';
  contentType: string;
  fileSizeBytes: number;
  width?: number | null;
  height?: number | null;
  publicUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UserProfileResponse {
  id?: UUID;
  userId?: UUID;
  email: string;
  firstName: string | null;
  lastName: string | null;
  fullName?: string | null;
  displayName?: string | null;
  phoneNumber?: string | null;
  locale?: string | null;
  avatarUrl?: string | null;
  profileImage?: ProfileImageInfo | null;
  systemRoles?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  locale?: string | null;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  expiresInSeconds?: number;
  user?: User;
}

export interface LoginChallengeResponse {
  status: 'TOTP_REQUIRED';
  challengeId: string;
  authenticationMethod: 'PASSWORD_AND_TOTP';
  expiresIn: number;
  email?: string;
}

export type LoginResponse = AuthTokenResponse | LoginChallengeResponse;

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

export type AcademicWorkspaceType = 'ACADEMIC_RESEARCH' | 'ACADEMIC_PROJECT' | 'COURSEWORK';

export type AcademicProjectType =
  | 'SOFTWARE_SYSTEM_DEVELOPMENT'
  | 'ENGINEERING_PROJECT'
  | 'RESEARCH_BASED_PROJECT'
  | 'BUSINESS_PROJECT'
  | 'GENERAL_ACADEMIC_PROJECT'
  | 'OTHER';

export interface ResearchProject {
  id: UUID;
  workspaceId?: UUID;
  title: string;
  description?: string;
  workspaceType?: AcademicWorkspaceType;
  workspaceTypeLabel?: string;
  projectType?: AcademicProjectType;
  reportType?: string;
  finalDocumentLabel?: string;
  workAreaLabel?: string;
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
  reportTemplateId?: UUID;
  reportTemplateName?: string;
  citationStyle?: string;
  citationStyleLocked?: boolean;
  citationPresentation?: string;
  bibliographySort?: string;
  includeDoi?: boolean;
  includeUrl?: boolean;
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

export interface DocumentVersionSummary {
  versionId: string;
  versionNumber: number;
  originalFilename: string;
  mimeType: string;
  fileSizeBytes: number;
  checksumSha256: string;
  scanStatus: string;
  quarantined: boolean;
  status: string;
  uploadedBy?: string;
  uploadedAt: string;
}

export interface DocumentItem {
  id: UUID;
  projectId?: UUID;
  documentId?: UUID;
  documentNumber?: number;
  documentCode?: string;
  docCode?: string;
  filename?: string;
  originalFilename?: string;
  title?: string;
  bibliographicTitle?: string;
  authors?: string;
  publicationYear?: number | null;
  journal?: string | null;
  conference?: string | null;
  publisher?: string | null;
  volume?: string | null;
  issue?: string | null;
  pages?: string | null;
  doi?: string | null;
  url?: string | null;
  sourceType?: string | null;
  keywords?: string | null;
  bibliographicMetadataStatus?: 'COMPLETE' | 'PARTIAL' | 'INCOMPLETE' | 'NEEDS_REVIEW' | 'VERIFIED' | string;
  bibliographicMetadataSource?: string | null;
  bibliographicMetadataConfidence?: number | null;
  bibliographicMetadataExtractedAt?: string | null;
  version?: number;
  currentVersion?: DocumentVersionSummary | null;
  status?: string;
  processingStatus?: string;
  semanticIndexStatus?: string;
  uploadedBy?: string;
  uploadedAt?: string;
  pageCount?: number | null;
  chunkCount?: number | null;
  type?: string;
  academicRole?: AcademicFileRole;
  createdAt?: string;
  updatedAt?: string;
  archivedAt?: string | null;
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

export type ConversationSearchScope = 'GENERAL' | 'WEB' | 'PROJECT' | 'PROJECT_WEB';
export type ConversationSourceScope = 'PROJECT_ALL_DOCUMENTS' | 'SELECTED_DOCUMENTS';
export type ConversationType = 'GENERAL' | 'PROJECT';
export type ConversationStatus = 'ACTIVE' | 'ARCHIVED' | 'TRASHED';
export type ConversationMessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';
export type ConversationRunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface ConversationSummary {
  id: UUID;
  userId: UUID;
  workspaceId?: UUID | null;
  workspaceName?: string | null;
  projectId?: UUID | null;
  projectTitle?: string | null;
  type: ConversationType;
  status: ConversationStatus;
  title: string;
  createdAt?: string;
  updatedAt?: string;
  lastMessageAt?: string;
  archivedAt?: string | null;
  deletedAt?: string | null;
}

export interface MessageCitation {
  id?: UUID;
  number: number;
  sourceId?: UUID;
  sourceType?: string;
  title?: string;
  url?: string | null;
  documentId?: UUID | null;
  documentCode?: string | null;
  documentTitle?: string | null;
  documentVersionId?: UUID | null;
  versionNumber?: number | null;
  projectReferenceId?: UUID | null;
  pageNumber?: number | null;
  chunkNumber?: number | null;
  supportingExcerpt?: string | null;
  formattedCitation?: string | null;
}

export interface ConversationMessage {
  id: UUID;
  role: ConversationMessageRole;
  content: string;
  structuredContent?: string | null;
  sequenceNumber: number;
  createdAt?: string;
  updatedAt?: string | null;
  editedAt?: string | null;
  citations: MessageCitation[];
}

export interface ConversationRun {
  id: UUID;
  userMessageId?: UUID | null;
  assistantMessageId?: UUID | null;
  aiRequestId?: UUID | null;
  ragQueryId?: UUID | null;
  operationType: string;
  searchScope: ConversationSearchScope;
  status: ConversationRunStatus;
  provider?: string | null;
  model?: string | null;
  inputTokens?: number | null;
  outputTokens?: number | null;
  totalTokens?: number | null;
  cachedInputTokens?: number | null;
  providerCost?: number | null;
  platformCredits?: number | null;
  errorCode?: string | null;
  failureMessage?: string | null;
  startedAt?: string;
  completedAt?: string | null;
}

export interface ConversationDetail {
  conversation: ConversationSummary;
  messages: ConversationMessage[];
  latestRun?: ConversationRun | null;
}

export interface SubmitConversationResponse {
  conversation: ConversationSummary;
  userMessage: ConversationMessage;
  assistantMessage?: ConversationMessage | null;
  run: ConversationRun;
}

export interface ConversationAttachment {
  id: UUID;
  conversationId: UUID;
  originalFilename: string;
  mediaType: string;
  sizeBytes: number;
  checksumSha256?: string;
  status: string;
  createdAt: string;
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
  formattedCitation?: string;
  metadataComplete?: boolean;
  metadataWarning?: string | null;
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

export interface AiUsageSummary {
  requestsToday: number;
  requestsThisMonth: number;
  tokensThisMonth: number;
  requestLimit: number | null;
  tokenLimit: number | null;
  isUnlimited: boolean;
  resetDate?: string;
}

export interface UserTaskSummary {
  id: UUID;
  projectId: UUID;
  projectTitle: string;
  title: string;
  status: string;
  priority: string;
  dueDate?: string;
  overdue: boolean;
  updatedAt?: string;
}

export interface UserActivitySummary {
  id: UUID;
  projectId: UUID;
  projectTitle: string;
  type: string;
  summary: string;
  actorName: string;
  occurredAt: string;
}

export interface UserDashboardResponse {
  greeting: string;
  currentWorkspace: Workspace;
  activeProjectCount: number;
  openTaskCount: number;
  documentCount: number;
  unreadNotifications: number;
  aiUsage: AiUsageSummary;
  recentProjects: ResearchProject[];
  recentTasks: UserTaskSummary[];
  recentActivities: UserActivitySummary[];
}

export interface WorkspaceDashboardResponse {
  workspace: Workspace;
  currentUserRole: string;
  memberCount: number;
  projectCount: number;
  activeProjectCount: number;
  documentCount: number;
  storageBytes: number;
  aiRequestsThisMonth: number;
  planCode?: string;
  recentProjects: ResearchProject[];
}

export interface DocumentMetricsSummary {
  total: number;
  ready: number;
  processing: number;
  failed: number;
}

export interface TaskMetricsSummary {
  total: number;
  todo: number;
  inProgress: number;
  inReview: number;
  completed: number;
  overdue: number;
}

export interface ResearchProgressSummary {
  completedStages: number;
  totalStages: number;
  percentComplete: number;
  nextIncompleteStage?: string;
  nextStageUrl?: string;
}

export interface ProjectDashboardResponse {
  project: ResearchProject;
  currentUserRole: string;
  memberCount: number;
  documents: DocumentMetricsSummary;
  tasks: TaskMetricsSummary;
  researchProgress: ResearchProgressSummary;
  recentActivities: UserActivitySummary[];
}

export interface ResearchStageDto {
  number: number;
  key: string;
  name: string;
  status: 'NOT_STARTED' | 'DRAFT' | 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETE' | string;
  description: string;
  itemCount: number;
  actionUrl: string;
  lastUpdated?: string;
}

export interface ResearchProgressResponse {
  projectId: UUID;
  projectTitle: string;
  completedStages: number;
  totalStages: number;
  percentComplete: number;
  nextIncompleteStage?: string;
  nextStageUrl?: string;
  stages: ResearchStageDto[];
}

export interface MyTasksResponse {
  openCount: number;
  inProgressCount: number;
  inReviewCount: number;
  overdueCount: number;
}

export interface MyDocumentsResponse {
  totalCount: number;
  readyCount: number;
  processingCount: number;
  failedCount: number;
}

export type LimitMode = 'LIMITED' | 'UNLIMITED' | 'DISABLED';

export interface AdminSubscriptionPlan {
  id: UUID;
  code: string;
  name: string;
  description?: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  billingInterval: 'NONE' | 'MONTHLY' | 'YEARLY';
  price: number;
  yearlyPrice?: number | null;
  currency: string;
  publiclyAvailable: boolean;
  featured: boolean;
  displayOrder: number;
  workspacesSubscribed: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubscriptionPlanRequest {
  code: string;
  name: string;
  description?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  billingInterval: 'NONE' | 'MONTHLY' | 'YEARLY';
  price: number;
  yearlyPrice?: number | null;
  currency: string;
  publiclyAvailable?: boolean;
  featured?: boolean;
  displayOrder?: number;
}

export interface UpdateSubscriptionPlanRequest {
  name?: string;
  description?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  billingInterval?: 'NONE' | 'MONTHLY' | 'YEARLY';
  price?: number;
  yearlyPrice?: number | null;
  currency?: string;
  publiclyAvailable?: boolean;
  featured?: boolean;
  displayOrder?: number;
}

export interface AdminPlanEntitlement {
  feature: string;
  enabled: boolean;
  limitMode: LimitMode;
  limitValue?: number | null;
  limitUnit: string;
  formattedValue?: string;
}

export interface AdminPaymentAttemptDetail {
  id: UUID;
  attemptNumber: number;
  internalReference: string;
  providerReference?: string | null;
  status: string;
  expectedAmount: number;
  currency: string;
  providerStatus?: string | null;
  failureCode?: string | null;
  failureMessageSafe?: string | null;
  authorizationUrl?: string | null;
  createdAt: string;
  completedAt?: string | null;
  providerVerifiedAt?: string | null;
}

export interface AdminPaymentItem {
  id: UUID;
  workspaceId: UUID;
  workspaceName: string;
  userId: UUID;
  userEmail: string;
  userName: string;
  planCode: string;
  planName: string;
  billingInterval: string;
  amount: number;
  currency: string;
  environment: string;
  intentStatus: string;
  attemptCount: number;
  latestAttemptStatus: string;
  latestReference?: string | null;
  latestProviderReference?: string | null;
  latestProviderStatus?: string | null;
  latestFailureCode?: string | null;
  latestFailureMessage?: string | null;
  createdAt: string;
  settledAt?: string | null;
  latestVerifiedAt?: string | null;
  attempts: AdminPaymentAttemptDetail[];
}

export interface PaymentAttemptDetail {
  paymentAttemptId: UUID;
  paymentIntentId: UUID;
  workspaceId: UUID;
  planCode: string;
  planName: string;
  expectedAmount: number;
  currency: string;
  attemptNumber: number;
  status: string;
  providerStatus?: string | null;
  internalReference: string;
  providerReference?: string | null;
  authorizationUrl?: string | null;
  failureCode?: string | null;
  failureMessageSafe?: string | null;
  retryable: boolean;
  createdAt: string;
  completedAt?: string | null;
  providerVerifiedAt?: string | null;
}

export interface AiCreditBalance {
  included: {
    limitMode: 'LIMITED' | 'UNLIMITED' | 'DISABLED';
    limit?: number | null;
    used?: number;
    remaining?: number | null;
    periodStart?: string;
    periodEnd?: string;
  };
  promotional: {
    remaining: number;
  };
  purchased: {
    remaining: number;
  };
  totalAvailable: number;
}

export interface AiCreditPack {
  id: UUID;
  code: string;
  name: string;
  description?: string;
  creditAmount: number;
  priceAmount: number;
  credits?: number;
  price?: number;
  currency: string;
  active: boolean;
  displayOrder: number;
  createdAt: string;
}

export interface AiCreditLedgerItem {
  id: UUID;
  bucket: 'PURCHASED' | 'PROMOTIONAL';
  type: string;
  creditAmount: number;
  balanceBefore: number;
  balanceAfter: number;
  sourceType: string;
  sourceId?: string;
  aiRequestId?: string;
  paymentIntentId?: string;
  paymentAttemptId?: string;
  purchaseId?: string;
  createdByName?: string;
  createdAt: string;
}

export interface CreateAiCreditPackRequest {
  code: string;
  name: string;
  description?: string;
  creditAmount: number;
  priceAmount: number;
  currency?: string;
  active?: boolean;
  displayOrder?: number;
}

export interface UpdateAiCreditPackRequest {
  name?: string;
  description?: string;
  creditAmount?: number;
  priceAmount?: number;
  currency?: string;
  active?: boolean;
  displayOrder?: number;
}

export interface AdminGrantAiCreditsRequest {
  creditAmount: number;
  bucket: 'PURCHASED' | 'PROMOTIONAL';
  reason: string;
}

export interface ReportTemplateItem {
  id: UUID;
  name: string;
  type?: string;
  institution?: string;
  description?: string;
  citationStyle?: string;
  defaultCitationStyle?: string;
  citationStyleLocked?: boolean;
  department?: string;
  supportedWorkspaceTypes?: string;
  active?: boolean;
  configurationJson?: string;
}

export interface SectionCapabilityItem {
  sectionId: UUID;
  chapterNumber?: number;
  chapterTitle?: string;
  sectionNumber?: string;
  sectionTitle?: string;
  status: 'READY' | 'DATA_REQUIRED' | 'FINDINGS_REQUIRED' | string;
  reason?: string;
  canGenerate: boolean;
  hasDataset: boolean;
  hasFindings: boolean;
  readyDocumentCount: number;
}

export interface SectionCapabilitiesResponse {
  projectId: UUID;
  canGenerateEmpirical: boolean;
  hasDataset: boolean;
  hasFindings: boolean;
  readyDocumentCount: number;
  sections: SectionCapabilityItem[];
}

export interface TableOfContentsSectionItem {
  sectionId?: UUID;
  sectionNumber?: string;
  title?: string;
  heading?: string;
  displayOrder?: number;
  type?: string;
  pageNumber?: number;
}

export interface TableOfContentsItem {
  chapterId?: UUID;
  chapterNumber?: number;
  title: string;
  displayOrder?: number;
  pageNumber?: number;
  sections: TableOfContentsSectionItem[];
}

export interface TableOfContentsResponse {
  reportId: UUID;
  title?: string;
  reportTitle?: string;
  items?: TableOfContentsItem[];
  chapters?: TableOfContentsItem[];
  formattedMarkdown?: string;
}

export type LiteratureMatrixInclusion = 'EXCLUDED' | 'CHAPTER_TWO' | 'APPENDIX';

export interface SectionStructureResponse {
  id: UUID;
  chapterId: UUID;
  parentSectionId?: UUID | null;
  sectionNumber?: string | null;
  title?: string;
  heading?: string;
  type?: string;
  sectionType: string;
  status: string;
  displayOrder: number;
  required: boolean;
  systemDefined: boolean;
  aiEnabled: boolean;
  semanticPurpose?: SectionSemanticPurpose;
  generationPolicy?: SectionGenerationPolicy;
  wordCount: number;
  citationCount: number;
  subsections: SectionStructureResponse[];
}

export interface ChapterStructureResponse {
  id: UUID;
  type?: string;
  chapterNumber?: number | null;
  title: string;
  description?: string | null;
  displayOrder: number;
  required: boolean;
  systemDefined: boolean;
  sections: SectionStructureResponse[];
}

export interface ReportStructureResponse {
  id: UUID;
  projectId: UUID;
  title: string;
  status: string;
  citationStyle: string;
  includeUncitedReferences: boolean;
  literatureMatrixInclusion: LiteratureMatrixInclusion;
  chapters: ChapterStructureResponse[];
}

export interface ReorderItem {
  id: UUID;
  parentId?: UUID | null;
  displayOrder: number;
}

export interface ReorderStructureRequest {
  chapters?: ReorderItem[];
  sections?: ReorderItem[];
}

export interface UpdateReportSettingsRequest {
  citationStyle?: string;
  includeUncitedReferences?: boolean;
  literatureMatrixInclusion?: LiteratureMatrixInclusion;
}

export interface LiteratureMatrixResponse {
  id: UUID;
  projectId: UUID;
  title: string;
  matrixDataJson?: string | null;
  markdownTable?: string | null;
  updatedAt: string;
}

export interface SaveLiteratureMatrixRequest {
  title?: string;
  matrixDataJson?: string;
  markdownTable?: string;
}

export type SectionSemanticPurpose =
  | 'TITLE_PAGE'
  | 'TABLE_OF_CONTENTS'
  | 'LIST_OF_FIGURES'
  | 'LIST_OF_TABLES'
  | 'DECLARATION'
  | 'CERTIFICATION'
  | 'DEDICATION'
  | 'ACKNOWLEDGEMENTS'
  | 'ABSTRACT'
  | 'BACKGROUND'
  | 'PROBLEM_STATEMENT'
  | 'OBJECTIVES'
  | 'RESEARCH_QUESTIONS'
  | 'SIGNIFICANCE'
  | 'SCOPE'
  | 'LIMITATIONS'
  | 'LITERATURE_REVIEW'
  | 'RELATED_SYSTEMS'
  | 'SYSTEM_REQUIREMENTS'
  | 'SYSTEM_DESIGN'
  | 'METHODOLOGY'
  | 'IMPLEMENTATION'
  | 'TESTING'
  | 'FINDINGS'
  | 'DISCUSSION'
  | 'CONCLUSIONS'
  | 'RECOMMENDATIONS'
  | 'FUTURE_WORK'
  | 'REFERENCES'
  | 'APPENDICES'
  | 'CUSTOM';

export type SectionGenerationPolicy =
  | 'DETERMINISTIC'
  | 'USER_AUTHORED_FRONT_MATTER'
  | 'SOURCE_GROUNDED_AI'
  | 'PROJECT_DERIVED_AI'
  | 'PROJECT_EVIDENCE_REQUIRED'
  | 'CONTEXTUAL_AI';

export interface GenerateSectionRequest {
  documentIds?: UUID[];
  generationType?: string;
  targetNodeId?: UUID;
  targetNodeTitle?: string;
  sourceScope?: string;
  instructions?: string;
  evidenceLimit?: number;
  applyMode?: 'PREVIEW' | 'APPEND' | 'REPLACE';
}

export interface UpdateTitlePageDetailsRequest {
  title?: string;
  authorName?: string;
  studentId?: string;
  institutionName?: string;
  departmentName?: string;
  degreeProgram?: string;
  supervisorName?: string;
  academicYear?: string;
  submissionYear?: number;
}

export interface DeterministicRepairResponse {
  repairedSectionsCount: number;
  repairedSectionTitles: string[];
  message: string;
}

export type AcademicFileRole = 'RESEARCH_SOURCE' | 'TEMPLATE_GUIDELINE' | 'EXAMPLE_REPORT';

export type SectionRequirementLevel = 'REQUIRED' | 'RECOMMENDED' | 'OPTIONAL';

export interface TemplateMargins {
  top: string;
  bottom: string;
  left: string;
  right: string;
}

export interface TemplateFormattingRules {
  fontFamily: string;
  bodyFontSize: string;
  heading1FontSize: string;
  heading2FontSize: string;
  lineSpacing: string;
  margins: TemplateMargins;
  numberingStyle: string;
  pageNumbering: string;
  chapterBreak: string;
}

export interface UncertainItem {
  field: string;
  reason: string;
}

export interface TemplateSectionDefinition {
  sectionNumber?: string | null;
  heading: string;
  requirementLevel: SectionRequirementLevel;
  semanticPurpose?: SectionSemanticPurpose | null;
  generationPolicy?: SectionGenerationPolicy | null;
  description?: string | null;
  subsections: TemplateSectionDefinition[];
}

export interface TemplateChapterDefinition {
  chapterNumber?: number | null;
  title: string;
  type: string;
  required: boolean;
  sections: TemplateSectionDefinition[];
}

export interface ExtractedAcademicTemplate {
  institution: string;
  department: string;
  programme: string;
  documentType: string;
  citationStyle: string;
  formattingRules: TemplateFormattingRules;
  frontMatter: TemplateSectionDefinition[];
  chapters: TemplateChapterDefinition[];
  appendices: TemplateSectionDefinition[];
  uncertainItems: UncertainItem[];
  rawTextPreview?: string | null;
}

export interface AcademicDocumentGuidelineResponse {
  id: UUID;
  workspaceId: UUID;
  projectId?: UUID | null;
  documentId?: UUID | null;
  originalFileName: string;
  sourceType: string;
  status: 'EXTRACTED' | 'EDITED' | 'APPROVED' | 'TEMPLATE_PROCESSING_FAILED' | 'PENDING_REVIEW' | 'REJECTED';
  version: number;
  extractedTemplate: ExtractedAcademicTemplate;
  uploadedAt: string;
  approvedAt?: string | null;
  approvedBy?: string | null;
}

export interface DynamicTocItem {
  id: string;
  chapterNumber?: number | null;
  sectionNumber?: string | null;
  title: string;
  pageNumber: number;
  level: number;
  requirementLevel?: SectionRequirementLevel;
}

export interface DynamicTocResponse {
  reportId: UUID;
  reportTitle: string;
  items: DynamicTocItem[];
  generatedAt: string;
}

export interface MissingRequiredSection {
  chapterTitle: string;
  heading: string;
  sectionNumber?: string | null;
  semanticPurpose?: string | null;
  description?: string | null;
}

export interface ReportStructureValidationResponse {
  valid: boolean;
  totalRequired: number;
  totalPresent: number;
  missingRequiredCount: number;
  missingRequiredSections: MissingRequiredSection[];
  warnings: string[];
}

export interface RecommendedTemplateItem {
  guidelineId?: UUID | null;
  templateId?: UUID | null;
  name: string;
  institution?: string | null;
  department?: string | null;
  programme?: string | null;
  documentType: string;
  relevanceScore: number;
  matchReasons: string[];
  builtIn: boolean;
}

export type EvidenceType =
  | 'SCREENSHOT'
  | 'FIGURE'
  | 'DIAGRAM'
  | 'ARCHITECTURE_DIAGRAM'
  | 'USE_CASE_DIAGRAM'
  | 'ER_DIAGRAM'
  | 'FLOWCHART'
  | 'TABLE'
  | 'CHART'
  | 'TEST_RESULT'
  | 'SYSTEM_OUTPUT'
  | 'DATASET_RESULT'
  | 'PHOTO'
  | 'USER_NOTE'
  | 'OTHER';

export type EvidenceAnalysisStatus = 'NOT_ANALYZED' | 'ANALYZING' | 'ANALYZED' | 'UNAVAILABLE' | 'FAILED';

export interface ProjectEvidenceItem {
  id: UUID;
  projectId: UUID;
  workspaceId: UUID;
  reportId?: UUID | null;
  sectionId?: UUID | null;
  sectionNumber?: string | null;
  sectionHeading?: string | null;
  chapterTitle?: string | null;
  chapterNumber?: number | null;
  storageObjectId?: UUID | null;
  downloadUrl?: string | null;
  originalFilename: string;
  mimeType: string;
  fileSizeBytes: number;
  evidenceType: EvidenceType;
  figureLabel: string;
  caption?: string | null;
  renderedCaption: string;
  description?: string | null;
  altText?: string | null;
  displayOrder: number;
  aiVisualAnalysis?: string | null;
  aiAnalysisStatus: EvidenceAnalysisStatus;
  aiAnalysisError?: string | null;
  metadataJson?: string | null;
  createdById?: UUID | null;
  createdByEmail?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectImageAnalysisResult {
  status: EvidenceAnalysisStatus;
  visibleSummary?: string | null;
  observableComponents: string[];
  observableWorkflow?: string | null;
  sectionRelevance?: string | null;
  rawAnalysis?: string | null;
  errorMessage?: string | null;
}

export interface ListOfFiguresItem {
  evidenceId: UUID;
  figureLabel: string;
  caption?: string | null;
  renderedCaption: string;
  chapterNumber?: number | null;
  chapterTitle?: string | null;
  sectionNumber?: string | null;
  sectionHeading?: string | null;
  pageNumber: number;
  evidenceType: EvidenceType;
  downloadUrl?: string | null;
}

export interface ListOfTablesItem {
  evidenceId: UUID;
  tableLabel: string;
  caption?: string | null;
  renderedCaption: string;
  chapterNumber?: number | null;
  chapterTitle?: string | null;
  sectionNumber?: string | null;
  sectionHeading?: string | null;
  pageNumber: number;
  metadataJson?: string | null;
}

