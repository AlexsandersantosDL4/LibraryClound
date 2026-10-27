/**
 * LibraryClound - Ponto de Entrada Principal da Aplicação (Bootstrap)
 * Integração dos módulos ES6: UI, Autenticação, Catálogo, Leitor Paginado e Painel Administrativo.
 */

import { initTheme, toggleTheme, setupModalDismissers, mostrarToast, abrirModal, fecharModal } from './ui.js';
import {
  inicializarSessao,
  getCurrentUser,
  realizarLogin,
  realizarCadastro,
  fazerLogout,
  abrirModalPerfil,
  selecionarAbaPerfil,
  salvarEdicaoPerfil,
  atualizarContadorDescricao,
  selecionarPresetAvatar,
  abrirModalAuth,
  alternarAbaAuth
} from './auth.js';
import {
  carregarLivros,
  carregarCategorias,
  carregarFavoritosUsuario,
  filtrarLivros,
  alternarFavorito
} from './books.js';
import {
  abrirLeitor,
  fecharLeitor,
  mudarPagina,
  irParaPagina,
  salvarMarcaPaginas,
  confirmarFinalizacaoLivro,
  aplicarTemaLeitura,
  ajustarTamanhoFonte,
  alternarFamiliaFonte
} from './reader.js';
import {
  alternarPainelAdmin,
  abrirModalLivro,
  fecharModalLivro,
  salvarLivroAdmin,
  excluirLivroAdmin,
  trocarAbaAdmin
} from './admin.js';

// Inicialização ao carregar o DOM
document.addEventListener('DOMContentLoaded', async () => {
  console.log('🚀 Inicializando frontend modular da LibraryClound...');

  // 1. Inicializa tema e listeners de modais
  initTheme();
  setupModalDismissers();

  // 2. Restaura sessão do usuário (se houver token)
  await inicializarSessao();

  // 3. Carrega categorias e catálogo de livros
  await carregarCategorias();
  await carregarLivros();

  // 4. Configura navegação por teclado para o leitor (Setas Esquerda e Direita)
  document.addEventListener('keydown', (e) => {
    const modalLeitor = document.getElementById('modal-leitor');
    if (modalLeitor && !modalLeitor.classList.contains('hidden')) {
      if (e.key === 'ArrowLeft') {
        mudarPagina(-1);
      } else if (e.key === 'ArrowRight') {
        mudarPagina(1);
      }
    }
  });

  // 5. Configura recarga quando status de autenticação mudar
  window.addEventListener('libraryclound:auth-changed', async () => {
    await carregarFavoritosUsuario();
    await carregarLivros();
  });

  window.addEventListener('libraryclound:load-favorites', async () => {
    await carregarFavoritosUsuario();
  });

  console.log('✨ LibraryClound inicializada com sucesso!');
});

// Exposição controlada para bindings inline de templates HTML
window.irParaHome = () => {
  const secaoAdmin = document.getElementById('secao-admin');
  const secaoCatalogo = document.getElementById('secao-catalogo');
  if (secaoAdmin) secaoAdmin.classList.add('hidden');
  if (secaoCatalogo) secaoCatalogo.classList.remove('hidden');
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

window.toggleTheme = toggleTheme;
window.abrirModalAuth = abrirModalAuth;
window.fecharModalAuth = () => fecharModal('modal-auth');
window.alternarAbaAuth = alternarAbaAuth;
window.realizarLogin = (e) => {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const senha = document.getElementById('login-senha').value.trim();
  realizarLogin(email, senha);
};
window.realizarCadastro = (e) => {
  e.preventDefault();
  const nome = document.getElementById('reg-nome').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const senha = document.getElementById('reg-senha').value.trim();
  realizarCadastro(nome, email, senha);
};
window.fazerLogout = fazerLogout;

window.abrirModalPerfil = abrirModalPerfil;
window.fecharModalPerfil = () => fecharModal('modal-perfil');
window.trocarAbaPerfil = selecionarAbaPerfil;
window.salvarEdicaoPerfil = salvarEdicaoPerfil;
window.atualizarContadorDescricao = atualizarContadorDescricao;
window.selecionarPresetAvatar = selecionarPresetAvatar;

window.filtrarLivros = filtrarLivros;
window.alternarFavorito = alternarFavorito;

window.abrirLeitor = abrirLeitor;
window.fecharModalLeitor = fecharLeitor;
window.alterarPaginaAtual = mudarPagina;
window.atualizarBarraProgresso = () => {
  const input = document.getElementById('reader-current-page');
  if (input) irParaPagina(input.value);
};
window.salvarMarcaPaginas = salvarMarcaPaginas;
window.confirmarFinalizacaoLivro = confirmarFinalizacaoLivro;
window.aplicarTemaLeitura = aplicarTemaLeitura;
window.ajustarTamanhoFonte = ajustarTamanhoFonte;
window.alternarFamiliaFonte = alternarFamiliaFonte;

window.alternarPainelAdmin = alternarPainelAdmin;
window.abrirModalLivro = abrirModalLivro;
window.fecharModalLivro = fecharModalLivro;
window.salvarLivroAdmin = salvarLivroAdmin;
window.excluirLivroAdmin = excluirLivroAdmin;
window.trocarAbaAdmin = trocarAbaAdmin;
