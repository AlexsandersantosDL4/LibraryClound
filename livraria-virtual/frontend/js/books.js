/**
 * LibraryClound - Módulo de Catálogo de Livros e Favoritos
 * Carregamento do acervo, busca em tempo real, categorias em abas e gestão de favoritos.
 */

import { apiFetch } from './api.js';
import { getCurrentUser, abrirModalAuth } from './auth.js';
import { mostrarToast } from './ui.js';
import { abrirLeitor } from './reader.js';

let todosLivros = [];
let categorias = [];
let categoriaSelecionada = '';
let favoritosIds = new Set();

export function getTodosLivros() {
  return todosLivros;
}

/**
 * Carrega a lista completa de livros do banco SQLite
 */
export async function carregarLivros() {
  const grid = document.getElementById('livros-grid');
  if (grid) {
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">Carregando acervo da LibraryClound...</div>';
  }

  try {
    const livros = await apiFetch('/api/livros');
    todosLivros = Array.isArray(livros) ? livros : [];
    await carregarFavoritosUsuario();
    renderizarLivros(todosLivros);
    atualizarContadorLivros(todosLivros.length);
  } catch (e) {
    console.error('Erro ao carregar livros:', e);
    if (grid) {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--danger);">Não foi possível carregar os livros. Verifique a conexão com o servidor.</div>';
    }
  }
}

/**
 * Carrega as categorias e renderiza os botões em formato de "pills"
 */
export async function carregarCategorias() {
  try {
    const dados = await apiFetch('/api/categorias');
    categorias = Array.isArray(dados) ? dados : [];
    renderizarCategoriasPills();
  } catch (e) {
    console.warn('Erro ao carregar categorias:', e);
  }
}

function renderizarCategoriasPills() {
  const container = document.getElementById('category-pills');
  if (!container) return;

  let html = `<button type="button" class="category-pill ${categoriaSelecionada === '' ? 'active' : ''}" data-cat="">Todas as Categorias</button>`;

  categorias.forEach(cat => {
    const nome = cat.nome || cat;
    const ativo = (categoriaSelecionada.toLowerCase() === nome.toLowerCase()) ? 'active' : '';
    html += `<button type="button" class="category-pill ${ativo}" data-cat="${nome}">${nome}</button>`;
  });

  container.innerHTML = html;

  // Adiciona listeners para os pills
  container.querySelectorAll('.category-pill').forEach(btn => {
    btn.addEventListener('click', () => {
      container.querySelectorAll('.category-pill').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      categoriaSelecionada = btn.getAttribute('data-cat') || '';
      filtrarLivros();
    });
  });
}

/**
 * Carrega os livros favoritados pelo usuário logado
 */
export async function carregarFavoritosUsuario() {
  const user = getCurrentUser();
  favoritosIds.clear();

  if (!user) {
    renderizarFavoritosNoPerfil([]);
    return;
  }

  try {
    const favs = await apiFetch('/api/favoritos/usuario');
    if (Array.isArray(favs)) {
      favs.forEach(f => favoritosIds.add(f.id));
      renderizarFavoritosNoPerfil(favs);
    }
  } catch (e) {
    console.warn('Não foi possível carregar favoritos:', e.message);
  }
}

/**
 * Alterna favorito de um livro (Adiciona ou Remove)
 */
export async function alternarFavorito(livroId, event) {
  if (event) event.stopPropagation();

  const user = getCurrentUser();
  if (!user) {
    mostrarToast('Faça login para adicionar livros aos seus favoritos!', 'warning');
    abrirModalAuth('login');
    return;
  }

  try {
    const res = await apiFetch('/api/favoritos/toggle', {
      method: 'POST',
      body: { livroId }
    });

    const isFav = res.favoritado ?? (!favoritosIds.has(livroId));
    if (isFav) {
      favoritosIds.add(livroId);
      mostrarToast('❤️ Livro adicionado aos seus favoritos!', 'success');
    } else {
      favoritosIds.delete(livroId);
      mostrarToast('🤍 Livro removido dos seus favoritos.', 'info');
    }

    // Atualiza botões no catálogo
    document.querySelectorAll(`.btn-fav-card[data-id="${livroId}"]`).forEach(btn => {
      if (isFav) {
        btn.classList.add('favorited');
        btn.innerHTML = '❤️';
      } else {
        btn.classList.remove('favorited');
        btn.innerHTML = '🤍';
      }
    });

    // Atualiza favoritos no perfil
    await carregarFavoritosUsuario();
  } catch (e) {
    mostrarToast(e.message || 'Erro ao atualizar favoritos', 'error');
  }
}

/**
 * Filtra os livros por texto de busca e categoria selecionada
 */
export function filtrarLivros() {
  const searchInput = document.getElementById('input-busca');
  const termo = searchInput ? searchInput.value.toLowerCase().trim() : '';

  const filtrados = todosLivros.filter(livro => {
    const matchBusca = !termo ||
      (livro.titulo && livro.titulo.toLowerCase().includes(termo)) ||
      (livro.autor && livro.autor.toLowerCase().includes(termo)) ||
      (livro.categoria && livro.categoria.toLowerCase().includes(termo));

    const matchCategoria = !categoriaSelecionada ||
      (livro.categoria && livro.categoria.toLowerCase() === categoriaSelecionada.toLowerCase());

    return matchBusca && matchCategoria;
  });

  renderizarLivros(filtrados);
  atualizarContadorLivros(filtrados.length);
}

/**
 * Renderiza os cards de livros na grade
 */
export function renderizarLivros(lista) {
  const grid = document.getElementById('livros-grid');
  if (!grid) return;

  if (lista.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; color: var(--text-muted);">
        <p style="font-size: 2.5rem; margin-bottom: 0.5rem;">🔍</p>
        <h3>Nenhum livro encontrado</h3>
        <p>Tente ajustar os termos da busca ou selecione outra categoria.</p>
      </div>`;
    return;
  }

  grid.innerHTML = lista.map(livro => {
    const isFavorito = favoritosIds.has(livro.id);
    const media = (livro.mediaAvaliacao != null) ? Number(livro.mediaAvaliacao).toFixed(1) : '5.0';
    const totalVotos = livro.totalAvaliacoes || 0;
    const capa = livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400';

    return `
      <article class="book-card" data-id="${livro.id}">
        <div class="book-cover-wrap">
          <img src="${capa}" alt="Capa de ${livro.titulo}" class="book-cover-img" loading="lazy" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'">
          <button type="button" class="btn-fav-card ${isFavorito ? 'favorited' : ''}" data-id="${livro.id}" title="${isFavorito ? 'Remover dos favoritos' : 'Favoritar livro'}">
            ${isFavorito ? '❤️' : '🤍'}
          </button>
          <span class="book-category-tag">${livro.categoria || 'Geral'}</span>
        </div>
        <div class="book-info-body">
          <h3 class="book-title" title="${livro.titulo}">${livro.titulo}</h3>
          <p class="book-author">por ${livro.autor || 'Autor Desconhecido'}</p>
          <div class="book-rating-row">
            <span class="stars-view">${renderizarEstrelasVisual(media)}</span>
            <span class="rating-score-num">${media}</span>
            <span class="rating-total-votes">(${totalVotos})</span>
          </div>
          <div class="book-card-footer">
            <button type="button" class="btn btn-primary btn-block btn-abrir-leitor" data-id="${livro.id}">
              📖 Ler Obra
            </button>
          </div>
        </div>
      </article>
    `;
  }).join('');

  // Adiciona listeners para os botões dos cards
  grid.querySelectorAll('.btn-fav-card').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = parseInt(btn.getAttribute('data-id'), 10);
      alternarFavorito(id, e);
    });
  });

  grid.querySelectorAll('.btn-abrir-leitor').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseInt(btn.getAttribute('data-id'), 10);
      abrirLeitor(id);
    });
  });
}

/**
 * Renderiza os favoritos na aba de favoritos do perfil
 * "uma aba para favoritos ... mostrando, nome, categoria e a capa do livro"
 */
export function renderizarFavoritosNoPerfil(favs) {
  const container = document.getElementById('favoritos-grid');
  const badge = document.getElementById('fav-count-badge');
  const statFavs = document.getElementById('view-profile-favs');

  if (badge) badge.textContent = favs.length;
  if (statFavs) statFavs.textContent = favs.length;

  if (!container) return;

  if (favs.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 2.5rem; color: var(--text-muted);">
        <p style="font-size: 2rem;">🤍</p>
        <p>Você ainda não favoritou nenhum livro.</p>
        <p style="font-size: 0.85rem;">Clique no coração dos livros do acervo para guardá-los aqui!</p>
      </div>`;
    return;
  }

  container.innerHTML = favs.map(livro => {
    const capa = livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400';
    return `
      <div class="favorite-item-card" style="display: flex; gap: 1rem; padding: 0.85rem; background: var(--bg-subtle); border-radius: var(--radius-md); border: 1px solid var(--border-subtle); align-items: center;">
        <img src="${capa}" alt="${livro.titulo}" style="width: 55px; height: 80px; object-fit: cover; border-radius: var(--radius-sm); flex-shrink: 0;">
        <div style="flex: 1; min-width: 0;">
          <span class="badge badge-primary" style="margin-bottom: 0.25rem;">${livro.categoria || 'Geral'}</span>
          <h4 style="font-size: 0.95rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 0.2rem;">${livro.titulo}</h4>
          <p style="font-size: 0.8rem; color: var(--text-muted);">${livro.autor || ''}</p>
        </div>
        <div style="display: flex; gap: 0.35rem; flex-shrink: 0;">
          <button type="button" class="btn btn-sm btn-primary btn-fav-ler" data-id="${livro.id}">Ler</button>
          <button type="button" class="btn btn-sm btn-ghost btn-fav-remover" data-id="${livro.id}" title="Remover dos favoritos">❌</button>
        </div>
      </div>
    `;
  }).join('');

  container.querySelectorAll('.btn-fav-ler').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseInt(btn.getAttribute('data-id'), 10);
      abrirLeitor(id);
    });
  });

  container.querySelectorAll('.btn-fav-remover').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = parseInt(btn.getAttribute('data-id'), 10);
      alternarFavorito(id, e);
    });
  });
}

function renderizarEstrelasVisual(notaNum) {
  const nota = parseFloat(notaNum) || 0;
  let estrelas = '';
  for (let i = 1; i <= 5; i++) {
    if (nota >= i) {
      estrelas += '★';
    } else if (nota >= i - 0.5) {
      estrelas += '★'; // visual meia estrela ou cheia
    } else {
      estrelas += '☆';
    }
  }
  return estrelas;
}

function atualizarContadorLivros(qtd) {
  const badge = document.getElementById('livros-count-badge');
  if (badge) badge.textContent = qtd;
}
