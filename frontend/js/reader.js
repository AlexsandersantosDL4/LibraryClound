/**
 * LibraryClound - Leitor de Livros Paginado, Temas e Avaliações
 * Implementa leitor com paginação real, temas (Claro, Sépia, Escuro),
 * marca-páginas no SQLite, avaliação de 0.5 a 5.0 estrelas e confirmação de finalização.
 */

import { apiFetch } from './api.js';
import { getCurrentUser, sincronizarPerfil } from './auth.js';
import { mostrarToast, abrirModal, fecharModal } from './ui.js';

let livroAtivo = null;
let capitulosLivro = [];
let capituloAtual = 1;
let totalCapitulos = 1;
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
    processarCapitulosEPaginacao(livro);

    // Carrega progresso/marca-página do usuário se logado
    const user = getCurrentUser();
    if (user) {
      await carregarMarcaPagina(livroId);
      await carregarAvaliacaoUsuario(livroId);
    } else {
      capituloAtual = 1;
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
 * Algoritmo de Detecção e Segmentação de Capítulos:
 * Permite que ao mudar de página o livro pule para o próximo capítulo.
 */
function processarCapitulosEPaginacao(livro) {
  const conteudo = livro.conteudo || livro.sinopse || 'Este livro ainda não possui texto completo cadastrado pelo administrador.';
  capitulosLivro = [];

  // 1. Verifica marcadores explícitos: ---CAPÍTULO--- ou ---CAPITULO---
  if (conteudo.includes('---CAPÍTULO---') || conteudo.includes('---CAPITULO---')) {
    const separador = conteudo.includes('---CAPÍTULO---') ? '---CAPÍTULO---' : '---CAPITULO---';
    const blocos = conteudo.split(separador).map(b => b.trim()).filter(Boolean);
    blocos.forEach((bloco, idx) => {
      const linhas = bloco.split('\n');
      const primeiraLinha = linhas[0].replace(/^[#\s*_-]+/, '').trim();
      const titulo = (primeiraLinha.length > 0 && primeiraLinha.length < 80)
        ? primeiraLinha
        : `Capítulo ${idx + 1}`;
      const corpo = (primeiraLinha === titulo && linhas.length > 1)
        ? linhas.slice(1).join('\n').trim()
        : bloco;

      capitulosLivro.push({
        index: idx + 1,
        titulo: (titulo.startsWith('Capítulo') || titulo.startsWith('Capitulo')) ? titulo : `Capítulo ${idx + 1}: ${titulo}`,
        conteudo: corpo
      });
    });
  } else {
    // 2. Busca padrões de cabeçalho de capítulos via Expressão Regular
    const regexCapitulo = /(?:^|\n)(?:#{1,3}\s*)?(?:Cap[ií]tulo|Chapter|PARTE)\s+([0-9IVXLCDM\w]+)(?:[:\.\s\-–—]+([^\n]*))?/gi;
    const matches = [...conteudo.matchAll(regexCapitulo)];

    if (matches.length > 1) {
      for (let i = 0; i < matches.length; i++) {
        const match = matches[i];
        const startPos = match.index + (match[0].startsWith('\n') ? 1 : 0);
        const endPos = (i + 1 < matches.length) ? matches[i + 1].index : conteudo.length;
        const textoCap = conteudo.substring(startPos, endPos).trim();

        const numCap = match[1] || `${i + 1}`;
        const subTitulo = match[2] ? `: ${match[2].trim()}` : '';
        const tituloCompleto = `Capítulo ${numCap}${subTitulo}`;

        capitulosLivro.push({
          index: i + 1,
          titulo: tituloCompleto,
          conteudo: textoCap
        });
      }
    } else {
      // 3. Se não houver cabeçalhos explícitos, particiona em capítulos legíveis (~2.500 car. cada)
      const paragrafos = conteudo.split(/\n\s*\n/);
      let blocoAtual = '';
      let capIdx = 1;

      for (const p of paragrafos) {
        const pLimpo = p.trim();
        if (!pLimpo) continue;

        if ((blocoAtual.length + pLimpo.length > 2500) && blocoAtual.length > 0) {
          capitulosLivro.push({
            index: capIdx,
            titulo: `Capítulo ${capIdx}`,
            conteudo: blocoAtual.trim()
          });
          capIdx++;
          blocoAtual = pLimpo + '\n\n';
        } else {
          blocoAtual += pLimpo + '\n\n';
        }
      }

      if (blocoAtual.trim().length > 0) {
        capitulosLivro.push({
          index: capIdx,
          titulo: `Capítulo ${capIdx}`,
          conteudo: blocoAtual.trim()
        });
      }
    }
  }

  if (capitulosLivro.length === 0) {
    capitulosLivro.push({
      index: 1,
      titulo: 'Capítulo 1: Início',
      conteudo: conteudo
    });
  }

  totalCapitulos = capitulosLivro.length;
  totalPaginas = totalCapitulos;
  popularDropdownCapitulos();
}

function popularDropdownCapitulos() {
  const select = document.getElementById('reader-chapter-select');
  if (!select) return;

  select.innerHTML = capitulosLivro.map(cap => `
    <option value="${cap.index}">${escapeHtml(cap.titulo)}</option>
  `).join('');
}

/**
 * Atualiza o texto exibido no leitor para o capítulo atual
 */
export function atualizarExibicaoPagina() {
  const container = document.getElementById('reader-text-container');
  const select = document.getElementById('reader-chapter-select');
  const counterText = document.getElementById('reader-chapter-counter-text');
  const elBarra = document.getElementById('reader-progress-bar');
  const elProgressoPct = document.getElementById('reader-progress-pct');
  const finishSection = document.querySelector('.finish-book-section');

  if (!container) return;

  // Garante limites válidos para os capítulos
  if (capituloAtual < 1) capituloAtual = 1;
  if (capituloAtual > totalCapitulos) capituloAtual = totalCapitulos;
  paginaAtual = capituloAtual;

  const cap = capitulosLivro[capituloAtual - 1] || capitulosLivro[0];
  const textoCapitulo = cap.conteudo || 'Fim do conteúdo deste capítulo.';

  // Formata os parágrafos do capítulo
  const paragrafosHtml = textoCapitulo
    .split(/\n+/)
    .map(p => `<p>${escapeHtml(p.trim())}</p>`)
    .join('');

  container.innerHTML = `
    <div class="reader-chapter-header">
      <span class="reader-chapter-badge">Capítulo ${cap.index} de ${totalCapitulos}</span>
      <h3 class="reader-chapter-title-large">${escapeHtml(cap.titulo)}</h3>
    </div>
    ${paragrafosHtml}
  `;

  // Sincroniza controles de interface
  if (select) select.value = capituloAtual;
  if (counterText) counterText.textContent = `Capítulo ${capituloAtual} de ${totalCapitulos}`;

  const pct = Math.min(100, Math.round((capituloAtual / totalCapitulos) * 100));
  if (elBarra) elBarra.style.width = `${pct}%`;
  if (elProgressoPct) elProgressoPct.textContent = `${pct}% lido (${capituloAtual}/${totalCapitulos} caps)`;

  if (finishSection) {
    finishSection.style.display = 'block';
  }

  // Rola painel para o topo
  const panel = document.querySelector('.reader-content-panel');
  if (panel) panel.scrollTop = 0;
}

/**
 * Avança ou recua diretamente entre os capítulos ao mudar de página
 */
export function mudarCapitulo(delta) {
  const novo = capituloAtual + delta;
  if (novo >= 1 && novo <= totalCapitulos) {
    capituloAtual = novo;
    paginaAtual = capituloAtual;
    atualizarExibicaoPagina();
  } else if (novo > totalCapitulos) {
    mostrarToast('Você já está no último capítulo desta obra!', 'info');
  } else if (novo < 1) {
    mostrarToast('Você já está no primeiro capítulo.', 'info');
  }
}

export function selecionarCapituloDropdown(val) {
  const n = parseInt(val, 10);
  if (!isNaN(n) && n >= 1 && n <= totalCapitulos) {
    capituloAtual = n;
    paginaAtual = capituloAtual;
    atualizarExibicaoPagina();
  }
}

// Mantém compatibilidade com atalhos de mudar página chamando mudarCapitulo
export function mudarPagina(delta) {
  mudarCapitulo(delta);
}

export function irParaPagina(num) {
  selecionarCapituloDropdown(num);
}

/**
 * Marca-Páginas: Carrega progresso salvo no SQLite
 */
async function carregarMarcaPagina(livroId) {
  try {
    const res = await apiFetch(`/api/leitura/marcapagina?livroId=${livroId}`);
    if (res && res.paginaAtual) {
      capituloAtual = res.paginaAtual;
      paginaAtual = capituloAtual;
      bookmarkSalvo = res;
      mostrarTimestampMarcaPagina(res.atualizadoEm);
    } else {
      capituloAtual = 1;
      paginaAtual = 1;
    }
  } catch (e) {
    capituloAtual = 1;
    paginaAtual = 1;
  }
  atualizarExibicaoPagina();
}
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
