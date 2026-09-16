import { useEffect } from 'react';

interface SeoMetadataProps {
  title?: string;
  description?: string;
  canonicalUrl?: string;
  ogType?: string;
}

export function SeoMetadata({
  title,
  description,
  canonicalUrl,
  ogType = 'website',
}: SeoMetadataProps) {
  useEffect(() => {
    const defaultTitle = 'AI Research Assistant | Grounded Academic Research Platform';
    const siteTitle = title ? `${title} | AI Research Assistant` : defaultTitle;
    document.title = siteTitle;

    const defaultDesc =
      'Professional AI Research Assistant for literature review, methodology drafting, qualitative coding, statistical analysis, and auditable academic reports.';
    const metaDesc = description || defaultDesc;

    let descTag = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    if (!descTag) {
      descTag = document.createElement('meta');
      descTag.name = 'description';
      document.head.appendChild(descTag);
    }
    descTag.content = metaDesc;

    // OpenGraph Tags
    let ogTitleTag = document.querySelector<HTMLMetaElement>('meta[property="og:title"]');
    if (!ogTitleTag) {
      ogTitleTag = document.createElement('meta');
      ogTitleTag.setAttribute('property', 'og:title');
      document.head.appendChild(ogTitleTag);
    }
    ogTitleTag.content = siteTitle;

    let ogDescTag = document.querySelector<HTMLMetaElement>('meta[property="og:description"]');
    if (!ogDescTag) {
      ogDescTag = document.createElement('meta');
      ogDescTag.setAttribute('property', 'og:description');
      document.head.appendChild(ogDescTag);
    }
    ogDescTag.content = metaDesc;

    let ogTypeTag = document.querySelector<HTMLMetaElement>('meta[property="og:type"]');
    if (!ogTypeTag) {
      ogTypeTag = document.createElement('meta');
      ogTypeTag.setAttribute('property', 'og:type');
      document.head.appendChild(ogTypeTag);
    }
    ogTypeTag.content = ogType;

    // Canonical link
    if (canonicalUrl) {
      let canonicalLink = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
      if (!canonicalLink) {
        canonicalLink = document.createElement('link');
        canonicalLink.rel = 'canonical';
        document.head.appendChild(canonicalLink);
      }
      canonicalLink.href = canonicalUrl;
    }
  }, [title, description, canonicalUrl, ogType]);

  return null;
}
