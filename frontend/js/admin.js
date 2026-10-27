/**
 * LibraryClound - Painel de Administração e Gestão de Livros
 * Operações exclusivas para administradores e donos do site:
 * Adicionar/editar obras com conteúdo completo (sem limite de páginas),
 * excluir livros e auditar usuários cadastrados.
 */

import { apiFetch } from './api.js';
import { getCurrentUser } from './auth.js';
import { mostrarToast, abrirModal, fecharModal } from './ui.js';
import { carregarLivros, carregarCategorias } from './books.js';

let adminLivros = [];
let adminUsuarios = [];

/**
 * Alterna a visualização entre o Catálogo e o Painel Administrativo
 */
export function alternarPainelAdmin() {
  const user = getCurrentUser();
  if (!user || user.tipo !== 'ADMIN') {
    mostrarToast('Acesso restrito aos administradores e donos da LibraryClound.', 'error');
    return;
  }

  const secaoCatalogo = document.getElementById('secao-catalogo');
  const secaoAdmin = document.getElementById('secao-admin');

  if (secaoAdmin.classList.contains('hidden')) {
    secaoCatalogo.classList.add('hidden');
    secaoAdmin.classList.remove('hidden');
    carregarDadosAdmin();
  } else {
    secaoAdmin.classList.add('hidden');
    secaoCatalogo.classList.remove('hidden');
  }
}

/**
 * Carrega livros e usuários para exibição nas tabelas do painel
 */
export async function carregarDadosAdmin() {
  await Promise.all([
    carregarTabelaLivrosAdmin(),
    carregarTabelaUsuariosAdmin()
  ]);
}

/**
 * Busca livros e renderiza a tabela de administração
 */
export async function carregarTabelaLivrosAdmin() {
  const tbody = document.getElementById('admin-livros-tbody');
  if (!tbody) return;

  try {
    const livros = await apiFetch('/api/livros');
    adminLivros = Array.isArray(livros) ? livros : [];

    if (adminLivros.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">Nenhum livro cadastrado.</td></tr>';
      return;
    }

    tbody.innerHTML = adminLivros.map(livro => {
      const capa = livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=100';
      const temConteudo = livro.conteudo && livro.conteudo.trim().length > 0;
      const media = (livro.mediaAvaliacao != null) ? Number(livro.mediaAvaliacao).toFixed(1) : '5.0';

      return `
        <tr>
          <td><img src="${capa}" alt="Capa" style="width: 40px; height: 56px; object-fit: cover; border-radius: var(--radius-xs);"></td>
          <td><strong>${livro.titulo}</strong></td>
          <td>${livro.autor || 'N/A'}</td>
          <td><span class="badge badge-primary">${livro.categoria || 'Geral'}</span></td>
          <td>${livro.totalPaginas || 1} págs</td>
          <td>⭐ ${media}</td>
          <td>
            <span class="badge ${temConteudo ? 'badge-gold' : ''}">
              ${temConteudo ? '✓ Texto Completo' : 'Apenas Sinopse'}
            </span>
          </td>
          <td>
            <div style="display: flex; gap: 0.35rem;">
              <button type="button" class="btn btn-sm btn-outline btn-admin-edit" data-id="${livro.id}">✏️ Editar</button>
              <button type="button" class="btn btn-sm btn-danger btn-admin-del" data-id="${livro.id}">🗑️ Excluir</button>
            </div>
          </td>
        </tr>
      `;
    }).join('');

    tbody.querySelectorAll('.btn-admin-edit').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-id'), 10);
        abrirModalLivro('editar', id);
      });
    });

    tbody.querySelectorAll('.btn-admin-del').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-id'), 10);
        excluirLivroAdmin(id);
      });
    });

  } catch (e) {
    console.error('Erro ao carregar tabela de livros admin:', e);
  }
}

/**
 * Busca usuários e renderiza a tabela de auditoria
 */
export async function carregarTabelaUsuariosAdmin() {
  const tbody = document.getElementById('admin-usuarios-tbody');
  if (!tbody) return;

  try {
    const usuarios = await apiFetch('/api/admin/usuarios');
    adminUsuarios = Array.isArray(usuarios) ? usuarios : [];

    if (adminUsuarios.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">Nenhum usuário cadastrado.</td></tr>';
      return;
    }

    tbody.innerHTML = adminUsuarios.map(u => {
      const avatarHtml = (u.fotoUrl && u.fotoUrl.trim() !== '')
        ? `<img src="${u.fotoUrl}" alt="Avatar" style="width: 36px; height: 36px; border-radius: 50%; object-fit: cover;">`
        : `<div class="avatar-circle" style="width: 36px; height: 36px; font-size: 0.85rem;">${(u.nomeFantasia || 'U').charAt(0).toUpperCase()}</div>`;

      return `
        <tr>
          <td>${avatarHtml}</td>
          <td><strong>${u.nomeFantasia || 'Sem Nome'}</strong></td>
          <td>${u.email}</td>
          <td style="max-width: 220px; font-size: 0.85rem; color: var(--text-muted);">${u.descricao || '—'}</td>
          <td><span class="badge ${u.tipo === 'ADMIN' ? 'badge-primary' : ''}">${u.tipo}</span></td>
          <td><strong style="color: var(--gold-500);">🏆 ${u.livrosLidos ?? 0}</strong></td>
        </tr>
      `;
    }).join('');

  } catch (e) {
    console.error('Erro ao carregar tabela de usuários admin:', e);
  }
}

/**
 * Abre modal de criação ou edição de livros
 * @param {'criar'|'editar'} modo 
 * @param {number} [livroId] 
 */
export async function abrirModalLivro(modo, livroId = null) {
  const tituloModal = document.getElementById('modal-livro-titulo');
  const inputId = document.getElementById('livro-id');
  const form = document.getElementById('form-livro');

  if (form) form.reset();

  if (modo === 'editar' && livroId) {
    if (tituloModal) tituloModal.textContent = 'Editar Obra na LibraryClound';
    try {
      const livro = await apiFetch(`/api/livros/${livroId}`);
      if (inputId) inputId.value = livro.id;

      document.getElementById('livro-titulo-input').value = livro.titulo || '';
      document.getElementById('livro-autor-input').value = livro.autor || '';
      document.getElementById('livro-categoria-input').value = livro.categoria || '';
      document.getElementById('livro-paginas-input').value = livro.totalPaginas || 100;
      document.getElementById('livro-capa-input').value = livro.capaUrl || '';
      document.getElementById('livro-sinopse-input').value = livro.sinopse || '';
      document.getElementById('livro-conteudo-input').value = livro.conteudo || '';
    } catch (e) {
      mostrarToast('Erro ao buscar dados do livro para edição', 'error');
      return;
    }
  } else {
    if (tituloModal) tituloModal.textContent = 'Adicionar Novo Livro à LibraryClound';
    if (inputId) inputId.value = '';
  }

  abrirModal('modal-livro');
}

export function fecharModalLivro() {
  fecharModal('modal-livro');
}

/**
 * Salva novo livro ou atualizações de livro existente no SQLite
 */
export async function salvarLivroAdmin(event) {
  event.preventDefault();

  const id = document.getElementById('livro-id').value;
  const isEdicao = Boolean(id && id.trim() !== '');

  const payload = {
    titulo: document.getElementById('livro-titulo-input').value.trim(),
    autor: document.getElementById('livro-autor-input').value.trim(),
    categoria: document.getElementById('livro-categoria-input').value.trim(),
    totalPaginas: parseInt(document.getElementById('livro-paginas-input').value, 10) || 1,
    capaUrl: document.getElementById('livro-capa-input').value.trim(),
    sinopse: document.getElementById('livro-sinopse-input').value.trim(),
    conteudo: document.getElementById('livro-conteudo-input').value.trim()
  };

  try {
    if (isEdicao) {
      await apiFetch(`/api/admin/livros/${id}`, {
        method: 'PUT',
        body: payload
      });
      mostrarToast('📚 Livro atualizado com sucesso no SQLite!', 'success');
    } else {
      await apiFetch('/api/admin/livros', {
        method: 'POST',
        body: payload
      });
      mostrarToast('🎉 Novo livro cadastrado no acervo da LibraryClound!', 'success');
    }

    fecharModalLivro();
    await Promise.all([
      carregarTabelaLivrosAdmin(),
      carregarLivros(),
      carregarCategorias()
    ]);
  } catch (e) {
    mostrarToast(e.message || 'Erro ao salvar dados do livro', 'error');
  }
}

/**
 * Exclui livro com confirmação
 */
export async function excluirLivroAdmin(livroId) {
  if (!confirm('Deseja realmente excluir este livro do acervo? Esta ação não pode ser desfeita.')) {
    return;
  }

  try {
    await apiFetch(`/api/admin/livros/${livroId}`, {
      method: 'DELETE'
    });
    mostrarToast('🗑️ Livro removido do acervo com sucesso!', 'info');
    await Promise.all([
      carregarTabelaLivrosAdmin(),
      carregarLivros(),
      carregarCategorias()
    ]);
  } catch (e) {
    mostrarToast(e.message || 'Erro ao excluir livro', 'error');
  }
}

/**
 * Alterna entre abas do painel administrativo
 */
export function trocarAbaAdmin(aba) {
  const tabLivros = document.getElementById('admin-tab-livros');
  const tabUsuarios = document.getElementById('admin-tab-usuarios');
  const btns = document.querySelectorAll('.tabs-container.admin-tabs .tab-btn');

  if (aba === 'livros') {
    if (tabLivros) tabLivros.classList.remove('hidden');
    if (tabUsuarios) tabUsuarios.classList.add('hidden');
  } else {
    if (tabLivros) tabLivros.classList.add('hidden');
    if (tabUsuarios) tabUsuarios.classList.remove('hidden');
  }

  btns.forEach(btn => {
    if (btn.getAttribute('data-tab') === aba) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
}
