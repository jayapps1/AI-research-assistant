import { useState } from 'react';

export interface AvatarProps {
  src?: string | null;
  name?: string | null;
  email?: string | null;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | number;
  shape?: 'circle' | 'square' | 'rounded';
  className?: string;
  alt?: string;
  border?: boolean;
}

const SIZE_MAP: Record<string, number> = {
  xs: 24,
  sm: 32,
  md: 40,
  lg: 48,
  xl: 64,
  '2xl': 96,
};

const PALETTES = [
  { bg: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #059669 0%, #047857 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #d97706 0%, #b45309 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #db2777 0%, #be185d 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)', text: '#ffffff' },
  { bg: 'linear-gradient(135deg, #0d9488 0%, #0f766e 100%)', text: '#ffffff' },
];

function getInitials(name?: string | null, email?: string | null): string {
  if (name && name.trim()) {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].slice(0, 2).toUpperCase();
  }
  if (email && email.trim()) {
    const localPart = email.split('@')[0];
    const clean = localPart.replace(/[^a-zA-Z0-9]/g, '');
    return (clean.slice(0, 2) || 'U').toUpperCase();
  }
  return 'U';
}

function getPalette(seed: string) {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  const index = Math.abs(hash) % PALETTES.length;
  return PALETTES[index];
}

export function Avatar({
  src,
  name,
  email,
  size = 'md',
  shape = 'circle',
  className = '',
  alt,
  border = false,
}: AvatarProps) {
  const [imgFailed, setImgFailed] = useState(false);

  const dimension = typeof size === 'number' ? size : SIZE_MAP[size] || 40;
  const fontSize = Math.max(10, Math.floor(dimension * 0.4));
  const initials = getInitials(name, email);
  const palette = getPalette(name || email || 'default');

  const borderRadius =
    shape === 'circle' ? '50%' : shape === 'rounded' ? `${Math.max(4, Math.floor(dimension * 0.2))}px` : '4px';

  const containerStyle: React.CSSProperties = {
    width: `${dimension}px`,
    height: `${dimension}px`,
    minWidth: `${dimension}px`,
    minHeight: `${dimension}px`,
    borderRadius,
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    position: 'relative',
    userSelect: 'none',
    boxSizing: 'border-box',
    border: border ? '2px solid var(--surface, #ffffff)' : undefined,
    boxShadow: border ? '0 1px 3px rgba(0, 0, 0, 0.1)' : undefined,
    background: !src || imgFailed ? palette.bg : 'var(--surface-2, #f0f3f7)',
  };

  const textStyle: React.CSSProperties = {
    fontSize: `${fontSize}px`,
    fontWeight: 600,
    lineHeight: 1,
    color: palette.text,
    letterSpacing: '0.02em',
    textTransform: 'uppercase',
  };

  const imageStyle: React.CSSProperties = {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    display: 'block',
  };

  const showImage = Boolean(src) && !imgFailed;

  return (
    <span
      className={`app-avatar ${className}`.trim()}
      style={containerStyle}
      title={name || email || 'User Avatar'}
      aria-label={!showImage ? (alt || `${name || email || 'User'}'s avatar`) : undefined}
      role={!showImage ? 'img' : undefined}
    >
      {showImage ? (
        <img
          src={src!}
          alt={alt || `${name || email || 'User'}'s avatar`}
          style={imageStyle}
          onError={() => setImgFailed(true)}
        />
      ) : (
        <span style={textStyle}>{initials}</span>
      )}
    </span>
  );
}
