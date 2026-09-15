const accessKey = 'raa.accessToken';
const refreshKey = 'raa.refreshToken';

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

let memoryAccessToken: string | null = sessionStorage.getItem(accessKey);
let memoryRefreshToken: string | null = sessionStorage.getItem(refreshKey);

export function getAccessToken() {
  return memoryAccessToken;
}

export function getRefreshToken() {
  return memoryRefreshToken;
}

export function setTokens(tokens: TokenPair) {
  memoryAccessToken = tokens.accessToken;
  memoryRefreshToken = tokens.refreshToken;
  sessionStorage.setItem(accessKey, tokens.accessToken);
  sessionStorage.setItem(refreshKey, tokens.refreshToken);
}

export function clearTokens() {
  memoryAccessToken = null;
  memoryRefreshToken = null;
  sessionStorage.removeItem(accessKey);
  sessionStorage.removeItem(refreshKey);
}
