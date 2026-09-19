import { api } from './client';
import type { PageResponse } from '../types/api';
import type {
  AdminContactResponseDto,
  AdminContactSubmissionResponse,
  AdminPublicStatistic,
  CreateOrUpdateFaqRequest,
  CreateOrUpdatePageRequest,
  CreateOrUpdateSectionRequest,
  CreateOrUpdateServiceRequest,
  CreatePublicStatisticRequest,
  MetricPreviewResponse,
  PublicContactSubmissionRequest,
  PublicContactSubmissionResponse,
  PublicFaqResponse,
  PublicPageResponse,
  PublicPageSectionResponse,
  PublicPageSummaryResponse,
  PublicPricingTierResponse,
  PublicServiceOfferingResponse,
  PublicSiteSettingsResponse,
  PublicStatistic,
  PublicSystemMetric,
  UpdatePublicStatisticRequest,
  UpdateSiteSettingsRequest,
} from '../types/publicSite';

export const publicApi = {
  // Public (Visitor) Endpoints
  getSiteSettings: async (): Promise<PublicSiteSettingsResponse> => {
    const { data } = await api.get<PublicSiteSettingsResponse>('/public/site');
    return data;
  },

  getNavigationPages: async (): Promise<PublicPageSummaryResponse[]> => {
    const { data } = await api.get<PublicPageSummaryResponse[]>('/public/pages');
    return data;
  },

  getPageBySlug: async (slug: string): Promise<PublicPageResponse> => {
    const { data } = await api.get<PublicPageResponse>(`/public/pages/${encodeURIComponent(slug)}`);
    return data;
  },

  getServices: async (): Promise<PublicServiceOfferingResponse[]> => {
    const { data } = await api.get<PublicServiceOfferingResponse[]>('/public/services');
    return data;
  },

  getFaqs: async (category?: string): Promise<PublicFaqResponse[]> => {
    const { data } = await api.get<PublicFaqResponse[]>('/public/faqs', {
      params: category ? { category } : undefined,
    });
    return data;
  },

  getPricing: async (): Promise<PublicPricingTierResponse[]> => {
    const { data } = await api.get<PublicPricingTierResponse[]>('/public/pricing');
    return data;
  },

  getStatistics: async (): Promise<PublicStatistic[]> => {
    const { data } = await api.get<PublicStatistic[]>('/public/statistics');
    return data;
  },

  submitContact: async (payload: PublicContactSubmissionRequest): Promise<PublicContactSubmissionResponse> => {
    const { data } = await api.post<PublicContactSubmissionResponse>('/public/contact', payload);
    return data;
  },

  // Admin Public Site Management Endpoints
  getAdminSiteSettings: async (): Promise<PublicSiteSettingsResponse> => {
    const { data } = await api.get<PublicSiteSettingsResponse>('/admin/public-site/settings');
    return data;
  },

  updateAdminSiteSettings: async (payload: UpdateSiteSettingsRequest): Promise<PublicSiteSettingsResponse> => {
    const { data } = await api.put<PublicSiteSettingsResponse>('/admin/public-site/settings', payload);
    return data;
  },

  getAdminPages: async (): Promise<PublicPageSummaryResponse[]> => {
    const { data } = await api.get<PublicPageSummaryResponse[]>('/admin/public-site/pages');
    return data;
  },

  getAdminPage: async (pageId: string): Promise<PublicPageResponse> => {
    const { data } = await api.get<PublicPageResponse>(`/admin/public-site/pages/${pageId}`);
    return data;
  },

  createAdminPage: async (payload: CreateOrUpdatePageRequest): Promise<PublicPageResponse> => {
    const { data } = await api.post<PublicPageResponse>('/admin/public-site/pages', payload);
    return data;
  },

  updateAdminPage: async (pageId: string, payload: CreateOrUpdatePageRequest): Promise<PublicPageResponse> => {
    const { data } = await api.put<PublicPageResponse>(`/admin/public-site/pages/${pageId}`, payload);
    return data;
  },

  deleteAdminPage: async (pageId: string): Promise<void> => {
    await api.delete(`/admin/public-site/pages/${pageId}`);
  },

  createAdminSection: async (pageId: string, payload: CreateOrUpdateSectionRequest): Promise<PublicPageSectionResponse> => {
    const { data } = await api.post<PublicPageSectionResponse>(`/admin/public-site/pages/${pageId}/sections`, payload);
    return data;
  },

  updateAdminSection: async (
    pageId: string,
    sectionId: string,
    payload: CreateOrUpdateSectionRequest,
  ): Promise<PublicPageSectionResponse> => {
    const { data } = await api.put<PublicPageSectionResponse>(
      `/admin/public-site/pages/${pageId}/sections/${sectionId}`,
      payload,
    );
    return data;
  },

  deleteAdminSection: async (pageId: string, sectionId: string): Promise<void> => {
    await api.delete(`/admin/public-site/pages/${pageId}/sections/${sectionId}`);
  },

  getAdminServices: async (): Promise<PublicServiceOfferingResponse[]> => {
    const { data } = await api.get<PublicServiceOfferingResponse[]>('/admin/public-site/services');
    return data;
  },

  createAdminService: async (payload: CreateOrUpdateServiceRequest): Promise<PublicServiceOfferingResponse> => {
    const { data } = await api.post<PublicServiceOfferingResponse>('/admin/public-site/services', payload);
    return data;
  },

  updateAdminService: async (serviceId: string, payload: CreateOrUpdateServiceRequest): Promise<PublicServiceOfferingResponse> => {
    const { data } = await api.put<PublicServiceOfferingResponse>(`/admin/public-site/services/${serviceId}`, payload);
    return data;
  },

  deleteAdminService: async (serviceId: string): Promise<void> => {
    await api.delete(`/admin/public-site/services/${serviceId}`);
  },

  getAdminFaqs: async (): Promise<PublicFaqResponse[]> => {
    const { data } = await api.get<PublicFaqResponse[]>('/admin/public-site/faqs');
    return data;
  },

  createAdminFaq: async (payload: CreateOrUpdateFaqRequest): Promise<PublicFaqResponse> => {
    const { data } = await api.post<PublicFaqResponse>('/admin/public-site/faqs', payload);
    return data;
  },

  updateAdminFaq: async (faqId: string, payload: CreateOrUpdateFaqRequest): Promise<PublicFaqResponse> => {
    const { data } = await api.put<PublicFaqResponse>(`/admin/public-site/faqs/${faqId}`, payload);
    return data;
  },

  deleteAdminFaq: async (faqId: string): Promise<void> => {
    await api.delete(`/admin/public-site/faqs/${faqId}`);
  },

  // Admin Statistics Management Endpoints
  getAdminStatistics: async (): Promise<AdminPublicStatistic[]> => {
    const { data } = await api.get<AdminPublicStatistic[]>('/admin/public-site/statistics');
    return data;
  },

  getAdminStatisticById: async (id: string): Promise<AdminPublicStatistic> => {
    const { data } = await api.get<AdminPublicStatistic>(`/admin/public-site/statistics/${id}`);
    return data;
  },

  createAdminStatistic: async (payload: CreatePublicStatisticRequest): Promise<AdminPublicStatistic> => {
    const { data } = await api.post<AdminPublicStatistic>('/admin/public-site/statistics', payload);
    return data;
  },

  updateAdminStatistic: async (id: string, payload: UpdatePublicStatisticRequest): Promise<AdminPublicStatistic> => {
    const { data } = await api.put<AdminPublicStatistic>(`/admin/public-site/statistics/${id}`, payload);
    return data;
  },

  deleteAdminStatistic: async (id: string): Promise<void> => {
    await api.delete(`/admin/public-site/statistics/${id}`);
  },

  previewMetric: async (metric: PublicSystemMetric): Promise<MetricPreviewResponse> => {
    const { data } = await api.get<MetricPreviewResponse>('/admin/public-site/statistics/preview-metric', {
      params: { metric },
    });
    return data;
  },

  // Admin Contact Submissions Management Endpoints
  getAdminContactSubmissions: async (params?: {
    status?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<AdminContactSubmissionResponse>> => {
    const { data } = await api.get<PageResponse<AdminContactSubmissionResponse>>('/admin/contact-submissions', {
      params,
    });
    return data;
  },

  getAdminContactSubmissionDetail: async (id: string): Promise<AdminContactSubmissionResponse> => {
    const { data } = await api.get<AdminContactSubmissionResponse>(`/admin/contact-submissions/${id}`);
    return data;
  },

  updateAdminContactSubmissionStatus: async (
    id: string,
    status: string,
  ): Promise<AdminContactSubmissionResponse> => {
    const { data } = await api.patch<AdminContactSubmissionResponse>(`/admin/contact-submissions/${id}/status`, {
      status,
    });
    return data;
  },

  assignAdminContactSubmission: async (
    id: string,
    assignedToUserId?: string,
  ): Promise<AdminContactSubmissionResponse> => {
    const { data } = await api.patch<AdminContactSubmissionResponse>(`/admin/contact-submissions/${id}/assign`, {
      assignedToUserId,
    });
    return data;
  },

  replyToAdminContactSubmission: async (
    id: string,
    payload: { messageBody: string; channel?: string },
  ): Promise<AdminContactResponseDto> => {
    const { data } = await api.post<AdminContactResponseDto>(`/admin/contact-submissions/${id}/respond`, payload);
    return data;
  },
};
