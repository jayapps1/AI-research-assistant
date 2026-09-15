import { useSearchParams } from 'react-router-dom';

export function useProjectId() {
  const [params] = useSearchParams();
  return params.get('projectId') ?? '';
}
