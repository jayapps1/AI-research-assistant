export type UUID = string;

export interface PublicSiteSettingsResponse {
  id?: UUID;
  siteName: string;
  tagline?: string;
  supportEmail?: string;
  supportPhone?: string;
  address?: string;
  logoUrl?: string;
  faviconUrl?: string;
  twitterUrl?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  privacyPolicyUrl?: string;
  termsOfServiceUrl?: string;
  defaultMetaTitle?: string;
  defaultMetaDescription?: string;
  copyrightText?: string;
  registrationEnabled: boolean;
  publicPricingEnabled: boolean;
}

export type PublicPageType = 'HOME' | 'SERVICES' | 'PRICING' | 'ABOUT' | 'FAQ' | 'CONTACT' | 'CUSTOM';
export type PublicPageStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type PublicSectionType =
  | 'HERO'
  | 'FEATURES'
  | 'METHODOLOGY'
  | 'ETHICS_CALLOUT'
  | 'PRICING_MATRIX'
  | 'STATS_COUNTER'
  | 'FAQ_ACCORDION'
  | 'TESTIMONIALS'
  | 'CTA_BANNER'
  | 'TEXT'
  | 'CONTACT_FORM';

export interface PublicPageSectionResponse {
  id: UUID;
  type: PublicSectionType;
  sectionKey: string;
  eyebrow?: string;
  heading?: string;
  subheading?: string;
  body?: string;
  imageStorageKey?: string;
  primaryCtaLabel?: string;
  primaryCtaUrl?: string;
  secondaryCtaLabel?: string;
  secondaryCtaUrl?: string;
  configurationJson?: string;
  enabled: boolean;
  displayOrder: number;
}

export interface PublicPageResponse {
  id: UUID;
  type: PublicPageType;
  slug: string;
  title: string;
  subtitle?: string;
  metaTitle?: string;
  metaDescription?: string;
  status: PublicPageStatus;
  showInNavigation: boolean;
  navigationOrder: number;
  publishedAt?: string;
  sections: PublicPageSectionResponse[];
}

export interface PublicPageSummaryResponse {
  id: UUID;
  type: PublicPageType;
  slug: string;
  title: string;
  status: PublicPageStatus;
  showInNavigation: boolean;
  navigationOrder: number;
}

export interface PublicServiceOfferingResponse {
  id: UUID;
  slug: string;
  name: string;
  tagline?: string;
  description: string;
  academicBenefit?: string;
  category?: string;
  iconName?: string;
  featured: boolean;
  displayOrder: number;
}

export type FaqCategory =
  | 'GENERAL'
  | 'SECURITY_AND_COMPLIANCE'
  | 'METHODOLOGY_AND_DATA'
  | 'BILLING_AND_LICENSING'
  | 'ACADEMIC_INTEGRITY';

export interface PublicFaqResponse {
  id: UUID;
  category: FaqCategory;
  question: string;
  answerMarkdown: string;
  displayOrder: number;
}

export interface PublicPricingTierResponse {
  id: UUID;
  code: string;
  name: string;
  description?: string;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  displayOrder: number;
  highlighted: boolean;
  highlightBadge?: string;
  ctaLabel?: string;
  ctaUrl?: string;
  features: string[];
  academicDiscountsAvailable?: string;
}

export interface PublicContactSubmissionRequest {
  name: string;
  email: string;
  phone?: string;
  organization?: string;
  academicRole?: string;
  subject: string;
  message: string;
  preferredChannel?: string;
  captchaToken?: string;
  honeypot?: string;
}

export interface PublicContactSubmissionResponse {
  referenceCode: string;
  message: string;
  receivedAt: string;
}

export type ContactSubmissionStatus = 'NEW' | 'READ' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'SPAM';
export type ContactResponseChannel = 'EMAIL' | 'INTERNAL_NOTE' | 'PHONE';

export interface AdminContactResponseDto {
  id: UUID;
  channel: ContactResponseChannel;
  status: string;
  messageBody: string;
  recipientEmail?: string;
  respondedByName?: string;
  sentAt?: string;
  createdAt: string;
}

export interface AdminContactSubmissionResponse {
  id: UUID;
  referenceCode: string;
  name: string;
  email: string;
  phone?: string;
  organization?: string;
  academicRole?: string;
  subject: string;
  message: string;
  preferredChannel?: string;
  status: ContactSubmissionStatus;
  source: string;
  assignedToUserId?: UUID;
  assignedToUserName?: string;
  firstReadAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  submittedAt: string;
  responses: AdminContactResponseDto[];
}

export interface UpdateSiteSettingsRequest {
  siteName: string;
  tagline?: string;
  supportEmail?: string;
  supportPhone?: string;
  address?: string;
  logoUrl?: string;
  faviconUrl?: string;
  twitterUrl?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  privacyPolicyUrl?: string;
  termsOfServiceUrl?: string;
  defaultMetaTitle?: string;
  defaultMetaDescription?: string;
  copyrightText?: string;
  registrationEnabled?: boolean;
  publicPricingEnabled?: boolean;
}

export interface CreateOrUpdatePageRequest {
  type: PublicPageType;
  slug: string;
  title: string;
  subtitle?: string;
  metaTitle?: string;
  metaDescription?: string;
  status?: PublicPageStatus;
  showInNavigation?: boolean;
  navigationOrder?: number;
}

export interface CreateOrUpdateSectionRequest {
  type: PublicSectionType;
  sectionKey: string;
  eyebrow?: string;
  heading?: string;
  subheading?: string;
  body?: string;
  imageStorageKey?: string;
  primaryCtaLabel?: string;
  primaryCtaUrl?: string;
  secondaryCtaLabel?: string;
  secondaryCtaUrl?: string;
  configurationJson?: string;
  enabled?: boolean;
  displayOrder?: number;
}

export interface CreateOrUpdateServiceRequest {
  slug: string;
  name: string;
  tagline?: string;
  description: string;
  academicBenefit?: string;
  category?: string;
  iconName?: string;
  status?: string;
  featured?: boolean;
  displayOrder?: number;
}

export interface CreateOrUpdateFaqRequest {
  category: FaqCategory;
  question: string;
  answerMarkdown: string;
  displayOrder?: number;
  enabled?: boolean;
}
