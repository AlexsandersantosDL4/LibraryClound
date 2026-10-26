// ==========================================
// LIBRARYCLOUND - JAVASCRIPT PRINCIPAL
// ==========================================

const API_BASE = '';
let currentUser = null;
let currentBook = null;
let currentBookmark = null;
let currentUserRating = 5.0;
let isCurrentBookFavorited = false;
let todosLivros = [];
let userFavorites = [];
let currentFontSize = 1.05; // rem

document.addEventListener('DOMContentLoaded', () => {
    carregarUsuarioSalvo();
    carregarCategorias();
    carregarLivros();
});

// ==========================================
// 1. AUTENTICAÇÃO E PERFIL DO USUÁRIO
// ==========================================

function carregarUsuarioSalvo() {
    const saved = localStorage.getItem('libraryclound_user') || localStorage.getItem('livraria_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            atualizarNavbar();
            sincronizarPerfil();
            carregarFavoritosDoUsuario();
        } catch (e) {
            localStorage.removeItem('libraryclound_user');
        }
    } else {
        atualizarNavbar();
    }
}

async function sincronizarPerfil() {
    if (!currentUser || !currentUser.id) return;
    try {
        const res = await fetch(`${API_BASE}/api/auth/me?id=${currentUser.id}`);
        if (res.ok) {
            const dados = await res.json();
            currentUser = { ...currentUser, ...dados };
            localStorage.setItem('libraryclound_user', JSON.stringify(currentUser));
            atualizarNavbar();
        }
    } catch (e) {
        console.warn('Erro ao sincronizar perfil:', e);
    }
}

function atualizarNavbar() {
    const navGuest = document.getElementById('nav-guest');
    const navUser = document.getElementById('nav-user');
    const btnAdmin = document.getElementById('btn-admin-panel');

    if (currentUser) {
        navGuest.classList.add('hidden');
        navUser.classList.remove('hidden');

        document.getElementById('user-name-display').textContent = currentUser.nomeFantasia || 'Leitor';
        document.getElementById('user-role-display').textContent = currentUser.tipo;
        document.getElementById('profile-books-read').textContent = currentUser.livrosLidos || 0;

        // Foto ou Inicial do Avatar
        const avatarImg = document.getElementById('user-avatar-img');
        const avatarInitial = document.getElementById('user-avatar-initial');

        if (currentUser.fotoUrl && currentUser.fotoUrl.trim() !== '') {
            avatarImg.src = currentUser.fotoUrl;
            avatarImg.classList.remove('hidden');
            avatarInitial.classList.add('hidden');
        } else {
            avatarImg.classList.add('hidden');
            avatarInitial.classList.remove('hidden');
            avatarInitial.textContent = (currentUser.nomeFantasia || 'U').charAt(0).toUpperCase();
        }

        if (currentUser.tipo === 'ADMIN') {
            btnAdmin.classList.remove('hidden');
        } else {
            btnAdmin.classList.add('hidden');
        }
    } else {
        navGuest.classList.remove('hidden');
        navUser.classList.add('hidden');
    }
}

async function realizarCadastro(event) {
    event.preventDefault();
    const nomeFantasia = document.getElementById('reg-nome').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const senha = document.getElementById('reg-senha').value.trim();

    if (!/^[0-9]{1,8}$/.test(senha)) {
        mostrarToast('⚠️ A senha deve conter exclusivamente números e no máximo 8 dígitos!');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nomeFantasia, email, senha, tipo: 'CLIENTE' })
        });

        const data = await res.json();
        if (res.ok) {
            currentUser = data.usuario;
            localStorage.setItem('libraryclound_user', JSON.stringify(currentUser));
            fecharModalAuth();
            atualizarNavbar();
            mostrarToast(`🎉 Bem-vindo(a) à LibraryClound, ${currentUser.nomeFantasia}!`);
            carregarLivros();
            carregarFavoritosDoUsuario();
        } else {
            mostrarToast(`❌ ${data.erro || 'Erro ao realizar cadastro'}`);
        }
    } catch (e) {
        mostrarToast('❌ Não foi possível conectar ao servidor da API.');
    }
}

async function realizarLogin(event) {
    event.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const senha = document.getElementById('login-senha').value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha })
        });

        const data = await res.json();
        if (res.ok) {
            currentUser = data.usuario;
            localStorage.setItem('libraryclound_user', JSON.stringify(currentUser));
            fecharModalAuth();
            atualizarNavbar();
            mostrarToast(`👋 Olá novamente, ${currentUser.nomeFantasia}!`);
            carregarLivros();
            carregarFavoritosDoUsuario();
        } else {
            mostrarToast(`❌ ${data.erro || 'Credenciais inválidas'}`);
        }
    } catch (e) {
        mostrarToast('❌ Falha na comunicação com o servidor.');
    }
}

function fazerLogout() {
    currentUser = null;
    localStorage.removeItem('libraryclound_user');
    userFavorites = [];
    atualizarNavbar();
    document.getElementById('secao-admin').classList.add('hidden');
    document.getElementById('secao-catalogo').classList.remove('hidden');
    mostrarToast('Sessão encerrada com sucesso.');
    carregarLivros();
}

function irParaHome() {
    document.getElementById('secao-admin').classList.add('hidden');
    document.getElementById('secao-catalogo').classList.remove('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ==========================================
// 2. MODAL DE PERFIL: EDIÇÃO E FAVORITOS
// ==========================================

function abrirModalPerfil(aba = 'dados') {
    if (!currentUser) {
        abrirModalAuth('login');
        return;
    }

    // Preencher dados da visualização
    document.getElementById('view-profile-nome').textContent = currentUser.nomeFantasia;
    document.getElementById('view-profile-role').textContent = currentUser.tipo;
    document.getElementById('view-profile-email').textContent = currentUser.email;
    document.getElementById('view-profile-desc').textContent = currentUser.descricao || 'Nenhuma descrição cadastrada ainda.';
    document.getElementById('view-profile-lidos').textContent = currentUser.livrosLidos || 0;
    document.getElementById('view-profile-favs').textContent = userFavorites.length;

    const vImg = document.getElementById('view-profile-photo');
    const vInit = document.getElementById('view-profile-initial');
    if (currentUser.fotoUrl && currentUser.fotoUrl.trim() !== '') {
        vImg.src = currentUser.fotoUrl;
        vImg.classList.remove('hidden');
        vInit.classList.add('hidden');
    } else {
        vImg.classList.add('hidden');
        vInit.classList.remove('hidden');
        vInit.textContent = (currentUser.nomeFantasia || 'U').charAt(0).toUpperCase();
    }

    // Preencher formulário de edição
    document.getElementById('edit-nome-fantasia').value = currentUser.nomeFantasia || '';
    document.getElementById('edit-foto-url').value = currentUser.fotoUrl || '';
    document.getElementById('edit-descricao').value = currentUser.descricao || '';
    atualizarContadorDescricao();

    document.getElementById('modal-perfil').classList.remove('hidden');
    trocarAbaPerfil(aba);
}

function fecharModalPerfil() {
    document.getElementById('modal-perfil').classList.add('hidden');
}

function trocarAbaPerfil(aba) {
    document.getElementById('tab-btn-dados').classList.remove('active');
    document.getElementById('tab-btn-editar').classList.remove('active');
    document.getElementById('tab-btn-favoritos').classList.remove('active');

    document.getElementById('perfil-tab-dados').classList.add('hidden');
    document.getElementById('perfil-tab-editar').classList.add('hidden');
    document.getElementById('perfil-tab-favoritos').classList.add('hidden');

    if (aba === 'dados') {
        document.getElementById('tab-btn-dados').classList.add('active');
        document.getElementById('perfil-tab-dados').classList.remove('hidden');
    } else if (aba === 'editar') {
        document.getElementById('tab-btn-editar').classList.add('active');
        document.getElementById('perfil-tab-editar').classList.remove('hidden');
    } else if (aba === 'favoritos') {
        document.getElementById('tab-btn-favoritos').classList.add('active');
        document.getElementById('perfil-tab-favoritos').classList.remove('hidden');
        renderizarGradeFavoritos();
    }
}

// Contador de caracteres para a descrição (máximo 150 caracteres)
function atualizarContadorDescricao() {
    const textarea = document.getElementById('edit-descricao');
    const counter = document.getElementById('char-counter');
    const len = textarea.value.length;
    counter.textContent = `${len} / 150`;

    if (len >= 140) {
        counter.className = 'char-counter limit';
    } else if (len >= 110) {
        counter.className = 'char-counter warning';
    } else {
        counter.className = 'char-counter';
    }
}

function selecionarPresetAvatar(url) {
    document.getElementById('edit-foto-url').value = url;
    mostrarToast('Avatar selecionado!');
}

function previsualizarFotoPerfil() {
    // Apenas para feedback visual
}

// Salvar alterações de perfil (Nome, Foto, Descrição até 150 caracteres)
async function salvarEdicaoPerfil(event) {
    event.preventDefault();
    if (!currentUser) return;

    const nomeFantasia = document.getElementById('edit-nome-fantasia').value.trim();
    const fotoUrl = document.getElementById('edit-foto-url').value.trim();
    const descricao = document.getElementById('edit-descricao').value.trim();

    if (descricao.length > 150) {
        mostrarToast('⚠️ A descrição não pode exceder 150 caracteres.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/auth/perfil`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                id: currentUser.id,
                nomeFantasia,
                fotoUrl,
                descricao
            })
        });

        const data = await res.json();
        if (res.ok) {
            currentUser = data.usuario;
            localStorage.setItem('libraryclound_user', JSON.stringify(currentUser));
            atualizarNavbar();
            trocarAbaPerfil('dados');
            abrirModalPerfil('dados');
            mostrarToast('✅ Perfil atualizado com sucesso no SQLite!');
        } else {
            mostrarToast(`❌ ${data.erro || 'Falha ao atualizar perfil'}`);
        }
    } catch (e) {
        mostrarToast('❌ Erro de comunicação com o servidor.');
    }
}

// ==========================================
// 3. FAVORITOS
// ==========================================

async function carregarFavoritosDoUsuario() {
    if (!currentUser) {
        userFavorites = [];
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/favoritos/usuario?usuarioId=${currentUser.id}`);
        if (res.ok) {
            userFavorites = await res.json();
            const badge = document.getElementById('fav-count-badge');
            if (badge) badge.textContent = userFavorites.length;
            const viewFavs = document.getElementById('view-profile-favs');
            if (viewFavs) viewFavs.textContent = userFavorites.length;
        }
    } catch (e) {
        console.warn('Erro ao carregar favoritos:', e);
    }
}

async function toggleFavorito(livroId, event = null) {
    if (event) event.stopPropagation();

    if (!currentUser) {
        abrirModalAuth('login');
        mostrarToast('⚠️ Faça login para adicionar livros aos seus favoritos!');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/favoritos/toggle`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ usuarioId: currentUser.id, livroId: livroId })
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast(data.mensagem);
            await carregarFavoritosDoUsuario();
            renderizarLivros(todosLivros);

            if (currentBook && currentBook.id === livroId) {
                isCurrentBookFavorited = data.favoritado;
                atualizarBotaoFavoritoLeitor();
            }
        }
    } catch (e) {
        mostrarToast('❌ Erro ao alternar favorito.');
    }
}

function toggleFavoritoAtual() {
    if (currentBook) {
        toggleFavorito(currentBook.id);
    }
}

function atualizarBotaoFavoritoLeitor() {
    const icon = document.getElementById('reader-fav-icon');
    if (icon) {
        icon.textContent = isCurrentBookFavorited ? '❤️' : '🤍';
    }
}

function renderizarGradeFavoritos() {
    const grid = document.getElementById('favoritos-grid');
    if (!grid) return;

    if (userFavorites.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 2rem; color: #64748b;">Nenhum livro favoritado ainda. Navegue pelo catálogo e clique no coração ❤️ para favoritar!</div>';
        return;
    }

    // Requisito: "essa funcao deve aparecer no perfil dos usuarios mostrando, nome, categoria e a capa do livro"
    grid.innerHTML = userFavorites.map(f => `
        <div class="fav-card">
            <img src="${f.livroCapaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'}" alt="${f.livroTitulo}" class="fav-cover" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'">
            <div class="fav-details">
                <span class="fav-cat">${f.livroCategoria}</span>
                <strong class="fav-title">${f.livroTitulo}</strong>
                <div style="margin-top: auto; padding-top: 0.5rem; display: flex; gap: 0.4rem;">
                    <button class="btn btn-sm btn-primary btn-block" onclick="fecharModalPerfil(); abrirLeitor(${f.livroId});">
                        📖 Abrir
                    </button>
                    <button class="btn btn-sm btn-outline" onclick="toggleFavorito(${f.livroId}); setTimeout(renderizarGradeFavoritos, 300);" title="Remover dos favoritos">
                        💔
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// ==========================================
// 4. CATÁLOGO, PESQUISA E AVALIAÇÕES
// ==========================================

async function carregarCategorias() {
    try {
        const res = await fetch(`${API_BASE}/api/livros/categorias`);
        if (res.ok) {
            const categorias = await res.json();
            const select = document.getElementById('select-categoria');
            select.innerHTML = '<option value="">Todas as Categorias</option>';
            categorias.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat;
                opt.textContent = cat;
                select.appendChild(opt);
            });
        }
    } catch (e) {
        console.error('Erro ao carregar categorias:', e);
    }
}

async function carregarLivros() {
    try {
        const termo = document.getElementById('input-busca').value.trim();
        const categoria = document.getElementById('select-categoria').value;

        let url = `${API_BASE}/api/livros`;
        const params = new URLSearchParams();
        if (termo) params.append('termo', termo);
        if (categoria) params.append('categoria', categoria);
        if (params.toString()) url += `?${params.toString()}`;

        const res = await fetch(url);
        if (res.ok) {
            todosLivros = await res.json();
            renderizarLivros(todosLivros);
        }
    } catch (e) {
        console.error('Erro ao buscar livros:', e);
    }
}

function filtrarLivros() {
    carregarLivros();
}

function renderizarLivros(livros) {
    const grid = document.getElementById('livros-grid');
    const badgeCount = document.getElementById('livros-count-badge');
    badgeCount.textContent = livros.length;

    if (livros.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: #64748b;">Nenhum livro encontrado com os filtros aplicados.</div>';
        return;
    }

    grid.innerHTML = livros.map(livro => {
        const isFav = userFavorites.some(f => f.livroId === livro.id);
        const ratingStr = (livro.mediaAvaliacao > 0) ? `⭐ ${livro.mediaAvaliacao.toFixed(1)} (${livro.totalAvaliacoes})` : '⭐ Novo';

        return `
            <div class="book-card">
                <div class="book-cover-wrap">
                    <img src="${livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'}" alt="${livro.titulo}" class="book-cover-img" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'">
                    <span class="book-category-tag">${livro.categoria}</span>
                    <button class="btn-fav-card ${isFav ? 'favorited' : ''}" onclick="toggleFavorito(${livro.id}, event)" title="${isFav ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}">
                        ${isFav ? '❤️' : '🤍'}
                    </button>
                </div>
                <div class="book-content">
                    <div class="book-rating-badge">${ratingStr}</div>
                    <h3 class="book-title">${livro.titulo}</h3>
                    <p class="book-author">por ${livro.autor}</p>
                    <p class="book-pages">📄 ${livro.totalPaginas} páginas</p>
                    <p class="book-synopsis">${livro.sinopse || 'Sem descrição cadastrada.'}</p>
                    <button class="btn btn-primary btn-block" onclick="abrirLeitor(${livro.id})">
                        📖 Abrir Livro
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// ==========================================
// 5. LEITOR COMPLETO COM CONTEÚDO & AVALIAÇÃO DE ESTRELAS
// ==========================================
// "os livros devem ter um botao para abrir ele e o admin pode por o conteudo do livro, independente de quantas paginas"
// "avaliar os livros com a interaçao com estrelas de meia estrela ate 5 estrelas"

async function abrirLeitor(livroId) {
    if (!currentUser) {
        abrirModalAuth('login');
        mostrarToast('⚠️ Faça login para ler, salvar marca-páginas e avaliar!');
        return;
    }

    const livro = todosLivros.find(l => l.id === livroId);
    if (!livro) return;

    currentBook = livro;
    isCurrentBookFavorited = userFavorites.some(f => f.livroId === livro.id);
    atualizarBotaoFavoritoLeitor();

    document.getElementById('reader-book-title').textContent = livro.titulo;
    document.getElementById('reader-book-author').textContent = `por ${livro.autor}`;
    document.getElementById('reader-book-category').textContent = livro.categoria;
    document.getElementById('reader-total-pages').textContent = livro.totalPaginas;
    document.getElementById('reader-book-synopsis').textContent = livro.sinopse || 'Sem sinopse disponível.';
    document.getElementById('reader-book-cover').src = livro.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400';

    // RENDERIZAR O CONTEÚDO DO LIVRO INSERIDO PELO ADMIN
    const textContainer = document.getElementById('reader-text-container');
    if (livro.conteudo && livro.conteudo.trim() !== '') {
        textContainer.textContent = livro.conteudo;
    } else {
        textContainer.innerHTML = `<em>Esta obra ainda não possui o conteúdo completo cadastrado pelo Administrador.</em><br><br>Sinopse: ${livro.sinopse}`;
    }

    // Carregar Marca-páginas do SQLite
    try {
        const res = await fetch(`${API_BASE}/api/leitura/status?usuarioId=${currentUser.id}&livroId=${livro.id}`);
        if (res.ok) {
            currentBookmark = await res.json();
            const pageInput = document.getElementById('reader-current-page');
            pageInput.value = currentBookmark.paginaAtual || 1;
            pageInput.max = livro.totalPaginas;

            const tsSpan = document.getElementById('reader-bookmark-timestamp');
            if (currentBookmark.dataAtualizacao) {
                tsSpan.textContent = `Última marcação: ${currentBookmark.dataAtualizacao}`;
            } else {
                tsSpan.textContent = '';
            }

            atualizarBarraProgresso();
        }
    } catch (e) {
        console.error('Erro ao consultar marca-páginas:', e);
    }

    // Carregar Avaliação em Estrelas do Usuário
    carregarAvaliacaoDoLivro(livro.id);

    document.getElementById('modal-leitor').classList.remove('hidden');
}

function fecharModalLeitor() {
    document.getElementById('modal-leitor').classList.add('hidden');
    currentBook = null;
    currentBookmark = null;
}

function ajustarTamanhoFonte(delta) {
    currentFontSize += delta * 0.1;
    if (currentFontSize < 0.8) currentFontSize = 0.8;
    if (currentFontSize > 1.6) currentFontSize = 1.6;
    document.getElementById('reader-text-container').style.fontSize = `${currentFontSize}rem`;
}

// ------------------------------------------------------------------
// WIDGET DE AVALIAÇÃO DE MEIA ESTRELA ATÉ 5 ESTRELAS
// ------------------------------------------------------------------
// "os usuarios tbm pode ter a opçao de avaliar os livros com a interaçao com estrelas de meia estrela ate 5 estrelas"

async function carregarAvaliacaoDoLivro(livroId) {
    try {
        const res = await fetch(`${API_BASE}/api/avaliacoes/livro?livroId=${livroId}&usuarioId=${currentUser ? currentUser.id : 0}`);
        if (res.ok) {
            const data = await res.json();
            currentUserRating = data.notaUsuario > 0 ? data.notaUsuario : (data.media > 0 ? data.media : 5.0);
            renderizarWidgetEstrelas(currentUserRating);
        }
    } catch (e) {
        renderizarWidgetEstrelas(5.0);
    }
}

function renderizarWidgetEstrelas(score) {
    const container = document.getElementById('star-rating-widget');
    const feedback = document.getElementById('star-rating-feedback');
    if (!container) return;

    feedback.textContent = `Nota: ${score.toFixed(1)} ⭐`;

    // 5 Estrelas, cada uma permitindo metade esquerda (0.5) ou metade direita (1.0)
    let html = '';
    for (let i = 1; i <= 5; i++) {
        let starClass = '';
        if (score >= i) {
            starClass = 'active'; // estrela cheia
        } else if (score >= i - 0.5) {
            starClass = 'half-active'; // meia estrela
        }

        html += `
            <span class="star-item ${starClass}"
                  data-star="${i}"
                  onclick="clicarEstrela(${i}, event)"
                  onmousemove="moverSobreEstrela(${i}, event)"
                  onmouseleave="restaurarEstrela()">
                ★
                <span class="half-star">★</span>
            </span>
        `;
    }
    container.innerHTML = html;
}

function moverSobreEstrela(starIndex, event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const isHalf = x < (rect.width / 2);
    const hoverScore = isHalf ? (starIndex - 0.5) : starIndex;

    const feedback = document.getElementById('star-rating-feedback');
    feedback.textContent = `Avaliar: ${hoverScore.toFixed(1)} ⭐`;

    const stars = document.querySelectorAll('.star-item');
    stars.forEach((s, idx) => {
        const i = idx + 1;
        s.classList.remove('active', 'half-active');
        if (hoverScore >= i) {
            s.classList.add('active');
        } else if (hoverScore >= i - 0.5) {
            s.classList.add('half-active');
        }
    });
}

function restaurarEstrela() {
    renderizarWidgetEstrelas(currentUserRating);
}

async function clicarEstrela(starIndex, event) {
    if (!currentUser || !currentBook) return;

    const rect = event.currentTarget.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const isHalf = x < (rect.width / 2);
    const finalScore = isHalf ? (starIndex - 0.5) : starIndex;

    currentUserRating = finalScore;
    renderizarWidgetEstrelas(finalScore);

    try {
        const res = await fetch(`${API_BASE}/api/avaliacoes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usuarioId: currentUser.id,
                livroId: currentBook.id,
                nota: finalScore
            })
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast(`⭐ Você avaliou este livro com nota ${finalScore.toFixed(1)}! Média atual: ${data.mediaAtualizada}`);
            carregarLivros(); // atualiza os badges do catálogo
        }
    } catch (e) {
        mostrarToast('❌ Erro ao salvar avaliação.');
    }
}

// ------------------------------------------------------------------
// MARCA-PÁGINAS E FINALIZAR LIVRO
// ------------------------------------------------------------------

function alterarPaginaAtual(delta) {
    if (!currentBook) return;
    const input = document.getElementById('reader-current-page');
    let val = parseInt(input.value) || 1;
    val += delta;
    if (val < 1) val = 1;
    if (val > currentBook.totalPaginas) val = currentBook.totalPaginas;
    input.value = val;
    atualizarBarraProgresso();
}

function atualizarBarraProgresso() {
    if (!currentBook) return;
    const input = document.getElementById('reader-current-page');
    const val = parseInt(input.value) || 1;
    const total = currentBook.totalPaginas || 1;
    const pct = Math.min(100, Math.round((val / total) * 100));

    document.getElementById('reader-progress-bar').style.width = `${pct}%`;
    document.getElementById('reader-progress-pct').textContent = `${pct}% lido (${val} de ${total})`;
}

async function salvarMarcaPaginas() {
    if (!currentUser || !currentBook) return;

    const input = document.getElementById('reader-current-page');
    const paginaAtual = parseInt(input.value) || 1;

    try {
        const res = await fetch(`${API_BASE}/api/leitura/marcapagina`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usuarioId: currentUser.id,
                livroId: currentBook.id,
                paginaAtual: paginaAtual
            })
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast(`🔖 Marca-páginas salvo na página ${paginaAtual}!`);
            if (data.marcaPagina && data.marcaPagina.dataAtualizacao) {
                document.getElementById('reader-bookmark-timestamp').textContent = `Última marcação: ${data.marcaPagina.dataAtualizacao}`;
            }
        } else {
            mostrarToast(`❌ ${data.erro || 'Falha ao salvar marca-páginas'}`);
        }
    } catch (e) {
        mostrarToast('❌ Falha na conexão com a API.');
    }
}

async function confirmarFinalizacaoLivro() {
    if (!currentUser || !currentBook) return;

    const btn = document.getElementById('btn-finalizar-livro');
    btn.disabled = true;
    btn.textContent = 'Processando no SQLite...';

    try {
        const res = await fetch(`${API_BASE}/api/leitura/finalizar`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usuarioId: currentUser.id,
                livroId: currentBook.id
            })
        });

        const data = await res.json();
        if (res.ok && data.sucesso) {
            currentUser.livrosLidos = data.livrosLidos;
            localStorage.setItem('libraryclound_user', JSON.stringify(currentUser));
            document.getElementById('profile-books-read').textContent = data.livrosLidos;

            mostrarToast(`🏆 Parabéns! Livro concluído! Total de Livros Lidos: ${data.livrosLidos}`);

            document.getElementById('reader-current-page').value = currentBook.totalPaginas;
            atualizarBarraProgresso();

            const counterCard = document.querySelector('.read-counter-card');
            counterCard.style.transform = 'scale(1.2)';
            setTimeout(() => {
                counterCard.style.transform = 'scale(1)';
            }, 600);

            setTimeout(() => {
                fecharModalLeitor();
            }, 1200);

        } else {
            mostrarToast(`❌ ${data.erro || 'Não foi possível concluir o livro.'}`);
        }
    } catch (e) {
        mostrarToast('❌ Erro ao comunicar com a API.');
    } finally {
        btn.disabled = false;
        btn.textContent = '🏆 Confirmar que Finalizou o Livro';
    }
}

// ==========================================
// 6. PAINEL DO ADMINISTRADOR / DONOS DO SITE
// ==========================================
// "o admin pode por o conteudo do livro, independente de quantas paginas"

function alternarPainelAdmin() {
    const secaoCatalogo = document.getElementById('secao-catalogo');
    const secaoAdmin = document.getElementById('secao-admin');

    if (secaoAdmin.classList.contains('hidden')) {
        secaoAdmin.classList.remove('hidden');
        secaoCatalogo.classList.add('hidden');
        carregarTabelaAdminLivros();
    } else {
        secaoAdmin.classList.add('hidden');
        secaoCatalogo.classList.remove('hidden');
        carregarLivros();
    }
}

function trocarAbaAdmin(aba) {
    const abasBtns = document.querySelectorAll('.admin-header ~ .tabs-container .tab-btn');
    abasBtns.forEach(b => b.classList.remove('active'));

    document.getElementById('admin-tab-livros').classList.add('hidden');
    document.getElementById('admin-tab-usuarios').classList.add('hidden');

    if (aba === 'livros') {
        abasBtns[0].classList.add('active');
        document.getElementById('admin-tab-livros').classList.remove('hidden');
        carregarTabelaAdminLivros();
    } else {
        abasBtns[1].classList.add('active');
        document.getElementById('admin-tab-usuarios').classList.remove('hidden');
        carregarTabelaAdminUsuarios();
    }
}

async function carregarTabelaAdminLivros() {
    try {
        const res = await fetch(`${API_BASE}/api/livros`);
        if (res.ok) {
            const livros = await res.json();
            const tbody = document.getElementById('admin-livros-tbody');
            tbody.innerHTML = livros.map(l => {
                const temConteudo = l.conteudo && l.conteudo.trim().length > 0;
                return `
                    <tr>
                        <td><img src="${l.capaUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'}" class="table-cover-thumb" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400'"></td>
                        <td><strong>${l.titulo}</strong></td>
                        <td>${l.autor}</td>
                        <td><span class="badge">${l.categoria}</span></td>
                        <td>${l.totalPaginas}</td>
                        <td>⭐ ${l.mediaAvaliacao.toFixed(1)}</td>
                        <td>${temConteudo ? '<span class="badge btn-success">Texto Completo</span>' : '<span class="badge">Apenas Sinopse</span>'}</td>
                        <td class="actions-cell">
                            <button class="btn btn-sm btn-outline" onclick="editarLivroAdmin(${l.id})">✏️ Editar</button>
                            <button class="btn btn-sm btn-danger" onclick="excluirLivroAdmin(${l.id})">🗑️ Excluir</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('Erro na tabela admin:', e);
    }
}

async function carregarTabelaAdminUsuarios() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/usuarios`);
        if (res.ok) {
            const usuarios = await res.json();
            const tbody = document.getElementById('admin-usuarios-tbody');
            tbody.innerHTML = usuarios.map(u => `
                <tr>
                    <td>
                        ${u.fotoUrl ? `<img src="${u.fotoUrl}" class="table-cover-thumb" style="border-radius:50%;" onerror="this.src='https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'">` : `<div class="avatar-circle" style="width:36px;height:36px;font-size:0.85rem;">${(u.nomeFantasia||'U').charAt(0)}</div>`}
                    </td>
                    <td><strong>${u.nomeFantasia}</strong></td>
                    <td>${u.email}</td>
                    <td style="font-size:0.85rem; max-width:200px; color:#475569;">${u.descricao || '<em>Sem descrição</em>'}</td>
                    <td><span class="badge ${u.tipo === 'ADMIN' ? 'btn-admin' : ''}">${u.tipo}</span></td>
                    <td><strong>${u.livrosLidos}</strong> livros concluídos</td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error('Erro ao listar usuários admin:', e);
    }
}

function abrirModalLivro(modo) {
    const modal = document.getElementById('modal-livro');
    const form = document.getElementById('form-livro');
    const titulo = document.getElementById('modal-livro-titulo');

    form.reset();
    document.getElementById('livro-id').value = '';

    if (modo === 'criar') {
        titulo.textContent = 'Adicionar Novo Livro à LibraryClound';
    }
    modal.classList.remove('hidden');
}

function fecharModalLivro() {
    document.getElementById('modal-livro').classList.add('hidden');
}

function editarLivroAdmin(id) {
    const livro = todosLivros.find(l => l.id === id);
    if (!livro) return;

    document.getElementById('modal-livro-titulo').textContent = 'Editar Livro & Conteúdo';
    document.getElementById('livro-id').value = livro.id;
    document.getElementById('livro-titulo-input').value = livro.titulo;
    document.getElementById('livro-autor-input').value = livro.autor;
    document.getElementById('livro-categoria-input').value = livro.categoria;
    document.getElementById('livro-paginas-input').value = livro.totalPaginas;
    document.getElementById('livro-capa-input').value = livro.capaUrl || '';
    document.getElementById('livro-sinopse-input').value = livro.sinopse || '';
    document.getElementById('livro-conteudo-input').value = livro.conteudo || '';

    document.getElementById('modal-livro').classList.remove('hidden');
}

async function salvarLivroAdmin(event) {
    event.preventDefault();
    const id = document.getElementById('livro-id').value;
    const titulo = document.getElementById('livro-titulo-input').value.trim();
    const autor = document.getElementById('livro-autor-input').value.trim();
    const categoria = document.getElementById('livro-categoria-input').value.trim();
    const totalPaginas = parseInt(document.getElementById('livro-paginas-input').value) || 100;
    const capaUrl = document.getElementById('livro-capa-input').value.trim();
    const sinopse = document.getElementById('livro-sinopse-input').value.trim();
    const conteudo = document.getElementById('livro-conteudo-input').value; // conteúdo completo sem limite

    const dados = { titulo, autor, categoria, totalPaginas, capaUrl, sinopse, conteudo };

    const isEdicao = !!id;
    const url = isEdicao ? `${API_BASE}/api/admin/livros/${id}` : `${API_BASE}/api/admin/livros`;
    const method = isEdicao ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dados)
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast(isEdicao ? '✅ Livro e conteúdo atualizados!' : '✅ Novo livro cadastrado na LibraryClound!');
            fecharModalLivro();
            carregarTabelaAdminLivros();
            carregarCategorias();
            carregarLivros();
        } else {
            mostrarToast(`❌ ${data.erro || 'Falha ao salvar livro.'}`);
        }
    } catch (e) {
        mostrarToast('❌ Erro de comunicação com o servidor.');
    }
}

async function excluirLivroAdmin(id) {
    if (!confirm('Deseja realmente excluir este livro do catálogo da LibraryClound?')) return;

    try {
        const res = await fetch(`${API_BASE}/api/admin/livros/${id}`, {
            method: 'DELETE'
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast('🗑️ Livro excluído com sucesso!');
            carregarTabelaAdminLivros();
            carregarCategorias();
            carregarLivros();
        } else {
            mostrarToast(`❌ ${data.erro || 'Erro ao excluir livro'}`);
        }
    } catch (e) {
        mostrarToast('❌ Erro ao contatar a API.');
    }
}

// ==========================================
// 7. MODAL DE AUTENTICAÇÃO E TOAST
// ==========================================

function abrirModalAuth(view = 'login') {
    document.getElementById('modal-auth').classList.remove('hidden');
    alternarAbaAuth(view);
}

function fecharModalAuth() {
    document.getElementById('modal-auth').classList.add('hidden');
}

function alternarAbaAuth(view) {
    const loginView = document.getElementById('auth-login-view');
    const regView = document.getElementById('auth-register-view');

    if (view === 'login') {
        loginView.classList.remove('hidden');
        regView.classList.add('hidden');
    } else {
        loginView.classList.add('hidden');
        regView.classList.remove('hidden');
    }
}

function mostrarToast(mensagem) {
    const toast = document.getElementById('toast');
    toast.textContent = mensagem;
    toast.classList.remove('hidden');
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 4500);
}
