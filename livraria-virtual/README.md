# ☁️ 📚 LibraryClound - Livraria Virtual & Leitura (Back-end)

Projeto desenvolvido para a entrega da última semana do curso de Back-end.

---

## 🎨 Identidade Visual da LibraryClound
- **Nome Oficial**: **LibraryClound**
- **Logo Oficial**: **Livro com uma Nuvem Vermelha na Capa** (representado em SVG vetorial de alta definição no cabeçalho da aplicação).

---

## 🎯 Requisitos Implementados

1. **Cadastro e Login de Usuários**:
   - `Nome Fantasia`, `Email` e `Senha`.
   - **Regra obrigatória**: Senha exclusivamente numérica com no máximo 8 dígitos (ex: `12345678`).
   - Diferenciação entre perfil `CLIENTE` e `ADMIN`.
2. **Aba para Editar o Próprio Perfil**:
   - Permite editar o `Nome Fantasia`.
   - Permite adicionar ou alterar a `Foto de Perfil` (URL ou seleção de presets visuais rápidos).
   - Permite criar ou alterar uma `Descrição sobre si` com **limite estrito de até 150 caracteres** (com contador em tempo real).
3. **Aba de Favoritos & Função de Favoritar**:
   - Botão de favoritar em cada livro (coração ❤️).
   - Aba dedicada dentro do modal de perfil que lista os livros favoritos exibindo:
     - **Nome do Livro**
     - **Categoria**
     - **Capa do Livro**
     - Botão para abrir o livro diretamente.
4. **Leitor de Livros & Conteúdo Livre pelo Administrador**:
   - Botão **"📖 Abrir Livro"** em cada obra.
   - O Administrador pode inserir e editar o **conteúdo completo do livro** (capítulos, textos longos, independente de quantas páginas).
   - Controle de zoom de leitura (A- / A+), exibição do texto e leitor imersivo.
5. **Avaliação dos Livros com Estrelas (Meia Estrela até 5 Estrelas)**:
   - Widget interativo que permite votar de **0.5 até 5.0 estrelas** (0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0).
   - Suporte visual a meia estrela com feedback em tempo real.
   - Cálculo automático da média de avaliação e quantidade de votos no catálogo.
6. **Função Marca-Páginas**:
   - Botão para salvar a página onde o leitor parou, persistido no SQLite por usuário e livro.
7. **Botão no Fim do Livro (Finalizar Leitura)**:
   - Botão: *"Confirmar que Finalizou o Livro"*.
   - A contagem de livros lidos no perfil **só atualiza quando este botão for clicado**!
8. **Painel do Administrador / Donos**:
   - Adicionar novos livros com título, autor, categoria, páginas, capa, sinopse e **conteúdo completo**.
   - Modificar/Editar livros existentes.
   - Excluir livros do acervo.
   - Listar usuários cadastrados, fotos, biografias e livros lidos.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Java (Standard Edition 17+)
- **Banco de Dados**: SQLite via JDBC (`org.xerial:sqlite-jdbc`)
- **API REST**: `com.sun.net.httpserver.HttpServer` com respostas e requisições em formato JSON
- **Frontend Integrado**: HTML5, CSS3 moderno e JavaScript ES6 (Fetch API)

---

## 🚀 Como Executar o Projeto

### Opção 1: Clique Duplo (Windows)
Basta dar dois cliques no arquivo:
```cmd
run.bat
```
ou no PowerShell:
```powershell
.\run.ps1
```

### Opção 2: Pelo IntelliJ IDEA ou Eclipse (Maven)
1. Abra o IntelliJ IDEA.
2. Clique em **Open** e selecione a pasta `livraria-virtual` (onde está o `pom.xml`).
3. Abra a classe `src/main/java/com/livraria/Main.java` e clique no botão verde **Run** (Executar).

### Acesso no Navegador:
Após iniciar, abra:
👉 **`http://localhost:8080`**

---

## 🔑 Credenciais para Demonstração

| Perfil | Email | Senha | O que pode fazer |
|---|---|---|---|
| 👑 **Administrador (Dono)** | `admin@livraria.com` | `12345678` | Cadastrar livros com conteúdo completo, editar, excluir e auditar usuários |
| 📖 **Leitor (Cliente)** | `leitor@livraria.com` | `1234` | Editar perfil (foto/bio 150 car.), favoritar, avaliar com estrelas (0.5 a 5.0), ler e marcar páginas |
