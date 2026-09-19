import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Avatar } from './Avatar';

describe('Avatar Component', () => {
  it('renders initials when no src is provided', () => {
    render(<Avatar name="Jane Doe" email="jane@example.com" />);
    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('renders initials from single word name', () => {
    render(<Avatar name="Researcher" />);
    expect(screen.getByText('RE')).toBeInTheDocument();
  });

  it('renders initials from email when name is omitted', () => {
    render(<Avatar email="alex@institution.edu" />);
    expect(screen.getByText('AL')).toBeInTheDocument();
  });

  it('renders default U when neither name nor email provided', () => {
    render(<Avatar />);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('renders img element when src is provided', () => {
    render(<Avatar src="/api/v1/users/123/avatar" name="Marie Curie" />);
    const img = screen.getByRole('img');
    expect(img).toBeInTheDocument();
    expect(screen.getByAltText(/Marie Curie's avatar/i)).toBeInTheDocument();
  });

  it('falls back to initials when img encounters onError', () => {
    render(<Avatar src="/broken-link.png" name="Albert Einstein" />);
    const img = screen.getByAltText(/Albert Einstein's avatar/i);
    fireEvent.error(img);
    // Now fallback initials AE should be visible
    expect(screen.getByText('AE')).toBeInTheDocument();
  });
});
