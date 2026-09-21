import { Component, lazy, Suspense, useEffect, useState, type ErrorInfo, type ReactNode } from 'react';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute, AdminRoute } from './auth/guards';
import { AppShell } from './layouts/AppShell';
import { ErrorState, NotFound, PageLoading } from './components/states';

const LoginPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.ForgotPasswordPage })));
const TotpPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.TotpPage })));
const ResetPasswordPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.ResetPasswordPage })));
const HomeDashboard = lazy(() => import('./pages/DashboardPages').then((m) => ({ default: m.HomeDashboard })));
const WorkspacePage = lazy(() => import('./pages/DashboardPages').then((m) => ({ default: m.WorkspacePage })));
const ProjectsPage = lazy(() => import('./pages/DashboardPages').then((m) => ({ default: m.ProjectsPage })));
const ProjectDashboard = lazy(() => import('./pages/DashboardPages').then((m) => ({ default: m.ProjectDashboard })));
const MembersPage = lazy(() => import('./pages/CollaborationPages').then((m) => ({ default: m.MembersPage })));
const TasksPage = lazy(() => import('./pages/CollaborationPages').then((m) => ({ default: m.TasksPage })));
const CommentsReviewsPage = lazy(() => import('./pages/CollaborationPages').then((m) => ({ default: m.CommentsReviewsPage })));
const ActivityPage = lazy(() => import('./pages/CollaborationPages').then((m) => ({ default: m.ActivityPage })));
const SourcesPage = lazy(() => import('./pages/SourcesPage').then((m) => ({ default: m.SourcesPage })));
const ResearchAssistantPage = lazy(() => import('./pages/ResearchAssistantPage').then((m) => ({ default: m.ResearchAssistantPage })));
const WritingPage = lazy(() => import('./pages/WritingPage').then((m) => ({ default: m.WritingPage })));
const ResearchPage = lazy(() => import('./pages/ResearchPage').then((m) => ({ default: m.ResearchPage })));
const AdvancedWorkflowPage = lazy(() => import('./pages/AdvancedWorkflowPage').then((m) => ({ default: m.AdvancedWorkflowPage })));
const DatasetWorkbenchPage = lazy(() => import('./pages/ResearchWorkbenches').then((m) => ({ default: m.DatasetWorkbenchPage })));
const AnalysisPage = lazy(() => import('./pages/ResearchWorkbenches').then((m) => ({ default: m.AnalysisWorkbenchPage })));
const QualitativeWorkbenchPage = lazy(() => import('./pages/ResearchWorkbenches').then((m) => ({ default: m.QualitativeWorkbenchPage })));
const FindingsWorkbenchPage = lazy(() => import('./pages/ResearchWorkbenches').then((m) => ({ default: m.FindingsWorkbenchPage })));
const ReportPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ReportPage })));
const ReferencesPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ReferencesPage })));
const BillingPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.BillingPage })));
const NotificationsPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.NotificationsPage })));
const ProfilePage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ProfilePage })));
const SecuritySettingsPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.SecuritySettingsPage })));
const NotificationSettingsPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.NotificationSettingsPage })));
const AdminPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.AdminPage })));

// Public Website Layout & Pages
const PublicLayout = lazy(() => import('./layouts/public/PublicLayout').then((m) => ({ default: m.PublicLayout })));
const HomePage = lazy(() => import('./pages/public/HomePage').then((m) => ({ default: m.HomePage })));
const ServicesPage = lazy(() => import('./pages/public/ServicesPage').then((m) => ({ default: m.ServicesPage })));
const PricingPage = lazy(() => import('./pages/public/PricingPage').then((m) => ({ default: m.PricingPage })));
const AboutPage = lazy(() => import('./pages/public/AboutPage').then((m) => ({ default: m.AboutPage })));
const FaqPage = lazy(() => import('./pages/public/FaqPage').then((m) => ({ default: m.FaqPage })));
const ContactPage = lazy(() => import('./pages/public/ContactPage').then((m) => ({ default: m.ContactPage })));
const PrivacyPolicyPage = lazy(() => import('./pages/public/LegalPages').then((m) => ({ default: m.PrivacyPolicyPage })));
const TermsOfServicePage = lazy(() => import('./pages/public/LegalPages').then((m) => ({ default: m.TermsOfServicePage })));
const AdminPublicSitePage = lazy(() => import('./pages/admin/AdminPublicSitePage').then((m) => ({ default: m.AdminPublicSitePage })));
const AdminContactInboxPage = lazy(() => import('./pages/admin/AdminContactInboxPage').then((m) => ({ default: m.AdminContactInboxPage })));
const AdminPlansPage = lazy(() => import('./pages/admin/AdminPlansPage').then((m) => ({ default: m.AdminPlansPage })));
const AdminPaymentsPage = lazy(() => import('./pages/admin/AdminPaymentsPage').then((m) => ({ default: m.AdminPaymentsPage })));
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage })));
const AdminWorkspacesPage = lazy(() => import('./pages/admin/AdminWorkspacesPage').then((m) => ({ default: m.AdminWorkspacesPage })));
const AdminAuditPage = lazy(() => import('./pages/admin/AdminAuditPage').then((m) => ({ default: m.AdminAuditPage })));
const AdminRecordsPage = lazy(() => import('./pages/admin/AdminRecordsPage').then((m) => ({ default: m.AdminRecordsPage })));
const PaymentResultPage = lazy(() => import('./pages/billing/PaymentResultPage').then((m) => ({ default: m.PaymentResultPage })));
const AdminLayout = lazy(() => import('./layouts/admin/AdminLayout').then((m) => ({ default: m.AdminLayout })));

export default function App() {
  return (
    <div className="app-root">
      <OfflineBanner />
      <RouteErrorBoundary>
        <Suspense fallback={<PageLoading label="Loading page" />}>
          <Routes>
          {/* Public Website Routes (Independent Shell with Header, Footer, Zero App Sidebar) */}
          <Route element={<PublicLayout />}>
            <Route index element={<HomePage />} />
            <Route path="services" element={<ServicesPage />} />
            <Route path="pricing" element={<PricingPage />} />
            <Route path="about" element={<AboutPage />} />
            <Route path="faq" element={<FaqPage />} />
            <Route path="contact" element={<ContactPage />} />
            <Route path="privacy" element={<PrivacyPolicyPage />} />
            <Route path="terms" element={<TermsOfServicePage />} />
          </Route>

          {/* Authentication Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/login" element={<LoginPage />} />
          <Route path="/auth/totp" element={<TotpPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          {/* Authenticated Application Routes (Hosted inside AppShell with Sidebar) */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route path="app" element={<HomeDashboard />} />
              <Route path="app/workspaces" element={<WorkspacePage />} />
              <Route path="app/workspaces/:workspaceId/projects" element={<ProjectsPage />} />
              <Route path="app/projects" element={<ProjectsPage />} />
              <Route path="app/projects/:projectId" element={<ProjectDashboard />} />
              <Route path="app/projects/:projectId/members" element={<MembersPage />} />
              <Route path="app/projects/:projectId/tasks" element={<TasksPage />} />
              <Route path="app/projects/:projectId/collaboration" element={<CommentsReviewsPage />} />
              <Route path="app/projects/:projectId/activity" element={<ActivityPage />} />
              <Route path="app/projects/:projectId/sources" element={<SourcesPage />} />
              <Route path="app/projects/:projectId/documents" element={<SourcesPage />} />
              <Route path="app/projects/:projectId/assistant" element={<ResearchAssistantPage />} />
              <Route path="app/projects/:projectId/ai" element={<ResearchAssistantPage />} />
              <Route path="app/projects/:projectId/writing" element={<WritingPage />} />
              <Route path="app/projects/:projectId/research" element={<ResearchPage />} />
              <Route path="app/projects/:projectId/research/advanced" element={<AdvancedWorkflowPage />} />
              <Route path="app/projects/:projectId/data" element={<DatasetWorkbenchPage />} />
              <Route path="app/projects/:projectId/analysis" element={<AnalysisPage />} />
              <Route path="app/projects/:projectId/analysis/qualitative" element={<QualitativeWorkbenchPage />} />
              <Route path="app/projects/:projectId/findings" element={<FindingsWorkbenchPage />} />
              <Route path="app/projects/:projectId/report" element={<ReportPage />} />
              <Route path="app/projects/:projectId/reports" element={<ReportPage />} />
              <Route path="app/projects/:projectId/references" element={<ReferencesPage />} />
              <Route path="app/projects/:projectId/settings" element={<ProjectDashboard />} />
              <Route path="app/tasks" element={<TasksPage />} />
              <Route path="app/collaboration" element={<CommentsReviewsPage />} />
              <Route path="app/activity" element={<ActivityPage />} />
              <Route path="app/sources" element={<SourcesPage />} />
              <Route path="app/documents" element={<SourcesPage />} />
              <Route path="app/assistant" element={<ResearchAssistantPage />} />
              <Route path="app/ai" element={<ResearchAssistantPage />} />
              <Route path="app/writing" element={<WritingPage />} />
              <Route path="app/research" element={<ResearchPage />} />
              <Route path="app/research/advanced" element={<AdvancedWorkflowPage />} />
              <Route path="app/data" element={<DatasetWorkbenchPage />} />
              <Route path="app/analysis" element={<AnalysisPage />} />
              <Route path="app/findings" element={<FindingsWorkbenchPage />} />
              <Route path="app/report" element={<ReportPage />} />
              <Route path="app/reports" element={<ReportPage />} />
              <Route path="app/references" element={<ReferencesPage />} />
              <Route path="app/billing" element={<BillingPage />} />
              <Route path="app/billing/payment-result" element={<PaymentResultPage />} />
              <Route path="app/notifications" element={<NotificationsPage />} />
              <Route path="app/settings" element={<ProfilePage />} />
              <Route path="app/settings/profile" element={<ProfilePage />} />
              <Route path="app/settings/security" element={<SecuritySettingsPage />} />
              <Route path="app/settings/notifications" element={<NotificationSettingsPage />} />
              <Route path="workspaces" element={<WorkspacePage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="projects/:projectId" element={<ProjectDashboard />} />
              <Route path="billing" element={<BillingPage />} />
              <Route path="billing/payment-result" element={<PaymentResultPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="settings" element={<ProfilePage />} />
            </Route>

            {/* System Admin Routes (Hosted inside dedicated AdminLayout) */}
            <Route element={<AdminRoute />}>
              <Route element={<AdminLayout />}>
                <Route path="admin" element={<AdminPage />} />
                <Route path="admin/users" element={<AdminUsersPage />} />
                <Route path="admin/workspaces" element={<AdminWorkspacesPage />} />
                <Route path="admin/projects" element={<AdminRecordsPage />} />
                <Route path="admin/documents" element={<AdminRecordsPage />} />
                <Route path="admin/processing-jobs" element={<AdminRecordsPage />} />
                <Route path="admin/research-templates" element={<AdminRecordsPage />} />
                <Route path="admin/report-templates" element={<AdminRecordsPage />} />
                <Route path="admin/plans" element={<AdminPlansPage />} />
                <Route path="admin/payments" element={<AdminPaymentsPage />} />
                <Route path="admin/complimentary-access" element={<AdminPage />} />
                <Route path="admin/ai-operations" element={<AdminRecordsPage />} />
                <Route path="admin/ai-usage" element={<AdminRecordsPage />} />
                <Route path="admin/jobs" element={<AdminRecordsPage />} />
                <Route path="admin/storage" element={<AdminRecordsPage />} />
                <Route path="admin/references" element={<AdminRecordsPage />} />
                <Route path="admin/notifications" element={<AdminRecordsPage />} />
                <Route path="admin/audit" element={<AdminAuditPage />} />
                <Route path="admin/health" element={<AdminRecordsPage />} />
                <Route path="admin/settings" element={<AdminRecordsPage />} />
                <Route path="admin/public-site" element={<AdminPublicSitePage />} />
                <Route path="admin/contact-submissions" element={<AdminContactInboxPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </RouteErrorBoundary>
    </div>
  );
}

function OfflineBanner() {
  const [online, setOnline] = useState(() => (typeof navigator === 'undefined' ? true : navigator.onLine));
  useEffect(() => {
    const sync = () => setOnline(typeof navigator === 'undefined' ? true : navigator.onLine);
    window.addEventListener('online', sync);
    window.addEventListener('offline', sync);
    return () => {
      window.removeEventListener('online', sync);
      window.removeEventListener('offline', sync);
    };
  }, []);
  if (online) return null;
  return <div className="offline-banner">You are offline. Server changes will not be reported as saved until the connection is restored.</div>;
}

class RouteErrorBoundary extends Component<{ children: ReactNode }, { error?: Error }> {
  state: { error?: Error } = {};

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Route rendering failed', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return <main className="main"><ErrorState title="This view could not be rendered" error={this.state.error} /></main>;
    }
    return this.props.children;
  }
}
