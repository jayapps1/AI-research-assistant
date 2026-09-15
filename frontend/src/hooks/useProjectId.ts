import { useSearchParams } from 'react-router-dom';
import { useParams } from 'react-router-dom';

export function useProjectId() {
  const paramsFromRoute = useParams();
  const [params] = useSearchParams();
  return paramsFromRoute.projectId ?? params.get('projectId') ?? '';
}
