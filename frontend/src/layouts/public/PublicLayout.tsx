import { Outlet } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { publicApi } from '../../api/public';
import { PublicHeader } from './PublicHeader';
import { PublicFooter } from './PublicFooter';
import { SeoMetadata } from './SeoMetadata';

export function PublicLayout() {
  const { data: settings } = useQuery({
    queryKey: ['publicSiteSettings'],
    queryFn: () => publicApi.getSiteSettings(),
    staleTime: 5 * 60 * 1000,
    retry: 1,
  });

  return (
    <div className="public-site-layout" id="public-website-root">
      <SeoMetadata
        title={settings?.defaultMetaTitle || 'Academic Research Platform'}
        description={settings?.defaultMetaDescription}
      />
      <PublicHeader settings={settings} />
      <main className="public-main-content" id="public-main-viewport">
        <Outlet context={{ settings }} />
      </main>
      <PublicFooter settings={settings} />
    </div>
  );
}
