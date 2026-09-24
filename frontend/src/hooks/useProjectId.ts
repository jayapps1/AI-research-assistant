import { useSearchParams } from 'react-router-dom';
import { useParams } from 'react-router-dom';
import { useOptionalActiveProject } from '../features/projects/ActiveProjectProvider';

export function useProjectId() {
  const paramsFromRoute = useParams();
  const [params] = useSearchParams();
  const activeProject = useOptionalActiveProject();
  return paramsFromRoute.projectId ?? params.get('projectId') ?? activeProject?.activeProjectId ?? '';
}
