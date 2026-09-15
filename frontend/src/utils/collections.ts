import type { PageResponse } from '../types/api';

export function pageContent<T>(value: PageResponse<T> | T[] | undefined): T[] {
  if (!value) return [];
  return Array.isArray(value) ? value : value.content;
}

export function displayValue(value: unknown, fallback = 'Not provided') {
  if (value === null || value === undefined || value === '') return fallback;
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value);
  return JSON.stringify(value);
}
