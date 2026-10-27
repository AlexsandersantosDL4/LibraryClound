/**
 * LibraryClound - Autenticação e Gestão de Perfil do Usuário
 * Suporte a BCrypt + JWT, edição de perfil com bio até 150 car., avatar e contagem de livros lidos.
 */

import { apiFetch, setToken, removeToken } from './api.js';
import { mostrarToast, abrirModal, fecharModal } from './ui.js';

let currentUser = null;

export function getCurrentUser() {
  return currentUser;
}

export function setCurrentUser(user) {
  currentUser = user;
  if (user) {
    localStorage.setItem('libraryclound_user', JSON.stringify(user));
  } else {
    localStorage.removeItem('libraryclound_user');
  }
  atualizarNavbar();
}

/**
 * Inicializa a sessão a partir do localStorage e sincroniza com o backend
 */
export async function inicializarSessao() {
  const salvo = localStorage.getItem('libraryclound_user');
  if (salvo) {
    try {
      currentUser = JSON.parse(salvo);
      atualizarNavbar();
      await sincronizarPerfil();
    } catch (e) {
      console.warn('Erro ao restaurar sessão de usuário:', e);
      fazerLogout();
    }
  } else {
    atualizarNavbar();
  }

  // Listener para evento de 401 disparado pelo cliente de API
  window.addEventListener('libraryclound:unauthorized', () => {
    fazerLogout();
    mostrarToast('Sua sessão expirou. Faça login novamente.', 'warning');
  });
}

/**
 * Sincroniza dados atualizados do usuário com o endpoint /api/auth/me
 */
export async function sincronizarPerfil() {
  if (!currentUser) return;
  try {
    const dados = await apiFetch('/api/auth/me');
    if (dados && dados.id) {
      setCurrentUser({ ...currentUser, ...dados });
    }
  } catch (e) {
    console.warn('Falha na sincronização de perfil:', e.message);
  }
}

/**
 * Atualiza visualmente a navbar com dados da sessão
 */
export function atualizarNavbar() {
  const navGuest = document.getElementById('nav-guest');
  const navUser = document.getElementById('nav-user');
  const btnAdmin = document.getElementById('btn-admin-panel');

  if (!navGuest || !navUser) return;

  if (currentUser) {
    navGuest.classList.add('hidden');
    navUser.classList.remove('hidden');

    const nameDisplay = document.getElementById('user-name-display');
    const roleDisplay = document.getElementById('user-role-display');
    const readCount = document.getElementById('profile-books-read');
    const avatarImg = document.getElementById('user-avatar-img');
    const avatarInit = document.getElementById('user-avatar-initial');

    if (nameDisplay) nameDisplay.textContent = currentUser.nomeFantasia || 'Leitor';
    if (roleDisplay) roleDisplay.textContent = currentUser.tipo || 'CLIENTE';
    if (readCount) readCount.textContent = currentUser.livrosLidos ?? 0;

    // Foto de perfil ou Círculo com Inicial
    if (avatarImg && avatarInit) {
      if (currentUser.fotoUrl && currentUser.fotoUrl.trim() !== '') {
        avatarImg.src = currentUser.fotoUrl;
        avatarImg.classList.remove('hidden');
        avatarInit.classList.add('hidden');
      } else {
        avatarImg.classList.add('hidden');
        avatarInit.classList.remove('hidden');
        avatarInit.textContent = (currentUser.nomeFantasia || 'U').charAt(0).toUpperCase();
      }
    }

    // Acesso ao Painel do Administrador/Dono
    if (btnAdmin) {
      if (currentUser.tipo === 'ADMIN') {
        btnAdmin.classList.remove('hidden');
      } else {
        btnAdmin.classList.add('hidden');
      }
    }
  } else {
    navGuest.classList.remove('hidden');
    navUser.classList.add('hidden');
  }
}

/**
 * Realiza login do usuário com BCrypt e JWT
 */
export async function realizarLogin(email, senha) {
  try {
    const data = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: { email, senha }
    });

    if (data.token) {
      setToken(data.token);
    }
    setCurrentUser(data.usuario);
    fecharModal('modal-auth');
    mostrarToast(`👋 Bem-vindo(a) à LibraryClound, ${data.usuario.nomeFantasia}!`, 'success');

    // Dispara evento customizado para outros módulos recarregarem favoritos e acervo
    window.dispatchEvent(new CustomEvent('libraryclound:auth-changed', { detail: data.usuario }));
  } catch (e) {
    mostrarToast(e.message || 'Erro ao realizar login', 'error');
    throw e;
  }
}

/**
 * Realiza cadastro de novo leitor na LibraryClound
 */
export async function realizarCadastro(nomeFantasia, email, senha) {
  if (!/^[0-9]{1,8}$/.test(senha)) {
    mostrarToast('⚠️ A senha deve conter exclusivamente números e no máximo 8 dígitos!', 'warning');
    return;
  }

  try {
    const data = await apiFetch('/api/auth/register', {
      method: 'POST',
      body: { nomeFantasia, email, senha, tipo: 'CLIENTE' }
    });

    if (data.token) {
      setToken(data.token);
    }
    setCurrentUser(data.usuario);
    fecharModal('modal-auth');
    mostrarToast(`🎉 Conta criada com sucesso! Bem-vindo(a), ${data.usuario.nomeFantasia}!`, 'success');

    window.dispatchEvent(new CustomEvent('libraryclound:auth-changed', { detail: data.usuario }));
  } catch (e) {
    mostrarToast(e.message || 'Erro ao realizar cadastro', 'error');
    throw e;
  }
}

/**
 * Encerra a sessão ativa
 */
export function fazerLogout() {
  removeToken();
  currentUser = null;
  localStorage.removeItem('libraryclound_user');
  atualizarNavbar();

  // Fecha modais e volta à visualização do catálogo
  fecharModal('modal-perfil');
  fecharModal('modal-leitor');

  const secaoAdmin = document.getElementById('secao-admin');
  const secaoCatalogo = document.getElementById('secao-catalogo');
  if (secaoAdmin) secaoAdmin.classList.add('hidden');
  if (secaoCatalogo) secaoCatalogo.classList.remove('hidden');

  mostrarToast('Sessão encerrada com segurança.', 'info');
  window.dispatchEvent(new CustomEvent('libraryclound:auth-changed', { detail: null }));
}

/**
 * Preenche os campos do modal de perfil (Aba de visualização e Aba de edição)
 */
export function abrirModalPerfil(abaInicial = 'dados') {
  if (!currentUser) {
    mostrarToast('Faça login para acessar seu perfil.', 'warning');
    abrirModalAuth('login');
    return;
  }

  // Preenche dados da visualização
  const viewNome = document.getElementById('view-profile-nome');
  const viewRole = document.getElementById('view-profile-role');
  const viewEmail = document.getElementById('view-profile-email');
  const viewDesc = document.getElementById('view-profile-desc');
  const viewLidos = document.getElementById('view-profile-lidos');
  const viewFoto = document.getElementById('view-profile-photo');
  const viewInit = document.getElementById('view-profile-initial');

  if (viewNome) viewNome.textContent = currentUser.nomeFantasia || 'Leitor';
  if (viewRole) viewRole.textContent = currentUser.tipo || 'CLIENTE';
  if (viewEmail) viewEmail.textContent = currentUser.email || '';
  if (viewDesc) viewDesc.textContent = currentUser.descricao || 'Nenhuma descrição cadastrada ainda.';
  if (viewLidos) viewLidos.textContent = currentUser.livrosLidos ?? 0;

  if (viewFoto && viewInit) {
    if (currentUser.fotoUrl && currentUser.fotoUrl.trim() !== '') {
      viewFoto.src = currentUser.fotoUrl;
      viewFoto.classList.remove('hidden');
      viewInit.classList.add('hidden');
    } else {
      viewFoto.classList.add('hidden');
      viewInit.classList.remove('hidden');
      viewInit.textContent = (currentUser.nomeFantasia || 'U').charAt(0).toUpperCase();
    }
  }

  // Preenche campos do formulário de edição
  const editNome = document.getElementById('edit-nome-fantasia');
  const editFoto = document.getElementById('edit-foto-url');
  const editDesc = document.getElementById('edit-descricao');
  const inputFile = document.getElementById('edit-foto-file');
  const previewContainer = document.getElementById('foto-preview-container');
  const previewImg = document.getElementById('edit-foto-preview');
  const previewNome = document.getElementById('foto-preview-nome');

  if (inputFile) inputFile.value = '';
  if (editNome) editNome.value = currentUser.nomeFantasia || '';
  if (editFoto) editFoto.value = currentUser.fotoUrl || '';
  if (editDesc) {
    editDesc.value = currentUser.descricao || '';
    atualizarContadorDescricao();
  }

  if (currentUser.fotoUrl && currentUser.fotoUrl.trim() !== '') {
    if (previewContainer) previewContainer.classList.remove('hidden');
    if (previewImg) previewImg.src = currentUser.fotoUrl;
    if (previewNome) previewNome.textContent = 'Foto atual do perfil';
  } else {
    if (previewContainer) previewContainer.classList.add('hidden');
  }

  // Abre modal e seleciona a aba
  abrirModal('modal-perfil');
  selecionarAbaPerfil(abaInicial);
}

export function selecionarAbaPerfil(aba) {
  const tabs = {
    dados: { btn: 'tab-btn-dados', content: 'perfil-tab-dados' },
    editar: { btn: 'tab-btn-editar', content: 'perfil-tab-editar' },
    favoritos: { btn: 'tab-btn-favoritos', content: 'perfil-tab-favoritos' }
  };

  Object.keys(tabs).forEach(key => {
    const btn = document.getElementById(tabs[key].btn);
    const content = document.getElementById(tabs[key].content);
    if (key === aba) {
      if (btn) btn.classList.add('active');
      if (content) content.classList.remove('hidden');
    } else {
      if (btn) btn.classList.remove('active');
      if (content) content.classList.add('hidden');
    }
  });

  if (aba === 'favoritos') {
    window.dispatchEvent(new CustomEvent('libraryclound:load-favorites'));
  }
}

/**
 * Salva as alterações de perfil com validação de até 150 caracteres
 */
export async function salvarEdicaoPerfil(event) {
  event.preventDefault();
  if (!currentUser) return;

  const nomeFantasia = document.getElementById('edit-nome-fantasia').value.trim();
  const fotoUrl = document.getElementById('edit-foto-url').value.trim();
  const descricao = document.getElementById('edit-descricao').value.trim();

  if (descricao.length > 150) {
    mostrarToast('⚠️ A descrição deve ter no máximo 150 caracteres!', 'warning');
    return;
  }

  try {
    const atualizado = await apiFetch('/api/auth/perfil', {
      method: 'PUT',
      body: { nomeFantasia, fotoUrl, descricao }
    });

    setCurrentUser({ ...currentUser, ...atualizado });
    mostrarToast('✨ Perfil atualizado com sucesso no SQLite!', 'success');
    selecionarAbaPerfil('dados');
  } catch (e) {
    mostrarToast(e.message || 'Erro ao atualizar perfil', 'error');
  }
}

/**
 * Atualiza o contador de caracteres da bio (máx 150)
 */
export function atualizarContadorDescricao() {
  const textarea = document.getElementById('edit-descricao');
  const counter = document.getElementById('char-counter');
  if (!textarea || !counter) return;

  const length = textarea.value.length;
  counter.textContent = `${length} / 150`;

  counter.classList.remove('limit-near', 'limit-reached');
  if (length >= 150) {
    counter.classList.add('limit-reached');
  } else if (length >= 130) {
    counter.classList.add('limit-near');
  }
}

export function selecionarPresetAvatar(url) {
  const input = document.getElementById('edit-foto-url');
  if (input) {
    input.value = url;
    atualizarPreviewPorUrl();
    mostrarToast('Avatar selecionado! Clique em salvar para confirmar.', 'info');
  }
}

/**
 * Processa imagem importada diretamente do computador ou celular do usuário
 */
export async function processarFotoLocal(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    mostrarToast('Por favor, selecione um arquivo de imagem válido (JPG, PNG, WEBP, GIF).', 'warning');
    return;
  }

  try {
    const dataUrl = await comprimirERedimensionarImagem(file, 320, 320, 0.85);

    const inputUrl = document.getElementById('edit-foto-url');
    if (inputUrl) inputUrl.value = dataUrl;

    const previewContainer = document.getElementById('foto-preview-container');
    const previewImg = document.getElementById('edit-foto-preview');
    const previewNome = document.getElementById('foto-preview-nome');

    if (previewContainer) previewContainer.classList.remove('hidden');
    if (previewImg) previewImg.src = dataUrl;
    if (previewNome) previewNome.textContent = file.name;

    const viewFoto = document.getElementById('view-profile-photo');
    const viewInit = document.getElementById('view-profile-initial');
    if (viewFoto && viewInit) {
      viewFoto.src = dataUrl;
      viewFoto.classList.remove('hidden');
      viewInit.classList.add('hidden');
    }

    mostrarToast('📷 Foto importada do seu dispositivo! Clique em "Salvar Alterações" para gravar.', 'info');
  } catch (err) {
    console.error('Erro ao processar imagem:', err);
    mostrarToast('Não foi possível ler a imagem selecionada.', 'error');
  }
}

function comprimirERedimensionarImagem(file, maxLargura, maxAltura, qualidade) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxLargura) {
            height = Math.round((height * maxLargura) / width);
            width = maxLargura;
          }
        } else {
          if (height > maxAltura) {
            width = Math.round((width * maxAltura) / height);
            height = maxAltura;
          }
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        resolve(canvas.toDataURL('image/jpeg', qualidade));
      };
      img.onerror = reject;
      img.src = e.target.result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export function removerFotoPreview() {
  const inputUrl = document.getElementById('edit-foto-url');
  const inputFile = document.getElementById('edit-foto-file');
  const previewContainer = document.getElementById('foto-preview-container');
  const previewImg = document.getElementById('edit-foto-preview');

  if (inputUrl) inputUrl.value = '';
  if (inputFile) inputFile.value = '';
  if (previewContainer) previewContainer.classList.add('hidden');
  if (previewImg) previewImg.src = '';

  const viewFoto = document.getElementById('view-profile-photo');
  const viewInit = document.getElementById('view-profile-initial');
  if (viewFoto && viewInit) {
    viewFoto.classList.add('hidden');
    viewInit.classList.remove('hidden');
  }
}

export function atualizarPreviewPorUrl() {
  const inputUrl = document.getElementById('edit-foto-url');
  const url = inputUrl ? inputUrl.value.trim() : '';
  const previewContainer = document.getElementById('foto-preview-container');
  const previewImg = document.getElementById('edit-foto-preview');
  const previewNome = document.getElementById('foto-preview-nome');

  if (url) {
    if (previewContainer) previewContainer.classList.remove('hidden');
    if (previewImg) previewImg.src = url;
    if (previewNome) previewNome.textContent = 'Imagem por Link Externo';
  } else {
    if (previewContainer) previewContainer.classList.add('hidden');
  }
}

export function abrirModalAuth(aba = 'login') {
  abrirModal('modal-auth');
  alternarAbaAuth(aba);
}

export function alternarAbaAuth(aba) {
  const viewLogin = document.getElementById('auth-login-view');
  const viewRegister = document.getElementById('auth-register-view');
  if (aba === 'login') {
    if (viewLogin) viewLogin.classList.remove('hidden');
    if (viewRegister) viewRegister.classList.add('hidden');
  } else {
    if (viewLogin) viewLogin.classList.add('hidden');
    if (viewRegister) viewRegister.classList.remove('hidden');
  }
}
