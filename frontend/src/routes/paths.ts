export const paths = {
  // Public Website
  home: '/',
  services: '/services',
  pricing: '/pricing',
  about: '/about',
  faq: '/faq',
  contact: '/contact',
  privacy: '/privacy',
  terms: '/terms',

  // Authentication
  login: '/login',
  register: '/register',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  totp: '/auth/totp',

  // Authenticated Application
  app: '/app',
  dashboard: '/app',
  workspaces: '/app/workspaces',
  projects: '/app/projects',
  tasks: '/app/tasks',
  documents: '/app/documents',
  research: '/app/research',
  ai: '/app/ai',
  analysis: '/app/analysis',
  reports: '/app/reports',
  references: '/app/references',
  billing: '/app/billing',
  notifications: '/app/notifications',
  profile: '/app/settings/profile',
  security: '/app/settings/security',
  settings: '/app/settings',

  // Administration
  admin: '/admin',
  adminPublicSite: '/admin/public-site',
  adminContactSubmissions: '/admin/contact-submissions',

  // Parametric routes
  project: (projectId: string) => `/app/projects/${projectId}`,
  projectResearch: (projectId: string) => `/app/projects/${projectId}/research`,
  projectDocuments: (projectId: string) => `/app/projects/${projectId}/documents`,
  projectAi: (projectId: string) => `/app/projects/${projectId}/ai`,
};
