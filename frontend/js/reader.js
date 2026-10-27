/**
 * LibraryClound - Leitor de Livros Paginado, Temas e Avaliações
 * Implementa leitor com paginação real, temas (Claro, Sépia, Escuro),
 * marca-páginas no SQLite, avaliação de 0.5 a 5.0 estrelas e confirmação de finalização.
 */

import { apiFetch } from './api.js';
import { getCurrentUser, sincronizarPerfil } from './auth.js';
import { mostrarToast, abrirModal, fecharModal } from './ui.js';

let livroAtivo = null;
let paginasLivro = [];
let paginaAtual = 1;
let totalPaginas = 1;
let bookmarkSalvo = null;
let notaSelecionada = 5.0;
let tamanhoFonteRem = 1.15;
let temaLeitura = 'light';
let fonteSerif = true;

/**
 * Abre o leitor para o livro selecionado
 * @param {number} livroId 
 */
export async function abrirLeitor(livroId) {
  try {
    const livro = await apiFetch(`/api/livros/${livroId}`);
    if (!livro) throw new Error('Livro não encontrado');

    livroAtivo = livro;
    configurarCabecalhoLeitor(livro);
    processarPaginacao(livro);

    // Carrega progresso/marca-página do usuário se logado
    const user = getCurrentUser();
    if (user) {
      await carregarMarcaPagina(livroId);
      await carregarAvaliacaoUsuario(livroId);
    } else {
      paginaAtual = 1;
      atualizarExibicaoPagina();
    }

    inicializarWidgetEstrelas();
    aplicarTemaLeitura(temaLeitura);
    abrirModal('modal-leitor');
  } catch (e) {
    mostrarToast(e.message || 'Erro ao carregar livro para leitura', 'error');
  }
}

export function fecharLeitor() {
  fecharModal('modal-leitor');
}

/**
 * Preenche os metadados do livro no topo do leitor
 */
function configurarCabecalhoLeitor(livro) {
  const elTitulo = document.getElementById('reader-book-title');
  const elAutor = document.getElementById('reader-book-author');
  const elCat = document.getElementById('reader-book-category');
  const elCapa = document.getElementById('reader-book-cover');
  const elSinopse = document.getElementById('reader-book-synopsis');

  if (elTitulo) elTitulo.textContent = livro.titulo;
  if (elAutor) elAutor.textContent = `por ${livro.autor || 'Autor Desconhecido'}`;
  if (elCat) elCat.textContent = livro.categoria || 'Geral';
  if (elSinopse) elSinopse.textContent = livro.sinopse || 'Sem sinopse disponível.';
  if (elCapa) {
    elCapa.src = livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400';
  }
}

/**
 * Algoritmo de Paginação Inteligente:
 * Divide o conteúdo do livro em páginas virtuais respeitando quebras de parágrafos.
 */
function processarPaginacao(livro) {
  const conteudo = livro.conteudo || livro.sinopse || 'Este livro ainda não possui texto completo cadastrado pelo administrador.';
  
  // 1. Verifica se existem marcadores explícitos de página
  if (conteudo.includes('---PÁGINA---') || conteudo.includes('\f')) {
    const separador = conteudo.includes('---PÁGINA---') ? '---PÁGINA---' : '\f';
    paginasLivro = conteudo.split(separador).map(p => p.trim()).filter(p => p.length > 0);
  } else {
    // 2. Divisão por parágrafos com agrupamento em páginas de aprox. 1.400 caracteres
    const paragrafos = conteudo.split(/\n\s*\n/);
    const paginas = [];
    let blocoAtual = '';

    for (const p of paragrafos) {
      const pLimpo = p.trim();
      if (!pLimpo) continue;

      if ((blocoAtual.length + pLimpo.length > 1400) && blocoAtual.length > 0) {
        paginas.push(blocoAtual.trim());
        blocoAtual = pLimpo + '\n\n';
      } else {
        blocoAtual += pLimpo + '\n\n';
      }
    }

    if (blocoAtual.trim().length > 0) {
      paginas.push(blocoAtual.trim());
    }

    paginasLivro = paginas.length > 0 ? paginas : [conteudo];
  }

  totalPaginas = Math.max(paginasLivro.length, livro.totalPaginas || 1);
}

/**
 * Atualiza o texto exibido no leitor para a página atual
 */
export function atualizarExibicaoPagina() {
  const container = document.getElementById('reader-text-container');
  const elPaginaAtual = document.getElementById('reader-current-page');
  const elTotalPaginas = document.getElementById('reader-total-pages');
  const elBarra = document.getElementById('reader-progress-bar');
  const elProgressoPct = document.getElementById('reader-progress-pct');
  const btnFinalizar = document.getElementById('btn-finalizar-livro');
  const finishSection = document.querySelector('.finish-book-section');

  if (!container) return;

  // Garante limites válidos
  if (paginaAtual < 1) paginaAtual = 1;
  if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

  // Obtém texto da página correspondente
  const indexPagina = Math.min(paginaAtual - 1, paginasLivro.length - 1);
  const textoPagina = paginasLivro[indexPagina] || 'Fim do conteúdo disponível.';

  // Formata em parágrafos HTML seguros
  const paragrafosHtml = textoPagina
    .split(/\n+/)
    .map(p => `<p>${escapeHtml(p.trim())}</p>`)
    .join('');

  container.innerHTML = `
    <div class="reader-page-header">Página ${paginaAtual} de ${totalPaginas}</div>
    ${paragrafosHtml}
  `;

  // Atualiza controles numéricos e barra de progresso
  if (elPaginaAtual) elPaginaAtual.value = paginaAtual;
  if (elTotalPaginas) elTotalPaginas.textContent = totalPaginas;

  const pct = Math.min(100, Math.round((paginaAtual / totalPaginas) * 100));
  if (elBarra) elBarra.style.width = `${pct}%`;
  if (elProgressoPct) elProgressoPct.textContent = `${pct}% lido`;

  // Exibe o botão de confirmação de finalização em destaque se estiver no final
  if (finishSection) {
    if (paginaAtual === totalPaginas || pct >= 95) {
      finishSection.style.display = 'block';
    } else {
      finishSection.style.display = 'block'; // Sempre visível para conveniência do leitor
    }
  }

  // Rola texto para o topo ao mudar de página
  const panel = document.querySelector('.reader-content-panel');
  if (panel) panel.scrollTop = 0;
}

export function mudarPagina(delta) {
  const nova = paginaAtual + delta;
  if (nova >= 1 && nova <= totalPaginas) {
    paginaAtual = nova;
    atualizarExibicaoPagina();
  }
}

export function irParaPagina(num) {
  const n = parseInt(num, 10);
  if (!isNaN(n) && n >= 1 && n <= totalPaginas) {
    paginaAtual = n;
    atualizarExibicaoPagina();
  }
}

/**
 * Marca-Páginas: Carrega progresso salvo no SQLite
 */
async function carregarMarcaPagina(livroId) {
  try {
    const res = await apiFetch(`/api/leitura/marcapagina?livroId=${livroId}`);
    if (res && res.paginaAtual) {
      paginaAtual = res.paginaAtual;
      bookmarkSalvo = res;
      mostrarTimestampMarcaPagina(res.atualizadoEm);
    } else {
      paginaAtual = 1;
    }
  } catch (e) {
    paginaAtual = 1;
  }
  atualizarExibicaoPagina();
}

/**
 * Salva a página atual no banco de dados SQLite
 */
export async function salvarMarcaPaginas() {
  const user = getCurrentUser();
  if (!user) {
    mostrarToast('Faça login para salvar seu marca-páginas no servidor!', 'warning');
    return;
  }
  if (!livroAtivo) return;

  try {
    const res = await apiFetch('/api/leitura/marcapagina', {
      method: 'POST',
      body: {
        livroId: livroAtivo.id,
        paginaAtual: paginaAtual
      }
    });

    bookmarkSalvo = res.marcaPagina || res;
    mostrarTimestampMarcaPagina(new Date().toISOString());
    mostrarToast(`🔖 Marca-páginas salvo na página ${paginaAtual}!`, 'success');
  } catch (e) {
    mostrarToast(e.message || 'Erro ao salvar marca-páginas', 'error');
  }
}

function mostrarTimestampMarcaPagina(dataIso) {
  const el = document.getElementById('reader-bookmark-timestamp');
  if (!el || !dataIso) return;
  try {
    const d = new Date(dataIso);
    el.textContent = `Salvo em ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  } catch (e) {
    el.textContent = '';
  }
}

/**
 * Confirmação de Finalização de Livro
 * "final da pagina do livro ter um botao para confirmar que finalizou o livro e essa confirmacao tem que ser enviada para o perfil dos usuarios para aumentar a contagem de livros lidos"
 */
export async function confirmarFinalizacaoLivro() {
  const user = getCurrentUser();
  if (!user) {
    mostrarToast('Faça login para registrar a conclusão do livro em seu perfil!', 'warning');
    return;
  }
  if (!livroAtivo) return;

  try {
    const res = await apiFetch('/api/leitura/finalizar', {
      method: 'POST',
      body: { livroId: livroAtivo.id }
    });

    mostrarToast('🏆 Parabéns! Livro concluído e adicionado ao seu contador de leituras!', 'success');
    
    // Atualiza o perfil para refletir imediatamente a contagem no SQLite
    await sincronizarPerfil();
  } catch (e) {
    mostrarToast(e.message || 'Erro ao confirmar finalização do livro', 'error');
  }
}

/**
 * Interação com Estrelas de Meia Estrela (0.5) até 5.0 Estrelas
 * "avaliar os livros com a interaçao com estrelas de meia estrela ate 5 estrelas"
 */
export function inicializarWidgetEstrelas() {
  const container = document.getElementById('star-rating-widget');
  const feedback = document.getElementById('star-rating-feedback');
  if (!container) return;

  container.innerHTML = '';

  // Cria 5 estrelas interativas
  for (let i = 1; i <= 5; i++) {
    const star = document.createElement('span');
    star.className = 'star-btn';
    star.textContent = '★';
    star.setAttribute('data-star', i.toString());

    // Suporte a meia estrela através da posição horizontal do mouse
    star.addEventListener('mousemove', (e) => {
      const rect = star.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const isHalf = x < (rect.width / 2);
      const score = isHalf ? (i - 0.5) : i;
      atualizarVisualEstrelas(score, true);
    });

    star.addEventListener('click', (e) => {
      const rect = star.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const isHalf = x < (rect.width / 2);
      const score = isHalf ? (i - 0.5) : i;
      submeterAvaliacao(score);
    });

    container.appendChild(star);
  }

  container.addEventListener('mouseleave', () => {
    atualizarVisualEstrelas(notaSelecionada, false);
  });

  atualizarVisualEstrelas(notaSelecionada, false);
}

function atualizarVisualEstrelas(score, isHover = false) {
  const container = document.getElementById('star-rating-widget');
  const feedback = document.getElementById('star-rating-feedback');
  if (!container) return;

  const stars = container.querySelectorAll('.star-btn');
  stars.forEach((star, index) => {
    const i = index + 1;
    star.className = 'star-btn';

    if (score >= i) {
      star.classList.add(isHover ? 'hover' : 'active');
    } else if (score >= i - 0.5) {
      star.classList.add(isHover ? 'hover' : 'active', 'half-active');
    }
  });

  if (feedback) {
    feedback.textContent = `Nota: ${score.toFixed(1)}`;
  }
}

async function carregarAvaliacaoUsuario(livroId) {
  try {
    const res = await apiFetch(`/api/avaliacoes/livro?livroId=${livroId}`);
    if (res && res.minhaNota != null) {
      notaSelecionada = parseFloat(res.minhaNota);
      atualizarVisualEstrelas(notaSelecionada, false);
    }
  } catch (e) {
    console.warn('Erro ao carregar avaliação do usuário:', e);
  }
}

async function submeterAvaliacao(nota) {
  const user = getCurrentUser();
  if (!user) {
    mostrarToast('Faça login para avaliar livros na LibraryClound!', 'warning');
    return;
  }
  if (!livroAtivo) return;

  notaSelecionada = nota;
  atualizarVisualEstrelas(nota, false);

  try {
    await apiFetch('/api/avaliacoes', {
      method: 'POST',
      body: {
        livroId: livroAtivo.id,
        nota: nota,
        comentario: ''
      }
    });
    mostrarToast(`⭐ Avaliação de ${nota.toFixed(1)} estrelas salva no SQLite!`, 'success');
  } catch (e) {
    mostrarToast(e.message || 'Erro ao enviar avaliação', 'error');
  }
}

/**
 * Controles de Tema de Leitura (Claro, Sépia, Escuro)
 */
export function aplicarTemaLeitura(tema) {
  temaLeitura = tema;
  const panel = document.querySelector('.reader-content-panel');
  if (!panel) return;

  panel.classList.remove('theme-light', 'theme-sepia', 'theme-dark');
  panel.classList.add(`theme-${tema}`);

  document.querySelectorAll('.theme-pill-btn').forEach(btn => {
    if (btn.getAttribute('data-theme') === tema) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
}

/**
 * Controles de Tipografia (Serif vs Sans e Tamanho)
 */
export function ajustarTamanhoFonte(delta) {
  tamanhoFonteRem = Math.max(0.9, Math.min(1.6, tamanhoFonteRem + (delta * 0.1)));
  const container = document.getElementById('reader-text-container');
  if (container) {
    container.style.fontSize = `${tamanhoFonteRem}rem`;
  }
}

export function alternarFamiliaFonte() {
  fonteSerif = !fonteSerif;
  const container = document.getElementById('reader-text-container');
  if (container) {
    container.classList.toggle('font-serif', fonteSerif);
    container.classList.toggle('font-sans', !fonteSerif);
  }
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
