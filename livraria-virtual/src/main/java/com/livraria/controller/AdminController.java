package com.livraria.controller;

import com.livraria.dao.LivroDAO;
import com.livraria.dao.UsuarioDAO;
import com.livraria.model.Livro;
import com.livraria.model.Usuario;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller com privilégios de Administrador/Donos do site LibraryClound:
 * Permite adicionar, modificar e excluir livros livremente,
 * incluindo o conteúdo completo do livro (independente de quantas páginas),
 * além de auditar todos os usuários cadastrados.
 */
public class AdminController implements HttpHandler {

    private final LivroDAO livroDAO = new LivroDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.startsWith("/api/admin/livros")) {
                handleLivrosCrud(exchange, method, path);
            } else if (path.startsWith("/api/admin/usuarios") && "GET".equalsIgnoreCase(method)) {
                handleListarUsuarios(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint administrativo não encontrado\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição administrativa inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleLivrosCrud(HttpExchange exchange, String method, String path) throws IOException, SQLException {
        if ("POST".equalsIgnoreCase(method)) {
            // Adicionar novo livro
            String body = HttpHelper.readRequestBody(exchange);
            Map<String, String> dados = JsonHelper.parseJson(body);

            String titulo = JsonHelper.getString(dados, "titulo", "");
            String autor = JsonHelper.getString(dados, "autor", "");
            String categoria = JsonHelper.getString(dados, "categoria", "Geral");
            int paginas = JsonHelper.getInt(dados, "totalPaginas", 100);
            String sinopse = JsonHelper.getString(dados, "sinopse", "");
            String capa = JsonHelper.getString(dados, "capaUrl", "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400");
            // Conteúdo completo do livro (independente da quantidade de páginas)
            String conteudo = JsonHelper.getString(dados, "conteudo", "");

            if (titulo.isBlank() || autor.isBlank()) {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Título e Autor são campos obrigatórios.\"}");
                return;
            }

            Livro novo = new Livro(titulo, autor, categoria, paginas, sinopse, capa, conteudo);
            Livro salvo = livroDAO.inserir(novo);

            String resposta = "{"
                    + "\"mensagem\":\"Livro adicionado com sucesso pelo administrador!\","
                    + "\"livro\":" + salvo.toJson()
                    + "}";
            HttpHelper.sendJsonResponse(exchange, 201, resposta);

        } else if ("PUT".equalsIgnoreCase(method)) {
            // Modificar livro existente
            int id = extrairIdDaUrl(path);
            if (id <= 0) {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"ID do livro não informado na URL.\"}");
                return;
            }

            Livro existente = livroDAO.buscarPorId(id);
            if (existente == null) {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Livro não encontrado para modificação.\"}");
                return;
            }

            String body = HttpHelper.readRequestBody(exchange);
            Map<String, String> dados = JsonHelper.parseJson(body);

            existente.setTitulo(JsonHelper.getString(dados, "titulo", existente.getTitulo()));
            existente.setAutor(JsonHelper.getString(dados, "autor", existente.getAutor()));
            existente.setCategoria(JsonHelper.getString(dados, "categoria", existente.getCategoria()));
            existente.setTotalPaginas(JsonHelper.getInt(dados, "totalPaginas", existente.getTotalPaginas()));
            existente.setSinopse(JsonHelper.getString(dados, "sinopse", existente.getSinopse()));
            existente.setCapaUrl(JsonHelper.getString(dados, "capaUrl", existente.getCapaUrl()));
            if (dados.containsKey("conteudo")) {
                existente.setConteudo(JsonHelper.getString(dados, "conteudo", existente.getConteudo()));
            }

            boolean sucesso = livroDAO.atualizar(existente);
            if (sucesso) {
                String resposta = "{"
                        + "\"mensagem\":\"Livro atualizado com sucesso!\","
                        + "\"livro\":" + existente.toJson()
                        + "}";
                HttpHelper.sendJsonResponse(exchange, 200, resposta);
            } else {
                HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Não foi possível atualizar o livro.\"}");
            }

        } else if ("DELETE".equalsIgnoreCase(method)) {
            // Excluir livro
            int id = extrairIdDaUrl(path);
            if (id <= 0) {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"ID do livro não informado na URL.\"}");
                return;
            }

            boolean excluido = livroDAO.excluir(id);
            if (excluido) {
                HttpHelper.sendJsonResponse(exchange, 200, "{\"mensagem\":\"Livro excluído com sucesso do catálogo!\"}");
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Livro não encontrado para exclusão.\"}");
            }
        } else {
            HttpHelper.sendJsonResponse(exchange, 405, "{\"erro\":\"Método não suportado para /api/admin/livros\"}");
        }
    }

    private void handleListarUsuarios(HttpExchange exchange) throws IOException, SQLException {
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < usuarios.size(); i++) {
            sb.append(usuarios.get(i).toJson());
            if (i < usuarios.size() - 1) sb.append(",");
        }
        sb.append("]");

        HttpHelper.sendJsonResponse(exchange, 200, sb.toString());
    }

    private int extrairIdDaUrl(String path) {
        try {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return Integer.parseInt(path.substring(lastSlash + 1));
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }
}
