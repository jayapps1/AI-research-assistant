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
const DocumentsPage = lazy(() => import('./pages/ResearchPages').then((m) => ({ default: m.DocumentsPage })));
const ResearchWorkflowPage = lazy(() => import('./pages/ResearchPages').then((m) => ({ default: m.ResearchWorkflowPage })));
const AiAssistantPage = lazy(() => import('./pages/AiAnalysisPages').then((m) => ({ default: m.AiAssistantPage })));
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

export default function App() {
  return (
    <div className="app-root">
      <OfflineBanner />
      <RouteErrorBoundary>
        <Suspense fallback={<PageLoading label="Loading page" />}>
          <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/totp" element={<TotpPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<HomeDashboard />} />
              <Route path="app" element={<HomeDashboard />} />
              <Route path="app/workspaces" element={<WorkspacePage />} />
              <Route path="app/workspaces/:workspaceId/projects" element={<ProjectsPage />} />
              <Route path="app/projects" element={<ProjectsPage />} />
              <Route path="app/projects/:projectId" element={<ProjectDashboard />} />
              <Route path="app/projects/:projectId/members" element={<MembersPage />} />
              <Route path="app/projects/:projectId/tasks" element={<TasksPage />} />
              <Route path="app/projects/:projectId/collaboration" element={<CommentsReviewsPage />} />
              <Route path="app/projects/:projectId/activity" element={<ActivityPage />} />
              <Route path="app/projects/:projectId/documents" element={<DocumentsPage />} />
              <Route path="app/projects/:projectId/research" element={<ResearchWorkflowPage />} />
              <Route path="app/projects/:projectId/ai" element={<AiAssistantPage />} />
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
              <Route path="app/documents" element={<DocumentsPage />} />
              <Route path="app/research" element={<ResearchWorkflowPage />} />
              <Route path="app/ai" element={<AiAssistantPage />} />
              <Route path="app/data" element={<DatasetWorkbenchPage />} />
              <Route path="app/analysis" element={<AnalysisPage />} />
              <Route path="app/findings" element={<FindingsWorkbenchPage />} />
              <Route path="app/report" element={<ReportPage />} />
              <Route path="app/reports" element={<ReportPage />} />
              <Route path="app/references" element={<ReferencesPage />} />
              <Route path="app/billing" element={<BillingPage />} />
              <Route path="app/notifications" element={<NotificationsPage />} />
              <Route path="app/settings" element={<ProfilePage />} />
              <Route path="app/settings/profile" element={<ProfilePage />} />
              <Route path="app/settings/security" element={<SecuritySettingsPage />} />
              <Route path="app/settings/notifications" element={<NotificationSettingsPage />} />
              <Route path="workspaces" element={<WorkspacePage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="projects/:projectId" element={<ProjectDashboard />} />
              <Route path="billing" element={<BillingPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="settings" element={<ProfilePage />} />
              <Route element={<AdminRoute />}>
                <Route path="admin" element={<AdminPage />} />
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
