/**
 * LibraryClound - Cliente Centralizado de API
 * Gerencia requisições HTTP, cabeçalhos JWT Bearer e tratamento uniforme de respostas e erros.
 */

const API_BASE = '';
const TOKEN_KEY = 'libraryclound_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * Executa chamadas HTTP com tratamento de token JWT e serialização JSON.
 * @param {string} endpoint - Ex: '/api/livros'
 * @param {RequestInit} [options={}] - Configuração do fetch
 * @returns {Promise<any>}
 */
export async function apiFetch(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const headers = new Headers(options.headers || {});

  // Anexa token JWT se disponível
  const token = getToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // Serializa body se for objeto e define Content-Type
  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json; charset=UTF-8');
    }
    body = JSON.stringify(body);
  }

  const response = await fetch(url, {
    ...options,
    headers,
    body
  });

  // Trata expiração de sessão / não autenticado
  if (response.status === 401) {
    console.warn('Sessão expirada ou não autenticado (401). Limpando credenciais...');
    // Não limpa imediatamente em rotas de login para exibir a mensagem correta
    if (!endpoint.includes('/api/auth/login')) {
      removeToken();
      window.dispatchEvent(new CustomEvent('libraryclound:unauthorized'));
    }
  }

  // Se resposta for No Content (204)
  if (response.status === 204) {
    return null;
  }

  // Tenta parsear JSON
  const contentType = response.headers.get('content-type') || '';
  let data;
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = (data && typeof data === 'object' && (data.erro || data.detalhes || data.message))
      ? (data.detalhes ? `${data.erro}: ${data.detalhes}` : data.erro || data.message)
      : `Erro ${response.status}: ${response.statusText}`;
    const error = new Error(errorMsg);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}
