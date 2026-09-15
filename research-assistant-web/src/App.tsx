import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute, AdminRoute } from './auth/guards';
import { AppShell } from './layouts/AppShell';
import { NotFound, PageLoading } from './components/states';

const LoginPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() => import('./pages/AuthPages').then((m) => ({ default: m.ForgotPasswordPage })));
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
const AnalysisPage = lazy(() => import('./pages/AiAnalysisPages').then((m) => ({ default: m.AnalysisPage })));
const ReportPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ReportPage })));
const ReferencesPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ReferencesPage })));
const BillingPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.BillingPage })));
const NotificationsPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.NotificationsPage })));
const ProfilePage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.ProfilePage })));
const AdminPage = lazy(() => import('./pages/OperationsPages').then((m) => ({ default: m.AdminPage })));

export default function App() {
  return (
    <div className="app-root">
      <Suspense fallback={<PageLoading label="Loading page" />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route index element={<HomeDashboard />} />
              <Route path="workspaces" element={<WorkspacePage />} />
              <Route path="workspaces/:workspaceId/projects" element={<ProjectsPage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="projects/:projectId" element={<ProjectDashboard />} />
              <Route path="members" element={<MembersPage />} />
              <Route path="tasks" element={<TasksPage />} />
              <Route path="collaboration" element={<CommentsReviewsPage />} />
              <Route path="activity" element={<ActivityPage />} />
              <Route path="documents" element={<DocumentsPage />} />
              <Route path="research" element={<ResearchWorkflowPage />} />
              <Route path="ai" element={<AiAssistantPage />} />
              <Route path="analysis" element={<AnalysisPage />} />
              <Route path="reports" element={<ReportPage />} />
              <Route path="references" element={<ReferencesPage />} />
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
    </div>
  );
}
