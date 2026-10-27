/**
 * LibraryClound - Utilitários de Interface de Usuário
 * Modais, Toasts, Temas Claro/Escuro e Acessibilidade
 */

let toastTimeout = null;

/**
 * Exibe notificação flutuante tipo Toast
 * @param {string} mensagem 
 * @param {'info'|'success'|'error'|'warning'} [tipo='info'] 
 * @param {number} [duracao=4000] 
 */
export function mostrarToast(mensagem, tipo = 'info', duracao = 4000) {
  const toast = document.getElementById('toast');
  if (!toast) return;

  clearTimeout(toastTimeout);

  const icones = {
    info: 'ℹ️',
    success: '🎉',
    error: '❌',
    warning: '⚠️'
  };

  toast.innerHTML = `<span>${icones[tipo] || 'ℹ️'}</span> <span>${mensagem}</span>`;
  toast.className = `toast toast-${tipo}`;
  toast.classList.remove('hidden');

  toastTimeout = setTimeout(() => {
    toast.classList.add('hidden');
  }, duracao);
}

/**
 * Abre um modal pelo ID
 * @param {string} modalId 
 */
export function abrirModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }
}

/**
 * Fecha um modal pelo ID
 * @param {string} modalId 
 */
export function fecharModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('hidden');
    // Se nenhum outro modal estiver aberto, restaura overflow
    const openModals = document.querySelectorAll('.modal-overlay:not(.hidden)');
    if (openModals.length === 0) {
      document.body.style.overflow = '';
    }
  }
}

/**
 * Alterna entre abas em um container
 * @param {string} containerSelector 
 * @param {string} activeContentId 
 * @param {HTMLElement} activeBtn 
 */
export function alternarAba(containerSelector, activeContentId, activeBtn) {
  const container = document.querySelector(containerSelector);
  if (!container) return;

  // Atualiza botões
  const btns = container.querySelectorAll('.tab-btn');
  btns.forEach(btn => btn.classList.remove('active'));
  if (activeBtn) activeBtn.classList.add('active');

  // Alterna visibilidade dos conteúdos
  const parent = container.parentElement;
  if (parent) {
    const contents = parent.querySelectorAll('[class*="-tab-content"]');
    contents.forEach(content => {
      if (content.id === activeContentId) {
        content.classList.remove('hidden');
      } else {
        content.classList.add('hidden');
      }
    });
  }
}

/**
 * Gerenciamento de Tema Claro / Escuro Global
 */
export function initTheme() {
  const savedTheme = localStorage.getItem('libraryclound_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  atualizarIconeTema(savedTheme);
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  const nextTheme = (current === 'dark') ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', nextTheme);
  localStorage.setItem('libraryclound_theme', nextTheme);
  atualizarIconeTema(nextTheme);
}

function atualizarIconeTema(theme) {
  const btn = document.getElementById('btn-theme-toggle');
  if (btn) {
    btn.innerHTML = (theme === 'dark') ? '☀️' : '🌙';
    btn.title = (theme === 'dark') ? 'Mudar para tema claro' : 'Mudar para tema escuro';
  }
}

/**
 * Configura fechamento de modais com clique fora e tecla Escape
 */
export function setupModalDismissers() {
  document.querySelectorAll('.modal-overlay').forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        fecharModal(modal.id);
      }
    });
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      const openModals = document.querySelectorAll('.modal-overlay:not(.hidden)');
      openModals.forEach(modal => fecharModal(modal.id));
    }
  });
}
